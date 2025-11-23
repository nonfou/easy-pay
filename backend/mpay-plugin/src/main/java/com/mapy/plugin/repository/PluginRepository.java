package com.mapy.plugin.repository;

import com.mapy.plugin.entity.PluginEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginRepository extends JpaRepository<PluginEntity, String> {

    List<PluginEntity> findByInstall(boolean install);
}
