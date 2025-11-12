package com.yunke.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 工作空间ID(可选)
     */
    private String workspaceId;
    
    /**
     * 订阅计划类型
     */
    private String planType;
    
    /**
     * 支付金额(分)
     */
    private Long amount;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 商品标题
     */
    private String subject;
    
    /**
     * 商品描述
     */
    private String description;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 支付成功跳转地址
     */
    private String returnUrl;
}