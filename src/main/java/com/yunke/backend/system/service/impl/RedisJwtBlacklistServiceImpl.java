package com.yunke.backend.service.impl;

import com.yunke.backend.security.service.JwtBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的JWT黑名单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisJwtBlacklistServiceImpl implements JwtBlacklistService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_PREFIX = "jwt:user_tokens:";
    
    @Override
    public void addToBlacklist(String jti, LocalDateTime expiresAt) {
        try {
            String key = BLACKLIST_PREFIX + jti;
            long ttl = expiresAt.toEpochSecond(ZoneOffset.UTC) - System.currentTimeMillis() / 1000;
            
            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.SECONDS);
                log.debug("Added JWT {} to blacklist, TTL: {} seconds", jti, ttl);
            }
        } catch (Exception e) {
            log.error("Failed to add JWT {} to blacklist", jti, e);
        }
    }
    
    @Override
    public boolean isBlacklisted(String jti) {
        try {
            String key = BLACKLIST_PREFIX + jti;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check blacklist for JWT {}", jti, e);
            // 安全起见，出错时认为已被撤销
            return true;
        }
    }
    
    @Override
    public void cleanupExpired() {
        try {
            // Redis会自动清理过期的key，这里主要用于统计
            Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            log.info("Current blacklist size: {}", keys != null ? keys.size() : 0);
        } catch (Exception e) {
            log.error("Failed to cleanup expired blacklist entries", e);
        }
    }
    
    @Override
    public void revokeAllUserTokens(String userId) {
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            Set<Object> tokens = redisTemplate.opsForSet().members(userTokensKey);
            
            if (tokens != null && !tokens.isEmpty()) {
                for (Object token : tokens) {
                    String jti = (String) token;
                    // 设置较长的过期时间，确保覆盖所有可能的token
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "revoked", 30, TimeUnit.DAYS);
                }
                // 清空用户token集合
                redisTemplate.delete(userTokensKey);
                log.info("Revoked {} tokens for user {}", tokens.size(), userId);
            }
        } catch (Exception e) {
            log.error("Failed to revoke all tokens for user {}", userId, e);
        }
    }
    
    @Override
    public long getBlacklistSize() {
        try {
            Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Failed to get blacklist size", e);
            return -1;
        }
    }
    
    /**
     * 关联用户和token
     */
    public void associateUserToken(String userId, String jti, long expirationSeconds) {
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, jti);
            redisTemplate.expire(userTokensKey, expirationSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to associate token {} with user {}", jti, userId, e);
        }
    }
    
    /**
     * 定时清理任务
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void scheduledCleanup() {
        cleanupExpired();
    }
}