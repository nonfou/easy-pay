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

### Frontend (Vue 3 + TypeScript)
```bash
cd frontend
pnpm install                # 安装所有依赖

# 开发模式
pnpm dev                    # 启动开发服务器 (localhost:5173)

# 构建
pnpm build                  # 构建生产版本

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
- `entity/` - JPA 实体 (MerchantEntity, OrderEntity, PayAccountEntity, PayChannelEntity, OrderNotifyLogEntity)
- `dto/` - 数据传输对象，按领域分子包 (account/, auth/, cashier/, order/, monitor/, statistics/, user/)
- `security/` - JWT 认证 (SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter)
- `common/` - 统一响应 (ApiResponse)、异常处理 (BusinessException, GlobalExceptionHandler)

### API Endpoint Categories
- `/api/public/**` - 公共接口 (下单、收银台)，无需认证
- `/api/auth/**` - 认证接口 (登录、刷新令牌)
- `/api/cashier/**` - 收银台接口
- `/api/console/**` - 后台管理接口，需要 JWT 认证
- `/api/internal/**` - 内部接口 (订单匹配)，需要权限控制
- `/api/listen/**` - 监听客户端接口

### Frontend Structure
```
frontend/
├── src/
│   ├── views/              # 页面组件
│   │   ├── cashier/        # 收银台页面 (公开，无需认证)
│   │   │   ├── CashierHomeView.vue   # 订单查询
│   │   │   ├── CashierPayView.vue    # 支付页面
│   │   │   └── CashierResultView.vue # 结果页面
│   │   ├── DashboardView.vue         # 仪表盘
│   │   ├── OrderListView.vue         # 订单管理
│   │   ├── AccountListView.vue       # 账号管理
│   │   ├── UserListView.vue          # 用户管理
│   │   ├── TestPayView.vue           # 测试支付
│   │   └── LoginView.vue             # 登录页
│   ├── layouts/            # 布局组件
│   ├── router/             # 路由配置
│   ├── services/           # API 服务
│   ├── stores/             # Pinia 状态管理
│   └── types/              # TypeScript 类型定义
├── package.json
├── vite.config.ts
└── tsconfig.json
```

**路由说明**:
- `/cashier/*` - 收银台页面，无需认证
- `/login` - 登录页
- `/` - 管理后台，需要 JWT 认证

### Key Business Flows
1. **订单创建**: PublicOrderController → PublicOrderService → ChannelSelector (选通道) → PriceAllocator (金额去重 +0.01)
2. **订单匹配**: 监听客户端上报收款 → OrderMatchService → 更新订单状态 → 通知商户
3. **认证**: AuthController → AuthService → JwtTokenProvider

## Database
- MySQL，使用 Flyway 迁移，脚本位于 `backend/src/main/resources/db/`
- 初始化脚本: `init.sql` (user, orders, pay_account, pay_channel, order_notify_log)
- 金额字段使用 `BigDecimal` (decimal(10,2))

## Configuration
环境变量优先，参见 `application.yml`:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - 数据库
- `REDIS_HOST`, `REDIS_PORT` - Redis
- `JWT_SECRET` - JWT 密钥 (生产环境必须设置)

## Testing
- 使用 H2 内存数据库进行测试
- 测试类在 `backend/src/test/java/`
