package com.yunke.backend.common.util;

import com.yunke.backend.common.exception.*;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 异常处理工具类
 * 
 * <p>提供常用的异常处理工具方法，减少重复代码。</p>
 * 
 * @author System
 * @since 2024-12-19
 */
public final class ExceptionUtils {
    
    private ExceptionUtils() {
        // 工具类，禁止实例化
    }
    
    /**
     * 如果条件为 false，则抛出 PermissionDeniedException
     * 
     * @param condition 条件
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param action 操作
     * @throws PermissionDeniedException 如果条件为 false
     */
    public static void requirePermission(boolean condition, String userId, String resourceType, String action) {
        if (!condition) {
            throw new PermissionDeniedException(userId, resourceType, action);
        }
    }
    
    /**
     * 如果条件为 false，则返回 Mono.error(PermissionDeniedException)
     * 
     * @param condition 条件
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param action 操作
     * @return Mono 如果条件为 true，否则 Mono.error(PermissionDeniedException)
     */
    public static Mono<Void> requirePermissionMono(boolean condition, String userId, String resourceType, String action) {
        if (condition) {
            return Mono.empty();
        }
        return Mono.error(new PermissionDeniedException(userId, resourceType, action));
    }
    
    /**
     * 如果 Optional 为空，则抛出 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param optional Optional 值
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 值
     * @throws ResourceNotFoundException 如果 Optional 为空
     */
    public static <T> T requirePresent(Optional<T> optional, String resourceType, String resourceId) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(resourceType, resourceId));
    }
    
    /**
     * 如果 Optional 为空，则返回 Mono.error(ResourceNotFoundException)
     * 
     * @param <T> 值类型
     * @param optional Optional 值
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return Mono 如果 Optional 有值，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T> Mono<T> requirePresentMono(Optional<T> optional, String resourceType, String resourceId) {
        return optional.map(Mono::just)
                .orElse(Mono.error(new ResourceNotFoundException(resourceType, resourceId)));
    }
    
    /**
     * 如果条件为 false，则抛出 ConflictException
     * 
     * @param condition 条件
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param reason 冲突原因
     * @throws ConflictException 如果条件为 false
     */
    public static void requireNotConflict(boolean condition, String resourceType, String resourceId, String reason) {
        if (!condition) {
            throw new ConflictException(resourceType, resourceId, reason);
        }
    }
    
    /**
     * 如果条件为 false，则返回 Mono.error(ConflictException)
     * 
     * @param condition 条件
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param reason 冲突原因
     * @return Mono 如果条件为 true，否则 Mono.error(ConflictException)
     */
    public static Mono<Void> requireNotConflictMono(boolean condition, String resourceType, String resourceId, String reason) {
        if (condition) {
            return Mono.empty();
        }
        return Mono.error(new ConflictException(resourceType, resourceId, reason));
    }
    
    /**
     * 如果条件为 false，则抛出 ValidationException
     * 
     * @param condition 条件
     * @param message 验证失败消息
     * @throws ValidationException 如果条件为 false
     */
    public static void requireValid(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
    
    /**
     * 如果条件为 false，则返回 Mono.error(ValidationException)
     * 
     * @param condition 条件
     * @param message 验证失败消息
     * @return Mono 如果条件为 true，否则 Mono.error(ValidationException)
     */
    public static Mono<Void> requireValidMono(boolean condition, String message) {
        if (condition) {
            return Mono.empty();
        }
        return Mono.error(new ValidationException(message));
    }
    
    /**
     * 如果值为 null，则抛出 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param value 值
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 值
     * @throws ResourceNotFoundException 如果值为 null
     */
    public static <T> T requireNotNull(T value, String resourceType, String resourceId) {
        if (value == null) {
            throw new ResourceNotFoundException(resourceType, resourceId);
        }
        return value;
    }
    
    /**
     * 如果值为 null，则返回 Mono.error(ResourceNotFoundException)
     * 
     * @param <T> 值类型
     * @param value 值
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return Mono 如果值不为 null，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T> Mono<T> requireNotNullMono(T value, String resourceType, String resourceId) {
        if (value == null) {
            return Mono.error(new ResourceNotFoundException(resourceType, resourceId));
        }
        return Mono.just(value);
    }
    
    /**
     * 如果条件为 false，则抛出 BusinessException
     * 
     * @param condition 条件
     * @param errorCode 错误代码
     * @param message 错误消息
     * @throws BusinessException 如果条件为 false
     */
    public static void requireBusinessRule(boolean condition, String errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode, message);
        }
    }
    
    /**
     * 如果条件为 false，则返回 Mono.error(BusinessException)
     * 
     * @param condition 条件
     * @param errorCode 错误代码
     * @param message 错误消息
     * @return Mono 如果条件为 true，否则 Mono.error(BusinessException)
     */
    public static Mono<Void> requireBusinessRuleMono(boolean condition, String errorCode, String message) {
        if (condition) {
            return Mono.empty();
        }
        return Mono.error(new BusinessException(errorCode, message));
    }
}

