-- V202501090006__Create_Post_Edit_History_Table.sql
-- 帖子编辑历史表：记录每次编辑前的旧版本

CREATE TABLE IF NOT EXISTS post_edit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '历史记录ID',
    post_id VARCHAR(50) NOT NULL COMMENT '帖子ID',
    old_title VARCHAR(200) COMMENT '编辑前标题',
    old_content LONGTEXT COMMENT '编辑前内容',
    editor_id VARCHAR(50) COMMENT '编辑者ID',
    editor_name VARCHAR(100) COMMENT '编辑者名称',
    edited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',

    INDEX idx_post_id (post_id, edited_at DESC),
    INDEX idx_editor_id (editor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子编辑历史表';

