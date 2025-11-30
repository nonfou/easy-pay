package com.github.nonfou.mpay.transaction.dto;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.RefundStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 退款查询请求
 */
@Data
public class RefundQueryRequest {

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 系统退款单号
     */
    private String refundNo;

    /**
     * 支付平台
     */
    private PaymentPlatform platform;

    /**
     * 退款状态
     */
    private RefundStatus status;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页数量
     */
    private Integer size = 20;
}
