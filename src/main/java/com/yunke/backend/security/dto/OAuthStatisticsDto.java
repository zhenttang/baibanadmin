package com.yunke.backend.security.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Map;

/**
 * OAuth统计信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthStatisticsDto {
    
    /**
     * 总登录次数
     */
    private Long totalLogins;
    
    /**
     * 成功登录次数
     */
    private Long successfulLogins;
    
    /**
     * 失败登录次数
     */
    private Long failedLogins;
    
    /**
     * 登录成功率（百分比）
     */
    private Double successRate;
    
    /**
     * 各提供商登录统计
     */
    private Map<String, ProviderStats> providerStats;
    
    /**
     * 最近7天登录趋势
     */
    private Map<LocalDate, Long> weeklyTrend;
    
    /**
     * 活跃用户数
     */
    private Long activeUsers;
    
    /**
     * 新用户数（通过OAuth注册）
     */
    private Long newUsers;
    
    /**
     * 最后更新时间
     */
    private String lastUpdated;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStats {
        /**
         * 提供商名称
         */
        private String provider;
        
        /**
         * 登录次数
         */
        private Long loginCount;
        
        /**
         * 成功次数
         */
        private Long successCount;
        
        /**
         * 失败次数
         */
        private Long failureCount;
        
        /**
         * 用户数
         */
        private Long userCount;
        
        /**
         * 是否启用
         */
        private Boolean enabled;
        
        /**
         * 配置状态
         */
        private String configStatus;
    }
}