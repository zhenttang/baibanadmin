-- V202501090005__Create_Post_Draft_Table.sql
-- 帖子草稿表

CREATE TABLE IF NOT EXISTS post_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '草稿ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    forum_id BIGINT NOT NULL COMMENT '板块ID',
    title VARCHAR(200) COMMENT '标题',
    content LONGTEXT COMMENT '内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子草稿表';

