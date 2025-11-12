-- ====================================================================
-- 社区功能完整数据库架构
-- 版本: 2.0
-- 日期: 2025-01-10
-- 说明: 全面实现社区功能的完整数据库结构
-- ====================================================================

-- ====================================================================
-- 1. 社区文档主表
-- ====================================================================
DROP TABLE IF EXISTS `community_documents`;
CREATE TABLE `community_documents` (
  -- 基础信息
  `id` VARCHAR(50) NOT NULL COMMENT '文档唯一ID',
  `workspace_id` VARCHAR(50) NOT NULL COMMENT '来源工作空间ID',
  `source_doc_id` VARCHAR(50) NOT NULL COMMENT '原始文档ID（workspace_pages.page_id）',

  -- 文档内容
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `description` TEXT COMMENT '文档描述/摘要',
  `cover_image` VARCHAR(500) COMMENT '封面图片URL',
  `content_snapshot` LONGTEXT COMMENT '内容快照（发布时的版本）',

  -- 作者信息
  `author_id` VARCHAR(50) NOT NULL COMMENT '作者用户ID',
  `author_name` VARCHAR(100) NOT NULL COMMENT '作者姓名（冗余字段）',
  `author_avatar` VARCHAR(500) COMMENT '作者头像URL（冗余字段）',

  -- 分类和标签
  `category_id` INT COMMENT '主分类ID',
  `subcategory_id` INT COMMENT '子分类ID',

  -- 权限和可见性
  `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开（1:公开 0:私密）',
  `require_follow` TINYINT(1) DEFAULT 0 COMMENT '是否需要关注作者才能查看',
  `require_purchase` TINYINT(1) DEFAULT 0 COMMENT '是否需要购买才能查看完整内容',

  -- 付费信息
  `is_paid` TINYINT(1) DEFAULT 0 COMMENT '是否付费文档',
  `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '文档价格（元）',
  `discount_price` DECIMAL(10,2) COMMENT '折扣价格',
  `free_preview_length` INT DEFAULT 500 COMMENT '免费预览字数',

  -- 统计数据
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `collect_count` INT DEFAULT 0 COMMENT '收藏数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `share_count` INT DEFAULT 0 COMMENT '分享次数',
  `purchase_count` INT DEFAULT 0 COMMENT '购买次数',

  -- 质量评分
  `quality_score` DECIMAL(3,2) DEFAULT 0.00 COMMENT '质量评分（0-10）',
  `avg_rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分（0-5）',
  `rating_count` INT DEFAULT 0 COMMENT '评分人数',

  -- 状态控制
  `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态: draft/published/archived/deleted',
  `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选推荐',
  `is_sticky` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',

  -- 时间戳
  `published_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` TIMESTAMP NULL COMMENT '软删除时间',

  PRIMARY KEY (`id`),

  -- 索引设计
  INDEX `idx_author_id` (`author_id`),
  INDEX `idx_category` (`category_id`, `subcategory_id`),
  INDEX `idx_workspace_source` (`workspace_id`, `source_doc_id`),
  INDEX `idx_status_public` (`status`, `is_public`),
  INDEX `idx_published_at` (`published_at` DESC),
  INDEX `idx_view_count` (`view_count` DESC),
  INDEX `idx_like_count` (`like_count` DESC),
  INDEX `idx_is_paid` (`is_paid`, `price`),
  INDEX `idx_featured_sticky` (`is_featured`, `is_sticky`, `published_at` DESC),

  -- 全文搜索索引
  FULLTEXT INDEX `idx_fulltext_search` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区文档主表';

-- ====================================================================
-- 2. 文档分类表
-- ====================================================================
DROP TABLE IF EXISTS `document_categories`;
CREATE TABLE `document_categories` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `parent_id` INT DEFAULT 0 COMMENT '父分类ID（0表示顶级分类）',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `slug` VARCHAR(50) NOT NULL UNIQUE COMMENT 'URL友好的唯一标识',
  `description` VARCHAR(200) COMMENT '分类描述',
  `icon` VARCHAR(100) COMMENT '分类图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分类表';

-- ====================================================================
-- 3. 文档标签表
-- ====================================================================
DROP TABLE IF EXISTS `document_tags`;
CREATE TABLE `document_tags` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(30) NOT NULL UNIQUE COMMENT '标签名称',
  `slug` VARCHAR(30) NOT NULL UNIQUE COMMENT 'URL友好标识',
  `color` VARCHAR(20) DEFAULT '#999999' COMMENT '标签颜色',
  `use_count` INT DEFAULT 0 COMMENT '使用次数',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  INDEX `idx_use_count` (`use_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档标签表';

-- ====================================================================
-- 4. 文档-标签关联表
-- ====================================================================
DROP TABLE IF EXISTS `document_tag_relations`;
CREATE TABLE `document_tag_relations` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `document_id` VARCHAR(50) NOT NULL COMMENT '文档ID',
  `tag_id` INT NOT NULL COMMENT '标签ID',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY `uk_doc_tag` (`document_id`, `tag_id`),
  INDEX `idx_tag_id` (`tag_id`),

  CONSTRAINT `fk_dtr_document` FOREIGN KEY (`document_id`)
    REFERENCES `community_documents` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dtr_tag` FOREIGN KEY (`tag_id`)
    REFERENCES `document_tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档标签关联表';

-- ====================================================================
-- 5. 文档点赞表
-- ====================================================================
DROP TABLE IF EXISTS `document_likes`;
CREATE TABLE `document_likes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `document_id` VARCHAR(50) NOT NULL COMMENT '文档ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY `uk_doc_user` (`document_id`, `user_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_created_at` (`created_at` DESC),

  CONSTRAINT `fk_like_document` FOREIGN KEY (`document_id`)
    REFERENCES `community_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档点赞表';

-- ====================================================================
-- 6. 文档收藏表
-- ====================================================================
DROP TABLE IF EXISTS `document_collections`;
CREATE TABLE `document_collections` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `document_id` VARCHAR(50) NOT NULL COMMENT '文档ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
  `folder_id` INT COMMENT '收藏夹ID',
  `notes` VARCHAR(500) COMMENT '收藏备注',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY `uk_doc_user` (`document_id`, `user_id`),
  INDEX `idx_user_folder` (`user_id`, `folder_id`),
  INDEX `idx_created_at` (`created_at` DESC),

  CONSTRAINT `fk_collect_document` FOREIGN KEY (`document_id`)
    REFERENCES `community_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档收藏表';

-- ====================================================================
-- 7. 用户关注表
-- ====================================================================
DROP TABLE IF EXISTS `user_follows`;
CREATE TABLE `user_follows` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `follower_id` VARCHAR(50) NOT NULL COMMENT '关注者ID',
  `following_id` VARCHAR(50) NOT NULL COMMENT '被关注者ID',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
  INDEX `idx_following` (`following_id`),
  INDEX `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- ====================================================================
-- 8. 文档评论表
-- ====================================================================
DROP TABLE IF EXISTS `document_comments`;
CREATE TABLE `document_comments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `document_id` VARCHAR(50) NOT NULL COMMENT '文档ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '评论用户ID',
  `user_name` VARCHAR(100) NOT NULL COMMENT '评论用户名（冗余）',
  `user_avatar` VARCHAR(500) COMMENT '用户头像（冗余）',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID（0表示顶级评论）',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `is_author` TINYINT(1) DEFAULT 0 COMMENT '是否作者回复',
  `status` VARCHAR(20) DEFAULT 'normal' COMMENT '状态: normal/hidden/deleted',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX `idx_document_id` (`document_id`, `created_at` DESC),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_parent_id` (`parent_id`),

  CONSTRAINT `fk_comment_document` FOREIGN KEY (`document_id`)
    REFERENCES `community_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档评论表';

-- ====================================================================
-- 9. 文档购买记录表
-- ====================================================================
DROP TABLE IF EXISTS `document_purchases`;
CREATE TABLE `document_purchases` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `document_id` VARCHAR(50) NOT NULL COMMENT '文档ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '购买用户ID',
  `price` DECIMAL(10,2) NOT NULL COMMENT '实际支付价格',
  `payment_method` VARCHAR(30) COMMENT '支付方式',
  `payment_id` VARCHAR(100) COMMENT '支付订单号',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/completed/refunded',
  `purchased_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `refunded_at` TIMESTAMP NULL COMMENT '退款时间',

  UNIQUE KEY `uk_doc_user` (`document_id`, `user_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_purchased_at` (`purchased_at` DESC),
  INDEX `idx_status` (`status`),

  CONSTRAINT `fk_purchase_document` FOREIGN KEY (`document_id`)
    REFERENCES `community_documents` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档购买记录表';

-- ====================================================================
-- 10. 文档浏览记录表
-- ====================================================================
DROP TABLE IF EXISTS `document_views`;
CREATE TABLE `document_views` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `document_id` VARCHAR(50) NOT NULL COMMENT '文档ID',
  `user_id` VARCHAR(50) COMMENT '用户ID（可为空，匿名用户）',
  `ip_address` VARCHAR(45) COMMENT 'IP地址',
  `user_agent` VARCHAR(500) COMMENT '用户代理',
  `view_duration` INT DEFAULT 0 COMMENT '浏览时长（秒）',
  `viewed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  INDEX `idx_document_id` (`document_id`, `viewed_at` DESC),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档浏览记录表';

-- ====================================================================
-- 初始化数据
-- ====================================================================

-- 插入默认分类
INSERT INTO `document_categories` (`name`, `slug`, `description`, `icon`, `sort_order`) VALUES
('技术文档', 'tech', '技术相关的文档和教程', '💻', 1),
('设计资源', 'design', '设计相关的资源和素材', '🎨', 2),
('产品方案', 'product', '产品设计和方案', '📱', 3),
('学习笔记', 'notes', '学习笔记和总结', '📝', 4),
('其他', 'others', '其他类型的文档', '📂', 99);

-- 插入常用标签
INSERT INTO `document_tags` (`name`, `slug`, `color`) VALUES
('前端', 'frontend', '#3B82F6'),
('后端', 'backend', '#10B981'),
('设计', 'design', '#F59E0B'),
('教程', 'tutorial', '#8B5CF6'),
('实战', 'practice', '#EF4444'),
('开源', 'opensource', '#06B6D4');

-- ====================================================================
-- 触发器（可选 - 用于维护统计数据一致性）
-- ====================================================================

-- 点赞触发器
DELIMITER $$
CREATE TRIGGER `trg_document_like_insert`
AFTER INSERT ON `document_likes`
FOR EACH ROW
BEGIN
  UPDATE `community_documents`
  SET `like_count` = `like_count` + 1
  WHERE `id` = NEW.`document_id`;
END$$

CREATE TRIGGER `trg_document_like_delete`
AFTER DELETE ON `document_likes`
FOR EACH ROW
BEGIN
  UPDATE `community_documents`
  SET `like_count` = `like_count` - 1
  WHERE `id` = OLD.`document_id`;
END$$

-- 收藏触发器
CREATE TRIGGER `trg_document_collect_insert`
AFTER INSERT ON `document_collections`
FOR EACH ROW
BEGIN
  UPDATE `community_documents`
  SET `collect_count` = `collect_count` + 1
  WHERE `id` = NEW.`document_id`;
END$$

CREATE TRIGGER `trg_document_collect_delete`
AFTER DELETE ON `document_collections`
FOR EACH ROW
BEGIN
  UPDATE `community_documents`
  SET `collect_count` = `collect_count` - 1
  WHERE `id` = OLD.`document_id`;
END$$

-- 评论触发器
CREATE TRIGGER `trg_document_comment_insert`
AFTER INSERT ON `document_comments`
FOR EACH ROW
BEGIN
  IF NEW.`status` = 'normal' THEN
    UPDATE `community_documents`
    SET `comment_count` = `comment_count` + 1
    WHERE `id` = NEW.`document_id`;
  END IF;
END$$

CREATE TRIGGER `trg_document_comment_delete`
AFTER DELETE ON `document_comments`
FOR EACH ROW
BEGIN
  IF OLD.`status` = 'normal' THEN
    UPDATE `community_documents`
    SET `comment_count` = `comment_count` - 1
    WHERE `id` = OLD.`document_id`;
  END IF;
END$$

DELIMITER ;

-- ====================================================================
-- 完成
-- ====================================================================
