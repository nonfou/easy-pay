package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.OrderNotifyLogEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNotifyLogRepository extends JpaRepository<OrderNotifyLogEntity, Long> {

    Optional<OrderNotifyLogEntity> findByOrderId(String orderId);
}
