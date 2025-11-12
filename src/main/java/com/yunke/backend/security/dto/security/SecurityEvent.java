package com.yunke.backend.security.dto.security;

import com.yunke.backend.security.enums.SecurityEventType;
import com.yunke.backend.security.enums.SecurityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent implements Serializable {
    
    /**
     * 事件类型
     */
    private SecurityEventType type;
    
    /**
     * 安全级别
     */
    private SecurityLevel level;
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 用户ID（如果已登录）
     */
    private String userId;
    
    /**
     * 请求路径
     */
    private String requestPath;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * User-Agent
     */
    private String userAgent;
    
    /**
     * 详细信息
     */
    private String details;
    
    /**
     * 处理动作
     */
    private String action;
    
    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

