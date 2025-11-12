-- V202501090002__Create_Forum_Additional_Tables.sql
-- 创建论坛系统的附加功能表

-- 1. 帖子点赞表
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    post_id VARCHAR(36) NOT NULL COMMENT '帖子ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    UNIQUE INDEX idx_post_user (post_id, user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子点赞表';

-- 2. 回复点赞表
CREATE TABLE IF NOT EXISTS reply_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    reply_id BIGINT NOT NULL COMMENT '回复ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    UNIQUE INDEX idx_reply_user (reply_id, user_id),
    INDEX idx_reply_id (reply_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='回复点赞表';

-- 3. 帖子收藏表
CREATE TABLE IF NOT EXISTS post_collections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    post_id VARCHAR(36) NOT NULL COMMENT '帖子ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

    UNIQUE INDEX idx_post_user (post_id, user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子收藏表';

-- 4. 标签表
CREATE TABLE IF NOT EXISTS forum_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    slug VARCHAR(50) NOT NULL UNIQUE COMMENT '标签标识',
    description VARCHAR(200) COMMENT '标签描述',
    color VARCHAR(20) COMMENT '标签颜色',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_name (name),
    INDEX idx_slug (slug),
    INDEX idx_use_count (use_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛标签表';

-- 5. 帖子标签关联表
CREATE TABLE IF NOT EXISTS post_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    post_id VARCHAR(36) NOT NULL COMMENT '帖子ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE INDEX idx_post_tag (post_id, tag_id),
    INDEX idx_post_id (post_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子标签关联表';

-- 6. 编辑历史表
CREATE TABLE IF NOT EXISTS post_edit_history (
    id VARCHAR(36) PRIMARY KEY COMMENT '历史记录ID（UUID）',
    post_id VARCHAR(36) NOT NULL COMMENT '帖子ID',
    editor_id VARCHAR(100) NOT NULL COMMENT '编辑者ID',
    editor_name VARCHAR(100) COMMENT '编辑者名称',
    title_before TEXT COMMENT '修改前标题',
    title_after TEXT COMMENT '修改后标题',
    content_before LONGTEXT COMMENT '修改前内容',
    content_after LONGTEXT COMMENT '修改后内容',
    edit_reason VARCHAR(500) COMMENT '编辑原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',

    INDEX idx_post_id (post_id),
    INDEX idx_editor_id (editor_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子编辑历史表';

-- 7. 草稿表
CREATE TABLE IF NOT EXISTS post_drafts (
    id VARCHAR(36) PRIMARY KEY COMMENT '草稿ID（UUID）',
    forum_id BIGINT NOT NULL COMMENT '板块ID',
    author_id VARCHAR(100) NOT NULL COMMENT '作者ID',
    author_name VARCHAR(100) COMMENT '作者名称',
    title VARCHAR(200) COMMENT '草稿标题',
    content LONGTEXT COMMENT '草稿内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_author_id (author_id),
    INDEX idx_forum_id (forum_id),
    INDEX idx_updated_at (updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子草稿表';

-- 8. 附件表
CREATE TABLE IF NOT EXISTS post_attachments (
    id VARCHAR(36) PRIMARY KEY COMMENT '附件ID（UUID）',
    post_id VARCHAR(36) NOT NULL COMMENT '帖子ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_url VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(100) COMMENT '文件类型',
    uploader_id VARCHAR(100) NOT NULL COMMENT '上传者ID',
    uploader_name VARCHAR(100) COMMENT '上传者名称',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    INDEX idx_post_id (post_id),
    INDEX idx_uploader_id (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='帖子附件表';

-- 9. 通知表
CREATE TABLE IF NOT EXISTS forum_notifications (
    id VARCHAR(36) PRIMARY KEY COMMENT '通知ID（UUID）',
    user_id VARCHAR(100) NOT NULL COMMENT '接收用户ID',
    type VARCHAR(50) NOT NULL COMMENT '通知类型：REPLY-回复，LIKE-点赞，MENTION-提及，SYSTEM-系统',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    link VARCHAR(500) COMMENT '相关链接',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    related_id VARCHAR(100) COMMENT '相关对象ID',
    related_type VARCHAR(50) COMMENT '相关对象类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_read (user_id, is_read),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛通知表';

-- 10. 积分变动记录表
CREATE TABLE IF NOT EXISTS point_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '交易ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    points INT NOT NULL COMMENT '积分变动数（正数为增加，负数为减少）',
    reason VARCHAR(200) NOT NULL COMMENT '变动原因',
    related_id VARCHAR(100) COMMENT '关联对象ID',
    related_type VARCHAR(50) COMMENT '关联对象类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='积分变动记录表';

-- 插入初始标签数据
INSERT INTO forum_tags (name, slug, description, color) VALUES
('技术', 'tech', '技术相关讨论', '#1890ff'),
('问答', 'qa', '问答求助', '#52c41a'),
('分享', 'share', '知识分享', '#faad14'),
('讨论', 'discussion', '一般讨论', '#722ed1'),
('公告', 'announcement', '官方公告', '#f5222d');

