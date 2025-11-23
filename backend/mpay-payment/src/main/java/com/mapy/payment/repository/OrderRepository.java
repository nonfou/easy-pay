package com.mapy.payment.repository;

import com.mapy.payment.entity.OrderEntity;
import com.mapy.payment.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    Optional<OrderEntity> findByOutTradeNoAndPid(String outTradeNo, Long pid);

    List<OrderEntity> findByAidAndCidAndTypeAndState(Long aid, Long cid, String type, Integer state);

    List<OrderEntity> findByPidAndAidAndTypeAndState(Long pid, Long aid, String type, Integer state);
}
