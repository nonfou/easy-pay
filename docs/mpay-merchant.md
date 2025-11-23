# mpay-merchant 模块说明

## 职责
- 管理商户的收款账号与二维码终端（legacy `PayManageController` 对应）。
- 提供账号/终端 CRUD、启停、批量删除、二维码上传、交易记录查询等 API。
- 后续将与插件系统交互，校验账号配置与监听模式。

## 当前进展
- Spring Boot 入口 `MpayMerchantApplication` 已创建，默认监听 `8110`。
- DTO：`AccountCreateRequest`、`AccountSummary`、`ChannelCreateRequest` 等，匹配前端表单字段。
- 服务接口：`AccountService` 提供列表、创建、启停、终端维护等能力。

## 下一步
1. 设计实体（`PayAccount`, `PayChannel`）与 Repository，对应数据库结构。
2. 实现 `AccountService` + REST Controller，支持分页/条件查询、上传二维码（对接对象存储）。
3. 与 `mpay-plugin` 对接，确保平台列表与插件配置一致。
4. 引入鉴权，确保商户只能操作所属账号。
