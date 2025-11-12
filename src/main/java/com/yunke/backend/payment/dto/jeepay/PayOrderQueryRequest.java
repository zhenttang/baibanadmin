package com.yunke.backend.payment.dto.jeepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付订单查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderQueryRequest {
    
    /**
     * 商户号
     */
    private String mchNo;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 商户订单号(与payOrderId二选一)
     */
    private String mchOrderNo;
    
    /**
     * 支付订单号(与mchOrderNo二选一)
     */
    private String payOrderId;
    
    /**
     * 签名
     */
    private String sign;
}