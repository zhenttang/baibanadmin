package com.yunke.backend.payment.dto;

import com.yunke.backend.payment.domain.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * AFFiNE订单ID
     */
    private String orderId;
    
    /**
     * Jeepay订单ID
     */
    private String jeepayOrderId;
    
    /**
     * 支付数据类型
     */
    private String payDataType;
    
    /**
     * 支付数据
     */
    private String payData;
    
    /**
     * 支付URL(跳转支付)
     */
    private String payUrl;
    
    /**
     * 二维码URL(扫码支付)
     */
    private String qrCodeUrl;
    
    /**
     * 支付金额(分)
     */
    private Long amount;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 支付状态
     */
    private PaymentStatus status;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 错误代码
     */
    private String errorCode;
}