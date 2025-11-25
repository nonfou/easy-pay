package com.github.nonfou.mpay.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.plugin.MarketplaceResponse;
import com.github.nonfou.mpay.dto.plugin.PluginCreateRequest;
import com.github.nonfou.mpay.dto.plugin.PluginView;
import com.github.nonfou.mpay.entity.PluginEntity;
import com.github.nonfou.mpay.repository.PluginRepository;
import com.github.nonfou.mpay.service.PluginService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PluginServiceImpl implements PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Value("${mpay.plugin.marketplace-url:https://api.zhaidashi.cn/plugins}")
    private String marketplaceUrl;

    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public PluginServiceImpl(PluginRepository pluginRepository, ObjectMapper objectMapper) {
        this.pluginRepository = pluginRepository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "mpay-client/1.0")
                .build();
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
        log.info("开始从插件市场同步: {}", marketplaceUrl);

        List<PluginView> result = new ArrayList<>();

        try {
            // 调用远程市场接口
            MarketplaceResponse response = webClient.get()
                    .uri(marketplaceUrl)
                    .retrieve()
                    .bodyToMono(MarketplaceResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.getCode() != 0 || response.getData() == null) {
                log.warn("插件市场响应异常: {}", response);
                // 返回本地已安装插件
                return pluginRepository.findAll().stream()
                        .map(this::toView)
                        .collect(Collectors.toList());
            }

            // 获取本地已安装插件
            Set<String> installedPlatforms = pluginRepository.findAll().stream()
                    .map(PluginEntity::getPlatform)
                    .collect(Collectors.toSet());

            // 处理远程插件列表
            for (MarketplaceResponse.MarketplacePlugin remotePlugin : response.getData()) {
                boolean isInstalled = installedPlatforms.contains(remotePlugin.getPlatform());

                // 如果本地有，更新远程信息
                if (isInstalled) {
                    pluginRepository.findById(remotePlugin.getPlatform()).ifPresent(entity -> {
                        entity.setVersion(remotePlugin.getVersion());
                        entity.setAuthor(remotePlugin.getAuthor());
                        entity.setDownloadUrl(remotePlugin.getDownloadUrl());
                        pluginRepository.save(entity);
                    });
                }

                // 构建视图
                result.add(PluginView.builder()
                        .platform(remotePlugin.getPlatform())
                        .name(remotePlugin.getName())
                        .className(remotePlugin.getClassName())
                        .price(remotePlugin.getPrice() != null ? String.valueOf(remotePlugin.getPrice()) : "免费")
                        .describe(remotePlugin.getDescribe())
                        .website(remotePlugin.getWebsite())
                        .state(isInstalled ? 1 : 0)
                        .install(isInstalled)
                        .query(Collections.emptyMap())
                        .version(remotePlugin.getVersion())
                        .author(remotePlugin.getAuthor())
                        .downloadUrl(remotePlugin.getDownloadUrl())
                        .hasUpdate(false) // TODO: 版本比较
                        .build());
            }

            log.info("插件市场同步完成, 共 {} 个插件, 已安装 {} 个",
                    result.size(), installedPlatforms.size());

        } catch (Exception e) {
            log.error("插件市场同步失败", e);
            // 降级返回本地已安装插件
            return pluginRepository.findAll().stream()
                    .map(this::toView)
                    .collect(Collectors.toList());
        }

        return result;
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
                .version(entity.getVersion())
                .author(entity.getAuthor())
                .downloadUrl(entity.getDownloadUrl())
                .hasUpdate(false)
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
                    : objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
