-- V4_0__Create_User_Roles_Table.sql
-- 创建用户角色表

-- 用户角色表
CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    role ENUM('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'USER') NOT NULL COMMENT '角色类型',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '角色是否启用',
    assigned_by VARCHAR(100) COMMENT '角色分配者ID',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '角色分配时间',
    expires_at TIMESTAMP NULL COMMENT '角色过期时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    
    -- 索引优化
    INDEX idx_user_roles_user_id (user_id),
    INDEX idx_user_roles_role (role),
    INDEX idx_user_roles_enabled (enabled),
    INDEX idx_user_roles_expires_at (expires_at),
    UNIQUE INDEX idx_user_roles_user_role (user_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='用户角色表 - 管理用户的系统角色和权限';

-- 插入一些基础数据（可选）
-- INSERT INTO user_roles (id, user_id, role, enabled, assigned_by) VALUES 
-- (UUID(), 'admin', 'SUPER_ADMIN', TRUE, 'system');