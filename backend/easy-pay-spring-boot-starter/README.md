# Easy Pay Spring Boot Starter

基于原有支付模块的 Spring Boot Starter，支持快速集成到其他 Spring Boot 应用。

## 项目结构

```
backend/
├── pom.xml                              # 父 POM
├── mpay/                                # 核心支付模块
│   ├── src/main/java/
│   │   └── com/github/nonfou/mpay/
│   │       ├── payment/                 # 支付相关代码
│   │       │   ├── config/              # 配置类
│   │       │   ├── properties/          # 配置属性
│   │       │   ├── service/             # 服务层
│   │       │   └── dto/                 # DTO
│   │       ├── controller/              # 控制器
│   │       └── websocket/               # WebSocket
│   └── pom.xml
└── easy-pay-spring-boot-starter/        # 自动配置 Starter
    ├── src/main/java/
    │   └── com/github/nonfou/mpay/autoconfigure/
    │       ├── EasyPayAutoConfiguration.java
    │       ├── AlipayAutoConfiguration.java
    │       └── WxPayAutoConfiguration.java
    ├── src/main/resources/META-INF/
    │   ├── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │   └── spring-configuration-metadata.json
    └── pom.xml
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.github.nonfou</groupId>
    <artifactId>easy-pay-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置支付参数

在 `application.yml` 中添加配置：

```yaml
easy-pay:
  alipay:
    app-id: 你的应用ID
    private-key: 你的商户私钥
    public-key: 支付宝公钥
    notify-url: https://yourdomain.com/api/payment/alipay/callback
    return-url: https://yourdomain.com/payment/result
    # 沙箱环境使用以下网关
    # gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do

  wxpay:
    app-id: 微信公众号/小程序 appId
    mch-id: 商户号
    mch-key: API 密钥
    pay-notify-url: https://yourdomain.com/api/payment/wxpay/callback
```

### 3. 使用

Starter 会自动配置以下 Bean：
- `AlipayClient` - 支付宝客户端
- `AlipayService` - 支付宝服务
- `WxPayService` - 微信支付服务
- `WxPayServiceWrapper` - 微信支付服务包装器
- `PaymentController` - 支付控制器
- `PaymentWebSocketHandler` - WebSocket 处理器

API 端点会自动注册在 `/api/payment` 下。

## 配置项

### 支付宝配置 (easy-pay.alipay)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| app-id | 应用ID | - |
| private-key | 商户私钥 | - |
| public-key | 支付宝公钥 | - |
| notify-url | 异步回调地址 | - |
| return-url | 同步回调页面 | - |
| gateway-url | 网关地址 | https://openapi.alipay.com/gateway.do |
| sign-type | 签名类型 | RSA2 |
| charset | 字符编码 | utf-8 |

### 微信支付配置 (easy-pay.wxpay)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| app-id | 公众号/小程序 appId | - |
| mch-id | 商户号 | - |
| mch-key | API 密钥 | - |
| trade-type | 交易类型 | NATIVE |
| pay-notify-url | 支付回调地址 | - |
| refund-notify-url | 退款回调地址 | - |
| cert-path | 退款证书路径 | - |

## 构建

```bash
cd backend
mvn clean install -DskipTests
```

会生成两个 jar：
- `mpay-1.0.0-SNAPSHOT.jar` - 普通库 jar（用于被其他项目依赖）
- `mpay-1.0.0-SNAPSHOT-exec.jar` - 可执行 jar（独立运行）

## 系统要求

- Java 21+
- Spring Boot 3.3+
