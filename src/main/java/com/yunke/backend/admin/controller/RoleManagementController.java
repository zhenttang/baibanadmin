package com.yunke.backend.controller.admin;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.domain.entity.UserRole;
import com.yunke.backend.security.service.AuthService;
import com.yunke.backend.security.service.permission.UnifiedPermissionManager;
import com.yunke.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理控制器
 * 仅超级管理员可访问
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RoleManagementController {

    private final UnifiedPermissionManager permissionManager;
    private final AuthService authService;
    private final UserService userService;

    /**
     * 获取所有角色列表
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableRoles() {
        log.info("获取可用角色列表");
        
        List<Map<String, String>> roles = Arrays.stream(UserRole.Role.values())
                .map(role -> {
                    Map<String, String> roleMap = new HashMap<>();
                    roleMap.put("code", role.name());
                    roleMap.put("name", role.getCode());
                    roleMap.put("displayName", role.getDisplayName());
                    return roleMap;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roles", roles);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有管理员列表
     */
    @GetMapping("/admins")
    public ResponseEntity<Map<String, Object>> getAllAdmins() {
        log.info("获取所有管理员列表");
        
        List<UserRole> adminRoles = permissionManager.getAllAdmins();
        
        // 获取用户详情
        List<Map<String, Object>> admins = new ArrayList<>();
        for (UserRole userRole : adminRoles) {
            Optional<User> userOpt = authService.findUserById(userRole.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("userId", user.getId());
                adminInfo.put("email", user.getEmail());
                adminInfo.put("name", user.getName());
                adminInfo.put("role", userRole.getRole().name());
                adminInfo.put("roleDisplayName", userRole.getRole().getDisplayName());
                adminInfo.put("enabled", userRole.getEnabled());
                adminInfo.put("assignedAt", userRole.getAssignedAt().toString());
                if (userRole.getExpiresAt() != null) {
                    adminInfo.put("expiresAt", userRole.getExpiresAt().toString());
                }
                admins.add(adminInfo);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("admins", admins);
        response.put("total", admins.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户的角色
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRoles(@PathVariable String userId) {
        log.info("获取用户角色 - userId: {}", userId);
        
        List<UserRole> userRoles = permissionManager.getUserRoleEntities(userId);
        
        List<Map<String, Object>> roles = userRoles.stream()
                .map(ur -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", ur.getId());
                    roleMap.put("role", ur.getRole().name());
                    roleMap.put("roleDisplayName", ur.getRole().getDisplayName());
                    roleMap.put("enabled", ur.getEnabled());
                    roleMap.put("assignedAt", ur.getAssignedAt().toString());
                    if (ur.getExpiresAt() != null) {
                        roleMap.put("expiresAt", ur.getExpiresAt().toString());
                    }
                    return roleMap;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("roles", roles);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignRole(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        String userId = (String) request.get("userId");
        String roleStr = (String) request.get("role");
        Integer durationDays = (Integer) request.get("durationDays"); // 可选的过期天数
        
        log.info("分配角色 - userId: {}, role: {}, durationDays: {}", userId, roleStr, durationDays);
        
        // 验证用户存在
        Optional<User> userOpt = authService.findUserById(userId);
        if (userOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "用户不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 解析角色
        UserRole.Role role;
        try {
            role = UserRole.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "无效的角色: " + roleStr);
            return ResponseEntity.badRequest().body(response);
        }
        
        // 获取当前用户ID（分配者）
        String assignedBy = authentication.getName(); // 这是email
        User assigner = userService.findByEmail(assignedBy).block();
        String assignerId = assigner != null ? assigner.getId() : null;
        
        // 计算过期时间
        LocalDateTime expiresAt = null;
        if (durationDays != null && durationDays > 0) {
            expiresAt = LocalDateTime.now().plusDays(durationDays);
        }
        
        // 分配角色
        UnifiedPermissionManager.SystemRole systemRole = convertToSystemRole(role);
        if (expiresAt != null) {
            permissionManager.assignSystemRole(userId, systemRole, assignerId, expiresAt);
        } else {
            permissionManager.assignSystemRole(userId, systemRole, assignerId);
        }
        
        // 获取分配后的角色实体用于返回
        List<UserRole> userRoles = permissionManager.getUserRoleEntities(userId);
        UserRole userRole = userRoles.stream()
                .filter(ur -> ur.getRole() == role)
                .findFirst()
                .orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "角色分配成功");
        response.put("userRole", userRole);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 移除用户的角色
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeRole(
            @RequestParam String userId,
            @RequestParam String role) {
        
        log.info("移除角色 - userId: {}, role: {}", userId, role);
        
        // 解析角色
        UserRole.Role roleEnum;
        try {
            roleEnum = UserRole.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "无效的角色: " + role);
            return ResponseEntity.badRequest().body(response);
        }
        
        // 移除角色
        UnifiedPermissionManager.SystemRole systemRole = convertToSystemRole(roleEnum);
        permissionManager.revokeSystemRole(userId, systemRole);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "角色移除成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 清理过期角色
     */
    @PostMapping("/clean-expired")
    public ResponseEntity<Map<String, Object>> cleanExpiredRoles() {
        log.info("手动清理过期角色");
        
        int count = permissionManager.cleanExpiredRoles();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "清理完成");
        response.put("cleanedCount", count);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 将 UserRole.Role 转换为 SystemRole
     */
    private UnifiedPermissionManager.SystemRole convertToSystemRole(UserRole.Role role) {
        return switch (role) {
            case SUPER_ADMIN -> UnifiedPermissionManager.SystemRole.SUPER_ADMIN;
            case ADMIN -> UnifiedPermissionManager.SystemRole.ADMIN;
            case MODERATOR -> UnifiedPermissionManager.SystemRole.MODERATOR;
            case USER -> UnifiedPermissionManager.SystemRole.USER;
        };
    }
}

