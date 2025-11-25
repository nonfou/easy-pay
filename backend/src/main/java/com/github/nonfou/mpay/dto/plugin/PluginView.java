package com.github.nonfou.mpay.dto.plugin;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PluginView {
    String platform;
    String name;
    String className;
    String price;
    String describe;
    String website;
    Integer state;
    Boolean install;
    Map<String, Object> query;

    // P2: 远程插件信息
    String version;
    String author;
    String downloadUrl;
    Boolean hasUpdate;  // 是否有更新
}
