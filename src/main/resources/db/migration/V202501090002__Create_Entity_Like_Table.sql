-- 通用点赞关系表：统一存储帖子/回复/文档的点赞
CREATE TABLE IF NOT EXISTS entity_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    entity_type VARCHAR(20) NOT NULL COMMENT '实体类型：POST/REPLY/DOCUMENT',
    entity_id VARCHAR(100) NOT NULL COMMENT '实体ID（统一字符串存储）',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_user_entity (user_id, entity_type, entity_id),
    KEY idx_entity (entity_type, entity_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='通用实体点赞表';

