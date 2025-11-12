package com.yunke.backend.notification.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知级别枚举
 * 对应 Prisma Schema 中的 NotificationLevel
 */
public enum NotificationLevel {
    /**
     * 高级别 - 发出声音并显示为抬头通知
     */
    HIGH("High"),
    
    /**
     * 默认级别 - 发出声音
     */
    DEFAULT("Default"),
    
    /**
     * 低级别 - 不发出声音
     */
    LOW("Low"),
    
    /**
     * 最小级别
     */
    MIN("Min"),
    
    /**
     * 无通知
     */
    NONE("None");

    private final String value;

    NotificationLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static NotificationLevel fromValue(String value) {
        for (NotificationLevel level : NotificationLevel.values()) {
            if (level.value.equals(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationLevel: " + value);
    }
} 