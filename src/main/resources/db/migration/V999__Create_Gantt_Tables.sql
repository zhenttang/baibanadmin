-- 甘特图数据库表设计
-- 基于 AFFiNE 现有命名规范和架构

-- 1. 甘特图视图配置表
CREATE TABLE gantt_view_configs (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    workspace_id VARCHAR(36) NOT NULL,
    doc_id VARCHAR(36) NOT NULL,
    
    -- 时间轴配置 (JSON格式存储)
    timeline_config JSON NOT NULL DEFAULT JSON_OBJECT(
        'startDate', UNIX_TIMESTAMP(CURDATE()) * 1000,
        'endDate', UNIX_TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 30 DAY)) * 1000,
        'unit', 'day',
        'showWeekends', true,
        'workingDays', JSON_ARRAY(1, 2, 3, 4, 5)
    ),
    
    -- 显示配置 (JSON格式存储)
    display_config JSON NOT NULL DEFAULT JSON_OBJECT(
        'showCriticalPath', false,
        'showProgress', true,
        'compactMode', false
    ),
    
    -- 工作日历配置 (JSON格式存储，可选)
    working_calendar JSON NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_gantt_config_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    
    -- 唯一约束：每个文档只能有一个甘特图配置
    UNIQUE KEY uk_gantt_config_doc (workspace_id, doc_id),
    
    -- 索引优化
    INDEX idx_gantt_config_workspace (workspace_id),
    INDEX idx_gantt_config_doc (doc_id)
);

-- 2. 任务依赖关系表
CREATE TABLE gantt_task_dependencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL,
    doc_id VARCHAR(36) NOT NULL,
    from_task_id VARCHAR(36) NOT NULL,
    to_task_id VARCHAR(36) NOT NULL,
    dependency_type ENUM('finish-to-start', 'start-to-start', 'finish-to-finish', 'start-to-finish') 
        DEFAULT 'finish-to-start',
    lag_days INT DEFAULT 0,
    is_flexible BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_gantt_dep_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    
    -- 唯一约束：避免重复依赖关系
    UNIQUE KEY uk_gantt_dependency (workspace_id, doc_id, from_task_id, to_task_id),
    
    -- 自引用检查：任务不能依赖自己
    CONSTRAINT chk_gantt_no_self_dependency CHECK (from_task_id != to_task_id),
    
    -- 索引优化
    INDEX idx_gantt_dep_workspace (workspace_id),
    INDEX idx_gantt_dep_doc (doc_id),
    INDEX idx_gantt_dep_from_task (from_task_id),
    INDEX idx_gantt_dep_to_task (to_task_id),
    INDEX idx_gantt_dep_doc_from (doc_id, from_task_id),
    INDEX idx_gantt_dep_doc_to (doc_id, to_task_id)
);

-- 3. 甘特图操作日志表（用于协作和版本控制）
CREATE TABLE gantt_operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL,
    doc_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    operation_type ENUM('config_update', 'dependency_add', 'dependency_remove', 'task_update') NOT NULL,
    operation_data JSON NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_gantt_log_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    
    -- 索引优化
    INDEX idx_gantt_log_workspace (workspace_id),
    INDEX idx_gantt_log_doc (doc_id),
    INDEX idx_gantt_log_user (user_id),
    INDEX idx_gantt_log_doc_time (doc_id, created_at)
);

-- 4. 存储过程：循环依赖检测
DELIMITER //

CREATE FUNCTION check_circular_dependency(
    p_workspace_id VARCHAR(36),
    p_doc_id VARCHAR(36),
    p_from_task_id VARCHAR(36),
    p_to_task_id VARCHAR(36)
) RETURNS BOOLEAN
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE has_cycle BOOLEAN DEFAULT FALSE;
    DECLARE done BOOLEAN DEFAULT FALSE;
    DECLARE current_task VARCHAR(36);
    DECLARE visited_tasks TEXT DEFAULT '';
    
    -- 使用递归逻辑检测循环依赖
    SET current_task = p_to_task_id;
    SET visited_tasks = CONCAT(',', p_from_task_id, ',');
    
    dependency_loop: LOOP
        -- 检查当前任务是否已被访问过
        IF FIND_IN_SET(current_task, REPLACE(visited_tasks, ',', '')) > 0 THEN
            SET has_cycle = TRUE;
            LEAVE dependency_loop;
        END IF;
        
        -- 添加当前任务到已访问列表
        SET visited_tasks = CONCAT(visited_tasks, current_task, ',');
        
        -- 查找当前任务的依赖任务
        SELECT to_task_id INTO current_task
        FROM gantt_task_dependencies 
        WHERE workspace_id = p_workspace_id 
          AND doc_id = p_doc_id 
          AND from_task_id = current_task
        LIMIT 1;
        
        -- 如果没有找到依赖任务，结束循环
        IF current_task IS NULL THEN
            LEAVE dependency_loop;
        END IF;
        
        -- 如果回到了起始任务，说明有循环
        IF current_task = p_from_task_id THEN
            SET has_cycle = TRUE;
            LEAVE dependency_loop;
        END IF;
    END LOOP;
    
    RETURN has_cycle;
END //

DELIMITER ;

-- 5. 触发器：更新时间戳
CREATE TRIGGER tr_gantt_config_updated_at
    BEFORE UPDATE ON gantt_view_configs
    FOR EACH ROW
    SET NEW.updated_at = CURRENT_TIMESTAMP;

CREATE TRIGGER tr_gantt_dependency_updated_at
    BEFORE UPDATE ON gantt_task_dependencies
    FOR EACH ROW
    SET NEW.updated_at = CURRENT_TIMESTAMP;