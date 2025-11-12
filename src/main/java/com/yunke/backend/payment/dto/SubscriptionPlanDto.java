package com.yunke.backend.payment.dto;

import com.yunke.backend.payment.domain.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订阅计划DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanDto {
    /**
     * 计划类型
     */
    private Subscription.SubscriptionPlan plan;
    
    /**
     * 计划名称
     */
    private String name;
    
    /**
     * 计划描述
     */
    private String description;
    
    /**
     * 月价格
     */
    private BigDecimal monthlyPrice;
    
    /**
     * 年价格
     */
    private BigDecimal yearlyPrice;
    
    /**
     * 功能特性列表
     */
    private List<String> features;
    
    /**
     * 是否推荐
     */
    private boolean recommended;
}