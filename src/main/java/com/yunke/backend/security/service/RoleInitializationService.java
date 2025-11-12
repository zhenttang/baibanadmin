package com.yunke.backend.security.service;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.domain.entity.UserRole;
import com.yunke.backend.security.service.permission.PermissionChecker;
import com.yunke.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色初始化服务
 * 在应用启动时自动为特定用户分配管理员角色
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Order(100) // 确保在其他初始化之后执行
public class RoleInitializationService implements CommandLineRunner {

    private final UserService userService;
    private final RoleService roleService; // 保留用于角色分配
    private final PermissionChecker permissionChecker; // 用于权限检查

    /**
     * 默认管理员邮箱列表
     */
    private static final String[] DEFAULT_ADMIN_EMAILS = {
            "admin@example.com",
            "admin"
    };

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🔑 开始初始化管理员角色...");
        
        int assignedCount = 0;
        
        for (String email : DEFAULT_ADMIN_EMAILS) {
            // 使用 block() 将 Mono 转换为阻塞调用
            User user = userService.findByEmail(email).block();
            
            if (user != null) {
                // 检查是否已有管理员角色（使用PermissionChecker，已集成缓存）
                if (permissionChecker.isSuperAdmin(user.getId())) {
                    log.info("✅ 用户 {} 已经是超级管理员", email);
                    continue;
                }
                
                // 分配超级管理员角色
                try {
                    roleService.assignRole(
                            user.getId(),
                            UserRole.Role.SUPER_ADMIN,
                            user.getId(), // 自己分配给自己
                            null // 永久有效
                    );
                    log.info("✅ 成功为用户 {} 分配超级管理员角色", email);
                    assignedCount++;
                } catch (Exception e) {
                    log.error("❌ 为用户 {} 分配管理员角色失败: {}", email, e.getMessage());
                }
            } else {
                log.warn("⚠️  未找到默认管理员账号: {}", email);
            }
        }
        
        if (assignedCount > 0) {
            log.info("🎉 角色初始化完成！成功分配 {} 个管理员角色", assignedCount);
        } else {
            log.warn("⚠️  没有分配任何管理员角色，请检查是否存在默认管理员账号");
            log.warn("💡 提示：请创建邮箱为 'admin@example.com' 或 'admin' 的用户，系统将自动分配管理员权限");
        }
    }
}

