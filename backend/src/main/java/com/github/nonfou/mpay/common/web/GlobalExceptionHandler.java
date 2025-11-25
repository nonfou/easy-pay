package com.github.nonfou.mpay.common.web;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.ApiResponse;
import com.github.nonfou.mpay.common.response.ErrorDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode() == null ? ErrorCode.SERVER_ERROR : ex.getErrorCode();
        HttpStatus status = mapHttpStatus(errorCode);
        // 使用 BusinessException 的自定义消息，如果没有则使用 ErrorCode 的默认消息
        String message = ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage();
        return ResponseEntity.status(status).body(ApiResponse.error(errorCode.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleConstraintViolation(
            ConstraintViolationException ex) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(this::toErrorDetail)
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.SERVER_ERROR));
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return ErrorDetail.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    private ErrorDetail toErrorDetail(ConstraintViolation<?> violation) {
        return ErrorDetail.builder()
                .field(violation.getPropertyPath().toString())
                .message(violation.getMessage())
                .build();
    }

    private HttpStatus mapHttpStatus(ErrorCode errorCode) {
        // 业务错误码 (1xxx) 返回 HTTP 200，让前端根据业务码处理
        if (errorCode.getCode() >= 1000 && errorCode.getCode() < 2000) {
            return HttpStatus.OK;
        }
        return switch (errorCode) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
