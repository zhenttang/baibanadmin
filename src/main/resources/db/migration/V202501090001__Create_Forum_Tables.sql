-- V202501090001__Create_Forum_Tables.sql
-- 创建论坛系统相关表

-- 1. 板块表
CREATE TABLE IF NOT EXISTS forums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '板块ID',
    name VARCHAR(100) NOT NULL COMMENT '板块名称',
    slug VARCHAR(100) NOT NULL UNIQUE COMMENT '板块标识（URL友好）',
    description TEXT COMMENT '板块描述',
    icon VARCHAR(500) COMMENT '板块图标URL',
    banner VARCHAR(500) COMMENT '板块横幅URL',
    parent_id BIGINT COMMENT '父板块ID',
    display_order INT DEFAULT 0 COMMENT '显示顺序',
    post_count INT DEFAULT 0 COMMENT '帖子数',
    topic_count INT DEFAULT 0 COMMENT '主题数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    is_private BOOLEAN DEFAULT FALSE COMMENT '是否私有',
    announcement TEXT COMMENT '板块公告',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_parent_id (parent_id),
    INDEX idx_slug (slug),
    INDEX idx_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛板块表';

-- 2. 版主表
CREATE TABLE IF NOT EXISTS forum_moderators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版主ID',
    forum_id BIGINT NOT NULL COMMENT '板块ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role ENUM('CHIEF', 'DEPUTY') NOT NULL COMMENT '版主角色：CHIEF-首席版主，DEPUTY-副版主',
    permissions VARCHAR(500) COMMENT '权限列表（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '任命时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE INDEX idx_forum_user (forum_id, user_id),
    INDEX idx_forum_id (forum_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛版主表';

-- 3. 帖子表
CREATE TABLE IF NOT EXISTS forum_posts (
    id VARCHAR(36) PRIMARY KEY COMMENT '帖子ID（UUID）',
    forum_id BIGINT NOT NULL COMMENT '所属板块ID',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    title VARCHAR(200) NOT NULL COMMENT '帖子标题',
    content LONGTEXT NOT NULL COMMENT '帖子内容',
    status ENUM('NORMAL', 'LOCKED', 'DELETED', 'HIDDEN') DEFAULT 'NORMAL' COMMENT '帖子状态',
    is_sticky BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    is_essence BOOLEAN DEFAULT FALSE COMMENT '是否精华',
    is_locked BOOLEAN DEFAULT FALSE COMMENT '是否锁定',
    is_hot BOOLEAN DEFAULT FALSE COMMENT '是否热门',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    reply_count INT DEFAULT 0 COMMENT '回复次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    hot_score DOUBLE DEFAULT 0.0 COMMENT '热度分数',
    last_reply_at TIMESTAMP NULL COMMENT '最后回复时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_forum_created (forum_id, created_at),
    INDEX idx_sticky_essence_reply (is_sticky, is_essence, last_reply_at),
    INDEX idx_hot_score (hot_score DESC),
    INDEX idx_author_id (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛帖子表';

-- 4. 回复表
CREATE TABLE IF NOT EXISTS forum_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '回复ID',
    post_id VARCHAR(36) NOT NULL COMMENT '帖子ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    floor INT NOT NULL COMMENT '楼层号',
    parent_id BIGINT DEFAULT 0 COMMENT '父回复ID（0表示直接回复帖子）',
    content TEXT NOT NULL COMMENT '回复内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    is_best_answer BOOLEAN DEFAULT FALSE COMMENT '是否最佳答案',
    status VARCHAR(20) DEFAULT 'normal' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_post_floor (post_id, floor),
    INDEX idx_post_status (post_id, status),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛回复表';

-- 5. 用户积分表
CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '积分记录ID',
    user_id VARCHAR(100) NOT NULL UNIQUE COMMENT '用户ID',
    total_points INT DEFAULT 0 COMMENT '总积分',
    level INT DEFAULT 0 COMMENT '等级',
    post_count INT DEFAULT 0 COMMENT '发帖数',
    reply_count INT DEFAULT 0 COMMENT '回复数',
    reputation INT DEFAULT 0 COMMENT '声望',
    last_sign_in_date TIMESTAMP NULL COMMENT '最后签到日期',
    continuous_sign_in_days INT DEFAULT 0 COMMENT '连续签到天数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_total_points (total_points DESC),
    INDEX idx_level (level DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='用户积分表';

-- 6. 举报表
CREATE TABLE IF NOT EXISTS forum_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '举报ID',
    target_type ENUM('POST', 'REPLY', 'USER') NOT NULL COMMENT '举报目标类型',
    target_id VARCHAR(100) NOT NULL COMMENT '举报目标ID',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    reason ENUM('SPAM', 'ILLEGAL_CONTENT', 'HARASSMENT', 'PORNOGRAPHY', 'ADVERTISING', 'ABUSE', 'OTHER') NOT NULL COMMENT '举报原因',
    description TEXT COMMENT '详细描述',
    status ENUM('PENDING', 'RESOLVED', 'REJECTED') DEFAULT 'PENDING' COMMENT '处理状态',
    handler_id BIGINT COMMENT '处理人ID',
    handle_note TEXT COMMENT '处理备注',
    handled_at TIMESTAMP NULL COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',

    INDEX idx_status (status),
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='论坛举报表';

-- 插入初始板块数据
INSERT INTO forums (name, slug, description, display_order) VALUES
('综合讨论区', 'general', '自由讨论各种话题', 1),
('技术交流', 'tech', '技术问题讨论与分享', 2),
('新手求助', 'help', '新手问题咨询', 3),
('公告专区', 'announcements', '官方公告与通知', 0);

-- 创建外键约束（可选，根据需求启用）
-- ALTER TABLE forum_moderators ADD CONSTRAINT fk_moderator_forum FOREIGN KEY (forum_id) REFERENCES forums(id) ON DELETE CASCADE;
-- ALTER TABLE forum_posts ADD CONSTRAINT fk_post_forum FOREIGN KEY (forum_id) REFERENCES forums(id) ON DELETE CASCADE;
-- ALTER TABLE forum_replies ADD CONSTRAINT fk_reply_post FOREIGN KEY (post_id) REFERENCES forum_posts(id) ON DELETE CASCADE;
