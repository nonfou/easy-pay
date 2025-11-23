package com.mapy.payment.repository;

import com.mapy.payment.entity.PayChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayChannelRepository extends JpaRepository<PayChannelEntity, Long> {
}
