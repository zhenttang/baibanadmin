package com.yunke.backend.payment.dto.request;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建支付订单请求
 */
@Data
public class PaymentOrderRequest {
    
    /**
     * 文档ID
     */
    @NotBlank(message = "文档ID不能为空")
    private String documentId;
    
    /**
     * 支付方式：WECHAT、ALIPAY、BANK_CARD等
     */
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;
    
    /**
     * 支付金额（可选，如果不传则使用文档价格）
     */
    @DecimalMin(value = "0.01", message = "支付金额必须大于0.01")
    private BigDecimal amount;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 回调地址
     */
    private String notifyUrl;
    
    /**
     * 备注信息
     */
    private String remark;
}