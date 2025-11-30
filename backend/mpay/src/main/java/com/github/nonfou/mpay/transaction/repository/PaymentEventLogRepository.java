package com.github.nonfou.mpay.transaction.repository;

import com.github.nonfou.mpay.transaction.entity.PaymentEventLogEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentEventType;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付事件日志 Repository
 */
@Repository
public interface PaymentEventLogRepository extends JpaRepository<PaymentEventLogEntity, Long> {

    /**
     * 根据订单号查询所有事件（按时间正序，用于追溯流程）
     */
    List<PaymentEventLogEntity> findByOrderIdOrderByCreatedAtAsc(String orderId);

    /**
     * 根据交易ID查询所有事件
     */
    List<PaymentEventLogEntity> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);

    /**
     * 根据退款ID查询所有事件
     */
    List<PaymentEventLogEntity> findByRefundIdOrderByCreatedAtAsc(Long refundId);

    /**
     * 根据事件类型查询
     */
    List<PaymentEventLogEntity> findByEventTypeOrderByCreatedAtDesc(PaymentEventType eventType);

    /**
     * 分页查询 - 多条件
     */
    @Query("SELECT e FROM PaymentEventLogEntity e WHERE " +
            "(:orderId IS NULL OR e.orderId = :orderId) AND " +
            "(:transactionId IS NULL OR e.transactionId = :transactionId) AND " +
            "(:eventType IS NULL OR e.eventType = :eventType) AND " +
            "(:platform IS NULL OR e.platform = :platform) AND " +
            "(:success IS NULL OR e.success = :success) AND " +
            "(:startTime IS NULL OR e.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR e.createdAt <= :endTime) " +
            "ORDER BY e.createdAt DESC")
    Page<PaymentEventLogEntity> findByConditions(
            @Param("orderId") String orderId,
            @Param("transactionId") Long transactionId,
            @Param("eventType") PaymentEventType eventType,
            @Param("platform") PaymentPlatform platform,
            @Param("success") Boolean success,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 查询失败的事件（用于告警监控）
     */
    @Query("SELECT e FROM PaymentEventLogEntity e WHERE " +
            "e.success = false AND e.createdAt >= :startTime " +
            "ORDER BY e.createdAt DESC")
    List<PaymentEventLogEntity> findFailedEventsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计某时间段内的事件数量
     */
    @Query("SELECT COUNT(e) FROM PaymentEventLogEntity e WHERE " +
            "e.eventType = :eventType AND " +
            "e.createdAt BETWEEN :startTime AND :endTime")
    long countByEventTypeAndTimeRange(
            @Param("eventType") PaymentEventType eventType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
