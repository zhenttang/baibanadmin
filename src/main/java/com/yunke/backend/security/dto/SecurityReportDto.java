package com.yunke.backend.security.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 安全报告DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityReportDto {
    
    /**
     * 报告ID
     */
    private String reportId;
    
    /**
     * 报告标题
     */
    private String title;
    
    /**
     * 报告类型
     */
    private String reportType;
    
    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 统计时间范围
     */
    private TimeRange timeRange;
    
    /**
     * 安全摘要
     */
    private SecuritySummary summary;
    
    /**
     * 威胁统计
     */
    private ThreatStatistics threats;
    
    /**
     * 登录统计
     */
    private LoginStatistics logins;
    
    /**
     * IP访问统计
     */
    private IpAccessStatistics ipAccess;
    
    /**
     * 安全建议
     */
    private List<SecurityRecommendation> recommendations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeRange {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecuritySummary {
        /**
         * 总体安全分数
         */
        private Integer securityScore;
        
        /**
         * 安全等级
         */
        private String securityLevel;
        
        /**
         * 总事件数
         */
        private Long totalEvents;
        
        /**
         * 高危事件数
         */
        private Long criticalEvents;
        
        /**
         * 中危事件数
         */
        private Long warningEvents;
        
        /**
         * 低危事件数
         */
        private Long infoEvents;
        
        /**
         * 已处理事件数
         */
        private Long handledEvents;
        
        /**
         * 未处理事件数
         */
        private Long unhandledEvents;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThreatStatistics {
        /**
         * 暴力破解尝试
         */
        private Long bruteForceAttempts;
        
        /**
         * 可疑IP访问
         */
        private Long suspiciousIpAccess;
        
        /**
         * 异常登录
         */
        private Long anomalousLogins;
        
        /**
         * API滥用
         */
        private Long apiAbuse;
        
        /**
         * 权限提升尝试
         */
        private Long privilegeEscalation;
        
        /**
         * 数据泄露风险
         */
        private Long dataLeakageRisks;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginStatistics {
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
         * 被锁定账户数
         */
        private Long lockedAccounts;
        
        /**
         * 新设备登录次数
         */
        private Long newDeviceLogins;
        
        /**
         * 异地登录次数
         */
        private Long remoteLogins;
        
        /**
         * 登录地点分布
         */
        private Map<String, Long> locationDistribution;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IpAccessStatistics {
        /**
         * 唯一IP数量
         */
        private Long uniqueIpCount;
        
        /**
         * 被阻止的IP数量
         */
        private Long blockedIpCount;
        
        /**
         * 白名单命中次数
         */
        private Long whitelistHits;
        
        /**
         * 黑名单命中次数
         */
        private Long blacklistHits;
        
        /**
         * 国家分布
         */
        private Map<String, Long> countryDistribution;
        
        /**
         * 高频访问IP
         */
        private List<IpAccessInfo> topAccessIps;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IpAccessInfo {
        private String ipAddress;
        private Long accessCount;
        private String country;
        private String lastAccess;
        private Boolean blocked;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityRecommendation {
        /**
         * 建议ID
         */
        private String id;
        
        /**
         * 优先级
         */
        private String priority;
        
        /**
         * 建议标题
         */
        private String title;
        
        /**
         * 建议描述
         */
        private String description;
        
        /**
         * 建议操作
         */
        private String action;
        
        /**
         * 风险等级
         */
        private String riskLevel;
        
        /**
         * 影响范围
         */
        private String impact;
    }
}