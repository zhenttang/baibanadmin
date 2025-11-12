package com.yunke.backend.security.service.permission;

import com.yunke.backend.user.domain.entity.UserRole;

import com.yunke.backend.security.service.RoleService;
import com.yunke.backend.security.service.permission.cache.PermissionCache;
import com.yunke.backend.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 统一权限检查器实现
 * 封装权限检查逻辑，提供统一的权限检查接口
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckerImpl implements PermissionChecker {
    
    private final RoleService roleService;
    private final PermissionCache permissionCache;
    private final UserRoleRepository userRoleRepository;
    
    @Override
    public List<GrantedAuthority> getUserAuthorities(String userId) {
        return roleService.getUserAuthorities(userId);
    }
    
    @Override
    public boolean hasRole(String userId, UserRole.Role role) {
        if (userId == null || userId.isBlank() || role == null) {
            return false;
        }
        
        // 先尝试从缓存检查
        if (permissionCache.hasRole(userId, role.name())) {
            return true;
        }
        
        // 缓存未命中，从数据库检查
        boolean hasRole = userRoleRepository.hasRole(userId, role.name(), LocalDateTime.now());
        
        // 如果用户有权限，确保权限已缓存
        if (hasRole) {
            // 触发权限加载以更新缓存
            roleService.getUserAuthorities(userId);
        }
        
        return hasRole;
    }
    
    @Override
    public boolean hasRole(String userId, String roleName) {
        if (userId == null || userId.isBlank() || roleName == null || roleName.isBlank()) {
            return false;
        }
        
        try {
            UserRole.Role role = UserRole.Role.valueOf(roleName.toUpperCase());
            return hasRole(userId, role);
        } catch (IllegalArgumentException e) {
            log.warn("无效的角色名称: {}", roleName);
            return false;
        }
    }
    
    @Override
    public boolean isAdmin(String userId) {
        return roleService.isAdmin(userId);
    }
    
    @Override
    public boolean isSuperAdmin(String userId) {
        return roleService.isSuperAdmin(userId);
    }
    
    @Override
    public boolean hasAnyRole(String userId, UserRole.Role... roles) {
        if (userId == null || userId.isBlank() || roles == null || roles.length == 0) {
            return false;
        }
        
        return Arrays.stream(roles)
                .anyMatch(role -> hasRole(userId, role));
    }
    
    @Override
    public boolean hasAllRoles(String userId, UserRole.Role... roles) {
        if (userId == null || userId.isBlank() || roles == null || roles.length == 0) {
            return false;
        }
        
        return Arrays.stream(roles)
                .allMatch(role -> hasRole(userId, role));
    }
}

