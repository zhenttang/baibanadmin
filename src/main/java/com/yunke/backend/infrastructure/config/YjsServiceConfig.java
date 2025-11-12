package com.yunke.backend.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * YJS服务配置
 * 配置RestTemplate用于调用YJS微服务
 */
@Configuration
public class YjsServiceConfig {

    /**
     * 为YJS服务创建专用的RestTemplate
     * 配置超时和连接池
     */
    @Bean
    public RestTemplate yjsRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);  // 连接超时 2秒
        factory.setReadTimeout(5000);     // 读取超时 5秒

        return builder
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(5))
            .requestFactory(() -> factory)
            .build();
    }
}
