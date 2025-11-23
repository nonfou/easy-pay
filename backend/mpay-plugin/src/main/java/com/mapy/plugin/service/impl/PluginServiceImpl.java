package com.mapy.plugin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapy.common.response.PageResponse;
import com.mapy.plugin.dto.PluginCreateRequest;
import com.mapy.plugin.dto.PluginView;
import com.mapy.plugin.entity.PluginEntity;
import com.mapy.plugin.repository.PluginRepository;
import com.mapy.plugin.service.PluginService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PluginServiceImpl implements PluginService {

    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper;

    public PluginServiceImpl(PluginRepository pluginRepository, ObjectMapper objectMapper) {
        this.pluginRepository = pluginRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResponse<PluginView> listPlugins(Integer show, int page, int pageSize) {
        PageRequest request = PageRequest.of(page - 1, pageSize);
        Page<PluginEntity> result = pluginRepository.findAll(request);
        List<PluginEntity> filtered = result.getContent().stream()
                .filter(plugin -> {
                    if (show == null || show == 0) return true;
                    if (show == 1) return plugin.getInstall();
                    if (show == 2) return !plugin.getInstall();
                    return true;
                })
                .collect(Collectors.toList());
        List<PluginView> views = filtered.stream()
                .map(this::toView)
                .collect(Collectors.toList());
        return PageResponse.of(page, pageSize, result.getTotalElements(), views);
    }

    @Override
    @Transactional
    public PluginView addOrUpdate(PluginCreateRequest request) {
        PluginEntity entity = pluginRepository.findById(request.getPlatform())
                .orElseGet(PluginEntity::new);
        entity.setPlatform(request.getPlatform());
        entity.setName(request.getName());
        entity.setClassName(request.getClassName());
        entity.setPrice(request.getPrice());
        entity.setDescribe(request.getDescribe());
        entity.setWebsite(request.getWebsite());
        entity.setState(1);
        entity.setInstall(true);
        entity.setQuery(serializeQuery(request.getQuery()));
        return toView(pluginRepository.save(entity));
    }

    @Override
    @Transactional
    public void uninstall(String platform) {
        pluginRepository.deleteById(platform);
    }

    @Override
    @Transactional
    public void setState(String platform, Integer state) {
        pluginRepository.findById(platform).ifPresent(plugin -> {
            plugin.setState(state);
            pluginRepository.save(plugin);
        });
    }

    @Override
    public List<PluginView> syncFromMarketplace() {
        // TODO: 调用远程市场接口，目前返回本地已安装插件
        return pluginRepository.findAll().stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    private PluginView toView(PluginEntity entity) {
        return PluginView.builder()
                .platform(entity.getPlatform())
                .name(entity.getName())
                .className(entity.getClassName())
                .price(entity.getPrice())
                .describe(entity.getDescribe())
                .website(entity.getWebsite())
                .state(entity.getState())
                .install(entity.getInstall())
                .query(deserializeQuery(entity.getQuery()))
                .build();
    }

    private String serializeQuery(Map<String, Object> query) {
        try {
            return objectMapper.writeValueAsString(query == null ? Collections.emptyMap() : query);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> deserializeQuery(String json) {
        try {
            return json == null ? Collections.emptyMap()
                    : objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
