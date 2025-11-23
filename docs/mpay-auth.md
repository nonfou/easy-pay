# mpay-auth 模块说明

## 职责
- 提供后台/商户登录、登出、刷新令牌、密钥重置等能力。
- 暂定作为独立 Spring Boot 服务运行，供 `mpay-gateway` 通过内部调用或 RPC 访问。
- 未来扩展：用户管理、角色权限（RBAC）、多因子认证、接口访问凭证。

## 当前状态
- 已创建 Spring Boot 入口 `MpayAuthApplication`，默认运行在 `8090` 端口。
- 引入 `spring-boot-starter-security`，后续将接入 JWT（推荐 jjwt 或 spring-security-oauth2-resource-server）。
- DTO：`LoginRequest`、`RefreshTokenRequest`、`TokenPair`，服务接口 `AuthService` 已定义，便于 `gateway` 编写 Feign/REST 客户端。

## 后续任务
1. 设计用户实体与仓储（MyBatis-Plus/JPA）。
2. 集成密码哈希（BCrypt）与 JWT 生成/验证。
3. 实现登录/刷新/登出 Controller，输出 OpenAPI。
4. 对接 `mpay-gateway`，提供统一的鉴权过滤器，确保公共与后台接口的访问控制。
