package com.mapy.merchant.repository;

import com.mapy.merchant.entity.PayChannelEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayChannelRepository extends JpaRepository<PayChannelEntity, Long> {

    List<PayChannelEntity> findByAccountId(Long accountId);
}
