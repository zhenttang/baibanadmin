-- AFFiNE Java后端文档同步重构 - 数据库迁移脚本
-- 版本: V2.0
-- 描述: 创建新的文档同步表结构，基于开源AFFiNE架构
-- 日期: 2025-01-29

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================
-- 1. 创建snapshots表 - 存储合并后的文档快照
-- ===================================================
DROP TABLE IF EXISTS `snapshots`;
CREATE TABLE `snapshots` (
    `workspace_id` VARCHAR(255) NOT NULL COMMENT '工作空间ID',
    `guid` VARCHAR(255) NOT NULL COMMENT '文档ID (docId)',
    `blob` LONGBLOB NOT NULL COMMENT 'YJS二进制文档数据',
    `state` BLOB NULL COMMENT 'YJS状态向量',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '最后更新时间（重要：记录最后合并的更新时间）',
    `created_by` VARCHAR(255) NULL COMMENT '创建者用户ID',
    `updated_by` VARCHAR(255) NULL COMMENT '最后更新者用户ID',
    
    PRIMARY KEY (`workspace_id`, `guid`),
    INDEX `idx_snapshots_workspace_id` (`workspace_id`),
    INDEX `idx_snapshots_updated_at` (`updated_at`),
    INDEX `idx_snapshots_created_by` (`created_by`),
    INDEX `idx_snapshots_updated_by` (`updated_by`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci 
COMMENT='文档快照表 - 存储合并后的文档快照，对应开源AFFiNE的snapshots表';

-- ===================================================
-- 2. 创建updates表 - 存储待合并的增量更新
-- ===================================================
DROP TABLE IF EXISTS `updates`;
CREATE TABLE `updates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    `workspace_id` VARCHAR(255) NOT NULL COMMENT '工作空间ID',
    `guid` VARCHAR(255) NOT NULL COMMENT '文档ID (docId)',
    `blob` LONGBLOB NOT NULL COMMENT 'YJS增量更新数据',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间（精确到微秒，用于排序）',
    `created_by` VARCHAR(255) NULL COMMENT '创建者用户ID',
    `seq` INT NULL COMMENT '序列号（用于排序和去重）',
    
    INDEX `idx_updates_workspace_doc` (`workspace_id`, `guid`),
    INDEX `idx_updates_created_at` (`created_at`),
    INDEX `idx_updates_workspace_doc_seq` (`workspace_id`, `guid`, `seq`),
    INDEX `idx_updates_created_by` (`created_by`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci 
COMMENT='文档更新表 - 存储待合并的增量更新，对应开源AFFiNE的updates表';

-- ===================================================
-- 3. 创建snapshot_histories表 - 存储文档历史版本
-- ===================================================
DROP TABLE IF EXISTS `snapshot_histories`;
CREATE TABLE `snapshot_histories` (
    `workspace_id` VARCHAR(255) NOT NULL COMMENT '工作空间ID',
    `guid` VARCHAR(255) NOT NULL COMMENT '文档ID (docId)', 
    `timestamp` DATETIME(6) NOT NULL COMMENT '历史版本时间戳',
    `blob` LONGBLOB NOT NULL COMMENT 'YJS历史版本数据',
    `state` BLOB NULL COMMENT 'YJS状态向量',
    `expired_at` DATETIME(6) NOT NULL COMMENT '过期时间',
    `created_by` VARCHAR(255) NULL COMMENT '创建者用户ID',
    
    PRIMARY KEY (`workspace_id`, `guid`, `timestamp`),
    INDEX `idx_snapshot_histories_workspace_id` (`workspace_id`),
    INDEX `idx_snapshot_histories_expired_at` (`expired_at`),
    INDEX `idx_snapshot_histories_created_by` (`created_by`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci 
COMMENT='文档历史表 - 存储文档历史版本，对应开源AFFiNE的snapshot_histories表';

-- ===================================================
-- 4. 修改workspace_pages表 - 移除binary_data，仅存储元数据
-- ===================================================

-- 4.1 备份现有数据到snapshots表
INSERT INTO `snapshots` (
    `workspace_id`, 
    `guid`, 
    `blob`, 
    `created_at`, 
    `updated_at`, 
    `created_by`, 
    `updated_by`
)
SELECT 
    `workspace_id`,
    `page_id` as `guid`,
    COALESCE(`binary_data`, '') as `blob`,  -- 处理NULL值
    `created_at`,
    `updated_at`,
    'migration' as `created_by`,  -- 标记为迁移数据
    'migration' as `updated_by`
FROM `workspace_pages` 
WHERE `binary_data` IS NOT NULL AND LENGTH(`binary_data`) > 0;

-- 4.2 添加新的字段（如果不存在）
ALTER TABLE `workspace_pages` 
ADD COLUMN IF NOT EXISTS `default_role` SMALLINT NOT NULL DEFAULT 30 COMMENT '默认角色权限',
ADD COLUMN IF NOT EXISTS `mode` SMALLINT NOT NULL DEFAULT 0 COMMENT '页面模式: 0=Page, 1=Edgeless',
ADD COLUMN IF NOT EXISTS `blocked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否被阻止',
ADD COLUMN IF NOT EXISTS `public` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开';

-- 4.3 移除binary_data字段
ALTER TABLE `workspace_pages` DROP COLUMN IF EXISTS `binary_data`;

-- 4.4 添加注释说明新的设计
ALTER TABLE `workspace_pages` COMMENT = '工作空间页面元数据表 - 仅存储文档元数据，文档内容存储在snapshots表中';

-- ===================================================
-- 5. 创建文档同步相关的索引优化
-- ===================================================

-- 为workspace_pages表添加复合索引
ALTER TABLE `workspace_pages` 
ADD INDEX IF NOT EXISTS `idx_workspace_pages_workspace_public` (`workspace_id`, `public`),
ADD INDEX IF NOT EXISTS `idx_workspace_pages_created_at` (`created_at`),
ADD INDEX IF NOT EXISTS `idx_workspace_pages_updated_at` (`updated_at`);

-- ===================================================
-- 6. 创建文档同步统计视图（可选）
-- ===================================================
CREATE OR REPLACE VIEW `v_document_sync_stats` AS
SELECT 
    s.workspace_id,
    s.guid as doc_id,
    s.updated_at as last_snapshot_time,
    COUNT(u.id) as pending_updates_count,
    MAX(u.created_at) as last_update_time,
    wp.title as doc_title,
    wp.public as is_public
FROM snapshots s
LEFT JOIN updates u ON s.workspace_id = u.workspace_id AND s.guid = u.guid
LEFT JOIN workspace_pages wp ON s.workspace_id = wp.workspace_id AND s.guid = wp.page_id
GROUP BY s.workspace_id, s.guid, s.updated_at, wp.title, wp.public;

-- ===================================================
-- 7. 创建清理过期数据的存储过程（可选）
-- ===================================================
DELIMITER //

DROP PROCEDURE IF EXISTS `CleanupExpiredDocumentData`//

CREATE PROCEDURE `CleanupExpiredDocumentData`()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- 清理过期的历史记录
    DELETE FROM `snapshot_histories` 
    WHERE `expired_at` < NOW();
    
    -- 清理孤立的updates记录（对应的snapshot不存在）
    DELETE u FROM `updates` u
    LEFT JOIN `snapshots` s ON u.workspace_id = s.workspace_id AND u.guid = s.guid
    WHERE s.workspace_id IS NULL;
    
    -- 清理空的文档记录（没有实际内容）
    DELETE FROM `snapshots` 
    WHERE `blob` IS NULL OR LENGTH(`blob`) = 0;
    
    COMMIT;
    
    SELECT 
        'Cleanup completed successfully' as status,
        NOW() as cleanup_time;
END//

DELIMITER ;

-- ===================================================
-- 8. 数据完整性检查
-- ===================================================

-- 检查迁移结果
SELECT 
    'snapshots' as table_name,
    COUNT(*) as record_count,
    MIN(created_at) as earliest_record,
    MAX(updated_at) as latest_record
FROM snapshots
UNION ALL
SELECT 
    'updates' as table_name,
    COUNT(*) as record_count, 
    MIN(created_at) as earliest_record,
    MAX(created_at) as latest_record
FROM updates
UNION ALL
SELECT 
    'workspace_pages' as table_name,
    COUNT(*) as record_count,
    MIN(created_at) as earliest_record,
    MAX(updated_at) as latest_record  
FROM workspace_pages;

-- ===================================================
-- 9. 创建迁移日志记录
-- ===================================================
INSERT INTO `_data_migrations` (`id`, `name`, `started_at`, `finished_at`)
VALUES (
    'V2_0_document_sync_migration',
    'AFFiNE Document Sync Architecture Migration', 
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    `finished_at` = NOW();

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 迁移完成提示
SELECT 
    '✅ 数据库迁移完成！' as status,
    '新建表: snapshots, updates, snapshot_histories' as created_tables,
    '修改表: workspace_pages (移除binary_data字段)' as modified_tables,
    '数据迁移: workspace_pages.binary_data → snapshots.blob' as data_migration,
    NOW() as completion_time;