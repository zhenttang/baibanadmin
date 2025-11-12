package com.yunke.backend.workspace.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 工作空间成员来源枚举
 * 对应 Prisma Schema 中的 WorkspaceMemberSource
 */
public enum WorkspaceMemberSource {
    /**
     * 通过电子邮件邀请
     */
    EMAIL("Email"),
    
    /**
     * 通过链接邀请
     */
    LINK("Link");

    private final String value;

    WorkspaceMemberSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static WorkspaceMemberSource fromValue(String value) {
        for (WorkspaceMemberSource source : WorkspaceMemberSource.values()) {
            if (source.value.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown WorkspaceMemberSource: " + value);
    }
} 