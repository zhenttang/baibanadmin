package com.yunke.backend.infrastructure.config;

import com.yunke.backend.payment.client.JeepayApiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jeepay支付系统配置
 */
@Configuration
@EnableConfigurationProperties(JeepayProperties.class)
public class JeepayConfig {
    
    /**
     * 创建Jeepay API客户端
     */
    @Bean
    public JeepayApiClient jeepayApiClient(JeepayProperties properties) {
        return JeepayApiClient.builder()
            .apiUrl(properties.getApiUrl())
            .mchNo(properties.getMchNo())
            .appId(properties.getAppId())
            .appSecret(properties.getAppSecret())
            .signType(properties.getSignType())
            .connectTimeout(properties.getConnectTimeout())
            .readTimeout(properties.getReadTimeout())
            .build();
    }
}