# mpay-common 模块说明

`mpay-common` 提供各服务复用的基础能力：

## 1. 依赖
- 引入 `spring-boot-starter`、`spring-boot-starter-validation`、`jackson-databind`、`lombok`。
- 作为普通 jar 供其它模块依赖，可在父 `pom` 里通过 `<dependency>` 引入。

## 2. 公共组件
| 包                     | 说明 |
|------------------------|------|
| `com.mapy.common.response` | `ApiResponse`（统一返回体）、`ErrorDetail`（字段错误信息）、`PageResponse`（分页封装）。 |
| `com.mapy.common.error`    | `ErrorCode`（枚举）、`BusinessException`（运行时异常）。 |
| `com.mapy.common.web`      | `GlobalExceptionHandler` 统一处理业务异常/参数校验错误/系统异常。 |

### ApiResponse 用法
```java
return ApiResponse.success(orderDto);
return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT);
```

### 抛出业务异常
```java
if (!signatureOk) {
    throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "签名错误");
}
```

`GlobalExceptionHandler` 会将异常转换为统一结构，并在校验失败时返回字段级错误列表，格式兼容旧系统的 `backMsg`。

## 3. 下一步
- 补充 `ErrorCode` 枚举项，结合业务完善 message。
- 统一日志格式（后续可在此模块加入 `LogHelper` 等工具）。
