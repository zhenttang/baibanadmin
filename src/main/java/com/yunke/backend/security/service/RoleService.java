package com.yunke.backend.security.service;

import com.yunke.backend.user.domain.entity.UserRole;
import com.yunke.backend.user.repository.UserRoleRepository;
import com.yunke.backend.security.service.permission.cache.PermissionCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户角色服务
 * 已集成权限缓存，提高性能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final UserRoleRepository userRoleRepository;
    private final PermissionCache permissionCache;

    /**
     * 获取用户的所有权限（带缓存）
     * @param userId 用户ID
     * @return Spring Security权限列表
     */
    public List<GrantedAuthority> getUserAuthorities(String userId) {
        log.debug("🔑 获取用户权限 - userId: {}", userId);
        
        // 1. 尝试从缓存获取
        List<GrantedAuthority> cachedAuthorities = permissionCache.getUserAuthorities(userId);
        if (cachedAuthorities != null) {
            log.debug("✅ 从缓存获取用户权限 - userId: {}, 权限数量: {}", userId, cachedAuthorities.size());
            return cachedAuthorities;
        }
        
        // 2. 缓存未命中，从数据库加载
        log.debug("缓存未命中，从数据库加载用户权限 - userId: {}", userId);
        List<GrantedAuthority> authorities = loadUserAuthoritiesFromDatabase(userId);
        
        // 3. 缓存权限列表
        permissionCache.cacheUserAuthorities(userId, authorities);
        
        log.info("📋 用户 {} 权限列表 (共{}个): {}", userId, authorities.size(), authorities);
        return authorities;
    }
    
    /**
     * 从数据库加载用户权限
     */
    private List<GrantedAuthority> loadUserAuthoritiesFromDatabase(String userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 所有用户都有基本用户权限
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 从数据库获取用户的角色
        List<UserRole> userRoles = userRoleRepository.findActiveRolesByUserId(userId, LocalDateTime.now());
        
        if (userRoles.isEmpty()) {
            log.debug("用户 {} 没有额外角色，仅具有基本用户权限 (ROLE_USER)", userId);
            return authorities;
        }
        
        // 转换为Spring Security权限
        List<GrantedAuthority> roleAuthorities = userRoles.stream()
                .map(userRole -> {
                    String roleName = "ROLE_" + userRole.getRole().name();
                    log.debug("用户 {} 具有角色: {}", userId, roleName);
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());
        
        authorities.addAll(roleAuthorities);
        
        // SUPER_ADMIN 自动拥有 ADMIN 权限
        boolean hasSuperAdmin = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean hasAdmin = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (hasSuperAdmin && !hasAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            log.debug("用户 {} 是 SUPER_ADMIN，自动添加 ROLE_ADMIN 权限", userId);
        }
        
        return authorities;
    }

    /**
     * 检查用户是否具有管理员权限（带缓存）
     * @param userId 用户ID
     * @return true表示是管理员
     */
    public boolean isAdmin(String userId) {
        // 先尝试从缓存检查
        if (permissionCache.isAdmin(userId)) {
            return true;
        }
        
        // 缓存未命中，从数据库检查
        boolean isAdmin = userRoleRepository.hasAdminRole(userId, LocalDateTime.now());
        
        // 如果用户有权限，确保权限已缓存
        if (isAdmin) {
            // 触发权限加载以更新缓存
            getUserAuthorities(userId);
        }
        
        return isAdmin;
    }

    /**
     * 检查用户是否具有超级管理员权限（带缓存）
     * @param userId 用户ID
     * @return true表示是超级管理员
     */
    public boolean isSuperAdmin(String userId) {
        // 先尝试从缓存检查
        if (permissionCache.isSuperAdmin(userId)) {
            return true;
        }
        
        // 缓存未命中，从数据库检查
        boolean isSuperAdmin = userRoleRepository.hasRole(userId, "SUPER_ADMIN", LocalDateTime.now());
        
        // 如果用户有权限，确保权限已缓存
        if (isSuperAdmin) {
            // 触发权限加载以更新缓存
            getUserAuthorities(userId);
        }
        
        return isSuperAdmin;
    }

    /**
     * 为用户分配角色
     * @param userId 用户ID
     * @param role 角色
     * @param assignedBy 分配者ID
     * @param expiresAt 过期时间（可选）
     * @return 创建的角色记录
     */
    @Transactional
    public UserRole assignRole(String userId, UserRole.Role role, String assignedBy, LocalDateTime expiresAt) {
        log.info("为用户 {} 分配角色: {}, 分配者: {}", userId, role, assignedBy);
        
        // 检查是否已经有该角色
        Optional<UserRole> existing = userRoleRepository.findByUserIdAndRole(userId, role);
        UserRole userRole;
        
        if (existing.isPresent()) {
            userRole = existing.get();
            if (userRole.getEnabled()) {
                log.warn("用户 {} 已经具有角色 {}", userId, role);
                return userRole;
            } else {
                // 重新启用已有的角色
                userRole.setEnabled(true);
                userRole.setAssignedBy(assignedBy);
                userRole.setAssignedAt(LocalDateTime.now());
                userRole.setExpiresAt(expiresAt);
                userRole = userRoleRepository.save(userRole);
            }
        } else {
            // 创建新角色
            userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRole(role);
            userRole.setEnabled(true);
            userRole.setAssignedBy(assignedBy);
            userRole.setAssignedAt(LocalDateTime.now());
            userRole.setExpiresAt(expiresAt);
            userRole = userRoleRepository.save(userRole);
        }
        
        // 清除用户权限缓存，强制下次重新加载
        permissionCache.invalidateUserAuthorities(userId);
        log.debug("已清除用户权限缓存 - userId: {}", userId);
        
        return userRole;
    }

    /**
     * 移除用户的角色
     * @param userId 用户ID
     * @param role 角色
     */
    @Transactional
    public void removeRole(String userId, UserRole.Role role) {
        log.info("移除用户 {} 的角色: {}", userId, role);
        
        Optional<UserRole> userRole = userRoleRepository.findByUserIdAndRole(userId, role);
        if (userRole.isPresent()) {
            UserRole ur = userRole.get();
            ur.setEnabled(false);
            userRoleRepository.save(ur);
            log.info("成功移除用户 {} 的角色 {}", userId, role);
            
            // 清除用户权限缓存，强制下次重新加载
            permissionCache.invalidateUserAuthorities(userId);
            log.debug("已清除用户权限缓存 - userId: {}", userId);
        } else {
            log.warn("用户 {} 不具有角色 {}", userId, role);
        }
    }

    /**
     * 获取用户的所有角色
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<UserRole> getUserRoles(String userId) {
        return userRoleRepository.findActiveRolesByUserId(userId, LocalDateTime.now());
    }

    /**
     * 清理过期角色
     * @return 清理的角色数量
     */
    @Transactional
    public int cleanExpiredRoles() {
        log.info("清理过期角色");
        int count = userRoleRepository.disableExpiredRoles(LocalDateTime.now());
        log.info("清理了 {} 个过期角色", count);
        return count;
    }

    /**
     * 获取所有管理员用户
     * @return 管理员角色列表
     */
    public List<UserRole> getAllAdmins() {
        List<UserRole> admins = new ArrayList<>();
        admins.addAll(userRoleRepository.findUsersByRole("ADMIN"));
        admins.addAll(userRoleRepository.findUsersByRole("SUPER_ADMIN"));
        return admins;
    }

    /**
     * 获取用户的权限特性列表
     * @param userId 用户ID
     * @return 特性列表（如 ["user", "admin"]）
     */
    public List<String> getUserFeatures(String userId) {
        List<UserRole> roles = getUserRoles(userId);
        List<String> features = roles.stream()
                .map(role -> role.getRole().name().toLowerCase())
                .collect(Collectors.toList());
        
        // 添加基础功能
        if (!features.contains("user")) {
            features.add("user");
        }
        
        // 如果有管理员权限，添加admin特性
        if (isAdmin(userId)) {
            if (!features.contains("admin")) {
                features.add("admin");
            }
        }
        
        log.debug("用户 {} 的特性列表: {}", userId, features);
        return features;
    }

    /**
     * 初始化默认管理员（仅在系统初始化时调用）
     * @param userId 用户ID
     */
    @Transactional
    public void initializeDefaultAdmin(String userId) {
        // 检查是否已经是管理员
        if (!isAdmin(userId)) {
            assignRole(userId, UserRole.Role.SUPER_ADMIN, "system", null);
            log.info("已为用户 {} 初始化默认超级管理员角色", userId);
        } else {
            log.debug("用户 {} 已经是管理员", userId);
        }
    }
}

