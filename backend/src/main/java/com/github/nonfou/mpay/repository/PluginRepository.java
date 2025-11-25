package com.github.nonfou.mpay.repository;

import com.github.nonfou.mpay.entity.PluginEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginRepository extends JpaRepository<PluginEntity, String> {

    List<PluginEntity> findByInstall(boolean install);
}
