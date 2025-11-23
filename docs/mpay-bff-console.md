# mpay-bff-console 模块说明

## 作用
- 为 Vue 控制台提供聚合/裁剪后的 API，避免前端直接调用多个后端服务。
- 负责菜单数据、仪表盘指标、消息通知等复合接口，也可承担缓存/降级职责。
- 采用 WebFlux，提高并发聚合能力，可通过 WebClient 调用 `mpay-payment`、`mpay-merchant`、`mpay-plugin` 等服务。

## 当前内容
- Spring Boot WebFlux 入口 `MpayBffConsoleApplication`，监听 8140。
- DTO：`MenuItemDTO`、`DashboardMetricsDTO`；服务接口 `ConsoleFacadeService` 定义菜单/仪表盘聚合。

## 下一步
1. 实现 `ConsoleFacadeService`，对接 `mpay-gateway` 或各后端服务获取数据。
2. 补充更多聚合接口（消息中心、待办、插件统计）。
3. 引入缓存/熔断策略，用于高频指标接口。
4. 安全方面：与 `mpay-auth` 打通，基于 JWT/用户角色控制菜单可见性。
