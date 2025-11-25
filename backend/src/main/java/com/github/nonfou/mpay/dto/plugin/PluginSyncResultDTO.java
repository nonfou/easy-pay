package com.github.nonfou.mpay.dto.plugin;

import lombok.Builder;
import lombok.Data;

/**
 * 插件同步结果 DTO
 */
@Data
@Builder
public class PluginSyncResultDTO {

    /** 新增插件数 */
    private Integer addedCount;

    /** 更新插件数 */
    private Integer updatedCount;

    /** 远程可用插件数 */
    private Integer availableCount;

    /** 本地已安装数 */
    private Integer installedCount;

    /** 同步消息 */
    private String message;
}
