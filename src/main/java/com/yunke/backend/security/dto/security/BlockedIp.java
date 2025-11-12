package com.yunke.backend.security.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 被封禁的IP DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedIp {
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 封禁原因
     */
    private String reason;
    
    /**
     * 剩余封禁时间（分钟）
     */
    private Long remainingMinutes;
    
    /**
     * 封禁时间
     */
    private String blockedAt;
}

