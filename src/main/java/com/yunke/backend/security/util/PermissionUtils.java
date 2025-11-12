package com.yunke.backend.security.util;

import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.security.constants.PermissionActions;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * 权限检查工具类
 * 
 * <p>提供统一的权限检查方法，减少重复代码，提高代码可维护性。
 * 所有权限检查相关的代码应使用此类中的方法，而不是直接调用 PermissionService。</p>
 * 
 * @author System
 * @since 2024-12-19
 */
public final class PermissionUtils {
    
    private PermissionUtils() {
        // 工具类，禁止实例化
    }
    
    /**
     * 要求工作空间权限
     * 
     * <p>检查用户是否具有指定工作空间的权限，如果没有权限则抛出 PermissionDeniedException。</p>
     * 
     * @param <T> 返回类型
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param action 权限动作（使用 PermissionActions 常量）
     * @param actionSupplier 有权限时执行的操作
     * @return 操作结果
     * @throws PermissionDeniedException 如果没有权限
     */
    public static <T> Mono<T> requireWorkspacePermission(
            PermissionService permissionService,
            String workspaceId,
            String userId,
            String action,
            Supplier<Mono<T>> actionSupplier) {
        return permissionService.checkWorkspacePermission(workspaceId, userId, action)
            .flatMap(hasPermission -> {
                if (!hasPermission) {
                    return Mono.error(new PermissionDeniedException(userId, "workspace", action));
                }
                return actionSupplier.get();
            });
    }
    
    
    /**
     * 要求文档权限
     * 
     * <p>检查用户是否具有指定文档的权限，如果没有权限则抛出 PermissionDeniedException。</p>
     * 
     * @param <T> 返回类型
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param userId 用户ID
     * @param action 权限动作（使用 PermissionActions 常量）
     * @param actionSupplier 有权限时执行的操作
     * @return 操作结果
     * @throws PermissionDeniedException 如果没有权限
     */
    public static <T> Mono<T> requireDocPermission(
            PermissionService permissionService,
            String workspaceId,
            String docId,
            String userId,
            String action,
            Supplier<Mono<T>> actionSupplier) {
        return permissionService.checkDocPermission(workspaceId, docId, userId, action)
            .flatMap(hasPermission -> {
                if (!hasPermission) {
                    return Mono.error(new PermissionDeniedException(userId, "document", action));
                }
                return actionSupplier.get();
            });
    }
    
    
    /**
     * 检查工作空间权限（返回布尔值）
     * 
     * <p>检查用户是否具有指定工作空间的权限，返回布尔值而不是抛出异常。</p>
     * 
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param action 权限动作
     * @return 是否有权限
     */
    public static Mono<Boolean> checkWorkspacePermission(
            PermissionService permissionService,
            String workspaceId,
            String userId,
            String action) {
        return permissionService.checkWorkspacePermission(workspaceId, userId, action);
    }
    
    /**
     * 检查文档权限（返回布尔值）
     * 
     * <p>检查用户是否具有指定文档的权限，返回布尔值而不是抛出异常。</p>
     * 
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param userId 用户ID
     * @param action 权限动作
     * @return 是否有权限
     */
    public static Mono<Boolean> checkDocPermission(
            PermissionService permissionService,
            String workspaceId,
            String docId,
            String userId,
            String action) {
        return permissionService.checkDocPermission(workspaceId, docId, userId, action);
    }
    
    /**
     * 要求工作空间读取权限
     * 
     * @param <T> 返回类型
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param actionSupplier 有权限时执行的操作
     * @return 操作结果
     */
    public static <T> Mono<T> requireWorkspaceRead(
            PermissionService permissionService,
            String workspaceId,
            String userId,
            Supplier<Mono<T>> actionSupplier) {
        return requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.READ, actionSupplier);
    }
    
    /**
     * 要求工作空间写入权限
     * 
     * @param <T> 返回类型
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param actionSupplier 有权限时执行的操作
     * @return 操作结果
     */
    public static <T> Mono<T> requireWorkspaceWrite(
            PermissionService permissionService,
            String workspaceId,
            String userId,
            Supplier<Mono<T>> actionSupplier) {
        return requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WRITE, actionSupplier);
    }
    
    /**
     * 要求工作空间删除权限
     * 
     * @param <T> 返回类型
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param actionSupplier 有权限时执行的操作
     * @return 操作结果
     */
    public static <T> Mono<T> requireWorkspaceDelete(
            PermissionService permissionService,
            String workspaceId,
            String userId,
            Supplier<Mono<T>> actionSupplier) {
        return requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.DELETE, actionSupplier);
    }
    
    /**
     * 要求工作空间用户管理权限
     * 
     * @param <T> 返回类型
     * @param permissionService 权限服务
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param actionSupplier 有权限时执行的操作
     * @return 操作结果
     */
    public static <T> Mono<T> requireWorkspaceManageUsers(
            PermissionService permissionService,
            String workspaceId,
            String userId,
            Supplier<Mono<T>> actionSupplier) {
        return requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.MANAGE_USERS, actionSupplier);
    }
}

