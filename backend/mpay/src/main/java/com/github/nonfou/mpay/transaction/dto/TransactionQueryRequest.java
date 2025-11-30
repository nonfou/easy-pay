package com.github.nonfou.mpay.transaction.dto;

import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 交易查询请求
 */
@Data
public class TransactionQueryRequest {

    /**
     * 商户订单号
     */
    private String orderId;

    /**
     * 系统交易号
     */
    private String tradeNo;

    /**
     * 支付平台
     */
    private PaymentPlatform platform;

    /**
     * 交易状态
     */
    private TransactionStatus status;

    /**
     * 商户ID
     */
    private String merchantId;

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
