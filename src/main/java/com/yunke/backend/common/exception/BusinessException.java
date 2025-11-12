package com.yunke.backend.common.exception;

/**
 * 业务异常基类
 * 所有业务异常都应该继承此类
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = null;
    }
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public BusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getArgs() {
        return args;
    }
}
