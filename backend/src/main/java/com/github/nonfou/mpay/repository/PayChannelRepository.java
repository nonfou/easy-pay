package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.entity.PayAccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayChannelRepository extends JpaRepository<PayChannelEntity, Long> {

    // 来自 mpay-merchant: 按账号查询通道
    List<PayChannelEntity> findByAccount(PayAccountEntity account);

    // 按账号ID查询通道
    List<PayChannelEntity> findByAccountId(Long accountId);
}
