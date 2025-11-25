package com.github.nonfou.mpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.plugin.PluginCreateRequest;
import com.github.nonfou.mpay.dto.plugin.PluginView;
import com.github.nonfou.mpay.entity.PluginEntity;
import com.github.nonfou.mpay.repository.PluginRepository;
import com.github.nonfou.mpay.service.impl.PluginServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PluginService 单元测试
 * 测试插件管理服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PluginService 插件管理服务测试")
class PluginServiceTest {

    @Mock
    private PluginRepository pluginRepository;

    private ObjectMapper objectMapper;

    private PluginServiceImpl pluginService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pluginService = new PluginServiceImpl(pluginRepository, objectMapper);
    }

    @Nested
    @DisplayName("插件列表查询测试")
    class ListPluginsTests {

        @Test
        @DisplayName("查询所有插件 - show=0")
        void listPlugins_shouldReturnAllPlugins_whenShowIsZero() {
            // Given
            PluginEntity plugin1 = createPlugin("wxpay", "微信支付", true);
            PluginEntity plugin2 = createPlugin("alipay", "支付宝", false);
            List<PluginEntity> plugins = List.of(plugin1, plugin2);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 2);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(0, 1, 10);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getItems().size());
            assertEquals(2L, result.getTotal());
        }

        @Test
        @DisplayName("查询已安装插件 - show=1")
        void listPlugins_shouldReturnInstalledOnly_whenShowIsOne() {
            // Given
            PluginEntity plugin1 = createPlugin("wxpay", "微信支付", true);
            PluginEntity plugin2 = createPlugin("alipay", "支付宝", false);
            List<PluginEntity> plugins = List.of(plugin1, plugin2);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 2);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(1, 1, 10);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getItems().size());
            assertEquals("wxpay", result.getItems().get(0).getPlatform());
        }

        @Test
        @DisplayName("查询未安装插件 - show=2")
        void listPlugins_shouldReturnNotInstalledOnly_whenShowIsTwo() {
            // Given
            PluginEntity plugin1 = createPlugin("wxpay", "微信支付", true);
            PluginEntity plugin2 = createPlugin("alipay", "支付宝", false);
            List<PluginEntity> plugins = List.of(plugin1, plugin2);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 2);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(2, 1, 10);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getItems().size());
            assertEquals("alipay", result.getItems().get(0).getPlatform());
        }

        @Test
        @DisplayName("查询插件 - show为null时返回全部")
        void listPlugins_shouldReturnAll_whenShowIsNull() {
            // Given
            PluginEntity plugin1 = createPlugin("wxpay", "微信支付", true);
            List<PluginEntity> plugins = List.of(plugin1);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 1);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(null, 1, 10);

            // Then
            assertEquals(1, result.getItems().size());
        }
    }

    @Nested
    @DisplayName("添加或更新插件测试")
    class AddOrUpdatePluginTests {

        @Test
        @DisplayName("添加新插件")
        void addOrUpdate_shouldCreateNewPlugin_whenNotExists() {
            // Given
            PluginCreateRequest request = new PluginCreateRequest();
            request.setPlatform("wxpay");
            request.setName("微信支付");
            request.setClassName("com.wxpay.WxPayPlugin");
            request.setPrice("免费");
            request.setDescribe("微信支付插件");
            request.setWebsite("https://pay.weixin.qq.com");
            request.setQuery(Map.of("appId", "wx123456"));

            when(pluginRepository.findById("wxpay")).thenReturn(Optional.empty());
            when(pluginRepository.save(any(PluginEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            PluginView result = pluginService.addOrUpdate(request);

            // Then
            assertNotNull(result);
            assertEquals("wxpay", result.getPlatform());
            assertEquals("微信支付", result.getName());
            assertTrue(result.getInstall());
            assertEquals(1, result.getState());

            ArgumentCaptor<PluginEntity> captor = ArgumentCaptor.forClass(PluginEntity.class);
            verify(pluginRepository).save(captor.capture());

            PluginEntity saved = captor.getValue();
            assertEquals("com.wxpay.WxPayPlugin", saved.getClassName());
            assertNotNull(saved.getQuery());
        }

        @Test
        @DisplayName("更新已存在的插件")
        void addOrUpdate_shouldUpdateExistingPlugin() {
            // Given
            PluginEntity existing = createPlugin("wxpay", "微信支付旧版", true);

            PluginCreateRequest request = new PluginCreateRequest();
            request.setPlatform("wxpay");
            request.setName("微信支付新版");
            request.setClassName("com.wxpay.WxPayPluginV2");

            when(pluginRepository.findById("wxpay")).thenReturn(Optional.of(existing));
            when(pluginRepository.save(any(PluginEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            PluginView result = pluginService.addOrUpdate(request);

            // Then
            assertEquals("微信支付新版", result.getName());

            ArgumentCaptor<PluginEntity> captor = ArgumentCaptor.forClass(PluginEntity.class);
            verify(pluginRepository).save(captor.capture());

            assertEquals("com.wxpay.WxPayPluginV2", captor.getValue().getClassName());
        }

        @Test
        @DisplayName("添加插件 - query为null时序列化为空对象")
        void addOrUpdate_shouldSerializeNullQueryAsEmptyObject() {
            // Given
            PluginCreateRequest request = new PluginCreateRequest();
            request.setPlatform("wxpay");
            request.setName("微信支付");
            request.setQuery(null);

            when(pluginRepository.findById("wxpay")).thenReturn(Optional.empty());
            when(pluginRepository.save(any(PluginEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            pluginService.addOrUpdate(request);

            // Then
            ArgumentCaptor<PluginEntity> captor = ArgumentCaptor.forClass(PluginEntity.class);
            verify(pluginRepository).save(captor.capture());

            assertEquals("{}", captor.getValue().getQuery());
        }
    }

    @Nested
    @DisplayName("卸载插件测试")
    class UninstallPluginTests {

        @Test
        @DisplayName("卸载插件成功")
        void uninstall_shouldDeletePlugin() {
            // When
            pluginService.uninstall("wxpay");

            // Then
            verify(pluginRepository).deleteById("wxpay");
        }
    }

    @Nested
    @DisplayName("设置插件状态测试")
    class SetStateTests {

        @Test
        @DisplayName("启用插件")
        void setState_shouldEnablePlugin() {
            // Given
            PluginEntity plugin = createPlugin("wxpay", "微信支付", true);
            plugin.setState(0);

            when(pluginRepository.findById("wxpay")).thenReturn(Optional.of(plugin));
            when(pluginRepository.save(any(PluginEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            pluginService.setState("wxpay", 1);

            // Then
            ArgumentCaptor<PluginEntity> captor = ArgumentCaptor.forClass(PluginEntity.class);
            verify(pluginRepository).save(captor.capture());

            assertEquals(1, captor.getValue().getState());
        }

        @Test
        @DisplayName("禁用插件")
        void setState_shouldDisablePlugin() {
            // Given
            PluginEntity plugin = createPlugin("wxpay", "微信支付", true);
            plugin.setState(1);

            when(pluginRepository.findById("wxpay")).thenReturn(Optional.of(plugin));
            when(pluginRepository.save(any(PluginEntity.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            pluginService.setState("wxpay", 0);

            // Then
            ArgumentCaptor<PluginEntity> captor = ArgumentCaptor.forClass(PluginEntity.class);
            verify(pluginRepository).save(captor.capture());

            assertEquals(0, captor.getValue().getState());
        }

        @Test
        @DisplayName("设置状态 - 插件不存在时不执行操作")
        void setState_shouldDoNothing_whenPluginNotFound() {
            // Given
            when(pluginRepository.findById("unknown")).thenReturn(Optional.empty());

            // When
            pluginService.setState("unknown", 1);

            // Then
            verify(pluginRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("插件视图转换测试")
    class PluginViewConversionTests {

        @Test
        @DisplayName("转换插件实体到视图 - 完整数据")
        void toView_shouldConvertAllFields() {
            // Given
            PluginEntity plugin = createPlugin("wxpay", "微信支付", true);
            plugin.setClassName("com.wxpay.Plugin");
            plugin.setPrice("免费");
            plugin.setDescribe("微信支付插件描述");
            plugin.setWebsite("https://pay.weixin.qq.com");
            plugin.setVersion("1.0.0");
            plugin.setAuthor("Tencent");
            plugin.setDownloadUrl("https://example.com/download/wxpay.jar");
            plugin.setQuery("{\"appId\":\"wx123\"}");

            when(pluginRepository.findById("wxpay")).thenReturn(Optional.empty());
            when(pluginRepository.save(any(PluginEntity.class))).thenReturn(plugin);

            PluginCreateRequest request = new PluginCreateRequest();
            request.setPlatform("wxpay");
            request.setName("微信支付");

            // When
            PluginView result = pluginService.addOrUpdate(request);

            // Then
            assertNotNull(result);
            assertEquals("wxpay", result.getPlatform());
            assertEquals("微信支付", result.getName());
        }

        @Test
        @DisplayName("反序列化query - 有效JSON")
        void deserializeQuery_shouldParseValidJson() {
            // Given
            PluginEntity plugin = createPlugin("wxpay", "微信支付", true);
            plugin.setQuery("{\"appId\":\"wx123\",\"secret\":\"abc\"}");

            List<PluginEntity> plugins = List.of(plugin);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 1);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(null, 1, 10);

            // Then
            PluginView view = result.getItems().get(0);
            assertNotNull(view.getQuery());
            assertEquals("wx123", view.getQuery().get("appId"));
        }

        @Test
        @DisplayName("反序列化query - 无效JSON返回空Map")
        void deserializeQuery_shouldReturnEmptyMap_whenInvalidJson() {
            // Given
            PluginEntity plugin = createPlugin("wxpay", "微信支付", true);
            plugin.setQuery("invalid json {{{");

            List<PluginEntity> plugins = List.of(plugin);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 1);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(null, 1, 10);

            // Then
            PluginView view = result.getItems().get(0);
            assertNotNull(view.getQuery());
            assertTrue(view.getQuery().isEmpty());
        }

        @Test
        @DisplayName("反序列化query - null返回空Map")
        void deserializeQuery_shouldReturnEmptyMap_whenNull() {
            // Given
            PluginEntity plugin = createPlugin("wxpay", "微信支付", true);
            plugin.setQuery(null);

            List<PluginEntity> plugins = List.of(plugin);
            Page<PluginEntity> page = new PageImpl<>(plugins, PageRequest.of(0, 10), 1);

            when(pluginRepository.findAll(any(PageRequest.class))).thenReturn(page);

            // When
            PageResponse<PluginView> result = pluginService.listPlugins(null, 1, 10);

            // Then
            PluginView view = result.getItems().get(0);
            assertTrue(view.getQuery().isEmpty());
        }
    }

    // ==================== 辅助方法 ====================

    private PluginEntity createPlugin(String platform, String name, boolean installed) {
        PluginEntity entity = new PluginEntity();
        entity.setPlatform(platform);
        entity.setName(name);
        entity.setInstall(installed);
        entity.setState(1);
        entity.setQuery("{}");
        return entity;
    }
}
