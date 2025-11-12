package com.yunke.backend.common.exception;

/**
 * 验证异常
 * 当输入数据验证失败时抛出
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, cause);
    }
    
    public ValidationException(String field, String message) {
        super("VALIDATION_ERROR", 
            String.format("Validation failed for field '%s': %s", field, message),
            field, message);
    }
}

