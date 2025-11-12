package com.yunke.backend.monitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

/**
 * 请求指标拦截器
 * 自动收集 HTTP 请求指标
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestMetricsInterceptor implements HandlerInterceptor {

    private final MetricsCollector metricsCollector;
    private static final String START_TIME_ATTRIBUTE = "request_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME_ATTRIBUTE, Instant.now());
        
        // 增加活跃连接数
        metricsCollector.incrementActiveConnections();
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            // 获取请求开始时间
            Instant startTime = (Instant) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime != null) {
                Duration duration = Duration.between(startTime, Instant.now());
                
                // 记录请求指标
                String method = request.getMethod();
                String endpoint = getEndpoint(request);
                int status = response.getStatus();
                
                metricsCollector.recordHttpRequest(method, endpoint, status, duration);
                
                // 记录慢请求
                if (duration.toMillis() > 1000) {
                    log.warn("Slow request detected: {} {} took {}ms", 
                        method, endpoint, duration.toMillis());
                }
            }
        } catch (Exception e) {
            log.error("Error recording request metrics", e);
        } finally {
            // 减少活跃连接数
            metricsCollector.decrementActiveConnections();
        }
    }

    /**
     * 获取端点路径（去除查询参数）
     */
    private String getEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }
        
        // 简化路径，将 ID 等动态部分替换为占位符
        return simplifyPath(uri);
    }

    /**
     * 简化路径，将动态部分替换为占位符
     */
    private String simplifyPath(String path) {
        if (path == null) {
            return "unknown";
        }
        
        // 替换常见的动态路径部分
        return path
            .replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}")  // UUID
            .replaceAll("/\\d+", "/{id}")  // 数字 ID
            .replaceAll("/[a-zA-Z0-9_-]{20,}", "/{id}")  // 长字符串 ID
            .replaceAll("//+", "/");  // 多个斜杠合并
    }
}