package com.yunke.backend.payment.dto.jeepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款订单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderRequest {
    
    /**
     * 商户号
     */
    private String mchNo;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 商户退款单号
     */
    private String mchRefundNo;
    
    /**
     * 支付订单号(与mchOrderNo二选一)
     */
    private String payOrderId;
    
    /**
     * 商户订单号(与payOrderId二选一)
     */
    private String mchOrderNo;
    
    /**
     * 退款金额(分)
     */
    private Long refundAmount;
    
    /**
     * 退款原因
     */
    private String refundReason;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 异步通知地址
     */
    private String notifyUrl;
    
    /**
     * 商户扩展参数
     */
    private String extParam;
    
    /**
     * 签名
     */
    private String sign;
}