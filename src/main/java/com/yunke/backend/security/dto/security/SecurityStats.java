package com.yunke.backend.security.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 安全统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityStats {
    
    /**
     * 今日安全事件总数
     */
    private Long todayEvents;
    
    /**
     * 按类型统计的攻击次数
     */
    private Map<String, Long> attacksByType;
    
    /**
     * 当前被封禁的IP数量
     */
    private Integer blockedIpCount;
    
    /**
     * 今日封禁的IP数量
     */
    private Integer todayBlockedIps;
    
    /**
     * 最近1小时的事件数
     */
    private Long lastHourEvents;
}

