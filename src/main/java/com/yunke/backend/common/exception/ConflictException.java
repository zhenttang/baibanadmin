package com.yunke.backend.common.exception;

/**
 * 冲突异常
 * 当操作与当前状态冲突时抛出（如重复创建、并发冲突等）
 */
public class ConflictException extends BusinessException {
    
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super("CONFLICT", message, cause);
    }
    
    public ConflictException(String resourceType, String resourceId, String reason) {
        super("CONFLICT", 
            String.format("Conflict with %s %s: %s", resourceType, resourceId, reason),
            resourceType, resourceId, reason);
    }
}

