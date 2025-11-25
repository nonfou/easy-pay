# 工程与协作规范

本规范适用于 mpay-spring 重构项目，涵盖代码风格、分支策略、提交信息与代码评审要求。随项目演进可在此文档基础上补充条目。

## 1. 分支策略
- 采用 `main` + `develop` + 功能分支模型：
  - `main`：仅存放可发布版本，受保护。
  - `develop`：日常集成分支，完成阶段性任务后再合并至 `main`。
  - 功能/修复分支命名：`feature/<module>-<keyword>`、`fix/<issue-id>`、`chore/<task>`。
- 合并要求：
  - 任何非紧急修复均通过 Pull Request 合并。
  - 至少 1 名 reviewer 通过并通过 CI 才可 merge。
  - 禁止直接在 `main` 上提交。

## 2. 提交信息
- 使用英文祈使句，格式：`<type>: <subject>`，其中 `<type>` 参考 Conventional Commits：
  - `feat`、`fix`、`refactor`、`docs`、`test`、`build`、`chore`、`perf`、`style` 等。
- 若对应 Issue/需求，在提交末尾附 `#123`。
- 单次提交聚焦单一主题，避免混合多个功能。

## 3. 代码风格（后端）
- Java 版本：21；使用 Spring Boot 3 + Maven。
- 格式化：遵循 Google Style（可通过 Spotless/EditorConfig 统一）。
- 目录结构：`com.github.nonfou.mpay.<layer>`，按领域划分 package。
- Controller 只做入参校验、调用 Service；业务逻辑放在 Service/Domain 层。
- DTO/VO 与 Entity 分离，借助 MapStruct 映射。
- 所有公共响应使用统一包装（对应 `mpay-common` 的 `ApiResponse`）。
- 数据访问使用 MyBatis-Plus 或 Spring Data；禁止在 Controller 中直接访问 Mapper。
- 重要逻辑编写单元测试（JUnit + AssertJ/Mockito），CI 中至少运行 `mvn verify`。

## 4. 代码风格（前端）
- Vue 3 + TypeScript + Vite。
- ESLint + Prettier + Stylelint 强制格式；统一使用 Composition API。
- 状态管理采用 Pinia，接口请求通过统一的 `apiClient`（封装 axios）。
- 组件命名 PascalCase，目录按 `features/<domain>/components|views` 分类。
- i18n 与主题支持在设计阶段同步考虑，字符串常量放置于 `locales/`。

## 5. 安全与配置
- 禁止在仓库提交敏感信息（数据库密码、JWT 秘钥等），统一从环境变量/配置中心读取。
- `.env`/`application-*.yml` 仅保留示例模板，真实配置走 Secret 管理。
- 所有外部请求需设置超时与重试策略，涉及支付/通知的接口必须幂等。

## 6. 代码评审
- PR 模板至少包含：变更内容、测试验证、风险/回滚方案。
- Reviewer 检查点：
  - 是否符合业务需求与规范；
  - 是否存在安全/性能问题；
  - 是否补充必要测试与文档。
- 代码覆盖率目标 >70%，关键模块（签名、订单匹配）需接近 100%。

## 7. 持续集成
- GitHub Actions（或 Jenkins）运行以下最小任务：
  1. `mvn -B -pl backend/mpay-common -am verify`（可随模块增多扩展）。
  2. 前端 lint/test（待脚手架完成后添加）。
- 所有 PR 必须通过 CI 后才允许合并。

## 8. 文档与知识共享
- 所有设计/决策记录在 `docs/`；图表可采用 PlantUML/draw.io。
- 每个阶段结束在 README 或 Release Note 汇总亮点。
- 新成员入项前阅读：《实施计划》《数据库迁移方案》《工程规范》（本文）。
