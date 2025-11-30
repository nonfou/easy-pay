package com.github.nonfou.mpay.common.error;

import lombok.Getter;

/**
 * 通用错误码枚举。
 */
@Getter
public enum ErrorCode {
    OK(0, "success"),
    INVALID_ARGUMENT(400, "invalid argument"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    CONFLICT(409, "conflict"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    SERVER_ERROR(500, "internal server error"),
    SERVICE_UNAVAILABLE(503, "service unavailable"),

    // 业务错误码 (1xxx)
    LOGIN_FAILED(1001, "登录失败"),
    ACCOUNT_DISABLED(1002, "账号已被禁用"),

    // 交易错误码 (2xxx)
    TRANSACTION_NOT_FOUND(2001, "交易记录不存在"),
    INVALID_TRANSACTION_STATUS(2002, "交易状态不正确"),
    INVALID_REFUND_AMOUNT(2003, "退款金额无效"),
    EXCEED_REFUND_AMOUNT(2004, "退款金额超出可退款金额"),
    UNSUPPORTED_PLATFORM(2005, "不支持的支付平台"),
    REFUND_FAILED(2006, "退款失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
