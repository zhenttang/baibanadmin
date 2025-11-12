package com.yunke.backend.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI 任务状态枚举
 * 对应 Prisma Schema 中的 AiJobStatus
 */
public enum AiJobStatus {
    /**
     * 待处理
     */
    PENDING("pending"),
    
    /**
     * 运行中
     */
    RUNNING("running"),
    
    /**
     * 已完成
     */
    FINISHED("finished"),
    
    /**
     * 已认领
     */
    CLAIMED("claimed"),
    
    /**
     * 失败
     */
    FAILED("failed");

    private final String value;

    AiJobStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static AiJobStatus fromValue(String value) {
        for (AiJobStatus status : AiJobStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown AiJobStatus: " + value);
    }
} 