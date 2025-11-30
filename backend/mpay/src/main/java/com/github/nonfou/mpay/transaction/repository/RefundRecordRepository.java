package com.github.nonfou.mpay.transaction.repository;

import com.github.nonfou.mpay.transaction.entity.RefundRecordEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 退款记录 Repository
 */
@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecordEntity, Long> {

    /**
     * 根据退款单号查询
     */
    Optional<RefundRecordEntity> findByRefundNo(String refundNo);

    /**
     * 根据平台退款单号查询
     */
    Optional<RefundRecordEntity> findByPlatformRefundNo(String platformRefundNo);

    /**
     * 根据交易ID查询所有退款记录
     */
    List<RefundRecordEntity> findByTransactionIdOrderByCreatedAtDesc(Long transactionId);

    /**
     * 根据订单号查询所有退款记录
     */
    List<RefundRecordEntity> findByOrderIdOrderByCreatedAtDesc(String orderId);

    /**
     * 根据状态查询
     */
    List<RefundRecordEntity> findByStatus(RefundStatus status);

    /**
     * 分页查询 - 多条件
     */
    @Query("SELECT r FROM RefundRecordEntity r WHERE " +
            "(:orderId IS NULL OR r.orderId = :orderId) AND " +
            "(:refundNo IS NULL OR r.refundNo = :refundNo) AND " +
            "(:platform IS NULL OR r.platform = :platform) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:startTime IS NULL OR r.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR r.createdAt <= :endTime) " +
            "ORDER BY r.createdAt DESC")
    Page<RefundRecordEntity> findByConditions(
            @Param("orderId") String orderId,
            @Param("refundNo") String refundNo,
            @Param("platform") PaymentPlatform platform,
            @Param("status") RefundStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 查询某交易的退款总数
     */
    @Query("SELECT COUNT(r) FROM RefundRecordEntity r WHERE r.transactionId = :transactionId")
    long countByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 查询处理中的退款（用于定时查询状态）
     */
    List<RefundRecordEntity> findByStatusAndCreatedAtBefore(RefundStatus status, LocalDateTime time);
}
