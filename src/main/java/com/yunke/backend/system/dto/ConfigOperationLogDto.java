package com.yunke.backend.system.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 配置操作日志DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigOperationLogDto {
    
    /**
     * 日志ID
     */
    private String id;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 模块名称
     */
    private String moduleName;
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 操作前的值
     */
    private String oldValue;
    
    /**
     * 操作后的值
     */
    private String newValue;
    
    /**
     * 操作用户
     */
    private String operator;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 操作来源IP
     */
    private String sourceIp;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 操作结果
     */
    private String result;
}