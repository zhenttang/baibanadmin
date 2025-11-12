package com.yunke.backend.infrastructure.config;

import com.yunke.backend.interceptor.MetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MetricsInterceptor metricsInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加请求日志拦截器 - 用于调试
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/copilot/**");
                
        // 添加指标收集拦截器
        registry.addInterceptor(metricsInterceptor)
                .addPathPatterns("/api/**") // 只对API路径应用
                .excludePathPatterns(
                    "/api/admin/metrics/**", // 排除指标查询API，避免循环
                    "/api/health/**",        // 排除健康检查
                    "/actuator/**"           // 排除actuator端点
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的静态资源访问 - 使用绝对路径
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir)
                .setCachePeriod(3600); // 缓存1小时
        
        // 添加调试日志
        System.out.println("静态资源映射配置: /uploads/** -> file:" + uploadDir);
    }
}