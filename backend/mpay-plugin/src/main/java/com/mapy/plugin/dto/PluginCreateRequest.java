package com.mapy.plugin.dto;

import jakarta.validation.constraints.NotBlank;
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
}
