# mpay-payment 模块设计要点

## 1. 目标
- 负责公共下单接口（对应 legacy `submit`/`mapi`）与收银台数据查询。
- 封装订单聚合逻辑：通道选择、金额去重、通知触发。
- 后续扩展监听事件、MQ 交互、Webhook 分发等职责。

## 2. 关键流程
1. `createOrder`
   - 校验商户签名（调用 `mpay-auth` 或本地缓存密钥）。
   - 按支付方式选择可用通道：
     - 查询当前商户启用的 `PayAccount` + `PayChannel`。
     - 结合监听模式/优先规则（参考 legacy `Order::setChannel`）。
   - 金额去重策略：对同一通道处于有效期的订单金额进行 +0.01 步进，直到唯一（`Order::checkMoney`）。
   - 持久化订单，返回 `orderId` 与收银台地址。
   - 发布 `OrderCreated` 事件给监听服务。
2. 收银台/订单状态
   - 提供 `GET /api/public/orders/{orderId}` 返回二维码、倒计时、通道类型。
   - `GET /state` 轮询，若已支付则生成通知 Payload + `returnUrl`。

## 3. 模块结构
- DTO：`PublicCreateOrderDTO`、`PublicCreateOrderResult`（已创建）。
- Service：`PublicOrderService`（接口）。
- 后续将增加 `OrderService`（后台管理）、`ChannelSelector`、`PriceAllocator` 等组件。

## 4. 下一步
- 通道选择：`ChannelSelector` 当前简单选择首个启用账号/终端，后续需引入更多策略（按类型、按 last_time 轮询）。
- 金额去重：`PriceAllocator` 采用 +0.01 逐步尝试的方式避免同通道金额冲突，未来可参考 legacy 逻辑优化性能。
- 写入单元/集成测试，覆盖签名校验、通道选择、事件发布。
- 与 `mpay-monitor` 对接：订单创建后通过 Redis Stream 推送给监听客户端。
- Flyway 初始化：`src/main/resources/db/migration/V1__init.sql` 包含 `user`、`pay_account`、`pay_channel`、`orders` 表结构，建议通过 `mvn -pl backend/mpay-payment -am flyway:migrate` 初始化数据库。
