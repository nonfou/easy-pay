# 阶段 0 · 数据库梳理与迁移计划

本说明面向 mpay-spring 阶段 0 的第 2 项任务：导入/梳理现有数据库结构，输出 ER 关系与迁移策略，为后续 Spring Boot 模块提供统一的数据底座。

## 1. 现有数据源概览
- 主要实体：`User`、`Order`、`PayAccount`、`PayChannel`，以及运行时依赖的插件配置文件 `config/extend/payplugin.php`。
- ThinkPHP 默认约定：模型名小写 + 下划线，即 `User` -> `user`、`PayAccount` -> `pay_account`。若后续从数据库导出结构可再校正命名。
- 文件 `runtime/order.json` 被用作订单监听数据交换，不属于持久化层，但迁移时需改为 Redis/MQ。

## 2. 表字段推断（基于 PHP 代码）
> 实际字段以数据库导出为准，以下为通过模型逻辑梳理的最小集合，便于提前规划 Java 实体与迁移脚本。

### 2.1 `user`（商户/后台用户）
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 自增主键 |
| `pid` | int | 商户编号，业务唯一键 |
| `username` | varchar | 登录账号 |
| `password` | varchar | Bcrypt/PasswordHash 结果 |
| `nickname` | varchar | 昵称，用于界面显示 |
| `secret_key` | varchar | API 签名 key (`submit/mapi`) |
| `state` | tinyint | 启用状态（`User::where('state',1)`） |
| `role` | tinyint | 角色/权限（`session('userrole')`） |
| `delete_time` | timestamp | 软删字段（`withTrashed()` 提示存在） |

### 2.2 `order`
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `order_id` | varchar | 平台订单号 (`H+日期+随机`) |
| `pid` | int | 商户编号 |
| `type` | varchar | 支付方式（`wxpay`/`alipay`/...） |
| `out_trade_no` | varchar | 商户订单号 |
| `notify_url` | varchar | 异步回调地址 |
| `return_url` | varchar | 成功跳转地址 |
| `name` | varchar | 商品名称 |
| `money` | decimal | 下单金额 |
| `really_price` | decimal | 实际匹配金额（避免重复金额） |
| `clientip` | varchar | 客户端 IP |
| `device` | varchar | 设备类型 |
| `param` | text | 扩展参数（序列化 array） |
| `state` | tinyint | 0=待/过期,1=成功 |
| `patt` | tinyint | 监听模式（继承自账号 pattern） |
| `create_time` | datetime | 下单时间 |
| `close_time` | datetime | 订单过期时间 |
| `pay_time` | datetime | 支付时间（默认=创建时间） |
| `aid` | bigint | 收款账号 ID |
| `cid` | bigint | 收款通道/二维码 ID |
| `platform_order` | varchar | 第三方流水号（`updateOrderState` 填充） |

### 2.3 `pay_account`
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `pid` | int | 所属商户 |
| `platform` | varchar | 插件平台标识（`wxpay`/`alipay`/`sqbpay` 等） |
| `account` | varchar | 登录账号/识别号 |
| `password` | varchar | 密码/密钥（部分需加密存储） |
| `state` | tinyint | 启用状态 |
| `pattern` | tinyint | 监听模式（0=单次,1=连续） |
| `params` | json | 自定义参数（与插件 query merge） |
| `created_at/updated_at` | datetime | 时间戳 |

### 2.4 `pay_channel`
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `account_id` | bigint | 关联 `pay_account.id` |
| `channel` | varchar | 终端编号（如 `wxpay1#账号`） |
| `qrcode` | varchar | 二维码 URL/路径 |
| `last_time` | datetime | 上次使用时间（用于轮询均衡） |
| `state` | tinyint | 启用状态 |
| `type` | tinyint/varchar | URL 类型/码类型（前端展示用） |

### 2.5 插件配置
- 目前保存在 `config/extend/payplugin.php`，字段：`platform`、`name`、`class_name`、`price`、`describe`、`website`、`state`、`query`。
- 迁移时计划拆分为：`plugin_definition`（市场信息）+ `plugin_settings`（本地启停状态、扩展参数）。

## 3. 关系/ER 描述
```
user (1) ──< pay_account (1) ──< pay_channel
   │                     │
   └──< order >── pay_channel (many-to-one)
```
- `order.pid` -> `user.pid` (逻辑外键)
- `order.aid` -> `pay_account.id`
- `order.cid` -> `pay_channel.id`
- `pay_channel.account_id` -> `pay_account.id`

> 建议在 MySQL 8 中显式创建外键（历史库可能未设置），并为高频查询字段 (`order_id`, `out_trade_no`, `state`, `create_time`) 添加索引。

## 4. 迁移策略
1. **导出结构**：从现有环境导出 MySQL DDL（或通过 ThinkPHP migration/安装脚本获取）。若无直接脚本，可连接 DB 使用 `mysqldump --no-data`。
2. **梳理差异**：对照上表校验字段、类型、默认值，确认是否存在额外业务表（如日志、公告等）。
3. **命名规范**：在新库中统一使用 `snake_case` 表名与 `gmt_create/gmt_modified` 等时间字段，并在 Java 实体层提供别名映射，必要时通过视图或兼容列名简化迁移。
4. **数据迁移**：
   - 初期全量导出/导入到新库，验证行数与校验字段。
   - 上线前执行增量同步（可选：双写或基于更新时间的增量脚本）。
5. **Flyway 管理**：将整理后的 DDL 写入 `backend/mpay-common/src/main/resources/db/migration/V1__init.sql` 等脚本，确保 CI 可自动建库。
6. **插件配置转换**：编写脚本读取 `config/extend/payplugin.php`，生成 `plugin_definition`/`plugin_settings` 插入语句，随迁移执行。

## 5. 待办
- [ ] 连接旧库获取真实 DDL，填充缺失字段（如索引、唯一约束、软删字段）。
- [ ] 在 `docs/` 增补 ER 图（可用 draw.io/PlantUML），并提交 PNG/SVG。
- [ ] 确认是否存在其他业务表（通知日志、系统消息等），补充到此文档。

完成以上任务后，即可开始设计 Spring/JPA 实体与仓储层，实现阶段 0 的数据准备目标。
