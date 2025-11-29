# Easy-Pay

Easy-Pay (mpay) 是一个支付聚合系统，提供商户下单、收款码分配、支付监听、异步通知等能力。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3 + Java 21 |
| 前端 | Vue 3 + TypeScript + Vite |
| 数据库 | MySQL 9.5 + Redis 8.4 |
| 认证 | Spring Security + JWT |

## 目录结构

```
easy-pay/
├── backend/                        # Maven 多模块项目
│   ├── pom.xml                     # 父 POM
│   ├── mpay/                       # 核心支付模块
│   │   └── src/main/java/com/github/nonfou/mpay/
│   │       ├── controller/         # REST 控制器
│   │       ├── payment/            # 支付相关
│   │       │   ├── config/         # 支付配置
│   │       │   ├── properties/     # 配置属性
│   │       │   ├── service/        # 支付服务
│   │       │   └── dto/            # 支付 DTO
│   │       ├── websocket/          # WebSocket 支付通知
│   │       └── common/             # 统一响应/异常
│   └── easy-pay-spring-boot-starter/  # Spring Boot Starter
│       └── src/main/java/.../autoconfigure/
├── frontend/                       # Vue 3 前端
├── docs/                           # 设计文档
└── docker-compose.yml              # MySQL + Redis
```

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

默认端口：MySQL `3307`、Redis `6380`

### 2. 启动后端

```bash
cd backend/mpay
mvn spring-boot:run
```

服务监听 `http://localhost:8080`

### 3. 启动前端

**管理后台**:
```bash
cd frontend/console
npm install && npm run dev
```

**收银台**:
```bash
cd frontend/cashier
npm install && npm run dev
```

## Spring Boot Starter 集成

如果你想在其他 Spring Boot 项目中使用支付功能，可以直接引入 Starter：

```xml
<dependency>
    <groupId>com.github.nonfou</groupId>
    <artifactId>easy-pay-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

配置 `application.yml`：

```yaml
easy-pay:
  alipay:
    app-id: 你的应用ID
    private-key: 你的商户私钥
    public-key: 支付宝公钥
    notify-url: https://yourdomain.com/api/payment/alipay/callback
    return-url: https://yourdomain.com/payment/result
  wxpay:
    app-id: 微信公众号/小程序 appId
    mch-id: 商户号
    mch-key: API 密钥
    pay-notify-url: https://yourdomain.com/api/payment/wxpay/callback
```

详见 [Starter 文档](backend/easy-pay-spring-boot-starter/README.md)

## 核心功能

- **商户下单**: `/api/public/orders/create` - 创建支付订单
- **收银台**: `/api/cashier/orders/{orderId}` - 展示二维码，轮询支付状态
- **订单管理**: `/api/console/orders` - 后台订单查询与管理
- **账号管理**: `/api/console/accounts` - 收款账号与通道配置
- **用户认证**: `/api/auth/login` - JWT 登录认证
- **支付接口**: `/api/payment/*` - 支付宝/微信支付接口

## 配置说明

环境变量（可选，有默认值）:

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_URL` | `jdbc:mysql://localhost:3307/mpay` | 数据库连接 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | `rootpass` | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6380` | Redis 端口 |
| `JWT_SECRET` | (开发密钥) | JWT 签名密钥 |

## 默认账号

- 用户名: `admin`
- 密码: `admin123`
- PID: `1000`

## 构建

```bash
cd backend
mvn clean install -DskipTests
```

生成的 jar：
- `mpay/target/mpay-1.0.0-SNAPSHOT.jar` - 库 jar（用于被依赖）
- `mpay/target/mpay-1.0.0-SNAPSHOT-exec.jar` - 可执行 jar（独立运行）
- `easy-pay-spring-boot-starter/target/easy-pay-spring-boot-starter-1.0.0-SNAPSHOT.jar` - Starter

## 文档

- [项目设计文档](docs/project-design.md) - 架构、数据库、模块说明
- [代码评审报告](docs/code-review-report.md) - 已知问题与修复进度
- [Starter 使用文档](backend/easy-pay-spring-boot-starter/README.md) - Spring Boot Starter 集成指南
