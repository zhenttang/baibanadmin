package com.yunke.backend.payment.dto.jeepay;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一下单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedOrderRequest {
    
    /**
     * 商户号
     */
    private String mchNo;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 商户订单号
     */
    private String mchOrderNo;
    
    /**
     * 支付方式代码
     */
    private String wayCode;
    
    /**
     * 支付金额(分)
     */
    private Long amount;
    
    /**
     * 货币代码
     */
    private String currency;
    
    /**
     * 商品标题
     */
    private String subject;
    
    /**
     * 商品描述
     */
    private String body;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 异步通知地址
     */
    private String notifyUrl;
    
    /**
     * 同步跳转地址
     */
    private String returnUrl;
    
    /**
     * 订单失效时间
     */
    private Long expiredTime;
    
    /**
     * 渠道用户标识
     */
    private String channelUserId;
    
    /**
     * 渠道扩展参数
     */
    private String channelExtra;
    
    /**
     * 商户扩展参数
     */
    private String extParam;
    
    /**
     * 签名
     */
    private String sign;
}