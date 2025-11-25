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
├── backend/                    # Spring Boot 单体应用
│   └── src/main/java/com/github/nonfou/mpay/
│       ├── controller/         # REST 控制器
│       ├── service/            # 业务逻辑层
│       ├── repository/         # 数据访问层
│       ├── entity/             # JPA 实体
│       ├── dto/                # 数据传输对象
│       ├── security/           # JWT 认证
│       └── common/             # 统一响应/异常
├── frontend/
│   ├── console/                # Vue 3 管理后台 (Element Plus)
│   └── cashier/                # Vue 3 收银台
├── docs/                       # 设计文档
│   ├── project-design.md       # 项目设计文档
│   └── code-review-report.md   # 代码评审报告
└── docker-compose.yml          # MySQL + Redis
```

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

默认端口：MySQL `3307`、Redis `6380`

### 2. 启动后端

```bash
cd backend
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

## 核心功能

- **商户下单**: `/api/public/orders/create` - 创建支付订单
- **收银台**: `/api/cashier/orders/{orderId}` - 展示二维码，轮询支付状态
- **订单管理**: `/api/console/orders` - 后台订单查询与管理
- **账号管理**: `/api/console/accounts` - 收款账号与通道配置
- **用户认证**: `/api/auth/login` - JWT 登录认证

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

## 文档

- [项目设计文档](docs/project-design.md) - 架构、数据库、模块说明
- [代码评审报告](docs/code-review-report.md) - 已知问题与修复进度
