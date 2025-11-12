-- 工作空间ID映射表
-- 用于在短格式ID和UUID之间进行映射
CREATE TABLE IF NOT EXISTS workspace_id_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    short_id VARCHAR(255) NOT NULL COMMENT '短格式ID',
    uuid_id VARCHAR(255) NOT NULL COMMENT 'UUID格式ID',
    entity_type VARCHAR(50) NOT NULL DEFAULT 'WORKSPACE' COMMENT '实体类型：WORKSPACE, DOCUMENT等',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    UNIQUE INDEX idx_short_id (short_id),
    UNIQUE INDEX idx_uuid_id (uuid_id),
    INDEX idx_entity_type (entity_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作空间ID映射表';

