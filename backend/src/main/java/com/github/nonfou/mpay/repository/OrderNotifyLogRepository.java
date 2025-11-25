package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderNotifyLogRepository extends JpaRepository<OrderNotifyLogEntity, Long> {

    Optional<OrderNotifyLogEntity> findByOrderId(String orderId);

    /**
     * 查询待重试的通知记录 (status=0 且 nextRetryTime 已到且重试次数未超限)
     */
    @Query("SELECT n FROM OrderNotifyLogEntity n WHERE n.status = 0 " +
            "AND n.nextRetryTime <= :now AND n.retryCount < :maxRetries " +
            "ORDER BY n.nextRetryTime ASC")
    List<OrderNotifyLogEntity> findPendingRetries(@Param("now") LocalDateTime now,
            @Param("maxRetries") int maxRetries);
}
