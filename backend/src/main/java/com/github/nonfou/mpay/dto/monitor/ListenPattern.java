package com.github.nonfou.mpay.dto.monitor;

/**
 * 监听模式枚举
 */
public enum ListenPattern {

    /**
     * 被动监听 - 等待推送回调
     */
    PASSIVE(0, "被动监听", "等待平台推送支付通知"),

    /**
     * 主动监听 - 定时查询
     */
    ACTIVE(1, "主动监听", "主动查询平台支付记录");

    private final int code;
    private final String name;
    private final String description;

    ListenPattern(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static ListenPattern fromCode(int code) {
        for (ListenPattern pattern : values()) {
            if (pattern.code == code) {
                return pattern;
            }
        }
        return PASSIVE; // 默认被动
    }
}
