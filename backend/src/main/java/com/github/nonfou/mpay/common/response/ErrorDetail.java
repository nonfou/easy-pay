package com.github.nonfou.mpay.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 用于返回字段级别错误详情。
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorDetail {
    private final String field;
    private final String message;
}
