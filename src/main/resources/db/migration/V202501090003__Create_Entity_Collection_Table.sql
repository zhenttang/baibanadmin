-- 通用收藏表：entity_collections
-- 用于存储用户对不同类型实体（POST/DOCUMENT/…）的收藏记录

CREATE TABLE IF NOT EXISTS entity_collections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    entity_type VARCHAR(20) NOT NULL COMMENT '实体类型：POST/DOCUMENT等',
    entity_id VARCHAR(100) NOT NULL COMMENT '实体ID（帖子ID、文档ID等）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_user_entity (user_id, entity_type, entity_id),
    KEY idx_user_entity_type (user_id, entity_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='通用收藏表';

