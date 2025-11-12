package com.yunke.backend.ai.dto;

import com.yunke.backend.ai.domain.entity.CopilotQuota;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Copilot配额DTO
 * 对应Node.js版本的CopilotQuotaType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotQuotaDto {

    private String userId;
    
    private String workspaceId;
    
    private CopilotQuota.CopilotFeature feature;
    
    private Integer limitPerDay;
    
    private Integer limitPerMonth;
    
    private Integer usedToday;
    
    private Integer usedThisMonth;
    
    private Integer tokenLimitPerDay;
    
    private Integer tokenLimitPerMonth;
    
    private Integer tokensUsedToday;
    
    private Integer tokensUsedThisMonth;
    
    private LocalDateTime lastResetDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // 计算字段
    private Boolean canUse;
    
    private Double requestUsagePercent;
    
    private Double tokenUsagePercent;
    
    private Integer remainingRequests;
    
    private Integer remainingTokens;
}