# Easy-Pay 待办事项清单

> 更新时间: 2025-11-25

## 已完成功能

### P0 - 核心支付功能 ✅

- [x] 手动补单 `POST /api/admin/orders/{id}/settle`
- [x] 重新通知 `POST /api/admin/orders/{id}/renotify`
- [x] 批量清理超时订单 `DELETE /api/admin/orders/expired`

### P1 - 管理功能增强 ✅

- [x] 统计分析功能（仪表盘）
  - `GET /api/console/statistics/revenue` - 收入统计
  - `GET /api/console/statistics/payment-types` - 支付类型统计
  - `GET /api/console/statistics/trend` - 订单趋势
- [x] 订单作用域查询
  - `GET /api/console/orders/active` - 活跃订单
  - `GET /api/console/orders/success` - 成交订单
  - `GET /api/console/orders/expired` - 超时订单
- [x] 账号交易流水查询 `GET /api/accounts/{id}/transactions`
- [x] 二维码图片上传 `POST /api/channels/{id}/qrcode/upload`

### P2 - 高级功能 ✅

- [x] 插件市场远程同步 `POST /api/plugins/sync`
- [x] 用户角色系统
  - `GET /api/users` - 用户列表
  - `PUT /api/users/{pid}/role` - 更新角色
  - `PUT /api/users/{pid}/state` - 更新状态
- [x] 监听模式区分处理
  - `GET /api/listen/status` - 监听状态
  - `PUT /api/listen/accounts/{id}/pattern` - 更新监听模式

---

## 待完成功能

### P3 - 可选功能（按需实现）

#### 1. 测试支付页面 ✅
**优先级**: 低
**说明**: 开发环境的测试支付页面，用于模拟提交订单

**已实现**:
- [x] 创建测试页面 `/test-pay`
- [x] 自动生成签名 (MD5)
- [x] 模拟订单提交
- [x] 支付结果展示
- [x] 集成到 Console 后台菜单

**参考**: mpay 中的 `/Order/testPay`

---

#### 2. 远程消息推送
**优先级**: 低
**说明**: 从远程 API 获取系统通知、插件更新提醒、公告消息

**待实现**:
- [ ] 创建 `NotificationService` 服务
- [ ] 定时拉取远程通知 API
- [ ] 通知消息存储和展示
- [ ] 前端通知��件

**API 设计**:
```
GET /api/notifications          # 获取通知列表
PUT /api/notifications/{id}/read # 标记已读
DELETE /api/notifications/{id}   # 删除通知
```

---

#### 3. 软删除机制
**优先级**: 低
**说明**: 为主要实体添加软删除支持，数据可恢复

**待实现**:
- [ ] 添加 `deleted_at` 字段到主要表
- [ ] 创建 `@SoftDelete` 注解或使用 Hibernate Filter
- [ ] 修改 Repository 查询排除已删除数据
- [ ] 添加数据恢复接口

**涉及表**:
- `orders` - 订单表
- `pay_account` - 收款账号表
- `pay_channel` - 收款通道表
- `user` - 用户表

**数据库迁移**:
```sql
ALTER TABLE orders ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE pay_account ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE pay_channel ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE user ADD COLUMN deleted_at DATETIME NULL;
```

---

#### 4. Spring Security + JWT 认证 ✅
**优先级**: 中
**说明**: 添加完整的认证授权机制

**已实现**:
- [x] 添加 Spring Security 依赖
- [x] 实现 JWT Token 生成和验证
- [x] 登录/登出接口
- [x] 接口权限控制（基于角色）
- [x] Token 刷新机制

**API 设计**:
```
POST /api/auth/login     # 登录
POST /api/auth/logout    # 登出
POST /api/auth/refresh   # 刷新 Token
GET  /api/auth/me        # 获取当前用户
```

**默认管理员账号**: admin / admin123

---

#### 5. API 限流和防重放
**优先级**: 中
**说明**: 保护 API 安全，防止恶意调用

**待实现**:
- [ ] 基于 Redis 的接口限流
- [ ] 请求签名时间戳验证
- [ ] Nonce 防重放攻击
- [ ] IP 黑名单机制

---

#### 6. 前端功能完善 ✅
**优先级**: 高
**说明**: 完善 Console 和 Cashier 前端应用

**Console 后台管理** ✅:
- [x] 仪表盘数据展示（对接统计 API）
- [x] 订单列表页面完善（分页/筛选/导出）
- [x] 账号管理页面
- [x] 插件管理页面
- [x] 用户管理页面（管理员功能）

**技术栈升级**:
- [x] 引入 Element Plus UI 组件库
- [x] 创建 Pinia Store (用户认证状态管理)
- [x] 完善 HTTP 客户端 (Token 拦截器)
- [x] 路由守卫和权限控制

**Cashier 收银台** ✅:
- [x] 订单详情页
- [x] 二维码展示组件
- [x] 支付状态轮询
- [x] 支付成功/失败页面

**后端 API**:
- `GET /api/cashier/orders/{orderId}` - 获取订单详情
- `GET /api/cashier/orders/{orderId}/state` - 获取订单状态（轮询）

---

#### 7. 单元测试和集成测试 ✅
**优先级**: 高
**说明**: 提高代码质量和可维护性

**已实现**:
- [x] Service 层单元测试
- [x] 测试基础设施配置 (H2 内存数据库)
- [x] 核心服务测试覆盖

**测试覆盖模块**:
- `AdminOrderService` - 补单逻辑 (11 测试用例)
- `OrderMatchService` - 订单匹配 (6 测试用例)
- `StatisticsService` - 统计计算 (10 测试用例)
- `PriceAllocator` - 金额碰撞检测 (5 测试用例)
- `CashierService` - 收银台服务 (9 测试用例)

**待扩展**:
- [ ] Controller 层集成测试
- [ ] Repository 层测试
- [ ] 测试覆盖率 > 70%

---

#### 8. 日志和监控增强
**优先级**: 中
**说明**: 完善运维和监控能力

**待实现**:
- [ ] 结构化日志（JSON 格式）
- [ ] Prometheus 监控指标
- [ ] 健康检查增强
- [ ] 慢查询日志

---

## 技术债务

### 需要关注的问题

1. **~~包名不一致~~**: ✅ 已统一为 `com.github.nonfou.mpay`
2. **~~PluginServiceImpl unchecked 警告~~**: ✅ 已添加 TypeReference 泛型类型
3. **~~Git 变更未提交~~**: ✅ 已提交 (commit: ea0638b)

### 代码优化建议

1. 统计查询添加 Redis 缓存
2. 批量操作使用分页处理
3. 文件上传考虑使用云存储（OSS/S3）
4. WebClient 添加超时和重试配置

---

## 参考资源

- [项目规划文档](../IMPLEMENTATION_PLAN.md)
- [API 规格文档](./openapi.yaml)
- [数据库设计](./er-diagram.puml)
- [工程规范](./engineering-guidelines.md)
