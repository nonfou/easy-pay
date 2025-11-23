package com.mapy.plugin.service;

import com.mapy.common.response.PageResponse;
import com.mapy.plugin.dto.PluginCreateRequest;
import com.mapy.plugin.dto.PluginView;
import java.util.List;

public interface PluginService {

    PageResponse<PluginView> listPlugins(Integer show, int page, int pageSize);

    PluginView addOrUpdate(PluginCreateRequest request);

    void uninstall(String platform);

    void setState(String platform, Integer state);

    List<PluginView> syncFromMarketplace();
}
