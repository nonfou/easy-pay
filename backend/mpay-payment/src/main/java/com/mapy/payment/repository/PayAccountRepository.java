package com.mapy.payment.repository;

import com.mapy.payment.entity.PayAccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayAccountRepository extends JpaRepository<PayAccountEntity, Long> {

    List<PayAccountEntity> findByPidAndState(Long pid, Integer state);

    Optional<PayAccountEntity> findByIdAndPid(Long id, Long pid);
}
