-- AFFiNE支付订单表
CREATE TABLE IF NOT EXISTS affine_payment_orders (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    workspace_id VARCHAR(36) COMMENT '工作空间ID(可选)',
    jeepay_order_no VARCHAR(64) COMMENT 'Jeepay订单号',
    plan_type VARCHAR(32) COMMENT '订阅计划类型',
    amount BIGINT NOT NULL COMMENT '支付金额(分)',
    payment_method VARCHAR(32) COMMENT '支付方式',
    subject VARCHAR(255) COMMENT '商品标题',
    description VARCHAR(500) COMMENT '商品描述',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
    pay_data_type VARCHAR(32) COMMENT '支付数据类型',
    pay_data TEXT COMMENT '支付数据',
    pay_url VARCHAR(500) COMMENT '支付URL',
    qr_code_url VARCHAR(500) COMMENT '二维码URL',
    expire_time DATETIME COMMENT '过期时间',
    completed_at DATETIME COMMENT '支付完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_jeepay_order_no (jeepay_order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AFFiNE支付订单表';

-- 支付订单历史表（用于审计）
CREATE TABLE IF NOT EXISTS affine_payment_order_history (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL COMMENT '订单ID',
    old_status VARCHAR(32) COMMENT '原状态',
    new_status VARCHAR(32) NOT NULL COMMENT '新状态',
    change_reason VARCHAR(255) COMMENT '状态变更原因',
    operator_id VARCHAR(36) COMMENT '操作者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_order_id (order_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (order_id) REFERENCES affine_payment_orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付订单历史表';

-- 支付方式配置表
CREATE TABLE IF NOT EXISTS affine_payment_methods (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE COMMENT '支付方式代码',
    name VARCHAR(64) NOT NULL COMMENT '支付方式名称',
    description VARCHAR(255) COMMENT '描述',
    icon_url VARCHAR(255) COMMENT '图标URL',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    config JSON COMMENT '支付方式配置',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方式配置表';

-- 初始化支付方式数据
INSERT INTO affine_payment_methods (id, code, name, description, icon_url, enabled, sort_order) VALUES 
(UUID(), 'alipay', '支付宝', '支付宝支付', '/icons/alipay.png', TRUE, 1),
(UUID(), 'wxpay', '微信支付', '微信支付', '/icons/wxpay.png', TRUE, 2),
(UUID(), 'unionpay', '银联支付', '中国银联', '/icons/unionpay.png', TRUE, 3)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 订阅计划配置表
CREATE TABLE IF NOT EXISTS affine_subscription_plans (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE COMMENT '计划代码',
    name VARCHAR(64) NOT NULL COMMENT '计划名称',
    description TEXT COMMENT '计划描述',
    price BIGINT NOT NULL COMMENT '价格(分)',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '货币',
    duration_type VARCHAR(16) NOT NULL COMMENT '周期类型: monthly, yearly',
    duration_value INT NOT NULL COMMENT '周期数值',
    features JSON COMMENT '功能特性配置',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅计划配置表';

-- 初始化订阅计划数据
INSERT INTO affine_subscription_plans (id, code, name, description, price, duration_type, duration_value, features) VALUES 
(UUID(), 'free', '免费版', 'AFFiNE免费版本', 0, 'monthly', 1, '{"storage": "10GB", "collaborators": 3}'),
(UUID(), 'pro_monthly', 'Pro月度版', 'AFFiNE Pro月度订阅', 2900, 'monthly', 1, '{"storage": "100GB", "collaborators": 10, "ai": true}'),
(UUID(), 'pro_yearly', 'Pro年度版', 'AFFiNE Pro年度订阅', 29900, 'yearly', 1, '{"storage": "100GB", "collaborators": 10, "ai": true}'),
(UUID(), 'team_monthly', '团队月度版', 'AFFiNE团队月度订阅', 8900, 'monthly', 1, '{"storage": "1TB", "collaborators": 50, "ai": true, "admin": true}')
ON DUPLICATE KEY UPDATE name=VALUES(name), price=VALUES(price), features=VALUES(features);