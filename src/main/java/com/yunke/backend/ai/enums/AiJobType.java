package com.yunke.backend.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI 任务类型枚举
 * 对应 Prisma Schema 中的 AiJobType
 */
public enum AiJobType {
    /**
     * 音频转录
     */
    TRANSCRIPTION("transcription");

    private final String value;

    AiJobType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static AiJobType fromValue(String value) {
        for (AiJobType type : AiJobType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AiJobType: " + value);
    }
} 