package com.mapy.plugin.dto;

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
}
