package com.yunke.backend.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分布式锁句柄
 * 管理锁的生命周期和自动释放
 */
@Slf4j
public class LockHandle implements AutoCloseable {
    
    private final String lockKey;
    private final String lockValue;
    private final long expireTime;
    private final RedisDistributedLock lockManager;
    private final AtomicBoolean released = new AtomicBoolean(false);
    private final long acquireTime;
    
    public LockHandle(String lockKey, String lockValue, long expireTime, RedisDistributedLock lockManager) {
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expireTime = expireTime;
        this.lockManager = lockManager;
        this.acquireTime = System.currentTimeMillis();
    }
    
    /**
     * 释放锁
     */
    public boolean release() {
        if (released.compareAndSet(false, true)) {
            boolean success = lockManager.releaseLock(lockKey, lockValue);
            long holdTime = System.currentTimeMillis() - acquireTime;
            
            if (success) {
                log.info("🔓 [LockHandle] 锁已释放: key={}, holdTime={}ms", lockKey, holdTime);
            } else {
                log.warn("⚠️ [LockHandle] 锁释放失败: key={}, holdTime={}ms", lockKey, holdTime);
            }
            
            return success;
        }
        return false;
    }
    
    /**
     * 续期锁
     */
    public boolean renew() {
        if (released.get()) {
            return false;
        }
        return lockManager.renewLock(lockKey, lockValue, expireTime);
    }
    
    /**
     * 检查锁是否已释放
     */
    public boolean isReleased() {
        return released.get();
    }
    
    /**
     * 获取锁持有时间
     */
    public long getHoldTime() {
        return System.currentTimeMillis() - acquireTime;
    }
    
    /**
     * 获取锁的剩余时间
     */
    public long getRemainingTime() {
        return lockManager.getLockTTL(lockKey);
    }
    
    // Getters
    public String getLockKey() {
        return lockKey;
    }
    
    public String getLockValue() {
        return lockValue;
    }
    
    public long getExpireTime() {
        return expireTime;
    }
    
    public long getAcquireTime() {
        return acquireTime;
    }
    
    @Override
    public void close() {
        release();
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (!released.get()) {
            log.warn("⚠️ [LockHandle] 锁句柄被GC时未释放: key={}", lockKey);
            release();
        }
        super.finalize();
    }
    
    @Override
    public String toString() {
        return String.format("LockHandle{key='%s', holdTime=%dms, released=%s}", 
                           lockKey, getHoldTime(), released.get());
    }
}