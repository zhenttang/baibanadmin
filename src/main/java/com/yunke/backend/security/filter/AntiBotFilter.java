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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 🤖 反爬虫防护过滤器
 * 
 * 防护策略：
 * 1. 检测User-Agent特征
 * 2. 检测访问频率
 * 3. 检测可疑行为（无Referer访问数据接口）
 * 4. 封禁疑似爬虫IP
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class AntiBotFilter implements Filter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityMonitorService securityMonitor;
    private final SecurityProtectionConfig config;
    private final ObjectMapper objectMapper;
    
    // 已知爬虫User-Agent特征（小写）
    private static final Set<String> BOT_PATTERNS = Set.of(
        "bot", "spider", "crawl", "scraper", "scraping",
        "curl", "wget", "python", "java", "go-http",
        "httpclient", "okhttp", "requests", "axios"
    );
    
    // 数据接口路径（容易被爬取）
    private static final Set<String> DATA_API_PATTERNS = Set.of(
        "/api/community/documents",
        "/api/search",
        "/api/workspaces",
        "/list",
        "/export"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!config.getBot().isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        String ip = getClientIp(req);
        String path = req.getRequestURI();
        String userAgent = req.getHeader("User-Agent");
        
        // 跳过静态资源
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        try {
            // 1. 检测User-Agent
            if (isBot(userAgent)) {
                handleBot(req, resp, "检测到爬虫User-Agent: " + userAgent);
                return;
            }
            
            // 2. 检测访问频率
            String visitKey = "security:page_visit:" + ip;
            Long visits = redisTemplate.opsForValue().increment(visitKey);
            if (visits != null && visits == 1) {
                redisTemplate.expire(visitKey, 1, TimeUnit.MINUTES);
            }
            
            int maxPages = config.getBot().getMaxPagesPerMinute();
            
            if (visits != null && visits > maxPages) {
                // 疑似爬虫，封禁IP
                int blockDuration = config.getBot().getBlockDurationMinutes();
                securityMonitor.blockIp(ip, 
                    "疑似爬虫 - 访问频率: " + visits + "页/分钟", blockDuration);
                
                handleBot(req, resp, 
                    String.format("访问频率过高: %d页/分钟（限制: %d页）", visits, maxPages));
                return;
            }
            
            // 3. 检测可疑行为
            if (isSuspiciousBehavior(req)) {
                log.warn("⚠️ 可疑访问：IP: {}, Path: {}, UA: {}, Referer: {}", 
                        ip, path, userAgent, req.getHeader("Referer"));
                
                // 记录但不拦截，给一次机会
                securityMonitor.recordSecurityEvent(SecurityEvent.builder()
                    .type(SecurityEventType.BOT)
                    .level(SecurityLevel.MEDIUM)
                    .ip(ip)
                    .requestPath(path)
                    .requestMethod(req.getMethod())
                    .userAgent(userAgent)
                    .details("可疑访问行为：无Referer直接访问数据接口")
                    .action("LOGGED")
                    .build());
            }
            
            // 继续处理请求
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("反爬虫过滤器异常: {}", e.getMessage(), e);
            chain.doFilter(request, response);
        }
    }
    
    /**
     * 判断是否是爬虫
     */
    private boolean isBot(String userAgent) {
        // 没有User-Agent很可疑
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return true;
        }
        
        String ua = userAgent.toLowerCase();
        
        // 检查是否包含爬虫特征
        for (String pattern : BOT_PATTERNS) {
            if (ua.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否是可疑行为
     */
    private boolean isSuspiciousBehavior(HttpServletRequest req) {
        String path = req.getRequestURI();
        String referer = req.getHeader("Referer");
        
        // 检查是否是数据接口
        boolean isDataApi = DATA_API_PATTERNS.stream()
            .anyMatch(path::contains);
        
        if (!isDataApi) {
            return false;
        }
        
        // 数据接口没有Referer或Referer不是本站
        if (referer == null || referer.isEmpty()) {
            return true;
        }
        
        String serverName = req.getServerName();
        return !referer.contains(serverName);
    }
    
    /**
     * 处理爬虫请求
     */
    private void handleBot(HttpServletRequest req, HttpServletResponse resp, String details) 
            throws IOException {
        String ip = getClientIp(req);
        String path = req.getRequestURI();
        String userAgent = req.getHeader("User-Agent");
        
        // 记录安全事件
        securityMonitor.recordSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.BOT)
            .level(SecurityLevel.HIGH)
            .ip(ip)
            .requestPath(path)
            .requestMethod(req.getMethod())
            .userAgent(userAgent)
            .details(details)
            .action("REQUEST_BLOCKED")
            .build());
        
        log.warn("🤖 检测到爬虫！IP: {}, Path: {}, UA: {}, 详情: {}", 
                ip, path, userAgent, details);
        
        // 返回403或者假数据
        sendBotResponse(resp);
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
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg");
    }
    
    /**
     * 发送爬虫响应
     */
    private void sendBotResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", 403);
        result.put("message", "Access Denied");
        result.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}

