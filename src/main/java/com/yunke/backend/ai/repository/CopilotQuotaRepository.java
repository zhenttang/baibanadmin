package com.yunke.backend.ai.repository;

import com.yunke.backend.ai.domain.entity.CopilotQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Copilot配额Repository接口
 * 对应Node.js版本的CopilotQuotaRepository
 */
@Repository
public interface CopilotQuotaRepository extends JpaRepository<CopilotQuota, Long> {

    /**
     * 根据用户ID和功能查找配额
     */
    @Query("SELECT q FROM CopilotQuota q WHERE q.userId = :userId AND q.feature = :feature")
    Optional<CopilotQuota> findByUserIdAndFeature(@Param("userId") String userId, @Param("feature") CopilotQuota.CopilotFeature feature);

    /**
     * 根据工作空间ID和功能查找配额
     */
    @Query("SELECT q FROM CopilotQuota q WHERE q.workspaceId = :workspaceId AND q.feature = :feature")
    Optional<CopilotQuota> findByWorkspaceIdAndFeature(@Param("workspaceId") String workspaceId, @Param("feature") CopilotQuota.CopilotFeature feature);

    /**
     * 查找用户的所有配额
     */
    @Query("SELECT q FROM CopilotQuota q WHERE q.userId = :userId ORDER BY q.feature")
    List<CopilotQuota> findAllByUserId(@Param("userId") String userId);

    /**
     * 查找工作空间的所有配额
     */
    @Query("SELECT q FROM CopilotQuota q WHERE q.workspaceId = :workspaceId ORDER BY q.feature")
    List<CopilotQuota> findAllByWorkspaceId(@Param("workspaceId") String workspaceId);

    /**
     * 检查用户是否有某功能的配额
     */
    @Query("SELECT CASE WHEN q.limitPerDay IS NULL OR q.usedToday < q.limitPerDay THEN true ELSE false END " +
           "FROM CopilotQuota q WHERE q.userId = :userId AND q.feature = :feature")
    Optional<Boolean> hasQuotaAvailable(@Param("userId") String userId, @Param("feature") CopilotQuota.CopilotFeature feature);

    /**
     * 检查工作空间是否有某功能的配额
     */
    @Query("SELECT CASE WHEN q.limitPerDay IS NULL OR q.usedToday < q.limitPerDay THEN true ELSE false END " +
           "FROM CopilotQuota q WHERE q.workspaceId = :workspaceId AND q.feature = :feature")
    Optional<Boolean> hasWorkspaceQuotaAvailable(@Param("workspaceId") String workspaceId, @Param("feature") CopilotQuota.CopilotFeature feature);

    /**
     * 增加用户功能使用量
     */
    @Modifying
    @Query("UPDATE CopilotQuota q SET q.usedToday = COALESCE(q.usedToday, 0) + 1, " +
           "q.usedThisMonth = COALESCE(q.usedThisMonth, 0) + 1, " +
           "q.tokensUsedToday = COALESCE(q.tokensUsedToday, 0) + :tokens, " +
           "q.tokensUsedThisMonth = COALESCE(q.tokensUsedThisMonth, 0) + :tokens, " +
           "q.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE q.userId = :userId AND q.feature = :feature")
    int incrementUserUsage(@Param("userId") String userId, @Param("feature") CopilotQuota.CopilotFeature feature, @Param("tokens") int tokens);

    /**
     * 增加工作空间功能使用量
     */
    @Modifying
    @Query("UPDATE CopilotQuota q SET q.usedToday = COALESCE(q.usedToday, 0) + 1, " +
           "q.usedThisMonth = COALESCE(q.usedThisMonth, 0) + 1, " +
           "q.tokensUsedToday = COALESCE(q.tokensUsedToday, 0) + :tokens, " +
           "q.tokensUsedThisMonth = COALESCE(q.tokensUsedThisMonth, 0) + :tokens, " +
           "q.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE q.workspaceId = :workspaceId AND q.feature = :feature")
    int incrementWorkspaceUsage(@Param("workspaceId") String workspaceId, @Param("feature") CopilotQuota.CopilotFeature feature, @Param("tokens") int tokens);

    /**
     * 重置每日配额
     */
    @Modifying
    @Query("UPDATE CopilotQuota q SET q.usedToday = 0, q.tokensUsedToday = 0, q.lastResetDate = CURRENT_TIMESTAMP")
    int resetDailyQuotas();

    /**
     * 重置每月配额
     */
    @Modifying
    @Query("UPDATE CopilotQuota q SET q.usedThisMonth = 0, q.tokensUsedThisMonth = 0, q.lastResetDate = CURRENT_TIMESTAMP")
    int resetMonthlyQuotas();

    /**
     * 查找需要重置的配额（基于最后重置时间）
     */
    @Query("SELECT q FROM CopilotQuota q WHERE q.lastResetDate IS NULL OR q.lastResetDate < :cutoffTime")
    List<CopilotQuota> findQuotasNeedingReset(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 获取用户今日配额使用情况
     */
    @Query("SELECT q.feature, q.usedToday, q.limitPerDay, q.tokensUsedToday, q.tokenLimitPerDay " +
           "FROM CopilotQuota q WHERE q.userId = :userId")
    List<Object[]> getUserTodayUsage(@Param("userId") String userId);

    /**
     * 获取工作空间今日配额使用情况
     */
    @Query("SELECT q.feature, q.usedToday, q.limitPerDay, q.tokensUsedToday, q.tokenLimitPerDay " +
           "FROM CopilotQuota q WHERE q.workspaceId = :workspaceId")
    List<Object[]> getWorkspaceTodayUsage(@Param("workspaceId") String workspaceId);

    /**
     * 查找超出限制的配额
     */
    @Query("SELECT q FROM CopilotQuota q WHERE " +
           "(q.limitPerDay IS NOT NULL AND q.usedToday >= q.limitPerDay) OR " +
           "(q.tokenLimitPerDay IS NOT NULL AND q.tokensUsedToday >= q.tokenLimitPerDay)")
    List<CopilotQuota> findExceededQuotas();

    /**
     * 获取配额统计信息
     */
    @Query("SELECT q.feature, COUNT(q), AVG(q.usedToday), MAX(q.usedToday) " +
           "FROM CopilotQuota q GROUP BY q.feature")
    List<Object[]> getQuotaStatistics();

    /**
     * 创建默认配额（如果不存在）
     */
    @Query("SELECT COUNT(q) FROM CopilotQuota q WHERE q.userId = :userId AND q.feature = :feature")
    long countExistingQuota(@Param("userId") String userId, @Param("feature") CopilotQuota.CopilotFeature feature);

    /**
     * 批量更新配额限制
     */
    @Modifying
    @Query("UPDATE CopilotQuota q SET q.limitPerDay = :limitPerDay, q.limitPerMonth = :limitPerMonth, " +
           "q.tokenLimitPerDay = :tokenLimitPerDay, q.tokenLimitPerMonth = :tokenLimitPerMonth " +
           "WHERE q.userId = :userId AND q.feature = :feature")
    int updateQuotaLimits(
            @Param("userId") String userId,
            @Param("feature") CopilotQuota.CopilotFeature feature,
            @Param("limitPerDay") Integer limitPerDay,
            @Param("limitPerMonth") Integer limitPerMonth,
            @Param("tokenLimitPerDay") Integer tokenLimitPerDay,
            @Param("tokenLimitPerMonth") Integer tokenLimitPerMonth
    );
}