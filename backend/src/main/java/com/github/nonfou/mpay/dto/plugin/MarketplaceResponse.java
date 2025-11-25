package com.github.nonfou.mpay.dto.plugin;

import java.util.List;
import lombok.Data;

/**
 * 远程插件市场响应 DTO
 */
@Data
public class MarketplaceResponse {

    private Integer code;
    private String msg;
    private List<MarketplacePlugin> data;

    @Data
    public static class MarketplacePlugin {
        private String platform;
        private String name;
        private String className;
        private Double price;
        private String describe;
        private String website;
        private String version;
        private String author;
        private String downloadUrl;
    }
}
