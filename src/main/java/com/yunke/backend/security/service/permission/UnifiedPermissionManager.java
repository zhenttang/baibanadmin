package com.yunke.backend.security.service.permission;

import com.yunke.backend.user.domain.entity.UserRole;
import com.yunke.backend.security.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一权限管理器
 * 提供系统级权限的统一管理接口
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedPermissionManager {

    private final RoleService roleService;

    /**
     * 系统角色枚举
     */
    public enum SystemRole {
        SUPER_ADMIN,
        ADMIN,
        MODERATOR,
        USER
    }

    /**
     * 获取所有管理员角色
     * @return 管理员角色列表
     */
    public List<UserRole> getAllAdmins() {
        log.debug("获取所有管理员角色");
        return roleService.getAllAdmins();
    }

    /**
     * 获取用户的所有角色实体
     * @param userId 用户ID
     * @return 用户角色列表
     */
    public List<UserRole> getUserRoleEntities(String userId) {
        log.debug("获取用户角色实体 - userId: {}", userId);
        return roleService.getUserRoles(userId);
    }

    /**
     * 为用户分配系统角色
     * @param userId 用户ID
     * @param role 系统角色
     * @param assignedBy 分配者ID
     */
    @Transactional
    public void assignSystemRole(String userId, SystemRole role, String assignedBy) {
        log.info("分配系统角色 - userId: {}, role: {}, assignedBy: {}", userId, role, assignedBy);
        
        UserRole.Role userRole = convertToUserRole(role);
        roleService.assignRole(userId, userRole, assignedBy, null);
        
        log.info("系统角色分配成功 - userId: {}, role: {}", userId, role);
    }

    /**
     * 为用户分配系统角色（带过期时间）
     * @param userId 用户ID
     * @param role 系统角色
     * @param assignedBy 分配者ID
     * @param expiresAt 过期时间
     */
    @Transactional
    public void assignSystemRole(String userId, SystemRole role, String assignedBy, LocalDateTime expiresAt) {
        log.info("分配系统角色（带过期时间） - userId: {}, role: {}, assignedBy: {}, expiresAt: {}", 
                userId, role, assignedBy, expiresAt);
        
        UserRole.Role userRole = convertToUserRole(role);
        roleService.assignRole(userId, userRole, assignedBy, expiresAt);
        
        log.info("系统角色分配成功 - userId: {}, role: {}, expiresAt: {}", userId, role, expiresAt);
    }

    /**
     * 移除用户的系统角色
     * @param userId 用户ID
     * @param role 系统角色
     */
    @Transactional
    public void revokeSystemRole(String userId, SystemRole role) {
        log.info("移除系统角色 - userId: {}, role: {}", userId, role);
        
        UserRole.Role userRole = convertToUserRole(role);
        roleService.removeRole(userId, userRole);
        
        log.info("系统角色移除成功 - userId: {}, role: {}", userId, role);
    }

    /**
     * 清理过期角色
     * @return 清理的角色数量
     */
    @Transactional
    public int cleanExpiredRoles() {
        log.info("清理过期角色");
        int count = roleService.cleanExpiredRoles();
        log.info("清理过期角色完成，共清理 {} 个角色", count);
        return count;
    }

    /**
     * 将SystemRole转换为UserRole.Role
     */
    private UserRole.Role convertToUserRole(SystemRole systemRole) {
        return switch (systemRole) {
            case SUPER_ADMIN -> UserRole.Role.SUPER_ADMIN;
            case ADMIN -> UserRole.Role.ADMIN;
            case MODERATOR -> UserRole.Role.MODERATOR;
            case USER -> UserRole.Role.USER;
        };
    }
}

