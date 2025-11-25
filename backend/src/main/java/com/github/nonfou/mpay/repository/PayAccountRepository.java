package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.PayAccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayAccountRepository extends JpaRepository<PayAccountEntity, Long> {

    // 来自 mpay-payment
    List<PayAccountEntity> findByPidAndState(Long pid, Integer state);

    Optional<PayAccountEntity> findByIdAndPid(Long id, Long pid);

    // 来自 mpay-merchant
    List<PayAccountEntity> findByPid(Long pid);

    List<PayAccountEntity> findByPidAndPlatformContainingIgnoreCase(Long pid, String platform);

    // P2: 按监听模式查询
    List<PayAccountEntity> findByPattern(Integer pattern);

    @Query("SELECT a FROM PayAccountEntity a WHERE " +
            "(:pid IS NULL OR a.pid = :pid) AND a.pattern = :pattern AND a.state = 1")
    List<PayAccountEntity> findActiveByPattern(@Param("pid") Long pid, @Param("pattern") Integer pattern);

    @Query("SELECT a FROM PayAccountEntity a WHERE " +
            "(:pid IS NULL OR a.pid = :pid) AND a.state = 1")
    List<PayAccountEntity> findActiveAccounts(@Param("pid") Long pid);
}
