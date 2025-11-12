package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Jeepay SDK配置 - 使用官方SDK
 */
@Configuration
@ConfigurationProperties(prefix = "affine.jeepay")
@Data
public class JeepaySDKProperties {
    
    /**
     * 商户号
     */
    private String mchNo = "AFFINE001";
    
    /**
     * 应用ID
     */
    private String appId = "APP_AFFINE_001";
    
    /**
     * API密钥
     */
    private String apiKey = "AFFINE_API_KEY_2024";
    
    /**
     * API地址
     */
    private String apiUrl = "http://172.24.48.1:9216";
    
    /**
     * 异步通知地址
     */
    private String notifyUrl = "http://172.24.48.1:8080/api/community/payments/callback";
    
    /**
     * 支付成功跳转地址
     */
    private String returnUrl = "http://172.24.48.1:3000/payment/success";
}