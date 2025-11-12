package com.yunke.backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunke.backend.infrastructure.config.SecurityProtectionConfig;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 🛡️ DDoS/CC攻击防护过滤器
 * 
 * 防护策略：
 * 1. 检查IP是否在黑名单中
 * 2. 统计单IP请求频率
 * 3. 超过阈值自动封禁IP
 * 4. 记录攻击事件并告警
 */
@Component
@Order(1)  // 最高优先级，第一个执行
@RequiredArgsConstructor
@Slf4j
public class AntiDDoSFilter implements Filter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityMonitorService securityMonitor;
    private final SecurityProtectionConfig config;
    private final ObjectMapper objectMapper;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!config.getDdos().isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        // 获取客户端IP
        String ip = getClientIp(req);
        String path = req.getRequestURI();
        
        // 跳过健康检查和静态资源
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        String blockKey = "security:blocked:ip:" + ip;
        String countKey = "security:req_count:" + ip;
        
        try {
            // 1. 检查是否在黑名单中
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
                String reason = (String) redisTemplate.opsForValue().get(blockKey);
                log.warn("🚫 拦截被封禁IP: {}, 原因: {}, 路径: {}", ip, reason, path);
                
                sendBlockedResponse(resp, "您的IP已被临时封禁，请稍后再试");
                return;
            }
            
            // 2. 统计请求次数
            Long count = redisTemplate.opsForValue().increment(countKey);
            if (count != null && count == 1) {
                redisTemplate.expire(countKey, 1, TimeUnit.MINUTES);
            }
            
            int maxRequests = config.getDdos().getMaxRequestsPerMinute();
            int warningThreshold = config.getDdos().getWarningThreshold();
            
            // 3. 警告日志（达到80%阈值）
            if (count != null && count >= (maxRequests * warningThreshold / 100)) {
                log.warn("⚠️ IP {} 请求频率较高: {}/{}/分钟", ip, count, maxRequests);
            }
            
            // 4. 判断是否超过阈值
            if (count != null && count > maxRequests) {
                // 封禁IP
                int blockDuration = config.getDdos().getBlockDurationMinutes();
                securityMonitor.blockIp(ip, "DDoS攻击 - 请求频率: " + count + "/分钟", blockDuration);
                
                // 记录安全事件
                securityMonitor.recordSecurityEvent(SecurityEvent.builder()
                    .type(SecurityEventType.DDOS)
                    .level(SecurityLevel.HIGH)
                    .ip(ip)
                    .requestPath(path)
                    .requestMethod(req.getMethod())
                    .userAgent(req.getHeader("User-Agent"))
                    .details(String.format("请求频率过高: %d次/分钟（限制: %d次）", 
                            count, maxRequests))
                    .action("IP_BLOCKED")
                    .build());
                
                log.error("🔴 检测到DDoS攻击！IP: {}, 请求次数: {}/分钟, 已封禁", 
                         ip, count);
                
                sendBlockedResponse(resp, "请求过于频繁，您的IP已被临时封禁");
                return;
            }
            
            // 5. 继续处理请求
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("DDoS过滤器异常: {}", e.getMessage(), e);
            // 发生异常时继续处理请求，避免影响正常用户
            chain.doFilter(request, response);
        }
    }
    
    /**
     * 获取客户端真实IP
     * 考虑代理和负载均衡的情况
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For 可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 判断是否应该跳过检查
     */
    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/static/") ||
               path.startsWith("/public/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".ico");
    }
    
    /**
     * 发送封禁响应
     */
    private void sendBlockedResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(429);  // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-RateLimit-Limit", 
            String.valueOf(config.getDdos().getMaxRequestsPerMinute()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("Retry-After", 
            String.valueOf(config.getDdos().getBlockDurationMinutes() * 60));
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", 429);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}

