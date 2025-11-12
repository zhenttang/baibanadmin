package com.yunke.backend.payment.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付订单响应
 */
@Data
public class PaymentOrderResponse {
    
    /**
     * 内部订单ID
     */
    private Long orderId;
    
    /**
     * 交易ID
     */
    private String transactionId;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 支付链接或二维码内容
     */
    private String paymentUrl;
    
    /**
     * 二维码Base64编码（如果是扫码支付）
     */
    private String qrCode;
    
    /**
     * 订单过期时间（时间戳）
     */
    private Long expireTime;
    
    /**
     * 支付状态
     */
    private String status;
    
    /**
     * 文档信息
     */
    private DocumentInfo documentInfo;
    
    @Data
    public static class DocumentInfo {
        private String id;
        private String title;
        private String description;
        private String authorName;
    }
}