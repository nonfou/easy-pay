package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.MerchantEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MerchantRepository extends JpaRepository<MerchantEntity, Long> {

    Optional<MerchantEntity> findByPid(Long pid);

    Optional<MerchantEntity> findByUsername(String username);

    // P2: 按角色和状态查询
    @Query("SELECT m FROM MerchantEntity m WHERE " +
            "(:role IS NULL OR m.role = :role) AND " +
            "(:state IS NULL OR m.state = :state)")
    Page<MerchantEntity> findByRoleAndState(@Param("role") Integer role,
            @Param("state") Integer state, Pageable pageable);

    // P2: 查询所有管理员
    List<MerchantEntity> findByRole(Integer role);
}
