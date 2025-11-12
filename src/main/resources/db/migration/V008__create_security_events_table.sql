-- =====================================================
-- 创建安全事件表 (security_events)
-- 用途：记录系统安全事件，包括登录失败、IP封禁、攻击检测等
-- =====================================================

-- 创建安全事件表
CREATE TABLE IF NOT EXISTS `security_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型（如：LOGIN_FAILED, IP_BLOCKED, DDoS_ATTACK等）',
    `severity` VARCHAR(20) NOT NULL COMMENT '事件级别（INFO, WARNING, ERROR, CRITICAL）',
    `description` VARCHAR(500) NOT NULL COMMENT '事件描述',
    `user_id` VARCHAR(100) NULL COMMENT '用户ID（如果事件与用户相关）',
    `username` VARCHAR(255) NULL COMMENT '用户名（如果事件与用户相关）',
    `source_ip` VARCHAR(45) NOT NULL COMMENT '来源IP地址（支持IPv4和IPv6）',
    `user_agent` TEXT NULL COMMENT '用户代理字符串',
    `event_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
    `request_path` VARCHAR(500) NULL COMMENT '请求路径',
    `request_method` VARCHAR(10) NULL COMMENT '请求方法（GET, POST等）',
    `country` VARCHAR(100) NULL COMMENT '国家',
    `region` VARCHAR(100) NULL COMMENT '地区/省份',
    `city` VARCHAR(100) NULL COMMENT '城市',
    `isp` VARCHAR(200) NULL COMMENT 'ISP服务商',
    `longitude` DOUBLE NULL COMMENT '经度',
    `latitude` DOUBLE NULL COMMENT '纬度',
    `details` TEXT NULL COMMENT '事件详情（JSON格式）',
    `handled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已处理（0=未处理，1=已处理）',
    `resolution` VARCHAR(500) NULL COMMENT '处理结果',
    `resolved_at` TIMESTAMP NULL COMMENT '处理时间',
    `resolved_by` VARCHAR(100) NULL COMMENT '处理人',
    PRIMARY KEY (`id`),
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_severity` (`severity`),
    INDEX `idx_source_ip` (`source_ip`),
    INDEX `idx_event_time` (`event_time`),
    INDEX `idx_handled` (`handled`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_event_time_type` (`event_time`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全事件表';

-- 创建分区表（可选，用于大数据量场景）
-- 如果不需要分区，可以注释掉以下代码
-- ALTER TABLE `security_events` PARTITION BY RANGE (YEAR(event_time)) (
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION p2026 VALUES LESS THAN (2027),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

