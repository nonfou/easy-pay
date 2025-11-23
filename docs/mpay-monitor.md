# mpay-monitor 模块说明

## 职责
- 取代 legacy `runtime/order.json` 轮询机制，负责订单监听、收款记录匹配、通知触发等异步流程。
- 订阅 `OrderCreated` 事件，将活跃订单写入 Redis Stream（或进程内事件），供监听客户端获取。
- 接收外部客户端推送的收款记录，匹配订单并通知 `mpay-payment` 更新状态。

## 当前设计
- Spring Boot 入口 `MpayMonitorApplication`（端口 8130），接入 Redis 与 RabbitMQ。
- DTO：`OrderHeartbeatDTO`（活跃订单信息）、`PaymentRecordDTO`（收款记录）。
- Service 接口：`OrderHeartbeatService`（发布/拉取活跃订单）、`PaymentMatchService`（处理收款记录）。

## 替代 order.json 的方案
1. `OrderHeartbeatService.publishActiveOrders` 由 `mpay-payment` 或定时任务调用，将待支付订单写入 Redis Stream（或本地事件缓冲），按 `pid`/`aid` 分组。
2. 监听客户端通过新 API 或 WebSocket/长轮询获取数据，而非访问文件。
3. 收款记录由 `PaymentMatchService` 统一处理：
   - 校验去重（避免重复通知）。
   - 使用内存/Redis 映射匹配订单（条件：`pid/aid/channel/payway/money`）。
   - 匹配成功后发布事件给 `mpay-payment` 更新订单状态与通知商户。

## 下一步
- 选型 Redis Stream vs MQ topic 作为活跃订单存储；实现实际消费者/生产者逻辑。
- 为监听客户端提供 REST/WebSocket API，并新增认证机制。
- 设计支付记录去重策略（hash + TTL）。
- 与 `mpay-payment`、`mpay-gateway` 联调，完成异步链路闭环。
