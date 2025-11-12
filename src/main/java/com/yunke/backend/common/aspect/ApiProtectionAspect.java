package com.yunke.backend.common.aspect;

import com.yunke.backend.common.annotation.ProtectAI;
import com.yunke.backend.common.annotation.ProtectApi;
import com.yunke.backend.common.annotation.ProtectUpload;
import com.yunke.backend.security.dto.security.SecurityEvent;
import com.yunke.backend.security.enums.SecurityEventType;
import com.yunke.backend.security.enums.SecurityLevel;
import com.yunke.backend.common.exception.BusinessException;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.security.service.SecurityMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 🎯 API保护切面
 * 
 * 拦截带有保护注解的方法，实现频率限制
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiProtectionAspect {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityMonitorService securityMonitor;
    
    /**
     * 保护AI接口
     */
    @Around("@annotation(protectAI)")
    public Object protectAIEndpoint(ProceedingJoinPoint joinPoint, ProtectAI protectAI) 
            throws Throwable {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        
        String key = "security:ai_calls:" + userId;
        int limit = protectAI.limit();
        int timeWindow = protectAI.timeWindowMinutes();
        
        if (!checkRateLimit(key, limit, timeWindow)) {
            // 记录滥用事件
            recordAbuseEvent(userId, "AI接口调用超限", 
                           String.format("超过限制: %d次/%d分钟", limit, timeWindow));
            
            throw new BusinessException(protectAI.message());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 保护文件上传接口
     */
    @Around("@annotation(protectUpload)")
    public Object protectUploadEndpoint(ProceedingJoinPoint joinPoint, ProtectUpload protectUpload) 
            throws Throwable {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        
        String key = "security:upload_count:" + userId;
        int limit = protectUpload.limit();
        int timeWindow = protectUpload.timeWindowMinutes();
        
        if (!checkRateLimit(key, limit, timeWindow)) {
            // 记录滥用事件
            recordAbuseEvent(userId, "文件上传超限", 
                           String.format("超过限制: %d次/%d分钟", limit, timeWindow));
            
            throw new BusinessException(protectUpload.message());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 保护通用API接口
     */
    @Around("@annotation(protectApi)")
    public Object protectApiEndpoint(ProceedingJoinPoint joinPoint, ProtectApi protectApi) 
            throws Throwable {
        
        String identifier;
        if (protectApi.perUser()) {
            identifier = getCurrentUserId();
            if (identifier == null) {
                throw new BusinessException("请先登录");
            }
        } else {
            identifier = getCurrentIp();
        }
        
        // 获取方法名作为key的一部分
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + 
                          "." + method.getName();
        
        String key = "security:api_limit:" + methodName + ":" + identifier;
        int limit = protectApi.limit();
        int timeWindow = protectApi.timeWindowMinutes();
        
        if (!checkRateLimit(key, limit, timeWindow)) {
            // 记录滥用事件
            recordAbuseEvent(identifier, "API调用超限: " + methodName, 
                           String.format("超过限制: %d次/%d分钟", limit, timeWindow));
            
            throw new BusinessException(protectApi.message());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 检查频率限制
     */
    private boolean checkRateLimit(String key, int limit, int timeWindowMinutes) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count != null && count == 1) {
                redisTemplate.expire(key, timeWindowMinutes, TimeUnit.MINUTES);
            }
            
            if (count != null && count > limit) {
                log.warn("⚠️ API调用超限: key={}, count={}, limit={}", 
                        key, count, limit);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("检查频率限制失败: {}", e.getMessage(), e);
            // 发生异常时允许访问，避免误伤
            return true;
        }
    }
    
    /**
     * 记录滥用事件
     */
    private void recordAbuseEvent(String userId, String operation, String details) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            securityMonitor.recordSecurityEvent(SecurityEvent.builder()
                .type(SecurityEventType.API_ABUSE)
                .level(SecurityLevel.MEDIUM)
                .ip(request != null ? getClientIp(request) : "unknown")
                .userId(userId)
                .requestPath(request != null ? request.getRequestURI() : null)
                .requestMethod(request != null ? request.getMethod() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .details(operation + " - " + details)
                .action("REQUEST_BLOCKED")
                .build());
                
        } catch (Exception e) {
            log.error("记录滥用事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && 
                auth.getPrincipal() instanceof AffineUserDetails) {
                return ((AffineUserDetails) auth.getPrincipal()).getUserId();
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取当前IP
     */
    private String getCurrentIp() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? getClientIp(request) : "unknown";
    }
    
    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
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
}

