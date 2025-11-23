package com.mapy.payment.repository;

import com.mapy.payment.entity.OrderNotifyLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNotifyLogRepository extends JpaRepository<OrderNotifyLogEntity, Long> {
}
