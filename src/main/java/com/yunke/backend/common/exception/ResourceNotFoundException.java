package com.yunke.backend.common.exception;

/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, cause);
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", 
            String.format("%s not found: %s", resourceType, resourceId),
            resourceType, resourceId);
    }
}

