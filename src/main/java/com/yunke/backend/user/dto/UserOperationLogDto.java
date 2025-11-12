package com.yunke.backend.user.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 用户操作日志DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOperationLogDto {
    
    /**
     * 日志ID
     */
    private String id;
    
    /**
     * 操作用户ID
     */
    private String operatorId;
    
    /**
     * 操作用户名称
     */
    private String operatorName;
    
    /**
     * 目标用户ID
     */
    private String targetUserId;
    
    /**
     * 目标用户名称
     */
    private String targetUserName;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 操作详情（JSON格式）
     */
    private String details;
    
    /**
     * 操作结果
     */
    private String result;
    
    /**
     * 操作IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 操作时间
     */
    private LocalDateTime operatedAt;
}