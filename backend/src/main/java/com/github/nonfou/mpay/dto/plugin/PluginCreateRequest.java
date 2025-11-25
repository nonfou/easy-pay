package com.github.nonfou.mpay.dto.plugin;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

@Data
public class PluginCreateRequest {
    @NotBlank
    private String platform;

    @NotBlank
    private String name;

    @NotBlank
    private String className;

    private String price;

    private String describe;

    private String website;

    private Map<String, Object> query;
}
