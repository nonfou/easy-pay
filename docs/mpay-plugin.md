# mpay-plugin 模块说明

## 角色
- 管理支付插件元数据与本地安装状态，替代 legacy `extend/payplugin.php` + `PluginController`。
- 负责同步远程插件市场信息、启停插件、卸载插件、维护查询参数。

## 当前内容
- Spring Boot 入口 `MpayPluginApplication`，端口 8120。
- DTO：`PluginCreateRequest`（新增/编辑）、`PluginView`（展示），Service 接口 `PluginService` 覆盖列表、启停、同步等方法。
- 新增 `plugin_definition` 表（Flyway V1），实现 CRUD 与启停 API：  
  - `GET /api/plugins?show=`：0 全部、1 已安装、2 未安装。  
  - `POST /api/plugins`：新增或更新插件元信息。  
  - `PATCH /api/plugins/{platform}/state`：启用/停用。  
  - `DELETE /api/plugins/{platform}`：卸载。  
  - `POST /api/plugins/sync`：占位，未来与远程市场同步。

## 远程市场交互方案（草稿）
1. 定义 `PluginMarketplaceClient`（后续实现 Feign/RestTemplate），从远端 API `GET /mpay/getplugins` 拉取 JSON。
2. 定时任务（Quartz/Scheduler）调用 `syncFromMarketplace`，对比本地/远端差异：
   - 新增：写入 `plugin_definition` 表。
   - 更新：同步说明/价格/官网等字段。
   - 删除：标记为待卸载或提示人工确认。
3. 插件启停：更新本地 `state` 字段，并通知相关服务（如 `mpay-merchant`）刷新下拉选项。

## 下一步
- 设计 `plugin_definition`、`plugin_settings` 实体及 Repository。
- 与远程调用 + 缓存策略，避免频繁请求外部 API。
- 接入远程调用 + 缓存策略，避免频繁请求外部 API。
- 规划 plugin SDK/SPI 机制，约定 `className` 对应的 Java 插件接口。
