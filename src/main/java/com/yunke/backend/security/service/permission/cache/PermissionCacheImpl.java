package com.yunke.backend.security.service.permission.cache;

import com.yunke.backend.infrastructure.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 权限缓存实现类
 * 使用Redis缓存用户权限信息
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheImpl implements PermissionCache {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_KEY_PREFIX = RedisConfig.CacheKeys.PERMISSION_PREFIX + "user:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    
    @Override
    public List<GrantedAuthority> getUserAuthorities(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        
        String key = CACHE_KEY_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                if (cached instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> authoritiesList = (List<Object>) cached;
                    
                    // 转换为GrantedAuthority列表
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    for (Object authObj : authoritiesList) {
                        if (authObj instanceof String) {
                            authorities.add(new SimpleGrantedAuthority((String) authObj));
                        } else if (authObj instanceof GrantedAuthority) {
                            authorities.add((GrantedAuthority) authObj);
                        }
                    }
                    
                    log.debug("从缓存获取用户权限 - userId: {}, 权限数量: {}", userId, authorities.size());
                    return authorities;
                }
            }
        } catch (Exception e) {
            log.warn("从缓存获取用户权限失败 - userId: {}", userId, e);
        }
        
        return null;
    }
    
    @Override
    public void cacheUserAuthorities(String userId, List<GrantedAuthority> authorities) {
        if (userId == null || userId.isBlank() || authorities == null) {
            return;
        }
        
        String key = CACHE_KEY_PREFIX + userId;
        try {
            // 将GrantedAuthority转换为字符串列表存储
            List<String> authorityStrings = new ArrayList<>();
            for (GrantedAuthority authority : authorities) {
                authorityStrings.add(authority.getAuthority());
            }
            
            redisTemplate.opsForValue().set(key, authorityStrings, CACHE_TTL);
            log.debug("缓存用户权限 - userId: {}, 权限数量: {}, TTL: {}小时", 
                    userId, authorities.size(), CACHE_TTL.toHours());
        } catch (Exception e) {
            log.warn("缓存用户权限失败 - userId: {}", userId, e);
        }
    }
    
    @Override
    public void invalidateUserAuthorities(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        
        String key = CACHE_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(key);
            log.debug("清除用户权限缓存 - userId: {}", userId);
            
            // 发布缓存失效事件（分布式环境，通知其他节点）
            redisTemplate.convertAndSend("permission:invalidate", userId);
        } catch (Exception e) {
            log.warn("清除用户权限缓存失败 - userId: {}", userId, e);
        }
    }
    
    @Override
    public void invalidateAll() {
        try {
            String pattern = CACHE_KEY_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除所有权限缓存 - 共清除 {} 个缓存项", keys.size());
            }
        } catch (Exception e) {
            log.warn("清除所有权限缓存失败", e);
        }
    }
    
    @Override
    public boolean hasRole(String userId, String roleName) {
        List<GrantedAuthority> authorities = getUserAuthorities(userId);
        if (authorities == null) {
            return false;
        }
        
        String roleAuthority = "ROLE_" + roleName;
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals(roleAuthority));
    }
    
    @Override
    public boolean isAdmin(String userId) {
        return hasRole(userId, "ADMIN") || hasRole(userId, "SUPER_ADMIN");
    }
    
    @Override
    public boolean isSuperAdmin(String userId) {
        return hasRole(userId, "SUPER_ADMIN");
    }
}

