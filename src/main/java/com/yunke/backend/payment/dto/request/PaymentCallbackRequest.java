package com.yunke.backend.payment.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 支付回调请求
 */
@Data
public class PaymentCallbackRequest {
    
    /**
     * 交易ID
     */
    @NotBlank(message = "交易ID不能为空")
    private String transactionId;
    
    /**
     * 支付状态
     */
    @NotBlank(message = "支付状态不能为空")
    private String status;
    
    /**
     * 第三方支付平台的交易号
     */
    private String thirdPartyTransactionId;
    
    /**
     * 支付时间
     */
    private String paymentTime;
    
    /**
     * 签名
     */
    private String signature;
    
    /**
     * 其他扩展字段
     */
    private String extra;
}