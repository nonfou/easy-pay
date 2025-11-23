# 公共下单接口（临时说明）

`mpay-payment` 暴露 `POST /api/public/orders`，目前接入 `PublicOrderController` 并返回统一 `ApiResponse`。服务尚未实现真实下单逻辑，调用会收到 `SERVICE_UNAVAILABLE`（TODO 标记）。

## 请求示例
```http
POST http://localhost:8100/api/public/orders
Content-Type: application/json

{
  "pid": 1000,
  "type": "wxpay",
  "outTradeNo": "TEST123",
  "name": "测试商品",
  "money": 1.00,
  "notifyUrl": "https://merchant.com/notify",
  "returnUrl": "https://merchant.com/return",
  "sign": "md5-sign",
  "signType": "MD5"
}
```

## 响应示例
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderId": "H1700000000000",
    "cashierUrl": "/cashier/H1700000000000"
  }
}
```

目前接口已验证金额、检查 `outTradeNo` 重复，并使用商户密钥进行 MD5 签名校验，生成订单后写入 Redis Stream 推送给监听服务。
