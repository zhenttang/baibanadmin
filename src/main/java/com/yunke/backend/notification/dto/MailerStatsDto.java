package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 邮件统计信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailerStatsDto {
    
    /**
     * 总发送邮件数
     */
    private Long totalSent;
    
    /**
     * 发送成功数
     */
    private Long successCount;
    
    /**
     * 发送失败数
     */
    private Long failureCount;
    
    /**
     * 队列中邮件数
     */
    private Long queueSize;
    
    /**
     * 今日发送数
     */
    private Long todaySent;
    
    /**
     * 本周发送数
     */
    private Long weekSent;
    
    /**
     * 本月发送数
     */
    private Long monthSent;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;
    
    /**
     * 成功率（百分比）
     */
    private Double successRate;
    
    /**
     * 当前配置状态
     */
    private String configStatus;
    
    /**
     * 服务状态
     */
    private String serviceStatus;
    
    /**
     * 最后发送时间
     */
    private LocalDateTime lastSentTime;
    
    /**
     * 最后失败时间
     */
    private LocalDateTime lastFailureTime;
    
    /**
     * 最后失败原因
     */
    private String lastFailureReason;
}