# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run                    # 启动后端服务 (localhost:8080)
mvn test                               # 运行所有测试
mvn test -Dtest=OrderMatchServiceTest  # 运行单个测试类
mvn test -Dtest=OrderMatchServiceTest#testMatchOrder  # 运行单个测试方法
mvn package -DskipTests                # 打包 (跳过测试)
```

### Frontend (pnpm workspace)
```bash
cd frontend
pnpm install                # 安装所有依赖

# 开发模式
pnpm dev                    # 同时启动 console (5173) 和 cashier (5174)
pnpm dev:console            # 仅启动 console (localhost:5173)
pnpm dev:cashier            # 仅启动 cashier (localhost:5174)

# 构建
pnpm build                  # 构建所有应用
pnpm build:console          # 仅构建 console
pnpm build:cashier          # 仅构建 cashier

# 代码质量
pnpm lint                   # ESLint 检查
pnpm lint:fix               # ESLint 自动修复
pnpm format                 # Prettier 格式化
pnpm type-check             # TypeScript 类型检查
```

### Infrastructure
```bash
docker compose up -d    # 启动 MySQL (3307) + Redis (6380)
docker compose down     # 停止基础设施
```

## Architecture Overview

**单体架构** - Spring Boot 3.3 + Java 21 后端，Vue 3 + TypeScript 前端。

### Backend Package Structure (`com.github.nonfou.mpay`)
- `controller/` - REST 控制器 (14个)，分为公共接口、内部接口、后台接口
- `service/` + `service/impl/` - 业务逻辑层
- `repository/` - Spring Data JPA 数据访问
- `entity/` - JPA 实体 (MerchantEntity, OrderEntity, PayAccountEntity, PayChannelEntity, PluginEntity, OrderNotifyLogEntity)
- `dto/` - 数据传输对象，按领域分子包 (account/, auth/, cashier/, order/, plugin/, monitor/, statistics/, user/)
- `security/` - JWT 认证 (SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter)
- `common/` - 统一响应 (ApiResponse)、异常处理 (BusinessException, GlobalExceptionHandler)

### API Endpoint Categories
- `/api/public/**` - 公共接口 (下单、收银台)，无需认证
- `/api/auth/**` - 认证接口 (登录、刷新令牌)
- `/api/cashier/**` - 收银台接口
- `/api/console/**` - 后台管理接口，需要 JWT 认证
- `/api/internal/**` - 内部接口 (订单匹配)，需要权限控制
- `/api/listen/**` - 监听客户端接口

### Frontend Structure (pnpm workspace)
```
frontend/
├── pnpm-workspace.yaml     # workspace 配置
├── package.json            # 根 package.json (脚本和代码质量工具)
├── .eslintrc.cjs           # ESLint 配置
├── .prettierrc             # Prettier 配置
├── console/                # 管理后台 (Element Plus + Pinia)
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
└── cashier/                # 收银台 (轻量级，qrcode.vue)
    ├── package.json
    ├── vite.config.ts
    └── src/
```

### Key Business Flows
1. **订单创建**: PublicOrderController → PublicOrderService → ChannelSelector (选通道) → PriceAllocator (金额去重 +0.01)
2. **订单匹配**: 监听客户端上报收款 → OrderMatchService → 更新订单状态 → 通知商户
3. **认证**: AuthController → AuthService → JwtTokenProvider

## Database
- MySQL，使用 Flyway 迁移，脚本位于 `backend/src/main/resources/db/`
- 初始化脚本: `init.sql` (user, orders, pay_account, pay_channel, order_notify_log, plugin_definition)
- 金额字段使用 `BigDecimal` (decimal(10,2))

## Configuration
环境变量优先，参见 `application.yml`:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - 数据库
- `REDIS_HOST`, `REDIS_PORT` - Redis
- `JWT_SECRET` - JWT 密钥 (生产环境必须设置)

## Testing
- 使用 H2 内存数据库进行测试
- 测试类在 `backend/src/test/java/`
