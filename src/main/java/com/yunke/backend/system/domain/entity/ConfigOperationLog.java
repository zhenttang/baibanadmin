package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 配置操作日志实体
 */
@Entity
@Table(name = "config_operation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigOperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 操作类型 (CREATE, UPDATE, DELETE, RELOAD)
     */
    @Column(name = "operation_type", nullable = false)
    private String operationType;
    
    /**
     * 模块名称
     */
    @Column(name = "module_name", nullable = false)
    private String moduleName;
    
    /**
     * 配置键
     */
    @Column(name = "config_key", nullable = false)
    private String configKey;
    
    /**
     * 操作前的值
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    /**
     * 操作后的值
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    /**
     * 操作用户
     */
    @Column(name = "operator", nullable = false)
    private String operator;
    
    /**
     * 操作时间
     */
    @CreationTimestamp
    @Column(name = "operation_time", nullable = false, updatable = false)
    private LocalDateTime operationTime;
    
    /**
     * 操作来源IP
     */
    @Column(name = "source_ip")
    private String sourceIp;
    
    /**
     * 操作描述
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 操作结果 (SUCCESS, FAILED)
     */
    @Column(name = "result", nullable = false)
    private String result;
    
    /**
     * 错误信息（操作失败时）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}