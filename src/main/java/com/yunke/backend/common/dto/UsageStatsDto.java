package com.yunke.backend.common.dto;

import com.yunke.backend.payment.domain.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 使用量统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsDto {
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 订阅计划
     */
    private Subscription.SubscriptionPlan plan;
    
    /**
     * 已使用文档数
     */
    private Long documentsUsed;
    
    /**
     * 已使用存储空间（字节）
     */
    private Long storageUsed;
    
    /**
     * API调用次数
     */
    private Long apiCalls;
    
    /**
     * 带宽使用量（字节）
     */
    private Long bandwidthUsed;
    
    /**
     * 是否在限制范围内
     */
    private Boolean withinLimits;
}