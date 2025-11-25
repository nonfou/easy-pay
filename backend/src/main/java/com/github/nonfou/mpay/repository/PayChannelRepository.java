package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.PayChannelEntity;
import com.github.nonfou.mpay.entity.PayAccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayChannelRepository extends JpaRepository<PayChannelEntity, Long> {

    // 来自 mpay-merchant: 按账号查询通道
    List<PayChannelEntity> findByAccount(PayAccountEntity account);

    // 按账号ID查询通道
    List<PayChannelEntity> findByAccountId(Long accountId);

    // 批量统计多个账号的通道数量，避免 N+1 查询
    @Query("SELECT c.account.id, COUNT(c) FROM PayChannelEntity c WHERE c.account.id IN :accountIds GROUP BY c.account.id")
    List<Object[]> countChannelsByAccountIds(@Param("accountIds") List<Long> accountIds);
}
