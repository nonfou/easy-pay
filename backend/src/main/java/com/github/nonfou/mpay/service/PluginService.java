package com.github.nonfou.mpay.service;

import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.plugin.PluginCreateRequest;
import com.github.nonfou.mpay.dto.plugin.PluginView;
import java.util.List;

public interface PluginService {

    PageResponse<PluginView> listPlugins(Integer show, int page, int pageSize);

    PluginView addOrUpdate(PluginCreateRequest request);

    void uninstall(String platform);

    void setState(String platform, Integer state);

    List<PluginView> syncFromMarketplace();
}
