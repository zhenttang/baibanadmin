package com.yunke.backend.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORP (Cross-Origin Resource Policy) 头部过滤器
 * 用于支持前端的 COEP (Cross-Origin Embedder Policy) 策略
 */
@Component
@WebFilter(urlPatterns = "/uploads/*")
@Slf4j
public class CorpHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 为上传的静态资源添加 CORP 头，支持跨域嵌入
        if (httpRequest.getRequestURI().startsWith("/uploads/")) {
            // cross-origin: 允许跨域访问
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
            log.debug("Added CORP header for: {}", httpRequest.getRequestURI());
        }
        
        chain.doFilter(request, response);
    }
}