package com.mapy.merchant.repository;

import com.mapy.merchant.entity.PayAccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayAccountRepository extends JpaRepository<PayAccountEntity, Long> {

    List<PayAccountEntity> findByPid(Long pid);

    List<PayAccountEntity> findByPidAndPlatformContainingIgnoreCase(Long pid, String platform);
}
