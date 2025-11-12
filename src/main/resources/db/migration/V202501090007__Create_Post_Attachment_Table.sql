-- 创建帖子附件表
CREATE TABLE IF NOT EXISTS post_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    post_id VARCHAR(50) NOT NULL COMMENT '帖子ID',
    file_url VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型/扩展名',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    uploader_id VARCHAR(50) NOT NULL COMMENT '上传者ID',
    uploader_name VARCHAR(100) COMMENT '上传者名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_post_id (post_id, created_at DESC),
    INDEX idx_uploader_id (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子附件表';

