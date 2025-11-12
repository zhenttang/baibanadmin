package com.yunke.backend.security.service;

import com.yunke.backend.infrastructure.config.SecurityProtectionConfig;
import com.yunke.backend.security.dto.security.LoginCheckResult;
import com.yunke.backend.security.dto.security.SecurityEvent;
import com.yunke.backend.security.enums.SecurityEventType;
import com.yunke.backend.security.enums.SecurityLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 🔐 登录保护服务
 * 
 * 功能：
 * 1. 检测并防止暴力破解
 * 2. 失败次数达到阈值触发验证码
 * 3. 失败次数过多锁定账号
 * 4. 记录异常登录行为
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginProtectionService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityMonitorService securityMonitor;
    private final SecurityProtectionConfig config;
    
    /**
     * 检查是否允许登录
     */
    public LoginCheckResult checkLoginAllowed(String username, String ip) {
        if (!config.getLogin().isEnabled()) {
            return LoginCheckResult.allowed();
        }
        
        String userKey = "security:login_fail:" + username;
        String ipKey = "security:login_fail_ip:" + ip;
        
        Integer userFails = getFailCount(userKey);
        Integer ipFails = getFailCount(ipKey);
        
        int maxAttempts = config.getLogin().getMaxFailedAttempts();
        int captchaThreshold = config.getLogin().getCaptchaThreshold();
        
        // 判断是否被锁定
        if (userFails >= maxAttempts) {
            Long ttl = redisTemplate.getExpire(userKey, TimeUnit.MINUTES);
            return LoginCheckResult.blocked(
                String.format("账号已被锁定，请%d分钟后再试", ttl != null ? ttl : 0));
        }
        
        if (ipFails >= maxAttempts * 2) {
            Long ttl = redisTemplate.getExpire(ipKey, TimeUnit.MINUTES);
            return LoginCheckResult.blocked(
                String.format("该IP已被临时封禁，请%d分钟后再试", ttl != null ? ttl : 0));
        }
        
        // 判断是否需要验证码
        if (userFails >= captchaThreshold || ipFails >= captchaThreshold) {
            return LoginCheckResult.needCaptcha("需要输入验证码");
        }
        
        // 允许登录
        return LoginCheckResult.allowed();
    }
    
    /**
     * 记录登录失败
     */
    public void recordLoginFailure(String username, String ip, String userAgent) {
        String userKey = "security:login_fail:" + username;
        String ipKey = "security:login_fail_ip:" + ip;
        
        int lockoutMinutes = config.getLogin().getLockoutMinutes();
        
        // 记录失败次数
        Long userFails = redisTemplate.opsForValue().increment(userKey);
        Long ipFails = redisTemplate.opsForValue().increment(ipKey);
        
        // 设置过期时间
        if (userFails != null && userFails == 1) {
            redisTemplate.expire(userKey, lockoutMinutes, TimeUnit.MINUTES);
        }
        if (ipFails != null && ipFails == 1) {
            redisTemplate.expire(ipKey, lockoutMinutes, TimeUnit.MINUTES);
        }
        
        int maxAttempts = config.getLogin().getMaxFailedAttempts();
        
        // 记录安全事件
        SecurityLevel level = SecurityLevel.LOW;
        String action = "LOGGED";
        
        if (userFails != null && userFails >= maxAttempts) {
            level = SecurityLevel.HIGH;
            action = "ACCOUNT_LOCKED";
            log.error("🔴 暴力破解警告！用户: {}, IP: {}, 失败次数: {}", 
                     username, ip, userFails);
        } else if (userFails != null && userFails >= maxAttempts / 2) {
            level = SecurityLevel.MEDIUM;
            log.warn("⚠️ 登录失败次数较多，用户: {}, IP: {}, 失败次数: {}", 
                    username, ip, userFails);
        }
        
        securityMonitor.recordSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.BRUTE_FORCE)
            .level(level)
            .ip(ip)
            .userId(username)
            .userAgent(userAgent)
            .details(String.format("登录失败 - 用户失败次数: %d, IP失败次数: %d", 
                    userFails, ipFails))
            .action(action)
            .build());
    }
    
    /**
     * 清除登录失败记录（登录成功时调用）
     */
    public void clearLoginFailures(String username, String ip) {
        String userKey = "security:login_fail:" + username;
        String ipKey = "security:login_fail_ip:" + ip;
        
        redisTemplate.delete(userKey);
        redisTemplate.delete(ipKey);
        
        log.debug("✅ 清除登录失败记录 - 用户: {}, IP: {}", username, ip);
    }
    
    /**
     * 手动解锁账号
     */
    public void unlockAccount(String username) {
        String userKey = "security:login_fail:" + username;
        redisTemplate.delete(userKey);
        log.info("🔓 手动解锁账号: {}", username);
    }
    
    /**
     * 获取失败次数
     */
    public int getFailureCount(String username) {
        String userKey = "security:login_fail:" + username;
        return getFailCount(userKey);
    }
    
    /**
     * 检测异常登录（新设备、新地区等）
     */
    public boolean isAnomalousLogin(String username, String ip, String userAgent) {
        // 检查是否是新IP
        String ipHistoryKey = "security:login_ip_history:" + username;
        Boolean isNewIp = !Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(ipHistoryKey, ip));
        
        if (isNewIp) {
            // 记录新IP
            redisTemplate.opsForSet().add(ipHistoryKey, ip);
            redisTemplate.expire(ipHistoryKey, 90, TimeUnit.DAYS);
            
            // 记录异常登录事件
            securityMonitor.recordSecurityEvent(SecurityEvent.builder()
                .type(SecurityEventType.SUSPICIOUS_LOGIN)
                .level(SecurityLevel.MEDIUM)
                .ip(ip)
                .userId(username)
                .userAgent(userAgent)
                .details("新IP登录")
                .action("LOGGED")
                .build());
            
            log.warn("⚠️ 检测到新IP登录 - 用户: {}, IP: {}", username, ip);
            return true;
        }
        
        return false;
    }
    
    /**
     * 从Redis获取失败次数
     */
    private int getFailCount(String key) {
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            return 0;
        }
        try {
            return Integer.parseInt(count.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

