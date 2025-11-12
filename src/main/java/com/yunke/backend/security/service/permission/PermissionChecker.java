package com.yunke.backend.security.service.permission;

import com.yunke.backend.user.domain.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * 统一权限检查器接口
 * 提供统一的权限检查方法，简化权限检查逻辑
 */
public interface PermissionChecker {
    
    /**
     * 获取用户的所有权限（Spring Security格式）
     * @param userId 用户ID
     * @return 权限列表
     */
    List<GrantedAuthority> getUserAuthorities(String userId);
    
    /**
     * 检查用户是否有指定角色
     * @param userId 用户ID
     * @param role 角色枚举
     * @return true表示有该角色
     */
    boolean hasRole(String userId, UserRole.Role role);
    
    /**
     * 检查用户是否有指定角色（字符串形式）
     * @param userId 用户ID
     * @param roleName 角色名称（如 "ADMIN", "SUPER_ADMIN"）
     * @return true表示有该角色
     */
    boolean hasRole(String userId, String roleName);
    
    /**
     * 检查用户是否是管理员
     * @param userId 用户ID
     * @return true表示是管理员（ADMIN或SUPER_ADMIN）
     */
    boolean isAdmin(String userId);
    
    /**
     * 检查用户是否是超级管理员
     * @param userId 用户ID
     * @return true表示是超级管理员
     */
    boolean isSuperAdmin(String userId);
    
    /**
     * 检查用户是否有任意一个角色
     * @param userId 用户ID
     * @param roles 角色列表
     * @return true表示有任意一个角色
     */
    boolean hasAnyRole(String userId, UserRole.Role... roles);
    
    /**
     * 检查用户是否有所有角色
     * @param userId 用户ID
     * @param roles 角色列表
     * @return true表示有所有角色
     */
    boolean hasAllRoles(String userId, UserRole.Role... roles);
}

