package com.yunke.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 收入统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsDto {
    /**
     * 总收入
     */
    private BigDecimal totalRevenue;
    
    /**
     * 月收入
     */
    private BigDecimal monthlyRevenue;
    
    /**
     * 年收入
     */
    private BigDecimal yearlyRevenue;
    
    /**
     * 平均订单价值
     */
    private BigDecimal averageOrderValue;
    
    /**
     * 总订单数
     */
    private Long totalOrders;
}