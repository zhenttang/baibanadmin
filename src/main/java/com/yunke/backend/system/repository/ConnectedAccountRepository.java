package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.ConnectedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 已连接账户仓库
 */
@Repository
public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, String> {
    
    /**
     * 根据用户ID和提供商查找账户
     */
    Optional<ConnectedAccount> findByUserIdAndProvider(String userId, String provider);
    
    /**
     * 根据提供商和提供商账户ID查找
     */
    Optional<ConnectedAccount> findByProviderAndProviderAccountId(String provider, String providerAccountId);
    
    /**
     * 获取用户的所有已连接账户
     */
    List<ConnectedAccount> findByUserId(String userId);
    
    /**
     * 根据提供商获取所有账户
     */
    List<ConnectedAccount> findByProvider(String provider);
    
    /**
     * 统计总登录次数
     */
    @Query("SELECT COUNT(ca) FROM ConnectedAccount ca")
    Long countTotalLogins();
    
    /**
     * 统计成功登录次数（简化为所有记录）
     */
    @Query("SELECT COUNT(ca) FROM ConnectedAccount ca")
    Long countSuccessfulLogins();
    
    /**
     * 根据提供商统计登录次数
     */
    @Query("SELECT COUNT(ca) FROM ConnectedAccount ca WHERE ca.provider = :provider")
    Long countLoginsByProvider(@Param("provider") String provider);
    
    /**
     * 根据提供商统计用户数
     */
    @Query("SELECT COUNT(DISTINCT ca.userId) FROM ConnectedAccount ca WHERE ca.provider = :provider")
    Long countUsersByProvider(@Param("provider") String provider);
    
    /**
     * 统计活跃用户数（最近30天有更新的）
     */
    @Query("SELECT COUNT(DISTINCT ca.userId) FROM ConnectedAccount ca WHERE ca.updatedAt >= :since")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);
    
    /**
     * 统计活跃用户数（默认最近30天）
     */
    default Long countActiveUsers() {
        return countActiveUsersSince(LocalDateTime.now().minusDays(30));
    }
    
    /**
     * 统计本月新用户数
     */
    @Query("SELECT COUNT(DISTINCT ca.userId) FROM ConnectedAccount ca WHERE ca.createdAt >= :since")
    Long countNewUsersSince(@Param("since") LocalDateTime since);
    
    /**
     * 统计本月新用户数（默认本月1号开始）
     */
    default Long countNewUsersThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return countNewUsersSince(startOfMonth);
    }
    
    /**
     * 根据日期统计登录次数
     */
    @Query("SELECT COUNT(ca) FROM ConnectedAccount ca WHERE DATE(ca.createdAt) = :date")
    Long countLoginsByDate(@Param("date") LocalDate date);
    
    /**
     * 获取指定时间范围内的账户
     */
    @Query("SELECT ca FROM ConnectedAccount ca WHERE ca.createdAt BETWEEN :start AND :end")
    List<ConnectedAccount> findByCreatedAtBetween(@Param("start") LocalDateTime start, 
                                                @Param("end") LocalDateTime end);
    
    /**
     * 删除过期的刷新令牌记录
     */
    @Query("DELETE FROM ConnectedAccount ca WHERE ca.expiresAt IS NOT NULL AND ca.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * 查找需要刷新令牌的账户
     */
    @Query("SELECT ca FROM ConnectedAccount ca WHERE ca.refreshToken IS NOT NULL " +
           "AND ca.expiresAt IS NOT NULL AND ca.expiresAt < :threshold")
    List<ConnectedAccount> findAccountsNeedingRefresh(@Param("threshold") LocalDateTime threshold);
}