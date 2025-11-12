package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝SDK配置 - 支持沙箱环境
 */
@Configuration
@ConfigurationProperties(prefix = "affine.alipay")
@Data
public class AlipayProperties {
    
    /**
     * 应用ID
     */
    private String appId = "9021000140675166"; // 沙箱应用ID示例
    
    /**
     * 商户私钥 - RSA2格式
     */
    private String privateKey = ""; // 需要配置您的沙箱私钥
    
    /**
     * 支付宝公钥 - RSA2格式
     */
    private String alipayPublicKey = ""; // 需要配置支付宝沙箱公钥
    
    /**
     * 签名方式
     */
    private String signType = "RSA2";
    
    /**
     * 字符编码格式
     */
    private String charset = "UTF-8";
    
    /**
     * 支付宝网关地址
     */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do"; // 沙箱环境
    
    /**
     * 数据格式
     */
    private String format = "json";
    
    /**
     * 异步通知地址
     */
    private String notifyUrl = "http://172.24.48.1:8080/api/community/payments/alipay/notify";
    
    /**
     * 同步跳转地址
     */
    private String returnUrl = "http://172.24.48.1:3000/payment/success";
    
    /**
     * 是否启用沙箱环境
     */
    private boolean sandbox = true; // 默认使用沙箱
}