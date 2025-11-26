package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.OrderEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    Optional<OrderEntity> findByOutTradeNoAndPid(String outTradeNo, Long pid);

    List<OrderEntity> findByAidAndCidAndTypeAndState(Long aid, Long cid, String type, Integer state);

    List<OrderEntity> findByPidAndAidAndTypeAndState(Long pid, Long aid, String type, Integer state);

    // P0: 批量删除超时订单
    @Modifying
    @Query("DELETE FROM OrderEntity o WHERE o.state = 0 AND o.createTime < :expireTime")
    int deleteExpiredOrders(@Param("expireTime") LocalDateTime expireTime);

    // P0: 查询超时订单数量
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.state = 0 AND o.createTime < :expireTime")
    long countExpiredOrders(@Param("expireTime") LocalDateTime expireTime);

    // P1: 统计分析 - 按时间范围查询成交订单
    @Query("SELECT COALESCE(SUM(o.reallyPrice), 0) FROM OrderEntity o WHERE o.state = 1 " +
            "AND o.payTime >= :startTime AND o.payTime < :endTime " +
            "AND (:pid IS NULL OR o.pid = :pid)")
    Double sumRevenueByTimeRange(@Param("pid") Long pid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.state = 1 " +
            "AND o.payTime >= :startTime AND o.payTime < :endTime " +
            "AND (:pid IS NULL OR o.pid = :pid)")
    Long countSuccessOrdersByTimeRange(@Param("pid") Long pid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(o) FROM OrderEntity o " +
            "WHERE o.createTime >= :startTime AND o.createTime < :endTime " +
            "AND (:pid IS NULL OR o.pid = :pid)")
    Long countAllOrdersByTimeRange(@Param("pid") Long pid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // P1: 按支付类型统计
    @Query("SELECT o.type, COUNT(o), COALESCE(SUM(o.reallyPrice), 0) FROM OrderEntity o " +
            "WHERE o.state = 1 AND o.payTime >= :startTime AND o.payTime < :endTime " +
            "AND (:pid IS NULL OR o.pid = :pid) GROUP BY o.type")
    List<Object[]> countByPaymentType(@Param("pid") Long pid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // P1: 按日期统计订单趋势
    @Query("SELECT CAST(o.createTime AS LocalDate), COUNT(o), " +
            "SUM(CASE WHEN o.state = 1 THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN o.state = 1 THEN o.reallyPrice ELSE 0 END), 0) " +
            "FROM OrderEntity o WHERE o.createTime >= :startTime " +
            "AND (:pid IS NULL OR o.pid = :pid) " +
            "GROUP BY CAST(o.createTime AS LocalDate) ORDER BY CAST(o.createTime AS LocalDate)")
    List<Object[]> getOrderTrendByDate(@Param("pid") Long pid, @Param("startTime") LocalDateTime startTime);

    // P1: 订单作用域查询 - 活跃订单（有效期内未支付）
    @Query("SELECT o FROM OrderEntity o WHERE o.state = 0 AND o.createTime >= :expireTime " +
            "AND (:pid IS NULL OR o.pid = :pid) ORDER BY o.createTime DESC")
    List<OrderEntity> findActiveOrders(@Param("pid") Long pid, @Param("expireTime") LocalDateTime expireTime);

    // P1: 订单作用域查询 - 活跃订单（分页）
    @Query("SELECT o FROM OrderEntity o WHERE o.state = 0 AND o.createTime >= :expireTime " +
            "AND (:pid IS NULL OR o.pid = :pid)")
    Page<OrderEntity> findActiveOrdersPage(@Param("pid") Long pid, @Param("expireTime") LocalDateTime expireTime, Pageable pageable);

    // P1: 订单作用域查询 - 成交订单
    @Query("SELECT o FROM OrderEntity o WHERE o.state = 1 " +
            "AND o.payTime >= :startTime AND o.payTime < :endTime " +
            "AND (:pid IS NULL OR o.pid = :pid) ORDER BY o.payTime DESC")
    List<OrderEntity> findSuccessOrders(@Param("pid") Long pid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // P1: 订单作用域查询 - 成交订单（分页）
    @Query("SELECT o FROM OrderEntity o WHERE o.state = 1 " +
            "AND o.payTime >= :startTime AND o.payTime < :endTime " +
            "AND (:pid IS NULL OR o.pid = :pid)")
    Page<OrderEntity> findSuccessOrdersPage(@Param("pid") Long pid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    // P1: 订单作用域查询 - 超时订单
    @Query("SELECT o FROM OrderEntity o WHERE o.state = 0 AND o.createTime < :expireTime " +
            "AND (:pid IS NULL OR o.pid = :pid) ORDER BY o.createTime DESC")
    List<OrderEntity> findExpiredOrders(@Param("pid") Long pid, @Param("expireTime") LocalDateTime expireTime);

    // P1: 订单作用域查询 - 超时订单（分页）
    @Query("SELECT o FROM OrderEntity o WHERE o.state = 0 AND o.createTime < :expireTime " +
            "AND (:pid IS NULL OR o.pid = :pid)")
    Page<OrderEntity> findExpiredOrdersPage(@Param("pid") Long pid, @Param("expireTime") LocalDateTime expireTime, Pageable pageable);

    // P1: 按账号ID查询交易流水
    @Query("SELECT o FROM OrderEntity o WHERE o.aid = :accountId " +
            "AND o.createTime >= :startTime AND o.createTime < :endTime " +
            "ORDER BY o.createTime DESC")
    List<OrderEntity> findByAccountIdAndTimeRange(@Param("accountId") Long accountId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // P1: 按账号ID统计成交金额
    @Query("SELECT COALESCE(SUM(o.reallyPrice), 0) FROM OrderEntity o " +
            "WHERE o.aid = :accountId AND o.state = 1 " +
            "AND o.payTime >= :startTime AND o.payTime < :endTime")
    Double sumRevenueByAccountId(@Param("accountId") Long accountId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
