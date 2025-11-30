package com.github.nonfou.mpay.transaction.entity;

import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 支付事件日志实体 - 记录全链路数据
 */
@Entity
@Table(name = "payment_event_log", indexes = {
        @Index(name = "idx_event_order_id", columnList = "orderId"),
        @Index(name = "idx_event_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_event_type", columnList = "eventType"),
        @Index(name = "idx_event_created_at", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PaymentEventLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 商户订单号
     */
    @Column(name = "order_id", length = 64)
    private String orderId;

    /**
     * 关联的交易ID
     */
    @Column(name = "transaction_id")
    private Long transactionId;

    /**
     * 关联的退款ID（如果是退款相关事件）
     */
    @Column(name = "refund_id")
    private Long refundId;

    /**
     * 事件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private PaymentEventType eventType;

    /**
     * 支付平台
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20)
    private PaymentPlatform platform;

    /**
     * 请求数据（JSON）
     */
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    /**
     * 响应数据（JSON）
     */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    /**
     * 结果码
     */
    @Column(name = "result_code", length = 32)
    private String resultCode;

    /**
     * 结果消息
     */
    @Column(name = "result_message", length = 512)
    private String resultMessage;

    /**
     * 是否成功
     */
    @Column(name = "success")
    private Boolean success;

    /**
     * 耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 客户端IP
     */
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 扩展数据（JSON格式）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
