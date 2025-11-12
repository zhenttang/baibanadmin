package com.yunke.backend.payment.dto.jeepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款订单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderResponse {
    
    /**
     * 退款订单号
     */
    private String refundOrderId;
    
    /**
     * 商户退款单号
     */
    private String mchRefundNo;
    
    /**
     * 支付订单号
     */
    private String payOrderId;
    
    /**
     * 商户订单号
     */
    private String mchOrderNo;
    
    /**
     * 退款金额(分)
     */
    private Long refundAmount;
    
    /**
     * 退款状态: 0-订单生成, 1-退款中, 2-退款成功, 3-退款失败
     */
    private Integer state;
    
    /**
     * 渠道退款单号
     */
    private String channelOrderNo;
    
    /**
     * 错误代码
     */
    private String errCode;
    
    /**
     * 错误描述
     */
    private String errMsg;
}