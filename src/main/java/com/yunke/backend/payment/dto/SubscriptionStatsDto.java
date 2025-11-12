package com.yunke.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订阅统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatsDto {
    /**
     * 总订阅数
     */
    private Long totalSubscriptions;
    
    /**
     * 活跃订阅数
     */
    private Long activeSubscriptions;
    
    /**
     * 试用订阅数
     */
    private Long trialingSubscriptions;
    
    /**
     * 已取消订阅数
     */
    private Long canceledSubscriptions;
    
    /**
     * 月度经常性收入
     */
    private BigDecimal monthlyRecurringRevenue;
    
    /**
     * 年度经常性收入
     */
    private BigDecimal annualRecurringRevenue;
}