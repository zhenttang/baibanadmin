-- 用户角色表已存在，跳过创建
-- 注意：表 user_roles 已经在数据库中存在
-- 
-- 表结构：
-- - id: varchar(36) PRIMARY KEY
-- - user_id: varchar(100) NOT NULL
-- - role: enum('SUPER_ADMIN','ADMIN','MODERATOR','USER')
-- - enabled: tinyint(1) DEFAULT 1
-- - assigned_by: varchar(100)
-- - assigned_at: timestamp DEFAULT CURRENT_TIMESTAMP
-- - expires_at: timestamp
-- - updated_at: timestamp ON UPDATE CURRENT_TIMESTAMP
--
-- 本迁移脚本只负责初始化管理员角色数据

-- 插入默认管理员角色（如果有admin用户）
-- 注意：这里使用admin@example.com作为默认管理员
-- 使用 COLLATE 解决字符集冲突问题
INSERT INTO user_roles (id, user_id, role, enabled, assigned_by, assigned_at)
SELECT 
    UUID(),
    u.id,
    'SUPER_ADMIN',
    TRUE,
    u.id,
    CURRENT_TIMESTAMP
FROM user u
WHERE u.email COLLATE utf8mb4_unicode_ci = 'admin@example.com'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur 
      WHERE ur.user_id COLLATE utf8mb4_unicode_ci = u.id COLLATE utf8mb4_unicode_ci 
        AND ur.role = 'SUPER_ADMIN'
  );

-- 为所有现有用户添加基本USER角色（可选，因为代码中会自动添加）
-- INSERT INTO user_roles (id, user_id, role, enabled, assigned_by, assigned_at)
-- SELECT 
--     UUID(),
--     id,
--     'USER',
--     TRUE,
--     id,
--     CURRENT_TIMESTAMP
-- FROM user
-- WHERE NOT EXISTS (
--     SELECT 1 FROM user_roles WHERE user_id = user.id AND role = 'USER'
-- );

