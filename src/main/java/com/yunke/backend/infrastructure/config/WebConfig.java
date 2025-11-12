package com.yunke.backend.infrastructure.config;

import com.yunke.backend.monitor.RequestMetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RequestMetricsInterceptor requestMetricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加请求指标拦截器
        registry.addInterceptor(requestMetricsInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/health/**", "/metrics/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000",
                        "http://172.24.48.1:8081",
                        "http://localhost:8082",
                        "http://localhost:8081",
                        "http://127.0.0.1:3000",
                        "http://localhost:8083",
                        "http://127.0.0.1:8083",
                        "http://localhost:8084",
                        "http://127.0.0.1:8084",
                        "http://127.0.0.1:8082",
                        // 添加Android开发服务器支持
                        "http://192.168.2.4:8082",
                        "http://192.168.2.4:8080",
                        "http://c.yckeji0316.cn",
                        "http://c.yckeji0316.cn",
                        "http://f.yckeji0316.cn",
                        "http://f.yckeji0316.cn",
                        // 添加部署域名支持  
                        "http://b.yckeji0316.cn",
                        "https://b.yckeji0316.cn",
                        // 添加新的域名支持
                        "http://ykweb.yckeji0316.cn",
                        "https://ykweb.yckeji0316.cn",
                        "http://ykmodile.yckeji0316.cn",
                        "https://ykmodile.yckeji0316.cn",
                        "http://ykadmin.yckeji0316.cn:8080",
                        "https://ykadmin.yckeji0316.cn:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * WebClient.Builder Bean定义
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * RestTemplate Bean定义 - 用于同步HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}