-- workspace_pages表结构同步脚本
-- 确保数据库表结构与WorkspaceDoc实体类完全一致
-- 执行日期: 2025-07-23
-- 说明: 修复workspace_pages表字段缺失问题

-- 1. 检查当前表结构（执行前运行此命令确认当前状态）
-- DESCRIBE `workspace_pages`;

-- 2. 逐个检查并添加缺失的字段
-- 注意：以下脚本适用于MySQL所有版本，手动检查字段是否存在

-- 添加community相关字段
-- 如果字段已存在，以下语句会报错但不会影响数据，可以忽略重复字段错误

-- community_shared字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'community_shared') > 0,
    'SELECT "community_shared字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `community_shared` tinyint(1) DEFAULT 0 COMMENT "是否分享到社区"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- community_permission字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'community_permission') > 0,
    'SELECT "community_permission字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `community_permission` enum("PUBLIC","COLLABORATOR","ADMIN","CUSTOM") DEFAULT NULL COMMENT "社区访问权限"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- community_shared_at字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'community_shared_at') > 0,
    'SELECT "community_shared_at字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `community_shared_at` timestamp NULL DEFAULT NULL COMMENT "分享到社区的时间"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- community_title字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'community_title') > 0,
    'SELECT "community_title字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `community_title` varchar(200) DEFAULT NULL COMMENT "社区显示标题"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- community_description字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'community_description') > 0,
    'SELECT "community_description字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `community_description` text DEFAULT NULL COMMENT "社区显示描述"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- community_view_count字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'community_view_count') > 0,
    'SELECT "community_view_count字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `community_view_count` int(11) DEFAULT 0 COMMENT "社区浏览次数统计"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加其他可能缺失的字段

-- public_mode字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'public_mode') > 0,
    'SELECT "public_mode字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `public_mode` varchar(20) DEFAULT NULL COMMENT "公开模式"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- public_permission字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'public_permission') > 0,
    'SELECT "public_permission字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `public_permission` varchar(50) DEFAULT NULL COMMENT "公开权限"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- binary_data字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'workspace_pages' 
     AND COLUMN_NAME = 'binary_data') > 0,
    'SELECT "binary_data字段已存在"',
    'ALTER TABLE `workspace_pages` ADD COLUMN `binary_data` longblob DEFAULT NULL COMMENT "二进制数据"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 创建索引（如果不存在）
CREATE INDEX IF NOT EXISTS `idx_community_shared` ON `workspace_pages`(`community_shared`, `community_permission`);
CREATE INDEX IF NOT EXISTS `idx_community_shared_at` ON `workspace_pages`(`community_shared_at`);
CREATE INDEX IF NOT EXISTS `idx_workspace_community` ON `workspace_pages`(`workspace_id`, `community_shared`);
CREATE INDEX IF NOT EXISTS `idx_community_view_count` ON `workspace_pages`(`community_shared`, `community_view_count`);

-- 4. 验证表结构
-- DESCRIBE `workspace_pages`;

-- 5. 测试查询是否正常
-- SELECT page_id, workspace_id, community_description, community_title FROM `workspace_pages` LIMIT 1;

-- 注意事项:
-- 1. 此脚本使用动态SQL检查字段是否存在，避免重复添加
-- 2. 执行前请备份数据库
-- 3. 建议在测试环境先执行验证
-- 4. 如果仍有问题，请检查数据库连接的schema是否正确