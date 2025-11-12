package com.yunke.backend.security.service.permission.cache;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * 权限缓存接口
 * 用于缓存用户权限信息，提高权限检查性能
 */
public interface PermissionCache {
    
    /**
     * 获取用户的权限列表（Spring Security格式）
     * @param userId 用户ID
     * @return 权限列表，如果缓存不存在返回null
     */
    List<GrantedAuthority> getUserAuthorities(String userId);
    
    /**
     * 缓存用户的权限列表
     * @param userId 用户ID
     * @param authorities 权限列表
     */
    void cacheUserAuthorities(String userId, List<GrantedAuthority> authorities);
    
    /**
     * 清除用户的权限缓存
     * @param userId 用户ID
     */
    void invalidateUserAuthorities(String userId);
    
    /**
     * 清除所有权限缓存
     */
    void invalidateAll();
    
    /**
     * 检查用户是否有指定角色（带缓存）
     * @param userId 用户ID
     * @param roleName 角色名称（如 "ADMIN", "SUPER_ADMIN"）
     * @return true表示有该角色
     */
    boolean hasRole(String userId, String roleName);
    
    /**
     * 检查用户是否是管理员（带缓存）
     * @param userId 用户ID
     * @return true表示是管理员
     */
    boolean isAdmin(String userId);
    
    /**
     * 检查用户是否是超级管理员（带缓存）
     * @param userId 用户ID
     * @return true表示是超级管理员
     */
    boolean isSuperAdmin(String userId);
}

