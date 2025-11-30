package com.github.nonfou.mpay.transaction.repository;

import com.github.nonfou.mpay.transaction.entity.PaymentTransactionEntity;
import com.github.nonfou.mpay.transaction.enums.PaymentPlatform;
import com.github.nonfou.mpay.transaction.enums.TransactionStatus;
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
 * 支付交易记录 Repository
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    /**
     * 根据订单号查询
     */
    Optional<PaymentTransactionEntity> findByOrderId(String orderId);

    /**
     * 根据系统交易号查询
     */
    Optional<PaymentTransactionEntity> findByTradeNo(String tradeNo);

    /**
     * 根据平台交易号查询
     */
    Optional<PaymentTransactionEntity> findByPlatformTradeNo(String platformTradeNo);

    /**
     * 根据状态查询
     */
    List<PaymentTransactionEntity> findByStatus(TransactionStatus status);

    /**
     * 根据平台和状态查询
     */
    List<PaymentTransactionEntity> findByPlatformAndStatus(PaymentPlatform platform, TransactionStatus status);

    /**
     * 分页查询 - 多条件
     */
    @Query("SELECT t FROM PaymentTransactionEntity t WHERE " +
            "(:orderId IS NULL OR t.orderId = :orderId) AND " +
            "(:tradeNo IS NULL OR t.tradeNo = :tradeNo) AND " +
            "(:platform IS NULL OR t.platform = :platform) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:merchantId IS NULL OR t.merchantId = :merchantId) AND " +
            "(:startTime IS NULL OR t.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR t.createdAt <= :endTime) " +
            "ORDER BY t.createdAt DESC")
    Page<PaymentTransactionEntity> findByConditions(
            @Param("orderId") String orderId,
            @Param("tradeNo") String tradeNo,
            @Param("platform") PaymentPlatform platform,
            @Param("status") TransactionStatus status,
            @Param("merchantId") String merchantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 查询超时未支付的订单（用于定时关闭）
     */
    @Query("SELECT t FROM PaymentTransactionEntity t WHERE " +
            "t.status = :status AND t.createdAt < :expireTime")
    List<PaymentTransactionEntity> findExpiredOrders(
            @Param("status") TransactionStatus status,
            @Param("expireTime") LocalDateTime expireTime
    );

    /**
     * 统计某时间段内的交易数量
     */
    @Query("SELECT COUNT(t) FROM PaymentTransactionEntity t WHERE " +
            "t.status = :status AND " +
            "t.createdAt BETWEEN :startTime AND :endTime")
    long countByStatusAndTimeRange(
            @Param("status") TransactionStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据商户ID查询
     */
    Page<PaymentTransactionEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);
}
