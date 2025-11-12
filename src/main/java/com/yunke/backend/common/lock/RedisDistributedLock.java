package com.yunke.backend.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁实现
 * 用于AFFiNE文档协作中的并发控制
 * 
 * 核心功能：
 * 1. 基于Redis的分布式锁
 * 2. 自动续期机制防止死锁
 * 3. 可重入锁支持
 * 4. 锁超时和释放机制
 * 
 * 对应AFFiNE的并发控制逻辑
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLock {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 锁前缀
    private static final String LOCK_PREFIX = "affine:lock:";
    
    // 默认锁超时时间（30秒）
    private static final long DEFAULT_EXPIRE_TIME = 30000;
    
    // 续期间隔（10秒）
    private static final long RENEWAL_INTERVAL = 10000;
    
    // 本地锁标识缓存
    private final ConcurrentHashMap<String, String> localLocks = new ConcurrentHashMap<>();
    
    // Lua脚本：获取锁
    private static final String ACQUIRE_SCRIPT = 
        "if redis.call('get', KEYS[1]) == false then " +
        "    redis.call('set', KEYS[1], ARGV[1]) " +
        "    redis.call('pexpire', KEYS[1], ARGV[2]) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    // Lua脚本：释放锁
    private static final String RELEASE_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    redis.call('del', KEYS[1]) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    // Lua脚本：续期锁
    private static final String RENEW_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    redis.call('pexpire', KEYS[1], ARGV[2]) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    /**
     * 获取文档锁
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param maxWaitTime 最大等待时间（毫秒）
     * @return 锁句柄，如果获取失败返回null
     */
    public LockHandle acquireDocLock(String workspaceId, String docId, long maxWaitTime) {
        String lockKey = LOCK_PREFIX + "doc:" + workspaceId + ":" + docId;
        return acquireLock(lockKey, maxWaitTime, DEFAULT_EXPIRE_TIME);
    }
    
    /**
     * 获取工作空间锁
     * 
     * @param workspaceId 工作空间ID
     * @param maxWaitTime 最大等待时间（毫秒）
     * @return 锁句柄，如果获取失败返回null
     */
    public LockHandle acquireWorkspaceLock(String workspaceId, long maxWaitTime) {
        String lockKey = LOCK_PREFIX + "workspace:" + workspaceId;
        return acquireLock(lockKey, maxWaitTime, DEFAULT_EXPIRE_TIME);
    }
    
    /**
     * 获取用户会话锁
     * 
     * @param userId 用户ID
     * @param maxWaitTime 最大等待时间（毫秒）
     * @return 锁句柄，如果获取失败返回null
     */
    public LockHandle acquireUserLock(String userId, long maxWaitTime) {
        String lockKey = LOCK_PREFIX + "user:" + userId;
        return acquireLock(lockKey, maxWaitTime, DEFAULT_EXPIRE_TIME);
    }
    
    /**
     * 通用锁获取方法
     * 
     * @param lockKey 锁键
     * @param maxWaitTime 最大等待时间
     * @param expireTime 锁过期时间
     * @return 锁句柄
     */
    public LockHandle acquireLock(String lockKey, long maxWaitTime, long expireTime) {
        String lockValue = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        log.debug("🔒 [RedisDistributedLock] 尝试获取锁: key={}, maxWait={}ms", lockKey, maxWaitTime);
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            try {
                // 执行获取锁的Lua脚本
                Long result = redisTemplate.execute(
                    RedisScript.of(ACQUIRE_SCRIPT, Long.class),
                    Collections.singletonList(lockKey),
                    lockValue,
                    expireTime
                );
                
                if (result != null && result == 1) {
                    // 成功获取锁
                    localLocks.put(lockKey, lockValue);
                    LockHandle handle = new LockHandle(lockKey, lockValue, expireTime, this);
                    
                    log.info("✅ [RedisDistributedLock] 成功获取锁: key={}, value={}, expire={}ms", 
                            lockKey, lockValue, expireTime);
                    
                    // 启动自动续期
                    startAutoRenewal(handle);
                    
                    return handle;
                }
                
                // 等待一小段时间后重试
                Thread.sleep(50);
                
            } catch (Exception e) {
                log.error("❌ [RedisDistributedLock] 获取锁异常: key={}", lockKey, e);
                break;
            }
        }
        
        log.warn("⚠️ [RedisDistributedLock] 获取锁超时: key={}, maxWait={}ms", lockKey, maxWaitTime);
        return null;
    }
    
    /**
     * 释放锁
     * 
     * @param lockKey 锁键
     * @param lockValue 锁值
     * @return 是否成功释放
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        log.debug("🔓 [RedisDistributedLock] 释放锁: key={}, value={}", lockKey, lockValue);
        
        try {
            Long result = redisTemplate.execute(
                RedisScript.of(RELEASE_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                lockValue
            );
            
            boolean success = result != null && result == 1;
            if (success) {
                localLocks.remove(lockKey);
                log.info("✅ [RedisDistributedLock] 成功释放锁: key={}", lockKey);
            } else {
                log.warn("⚠️ [RedisDistributedLock] 释放锁失败: key={}, 可能已过期或被其他进程持有", lockKey);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [RedisDistributedLock] 释放锁异常: key={}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 续期锁
     * 
     * @param lockKey 锁键
     * @param lockValue 锁值
     * @param expireTime 新的过期时间
     * @return 是否成功续期
     */
    public boolean renewLock(String lockKey, String lockValue, long expireTime) {
        log.debug("🔄 [RedisDistributedLock] 续期锁: key={}, expire={}ms", lockKey, expireTime);
        
        try {
            Long result = redisTemplate.execute(
                RedisScript.of(RENEW_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                lockValue,
                expireTime
            );
            
            boolean success = result != null && result == 1;
            if (success) {
                log.debug("✅ [RedisDistributedLock] 成功续期锁: key={}", lockKey);
            } else {
                log.warn("⚠️ [RedisDistributedLock] 续期锁失败: key={}, 锁可能已丢失", lockKey);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ [RedisDistributedLock] 续期锁异常: key={}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 启动自动续期
     */
    private void startAutoRenewal(LockHandle handle) {
        Thread renewalThread = new Thread(() -> {
            while (!handle.isReleased()) {
                try {
                    Thread.sleep(RENEWAL_INTERVAL);
                    
                    if (!handle.isReleased()) {
                        boolean renewed = renewLock(handle.getLockKey(), handle.getLockValue(), handle.getExpireTime());
                        if (!renewed) {
                            log.warn("⚠️ [RedisDistributedLock] 自动续期失败，锁可能已丢失: key={}", handle.getLockKey());
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    log.debug("🔄 [RedisDistributedLock] 续期线程被中断: key={}", handle.getLockKey());
                    break;
                } catch (Exception e) {
                    log.error("❌ [RedisDistributedLock] 自动续期异常: key={}", handle.getLockKey(), e);
                }
            }
        });
        
        renewalThread.setName("redis-lock-renewal-" + handle.getLockKey());
        renewalThread.setDaemon(true);
        renewalThread.start();
    }
    
    /**
     * 检查锁是否存在
     */
    public boolean isLocked(String lockKey) {
        try {
            Object value = redisTemplate.opsForValue().get(lockKey);
            return value != null;
        } catch (Exception e) {
            log.error("❌ [RedisDistributedLock] 检查锁状态异常: key={}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 获取锁的剩余时间
     */
    public long getLockTTL(String lockKey) {
        try {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MILLISECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("❌ [RedisDistributedLock] 获取锁TTL异常: key={}", lockKey, e);
            return -1;
        }
    }
    
    /**
     * 清理本地锁缓存
     */
    public void cleanupLocalLocks() {
        localLocks.clear();
        log.info("🧹 [RedisDistributedLock] 清理本地锁缓存完成");
    }
    
    /**
     * 获取当前持有的锁数量
     */
    public int getHeldLockCount() {
        return localLocks.size();
    }
}