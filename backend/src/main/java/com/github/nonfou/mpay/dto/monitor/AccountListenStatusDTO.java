package com.github.nonfou.mpay.dto.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * 账号监听状态 DTO
 */
@Data
@Builder
public class AccountListenStatusDTO {

    /** 账号ID */
    private Long accountId;

    /** 账号名 */
    private String account;

    /** 平台 */
    private String platform;

    /** 监听模式 */
    private Integer pattern;

    /** 模式名称 */
    private String patternName;

    /** 模式描述 */
    private String patternDescription;

    /** 账号状态 */
    private Integer state;

    /** 是否在线 */
    private Boolean online;

    /** 最后心跳时间 */
    private String lastHeartbeat;

    /** 活跃订单数 */
    private Integer activeOrderCount;
}
