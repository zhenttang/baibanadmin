package com.yunke.backend.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

/**
 * 请求日志拦截器
 * 用于调试请求处理问题
 */
@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().contains("/api/copilot/") && log.isDebugEnabled()) {
            log.debug("=== 请求拦截器 ===");
            log.debug("URI: {}", request.getRequestURI());
            log.debug("Method: {}", request.getMethod());
            log.debug("Content-Type: {}", request.getContentType());
            log.debug("Content-Length: {}", request.getContentLength());
            
            // 记录请求头
            log.debug("请求头:");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.debug("  {}: {}", headerName, request.getHeader(headerName));
            }
            
            // 记录查询参数
            if (request.getQueryString() != null) {
                log.debug("查询参数: {}", request.getQueryString());
            }
            
            log.debug("Handler: {}", handler);
            log.debug("=== 请求拦截器结束 ===");
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (request.getRequestURI().contains("/api/copilot/") && log.isDebugEnabled()) {
            log.debug("=== 响应拦截器 ===");
            log.debug("URI: {}", request.getRequestURI());
            log.debug("Response Status: {}", response.getStatus());
            if (ex != null) {
                log.error("Exception occurred: ", ex);
            }
            log.debug("=== 响应拦截器结束 ===");
        }
    }
}
