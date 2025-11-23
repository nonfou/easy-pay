# mpay-spring

`mpay-spring` 是 mpay 支付系统的 Java + Vue 重构工程。项目采用多模块 Maven + 前后端分离结构，并配套基础设施（MySQL、Redis、RabbitMQ）以便快速拉起本地环境。

## 目录结构
```
mpay-spring/
├── backend/                # Spring Boot 多模块
│   ├── mpay-common         # 通用 DTO/异常/工具包
│   ├── mpay-gateway        # API Gateway / 统一入口
│   ├── mpay-auth           # 鉴权与用户/密钥管理
│   ├── mpay-payment        # 订单提交与收银台逻辑
│   ├── mpay-merchant       # 商户账号、收款终端管理
│   ├── mpay-plugin         # 插件市场与配置
│   ├── mpay-monitor        # 监听、心跳、定时任务
│   └── mpay-bff-console    # 后台 BFF（可选）
├── frontend/
│   ├── console             # Vue 3 后台管理 SPA（待初始化）
│   └── cashier             # Vue 3 收银台 SPA（待初始化）
├── docs/                   # 设计文档、API 规格等
├── docker-compose.yml      # 基础设施编排：MySQL / Redis / RabbitMQ
└── IMPLEMENTATION_PLAN.md  # 中文实施计划
```

## 快速启动基础设施
```bash
cd mpay-spring
# 首次可提前创建数据目录
mkdir -p infra/mysql/data infra/mysql/init infra/redis/data
# 启动 MySQL/Redis/RabbitMQ
podman-compose up -d  # 或 docker compose up -d
```
> 默认端口：MySQL `3307`、Redis `6380`、RabbitMQ `5673`（控制台 `15673`）。凭证可在 `docker-compose.yml` 修改。

## 下一步
1. 根据 `IMPLEMENTATION_PLAN.md` 继续完成阶段 0 的数据库梳理与 CI 雏形。
2. 在 `docs/` 目录记录数据库模型、API 草稿。
3. 为 `backend` 各模块逐步添加 Spring Boot 代码骨架；在 `frontend` 目录创建 Vite + Vue 3 脚手架。
## 模块速览
- `mpay-common`：基础响应/异常/工具。
- `mpay-gateway`：Spring Boot 入口，承载公共 API 与后台接口的网关层。

### 启动本地后端
```bash
cd backend
mvn -pl mpay-gateway -am spring-boot:run
# 或打包后 java -jar target/mpay-gateway-0.1.0-SNAPSHOT.jar
```
服务默认监听 `http://localhost:8080`，示例接口：`GET /api/_internal/ping` 返回统一 `ApiResponse`。
```
{
  "code": 0,
  "msg": "success",
  "data": {
    "service": "mpay-gateway",
    "timestamp": "2025-11-23T15:30:00Z"
  }
}
```

### 下一步
- 在 `mpay-gateway` 引入安全配置（Spring Security + JWT）。
- 根据 `docs/openapi.yaml` 增量实现公共下单与后台 API。

### 数据库初始化（Flyway）
```bash
cd backend
mvn -pl mpay-payment -am flyway:migrate \
  -Dflyway.url=jdbc:mysql://localhost:3307/mpay \
  -Dflyway.user=root \
  -Dflyway.password=rootpass
```
脚本位于 `backend/mpay-payment/src/main/resources/db/migration/`，包含 `user`、`pay_account`、`pay_channel`、`orders` 等表结构。

### Frontend Apps
```bash
cd frontend/console
npm install
npm run dev

cd ../cashier
npm install
npm run dev
```
在 `console/.env` 设置 `VITE_API_BASE`（默认指向 `http://localhost:8080`），在 `cashier/.env` 设置 `VITE_PAYMENT_BASE`（默认 `http://localhost:8100`）。控制台默认加载 `/api/_internal/ping` 进行健康检查，收银台页面可输入订单号调用 `/api/public/orders/{orderId}`。
