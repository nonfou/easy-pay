# 遗留系统领域流程梳理

本文总结 ThinkPHP 版本（`mpay/`）的核心业务流程，为 Stage 1 领域建模提供输入。内容依据源码（`app/controller/*.php`、`app/model/*.php` 等）整理。

## 1. 商户下单流程
1. **提交入口**
   - `Route::rule('submit', 'Pay/submit')`
   - `Route::rule('mapi', 'Pay/mapi')`
   - GET/POST/JSON 形式提交，核心参数：`pid,type,out_trade_no,notify_url,return_url,name,money,sign`。
2. **校验逻辑**（`PayController::submit/mapi`）：
   - 读取请求数据，依据 `pid` 查询 `User.secret_key` 并校验 MD5 签名。
   - 防重：检查 `Order.out_trade_no` 是否存在。
   - `Order::createOrder`：
     - 选通道 `Order::setChannel(pid,type)` → 查询 `PayAccount` 与 `PayChannel`（按 `last_time` 顺序匹配 `wxpay/alipay` 码）。
     - 金额去重 `Order::checkMoney`：若同通道在有效期已有相同金额，则 +0.01 直到唯一。
     - 写入订单，默认 `state=0`，`patt` 承袭账号监听模式。
   - 结果：同步重定向 `Pay/console/{order_id}` 或返回 JSON（mapi）。

## 2. 收银台 & 订单查询
1. **收银台页面**（`PayController::console`）
   - 根据 `order_id` 查询订单 & 关联渠道。
   - 计算剩余时间 `passtime = close_time - now`，渲染二维码/收款方式。
2. **轮询订单状态**（`Route::rule('getOrderState/[:order_id]', 'Pay/getOrderState')`）
   - `state=0`：返回倒计时；`state=1`：组装通知参数（`crateNotify` + `getSign`）并返回 `return_url`。
3. **订单详情/后台管理**（`app/controller/OrderController.php` + `view/order/*` + `api/OrderController.php`）
   - 列表查询支持多字段过滤、分页。
   - 操作项：改状态、手动补单（触发通知）、重新通知、删除/批量删除、清理过期。

## 3. 监听与通知
1. **订单监听文件**（`PayController::checkOrder`）
   - 商户调用 `/checkOrder/{pid}/{sign}`。
   - 校验 `md5(pid+secret_key)`，将有效订单写入 `runtime/order.json`（`code=1` 时包含订单列表）。
2. **客户端心跳**（外部监听程序）
   - 轮询 `order.json` 并在本地拉取支付平台流水。
   - 调用 `/checkPayResult?pid=..&aid=..`（`PayController::checkPayResult`）：
     - 读取 `order.json` → 过滤匹配 `pid/aid/patt`。
     - `PayAccount::getAccountConfig`：组合插件 query + 账号配置。
     - 通过 `extend/payclient/{Plugin}` 实例化客户端，获取收款记录 `getOrderInfo`。
     - 调用 `payHeart(records, config)` 进行匹配。
3. **Match & 通知**（`PayController::payHeart` + `updateOrderState`）
   - 排除已支付订单（`Order::scope('dealOrder')`）。
   - 按 `payway/channel/price` 匹配活跃订单。
   - 订单命中后：
     - 更新 `state=1`、`pay_time`、`platform_order`。
     - 构造通知数据（含扩展参数），调用 `getHttpResponse` 同步请求商户 `notify_url`，期望返回 `success`。
4. **终端推送**（`PayController::mpayNotify`）
   - 外部程序 POST `action=mpay` + 数据；服务端创建插件实例并调用 `notify()` 解析记录，再走 `payHeart`。

## 4. 插件管理
1. **插件配置**：`config/extend/payplugin.php` 存储所有插件元信息（名称、class、price、state、query 默认值）。
2. **控制器**：`app/controller/api/PluginController.php`
   - `getPluginList`：本地 + 远程（`Plugin::getAllPlugins`）合并。
   - `pluginEnable`：更新配置文件。
   - `uninstallPlugin`：删除配置并 unlink `extend/payclient/*.php`。
   - `pluginOption`：供前端填充下拉。
3. **运行时加载**：
   - `PayAccount::getAccountConfig` 根据 `platform` 找到插件 `class_name`，并 merge `query` 参数。
   - 插件类实现 `getOrderInfo`、`notify` 等方法。

## 5. 用户/权限
- 中间件 `app/middleware/Auth.php` 判断 `session('?islogin')`，未登录跳转 `/User/login`。
- `api/UserController` 处理登录、登出、信息更新、密码修改、密钥重置。
- 会话存储字段：`userid`、`pid`、`nickname`、`userrole`、`islogin`。

## 6. 已识别痛点（迁移关注）
| 分类 | 现状 | 新系统要求 |
|------|------|------------|
| 订单监听 | 依赖 `runtime/order.json` + 轮询 | 改为 MQ/Redis Stream，支持分布式与高并发 |
| 插件配置 | PHP 文件维护，手工操作 | 入库 + 后台 CRUD + 版本管理 |
| 通知机制 | 同步 curl，无重试/日志 | 异步队列 + 重试策略 + 状态表 |
| 安全 | Session + MD5 签名，无 JWT/2FA | Spring Security + JWT + RBAC + 更强签名 |
| 前端 | Layui 模板嵌套 PHP | Vue 3 SPA，接口与 UI 分离 |

## 7. 输出与下一步
- 本文即 Stage 1 第一步“领域梳理”成果，后续将根据这些流程定义 OpenAPI 规格。
- 建议同步在 `docs/` 添加流程图（如 PlantUML 活动图/Sequence），以便开发 & 测试对齐。
