package com.yunke.backend.security.service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * JWT黑名单服务接口
 */
public interface JwtBlacklistService {
    
    /**
     * 将JWT添加到黑名单
     * @param jti JWT ID
     * @param expiresAt 过期时间
     */
    void addToBlacklist(String jti, LocalDateTime expiresAt);
    
    /**
     * 检查JWT是否在黑名单中
     * @param jti JWT ID
     * @return 是否在黑名单中
     */
    boolean isBlacklisted(String jti);
    
    /**
     * 清理过期的黑名单记录
     */
    void cleanupExpired();
    
    /**
     * 撤销用户的所有token
     * @param userId 用户ID
     */
    void revokeAllUserTokens(String userId);
    
    /**
     * 获取黑名单大小
     * @return 黑名单条目数量
     */
    long getBlacklistSize();
}