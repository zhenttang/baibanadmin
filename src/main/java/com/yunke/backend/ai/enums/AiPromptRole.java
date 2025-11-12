package com.yunke.backend.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI提示角色枚举
 * 对应 Prisma Schema 中的 AiPromptRole
 */
public enum AiPromptRole {
    /**
     * 系统角色
     */
    SYSTEM("system"),
    
    /**
     * 助手角色
     */
    ASSISTANT("assistant"),
    
    /**
     * 用户角色
     */
    USER("user");

    private final String value;

    AiPromptRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static AiPromptRole fromValue(String value) {
        for (AiPromptRole role : AiPromptRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown AiPromptRole: " + value);
    }
} 