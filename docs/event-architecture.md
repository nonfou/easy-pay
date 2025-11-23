# 事件机制（Redis Stream）说明

为替换旧版 `order.json` 文件轮询，系统采用 Redis Stream 传递订单监听事件：

1. `mpay-payment` 在订单创建后调用 `OrderHeartbeatService.publishActiveOrders`，将订单摘要写入 `mpay:order:heartbeat` Stream。
2. `mpay-monitor` 的 `OrderHeartbeatService` 默认实现 `RedisStreamOrderHeartbeatService`，负责写入/读取 Stream。
3. 监听客户端可通过 `mpay-monitor` 暴露的 API 获取活跃订单，也可直接消费 Redis Stream（后续根据安全策略决定）。
4. 收到收款记录后，监听客户端调用 `PaymentMatchService` 对应的 API，将记录回传给监控服务，由监控服务匹配订单并通知 `mpay-payment`。

### Stream 字段
- `orderId`：平台订单号
- `pid/aid/cid`：商户、账号、通道 ID
- `type`：支付方式
- `expiresAt`：订单过期时间
- `pattern`：监听模式（0/1）

### 示例
```java
OrderHeartbeatDTO dto = OrderHeartbeatDTO.builder()
    .orderId("H202411230001")
    .pid(1000L)
    .aid(1L)
    .cid(2L)
    .type("wxpay")
    .expiresAt(Instant.now().plusSeconds(180))
    .pattern(1)
    .build();
orderHeartbeatService.publishActiveOrders(List.of(dto));
```

Redis CLI 查看：
```
XREAD COUNT 10 STREAMS mpay:order:heartbeat 0-0
```

后续可在 `mpay-monitor` 增加消费者组、自动清理策略、失败重试等功能。

## 监听器 API

`mpay-monitor` 暴露以下接口：

- `GET /api/monitor/orders/active?pid=`：获取当前活跃订单列表，可按商户 `pid` 过滤。
- `POST /api/monitor/records`：监听客户端推送收款记录，参数见 `PaymentRecordDTO`。

示例请求：
```http
GET http://localhost:8130/api/monitor/orders/active?pid=1000
```
```http
POST http://localhost:8130/api/monitor/records
Content-Type: application/json

{
  "pid":1000,
  "aid":1,
  "payway":"wxpay",
  "channel":"wxpay1#1001",
  "price":10.0,
  "platformOrder":"wx123",
  "rawPayload":"{...}"
}
```

监控服务会将该记录转发至 `mpay-payment` 的内部接口 `/api/internal/orders/match`，由支付服务完成订单匹配与通知。

## 接口鉴权（草案）
- 监听客户端调用上述接口时需携带签名参数，例如 `timestamp`、`nonce`、`signature`。
- 服务端根据商户密钥（或客户端凭证）验证签名，未通过则拒绝请求。
- 后续可扩展为：在 `POST /api/monitor/records` 中要求 `Authorization: Bearer <token>` 或 HMAC-SHA256 签名。

## 失败重试提示
- 若 monitor 上报 `/records` 后接收到非 2xx 响应，监听客户端应记录该条收款记录，并按指数退避策略重试。
- Webhook 出错可能由网络、匹配失败等原因导致，客户端可设置最大重试次数并在超过阈值时告警。


## 监听器 API

mpay-monitor 暴露以下接口：

- GET /api/monitor/orders/active?pid=：获取当前活跃订单列表，可按商户 pid 过滤。
- POST /api/monitor/records：监听客户端推送收款记录，参数见 PaymentRecordDTO。

示例请求：
`http
GET http://localhost:8130/api/monitor/orders/active?pid=1000
`
`http
POST http://localhost:8130/api/monitor/records
Content-Type: application/json

{
  "pid":1000,
  "aid":1,
  "payway":"wxpay",
  "channel":"wxpay1#1001",
  "price":10.0,
  "platformOrder":"wx123",
  "rawPayload":"{...}"
}
`
