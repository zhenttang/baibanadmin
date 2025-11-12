package com.yunke.backend.payment.dto.jeepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一下单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedOrderResponse {
    
    /**
     * 支付订单号
     */
    private String payOrderId;
    
    /**
     * 商户订单号
     */
    private String mchOrderNo;
    
    /**
     * 订单状态
     */
    private Integer orderState;
    
    /**
     * 支付参数类型
     */
    private String payDataType;
    
    /**
     * 支付参数
     */
    private String payData;
    
    /**
     * 支付地址(重定向地址)
     */
    private String payUrl;
    
    /**
     * 错误代码
     */
    private String errCode;
    
    /**
     * 错误描述
     */
    private String errMsg;
}