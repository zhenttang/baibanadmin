package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jeepay支付系统配置属性
 */
@ConfigurationProperties(prefix = "affine.jeepay")
@Data
public class JeepayProperties {
    
    /**
     * Jeepay API URL
     */
    private String apiUrl = "http://localhost:9216";
    
    /**
     * 商户号
     */
    private String mchNo = "AFFINE001";
    
    /**
     * 应用ID
     */
    private String appId = "APP_AFFINE_001";
    
    /**
     * 应用密钥
     */
    private String appSecret;
    
    /**
     * 签名方式
     */
    private String signType = "MD5";
    
    /**
     * 连接超时时间(毫秒)
     */
    private Integer connectTimeout = 10000;
    
    /**
     * 读取超时时间(毫秒)
     */
    private Integer readTimeout = 30000;
    
    /**
     * 支付回调通知URL
     */
    private String notifyUrl = "http://172.24.48.1:8080/api/payment/notify";
    
    /**
     * 支付成功跳转URL
     */
    private String returnUrl = "https://affine.pro/payment/success";
    
    /**
     * 是否启用沙箱环境
     */
    private Boolean sandbox = true;
}