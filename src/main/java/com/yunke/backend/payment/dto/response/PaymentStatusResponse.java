package com.yunke.backend.payment.dto.response;

import lombok.Data;

/**
 * 支付状态响应
 */
@Data
public class PaymentStatusResponse {
    
    /**
     * 是否已支付
     */
    private Boolean hasPaid;
    
    /**
     * 支付状态
     */
    private String status;
    
    /**
     * 交易ID
     */
    private String transactionId;
    
    /**
     * 支付时间
     */
    private String paymentTime;
    
    /**
     * 支付金额
     */
    private String amount;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
}