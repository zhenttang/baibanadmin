package com.yunke.backend.security.repository;

import com.yunke.backend.system.domain.entity.IpAccessControl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * IP访问控制Repository
 */
@Repository
public interface IpAccessControlRepository extends JpaRepository<IpAccessControl, Long> {
    
    /**
     * 根据IP地址查询
     */
    Optional<IpAccessControl> findByIpAddress(String ipAddress);
    
    /**
     * 根据访问类型查询启用的规则
     */
    List<IpAccessControl> findByAccessTypeAndEnabledTrueOrderByCreatedAtDesc(String accessType);
    
    /**
     * 查询所有启用的白名单
     */
    @Query("SELECT i FROM IpAccessControl i WHERE i.accessType = 'WHITELIST' AND i.enabled = true AND (i.expiresAt IS NULL OR i.expiresAt > :now)")
    List<IpAccessControl> findActiveWhitelist(@Param("now") LocalDateTime now);
    
    /**
     * 查询所有启用的黑名单
     */
    @Query("SELECT i FROM IpAccessControl i WHERE i.accessType = 'BLACKLIST' AND i.enabled = true AND (i.expiresAt IS NULL OR i.expiresAt > :now)")
    List<IpAccessControl> findActiveBlacklist(@Param("now") LocalDateTime now);
    
    /**
     * 根据访问类型分页查询
     */
    Page<IpAccessControl> findByAccessTypeOrderByCreatedAtDesc(String accessType, Pageable pageable);
    
    /**
     * 查询即将过期的规则
     */
    @Query("SELECT i FROM IpAccessControl i WHERE i.enabled = true AND i.expiresAt IS NOT NULL AND i.expiresAt BETWEEN :now AND :futureTime")
    List<IpAccessControl> findExpiringRules(@Param("now") LocalDateTime now, @Param("futureTime") LocalDateTime futureTime);
    
    /**
     * 更新命中统计
     */
    @Query("UPDATE IpAccessControl i SET i.hitCount = i.hitCount + 1, i.lastHitAt = :hitTime WHERE i.id = :id")
    void updateHitCount(@Param("id") Long id, @Param("hitTime") LocalDateTime hitTime);
    
    /**
     * 查询高频命中的IP
     */
    @Query("SELECT i FROM IpAccessControl i WHERE i.hitCount > :threshold ORDER BY i.hitCount DESC")
    List<IpAccessControl> findHighFrequencyIps(@Param("threshold") Long threshold);
    
    /**
     * 根据创建人查询
     */
    Page<IpAccessControl> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);
    
    /**
     * 删除过期的规则
     */
    void deleteByExpiresAtBefore(LocalDateTime cutoffTime);
}