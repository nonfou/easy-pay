package com.mapy.plugin.controller;

import com.mapy.common.response.ApiResponse;
import com.mapy.common.response.PageResponse;
import com.mapy.plugin.dto.PluginCreateRequest;
import com.mapy.plugin.dto.PluginView;
import com.mapy.plugin.service.PluginService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginService pluginService;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PluginView>> list(
            @RequestParam(defaultValue = "0") Integer show,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(pluginService.listPlugins(show, page, pageSize));
    }

    @PostMapping
    public ApiResponse<PluginView> add(@RequestBody @Valid PluginCreateRequest request) {
        return ApiResponse.success(pluginService.addOrUpdate(request));
    }

    @PatchMapping("/{platform}/state")
    public ApiResponse<Void> changeState(
            @PathVariable String platform,
            @RequestParam Integer state) {
        pluginService.setState(platform, state);
        return ApiResponse.success();
    }

    @DeleteMapping("/{platform}")
    public ApiResponse<Void> uninstall(@PathVariable String platform) {
        pluginService.uninstall(platform);
        return ApiResponse.success();
    }

    @PostMapping("/sync")
    public ApiResponse<List<PluginView>> sync() {
        return ApiResponse.success(pluginService.syncFromMarketplace());
    }
}
