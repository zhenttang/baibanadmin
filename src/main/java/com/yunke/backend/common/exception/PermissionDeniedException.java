package com.yunke.backend.common.exception;

/**
 * 权限拒绝异常
 * 当用户尝试执行没有权限的操作时抛出
 */
public class PermissionDeniedException extends BusinessException {
    
    public PermissionDeniedException(String message) {
        super("PERMISSION_DENIED", message);
    }
    
    public PermissionDeniedException(String message, Throwable cause) {
        super("PERMISSION_DENIED", message, cause);
    }
    
    public PermissionDeniedException(String userId, String resource, String action) {
        super("PERMISSION_DENIED", 
            String.format("User %s does not have permission for action %s on resource %s", 
                userId, action, resource),
            userId, resource, action);
    }
}

