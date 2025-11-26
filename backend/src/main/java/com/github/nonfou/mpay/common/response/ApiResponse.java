package com.github.nonfou.mpay.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.nonfou.mpay.common.error.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 统一 API 返回体，仿照遗留系统的 backMsg 结构。
 *
 * @param <T> payload 类型
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 业务状态码，0 表示成功，其他见 {@link ErrorCode}。
     */
    private final int code;

    /**
     * 人类可读的提示语。
     */
    private final String msg;

    /**
     * 业务数据。
     */
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.OK.getCode())
                .msg(ErrorCode.OK.getMessage())
                .data(data)
                .build();
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return failure(errorCode, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, T data) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .msg(errorCode.getMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .msg(message)
                .build();
    }

    // 别名方法 - 兼容不同调用风格
    public static <T> ApiResponse<T> ok(T data) {
        return success(data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return failure(code, message);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .code(-1)
                .msg(errorCode + ": " + message)
                .build();
    }
}
