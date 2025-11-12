package com.yunke.backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunke.backend.security.dto.security.SecurityEvent;
import com.yunke.backend.security.enums.SecurityEventType;
import com.yunke.backend.security.enums.SecurityLevel;
import com.yunke.backend.security.service.SecurityMonitorService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 🛡️ SQL注入防护过滤器
 * 
 * 检测并拦截SQL注入攻击尝试
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SqlInjectionFilter implements Filter {
    
    private final SecurityMonitorService securityMonitor;
    private final ObjectMapper objectMapper;
    
    // SQL注入特征关键词正则表达式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|" +
        "script|javascript|alert|onerror|onclick|onfocus|onload|onmouseover|" +
        "eval|expression|vbscript|behaviour|<script|</script>).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // 常见SQL注入符号
    private static final Pattern SQL_SYMBOL_PATTERN = Pattern.compile(
        ".*('|(--|;|\\|\\||/\\*|\\*/|xp_|sp_|0x)).*"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        String path = req.getRequestURI();
        String method = req.getMethod();
        
        // 跳过不需要检查的路径
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        try {
            // 检查URL参数
            if (req.getQueryString() != null && 
                isSqlInjection(req.getQueryString())) {
                handleSqlInjection(req, resp, "QueryString", req.getQueryString());
                return;
            }
            
            // 检查所有请求参数
            Map<String, String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                for (String value : entry.getValue()) {
                    if (isSqlInjection(value)) {
                        handleSqlInjection(req, resp, entry.getKey(), value);
                        return;
                    }
                }
            }
            
            // 继续处理请求
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("SQL注入过滤器异常: {}", e.getMessage(), e);
            chain.doFilter(request, response);
        }
    }
    
    /**
     * 判断是否包含SQL注入特征
     */
    private boolean isSqlInjection(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        String decoded = value;
        try {
            // 尝试URL解码
            decoded = java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
        }
        
        // 检查SQL关键词
        if (SQL_INJECTION_PATTERN.matcher(decoded).matches()) {
            return true;
        }
        
        // 检查SQL符号
        if (SQL_SYMBOL_PATTERN.matcher(decoded).matches()) {
            // 排除正常的单引号使用（如搜索"it's"）
            if (!decoded.matches("^[a-zA-Z0-9\\s']+$")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 处理SQL注入攻击
     */
    private void handleSqlInjection(HttpServletRequest req, HttpServletResponse resp,
                                   String paramName, String paramValue) throws IOException {
        String ip = getClientIp(req);
        String path = req.getRequestURI();
        
        // 记录安全事件
        securityMonitor.recordSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.SQL_INJECTION)
            .level(SecurityLevel.CRITICAL)
            .ip(ip)
            .requestPath(path)
            .requestMethod(req.getMethod())
            .userAgent(req.getHeader("User-Agent"))
            .details(String.format("SQL注入尝试 - 参数: %s, 值: %s", paramName, paramValue))
            .action("REQUEST_BLOCKED")
            .build());
        
        log.error("🔴 检测到SQL注入尝试！IP: {}, 路径: {}, 参数: {}, 值: {}", 
                 ip, path, paramName, paramValue);
        
        // 返回错误响应
        sendErrorResponse(resp, "检测到非法输入，请检查您的请求参数");
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 判断是否应该跳过检查
     */
    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/static/") ||
               path.startsWith("/public/") ||
               path.startsWith("/uploads/");
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", 400);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}

