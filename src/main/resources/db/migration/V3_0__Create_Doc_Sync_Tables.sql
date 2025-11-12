-- V3_0__Create_Doc_Sync_Tables.sql
-- 完全参考上游架构的文档同步表结构

-- 文档快照表 - 对应上游的DocRecord
CREATE TABLE IF NOT EXISTS doc_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    space_id VARCHAR(100) NOT NULL COMMENT '空间ID，可以是workspace或userspace',
    doc_id VARCHAR(100) NOT NULL COMMENT '文档ID，YJS文档的唯一标识符',
    bin LONGBLOB NOT NULL COMMENT 'YJS编码的文档数据，对应上游的bin: Uint8Array',
    `timestamp` BIGINT NOT NULL COMMENT '文档时间戳，用于版本控制和同步',
    editor_id VARCHAR(100) COMMENT '最后编辑该文档的用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引优化
    INDEX idx_doc_space_id (space_id),
    INDEX idx_doc_doc_id (doc_id), 
    INDEX idx_doc_timestamp (`timestamp`),
    UNIQUE INDEX idx_doc_space_doc (space_id, doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='文档快照表 - 完全参考上游的DocRecord实现';

-- 文档更新表 - 对应上游的DocUpdate
CREATE TABLE IF NOT EXISTS doc_updates (
    id VARCHAR(36) PRIMARY KEY,
    space_id VARCHAR(100) NOT NULL COMMENT '空间ID',
    doc_id VARCHAR(100) NOT NULL COMMENT '文档ID',
    bin LONGBLOB NOT NULL COMMENT 'YJS更新数据，对应上游的bin: Uint8Array',
    `timestamp` BIGINT NOT NULL COMMENT '更新时间戳，用于排序和合并更新',
    editor_id VARCHAR(100) COMMENT '执行此次更新的用户ID',
    merged BOOLEAN DEFAULT FALSE NOT NULL COMMENT '是否已合并到快照',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引优化
    INDEX idx_update_space_id (space_id),
    INDEX idx_update_doc_id (doc_id),
    INDEX idx_update_timestamp (`timestamp`),
    INDEX idx_update_space_doc (space_id, doc_id),
    INDEX idx_update_seq (space_id, doc_id, `timestamp`),
    INDEX idx_update_merged (merged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='文档更新表 - 存储文档的增量更新';

-- 文档历史表 - 对应上游的DocHistory
CREATE TABLE IF NOT EXISTS doc_histories (
    id VARCHAR(36) PRIMARY KEY,
    space_id VARCHAR(100) NOT NULL COMMENT '空间ID',
    doc_id VARCHAR(100) NOT NULL COMMENT '文档ID',
    `blob` LONGBLOB NOT NULL COMMENT '历史版本的二进制数据',
    `timestamp` BIGINT NOT NULL COMMENT '历史版本时间戳',
    editor_id VARCHAR(100) COMMENT '创建该历史版本时的编辑者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP COMMENT '过期时间，用于自动清理',
    
    -- 索引优化
    INDEX idx_history_space_id (space_id),
    INDEX idx_history_doc_id (doc_id),
    INDEX idx_history_timestamp (`timestamp`),
    INDEX idx_history_space_doc (space_id, doc_id),
    INDEX idx_history_expires_at (expires_at),
    UNIQUE INDEX idx_history_space_doc_time (space_id, doc_id, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='文档历史表 - 用于存储文档的历史版本，支持版本回滚功能';

-- 空间权限表 - 用于控制文档访问权限（简化版本）
CREATE TABLE IF NOT EXISTS space_permissions (
    id VARCHAR(36) PRIMARY KEY,
    space_id VARCHAR(100) NOT NULL COMMENT '空间ID',
    user_id VARCHAR(100) NOT NULL COMMENT '用户ID',
    role ENUM('owner', 'admin', 'member', 'viewer') DEFAULT 'viewer' COMMENT '用户在空间中的角色',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引优化
    INDEX idx_perm_space_id (space_id),
    INDEX idx_perm_user_id (user_id),
    UNIQUE INDEX idx_perm_space_user (space_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='空间权限表 - 控制用户对空间的访问权限';

-- 文档元数据表 - 用于存储文档的元信息
CREATE TABLE IF NOT EXISTS doc_metadata (
    id VARCHAR(36) PRIMARY KEY,
    space_id VARCHAR(100) NOT NULL COMMENT '空间ID',
    doc_id VARCHAR(100) NOT NULL COMMENT '文档ID',
    title VARCHAR(255) COMMENT '文档标题',
    summary TEXT COMMENT '文档摘要',
    tags JSON COMMENT '文档标签',
    blocked BOOLEAN DEFAULT FALSE COMMENT '是否被阻止更新',
    created_by VARCHAR(100) COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引优化
    INDEX idx_meta_space_id (space_id),
    INDEX idx_meta_doc_id (doc_id),
    INDEX idx_meta_created_by (created_by),
    INDEX idx_meta_blocked (blocked),
    UNIQUE INDEX idx_meta_space_doc (space_id, doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='文档元数据表 - 存储文档的标题、摘要等元信息';

-- 插入一些测试数据（可选）
-- INSERT INTO space_permissions (id, space_id, user_id, role) VALUES 
-- (UUID(), 'workspace-1', 'user-1', 'owner'),
-- (UUID(), 'workspace-1', 'user-2', 'member');
