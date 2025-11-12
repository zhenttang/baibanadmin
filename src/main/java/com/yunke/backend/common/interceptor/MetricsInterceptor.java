package com.yunke.backend.interceptor;

import com.yunke.backend.service.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API调用指标拦截器
 * 用于自动记录API调用的性能指标
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsInterceptor implements HandlerInterceptor {

    private final MetricsService metricsService;
    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        try {
            // 计算响应时间
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime != null) {
                long responseTime = System.currentTimeMillis() - startTime;
                String endpoint = request.getRequestURI();
                int statusCode = response.getStatus();
                
                // 记录API调用指标
                metricsService.recordApiCall(endpoint, responseTime, statusCode);
                
                // 如果响应时间过长，记录警告
                if (responseTime > 5000) { // 5秒
                    log.warn("Slow API call detected: {} - {}ms - Status: {}", 
                        endpoint, responseTime, statusCode);
                }
            }
        } catch (Exception e) {
            log.error("Error recording API metrics", e);
        }
    }
}