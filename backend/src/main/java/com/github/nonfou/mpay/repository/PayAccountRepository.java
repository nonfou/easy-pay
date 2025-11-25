package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.PayAccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 数据库分页查询，避免内存分页
    @Query("SELECT a FROM PayAccountEntity a WHERE a.pid = :pid " +
            "AND (:platform IS NULL OR a.platform LIKE %:platform%) " +
            "AND (:state IS NULL OR a.state = :state) " +
            "AND (:pattern IS NULL OR a.pattern = :pattern)")
    Page<PayAccountEntity> findByConditions(
            @Param("pid") Long pid,
            @Param("platform") String platform,
            @Param("state") Integer state,
            @Param("pattern") Integer pattern,
            Pageable pageable);
}
