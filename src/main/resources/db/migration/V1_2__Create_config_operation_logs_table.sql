-- 创建配置操作日志表
CREATE TABLE IF NOT EXISTS config_operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    config_key VARCHAR(200) NOT NULL COMMENT '配置键',
    old_value TEXT COMMENT '操作前的值',
    new_value TEXT COMMENT '操作后的值',
    operator VARCHAR(100) NOT NULL COMMENT '操作用户',
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    source_ip VARCHAR(45) COMMENT '操作来源IP',
    description VARCHAR(500) COMMENT '操作描述',
    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
    error_message TEXT COMMENT '错误信息',
    INDEX idx_module_name (module_name),
    INDEX idx_operator (operator),
    INDEX idx_operation_time (operation_time),
    INDEX idx_operation_type (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置操作日志表';