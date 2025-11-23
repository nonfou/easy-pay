# mpay-spring 实施计划

## 1. 背景与目标
- 以 Spring Boot + Vue 3 方案重构现有 ThinkPHP + Layui 系统（目录 `mpay/`），保持商户下单、管理后台、插件生态等功能，同时提升可维护性、安全性与扩展性。
- 引入分层架构（API Gateway -> BFF -> 领域服务 -> 持久化）与异步通信，覆盖订单生命周期、Webhook 通知、插件心跳等场景。
- 打造现代化 SPA 运维后台与收银台，所有客户端通过标准化 REST API 接入，便于未来移动端与三方集成。

## 2. 目标架构
- **后端**：Spring Boot 3 多模块工程（`mpay-gateway`、`mpay-auth`、`mpay-payment`、`mpay-merchant`、`mpay-plugin`、`mpay-monitor`），结合 Spring Security + JWT、Spring Validation、MapStruct、MyBatis-Plus/MySQL8、Redis、消息队列（RocketMQ/RabbitMQ）。
- **前端**：Vue 3 + Vite + TypeScript + Pinia + Element Plus，拆分 `console`（后台）与 `cashier`（收银台）两个应用，CI 构建后由 Nginx/CDN 服务。
- **可观测性**：ELK 日志、Prometheus/Grafana 指标、OpenTelemetry 链路；统一审计与事件日志。
- **插件体系**：以 Java SPI 定义 `PaymentPlugin` 接口，替代 PHP `extend/payclient/*`；插件元数据存表并定时与远程市场同步。

## 3. 工作拆解与里程碑

### 阶段 0 · 基础搭建（第 1 周）
1. 初始化 `mpay-spring` 仓库结构（多模块 Maven/Gradle 架构），并提供基础 Docker Compose（MySQL、Redis、MQ）。
2. 导入/梳理现有数据库结构，输出 ER 图与迁移策略。
3. 约定编码规范、分支策略，搭建 CI 雏形（GitHub Actions/Jenkins Stub）。

### 阶段 1 · 领域建模与 API 规格（第 2 周）
- 依据 PHP 控制器（`PayController`、`OrderController`、`PayManageController`、`PluginController` 等）整理领域图。
- 编写 OpenAPI 规范：
  - 公共下单接口（`submit`、`mapi`、`console`、`getOrderState`）。
  - 后台/商户接口（订单、账号、终端、插件、用户）。
  - 内部监听/心跳协议。
- 评审数据契约与错误码，制定兼容旧参数的 DTO 映射策略。

### 阶段 2 · 后端核心服务（第 3-4 周）
- 实现通用模块 `mpay-common`（DTO、异常、签名、统一返回体）。
- 搭建 `mpay-auth`：用户模型、RBAC、JWT 登录、密码与密钥重置。
- 搭建 `mpay-payment`：订单聚合、通道选择（移植 `Order::setChannel`）、金额去重、签名校验，对外暴露 `/api/public/orders` 及收银台查询接口。
- 引入 Flyway 数据迁移，并编写订单创建/校验的集成测试。

### 阶段 3 · 商户与后台 API（第 5-6 周）
- `mpay-merchant`：账号/终端 CRUD、二维码上传（S3/MinIO）、监听配置、账户流水查询。
- `mpay-plugin`：插件 CRUD、市场同步任务、启停接口，对接远程市场（代替 `extend/Plugin.php`）。
- 视情况新增 `mpay-bff-console` 汇聚后台所需数据，确保分页/筛选/导出能力与旧版一致。

### 阶段 4 · 异步处理与监控（第 7-8 周）
- 以 Redis Stream/事件总线替代 `runtime/order.json` 轮询，发布 `OrderCreated` 事件。
- 实现监听服务，消费支付通知并匹配订单（复用 `payHeart`、`updateOrderState` 逻辑）。
- 构建 Webhook 分发器：重试策略、死信队列、通知状态表（替代 `getHttpResponse` 同步请求）。
- 加入定时任务（Quartz/Spring Scheduler）处理订单过期、插件同步、营收看板聚合，并接入监控告警。

### 阶段 5 · 前端交付（第 5-9 周交叉）
- **Console SPA**：重写仪表盘、订单列表、账号/终端管理、插件市场、用户中心。
- **Cashier SPA**：展示订单详情、二维码、倒计时、状态轮询/WS、返回跳转。
- 共用 UI SDK（主题、鉴权守卫、API 客户端），并以 Cypress/Vitest 覆盖关键流。

### 阶段 6 · 迁移与上线（第 10-12 周）
- 编写数据迁移脚本，导出现有 MySQL / 插件配置（`config/extend/payplugin.php`）到新 schema，并多轮演练。
- 构建兼容层（临时 PHP 代理或新旧 API 双写）以灰度切换商户。
- 完成性能压测、安全审计、混沌演练，制定蓝绿切换与回滚方案，并对商户进行沟通。
- 上线后监控运行指标，整理遗留问题进入迭代清单。

## 4. 跨领域关注点
- **安全**：全链路 HTTPS、请求签名、限流、审计日志、密钥轮换、后台登陆验证码/双因素认证。
- **测试**：单元/集成/契约测试 + 端到端回归，对比新旧接口结果，并添加合成交易监控。
- **文档**：开发手册、运维 Runbook、API 门户、插件 SDK 指南。
- **DevOps**：各服务容器镜像、Helm/Kustomize 部署、Spring Config/Nacos 管理多环境配置。

## 5. 风险与对策
- **插件功能差异**：尽早盘点现有插件，必要时提供 PHP 插件适配层。
- **数据一致性**：采用双写或复制监控，在迁移阶段保留旧库只读兜底。
- **进度失控**：以订单+后台核心功能为 MVP，非关键需求（如插件自动安装）延后。
- **团队熟悉度**：组织 Legacy 代码分享，实施代码评审与规范检查。

## 6. 近期行动项
1. 与干系人确认数据库、消息队列、对象存储等基础技术选型。
2. 完成阶段 0 任务：仓库脚手架、CI 雏形、环境 Compose。
3. 启动 OpenAPI 草稿编写，对照旧路由整理接口清单。
