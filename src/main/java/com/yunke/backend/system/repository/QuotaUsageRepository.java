package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.QuotaUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 配额使用记录Repository
 * 对应Node.js版本的配额使用统计
 * 参考: /packages/backend/server/src/core/quota/service.ts
 */
@Repository
public interface QuotaUsageRepository extends JpaRepository<QuotaUsage, Integer> {

    // ==================== 基础查询 ====================

    /**
     * 根据目标ID和使用类型查询当前使用记录
     */
    Optional<QuotaUsage> findByTargetIdAndUsageType(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 根据目标ID、使用类型和统计周期查询
     */
    Optional<QuotaUsage> findByTargetIdAndUsageTypeAndPeriod(String targetId, 
                                                            QuotaUsage.UsageType usageType, 
                                                            String period);

    /**
     * 根据使用类型查询所有记录
     */
    List<QuotaUsage> findByUsageType(QuotaUsage.UsageType usageType);

    /**
     * 根据资源类型查询记录
     */
    List<QuotaUsage> findByResourceType(QuotaUsage.ResourceType resourceType);

    /**
     * 根据统计周期查询记录
     */
    List<QuotaUsage> findByPeriod(String period);

    // ==================== 用户配额使用查询 ====================

    /**
     * 获取用户当前存储使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedStorage), 0) FROM QuotaUsage u " +
           "WHERE u.targetId = :userId AND u.usageType = 'USER'")
    Long getUserStorageUsage(@Param("userId") String userId);

    /**
     * 获取用户当前成员使用量
     */
    @Query("SELECT COALESCE(MAX(u.usedMembers), 0) FROM QuotaUsage u " +
           "WHERE u.targetId = :userId AND u.usageType = 'USER'")
    Integer getUserMemberUsage(@Param("userId") String userId);

    /**
     * 获取用户当前AI操作使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedCopilotActions), 0) FROM QuotaUsage u " +
           "WHERE u.targetId = :userId AND u.usageType = 'USER' AND u.period = :period")
    Integer getUserCopilotUsage(@Param("userId") String userId, @Param("period") String period);

    /**
     * 获取用户历史使用记录
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.targetId = :userId AND u.usageType = 'USER' " +
           "ORDER BY u.period DESC")
    Page<QuotaUsage> getUserUsageHistory(@Param("userId") String userId, Pageable pageable);

    // ==================== 工作空间配额使用查询 ====================

    /**
     * 获取工作空间当前存储使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedStorage), 0) FROM QuotaUsage u " +
           "WHERE u.targetId = :workspaceId AND u.usageType = 'WORKSPACE'")
    Long getWorkspaceStorageUsage(@Param("workspaceId") String workspaceId);

    /**
     * 获取工作空间当前成员使用量
     */
    @Query("SELECT COALESCE(MAX(u.usedMembers), 0) FROM QuotaUsage u " +
           "WHERE u.targetId = :workspaceId AND u.usageType = 'WORKSPACE'")
    Integer getWorkspaceMemberUsage(@Param("workspaceId") String workspaceId);

    /**
     * 获取工作空间当前AI操作使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedCopilotActions), 0) FROM QuotaUsage u " +
           "WHERE u.targetId = :workspaceId AND u.usageType = 'WORKSPACE' AND u.period = :period")
    Integer getWorkspaceCopilotUsage(@Param("workspaceId") String workspaceId, @Param("period") String period);

    /**
     * 获取工作空间历史使用记录
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.targetId = :workspaceId AND u.usageType = 'WORKSPACE' " +
           "ORDER BY u.period DESC")
    Page<QuotaUsage> getWorkspaceUsageHistory(@Param("workspaceId") String workspaceId, Pageable pageable);

    // ==================== 使用量更新 ====================

    /**
     * 增加存储使用量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.usedStorage = u.usedStorage + :bytes, " +
           "u.updatedAt = :now WHERE u.targetId = :targetId AND u.usageType = :usageType")
    int addStorageUsage(@Param("targetId") String targetId, 
                       @Param("usageType") QuotaUsage.UsageType usageType,
                       @Param("bytes") Long bytes,
                       @Param("now") LocalDateTime now);

    /**
     * 减少存储使用量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.usedStorage = GREATEST(0, u.usedStorage - :bytes), " +
           "u.updatedAt = :now WHERE u.targetId = :targetId AND u.usageType = :usageType")
    int reduceStorageUsage(@Param("targetId") String targetId, 
                          @Param("usageType") QuotaUsage.UsageType usageType,
                          @Param("bytes") Long bytes,
                          @Param("now") LocalDateTime now);

    /**
     * 更新成员使用量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.usedMembers = :memberCount, " +
           "u.updatedAt = :now WHERE u.targetId = :targetId AND u.usageType = :usageType")
    int updateMemberUsage(@Param("targetId") String targetId, 
                         @Param("usageType") QuotaUsage.UsageType usageType,
                         @Param("memberCount") Integer memberCount,
                         @Param("now") LocalDateTime now);

    /**
     * 增加AI操作使用量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.usedCopilotActions = u.usedCopilotActions + :actions, " +
           "u.updatedAt = :now WHERE u.targetId = :targetId AND u.usageType = :usageType " +
           "AND u.period = :period")
    int addCopilotUsage(@Param("targetId") String targetId, 
                       @Param("usageType") QuotaUsage.UsageType usageType,
                       @Param("actions") Integer actions,
                       @Param("period") String period,
                       @Param("now") LocalDateTime now);

    /**
     * 增加文件数量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.fileCount = u.fileCount + :count, " +
           "u.updatedAt = :now WHERE u.targetId = :targetId AND u.usageType = :usageType")
    int addFileCount(@Param("targetId") String targetId, 
                    @Param("usageType") QuotaUsage.UsageType usageType,
                    @Param("count") Integer count,
                    @Param("now") LocalDateTime now);

    /**
     * 增加文档数量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.documentCount = u.documentCount + :count, " +
           "u.updatedAt = :now WHERE u.targetId = :targetId AND u.usageType = :usageType")
    int addDocumentCount(@Param("targetId") String targetId, 
                        @Param("usageType") QuotaUsage.UsageType usageType,
                        @Param("count") Integer count,
                        @Param("now") LocalDateTime now);

    // ==================== 统计查询 ====================

    /**
     * 统计指定周期的总存储使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedStorage), 0) FROM QuotaUsage u WHERE u.period = :period")
    Long getTotalStorageUsageByPeriod(@Param("period") String period);

    /**
     * 统计指定周期的用户存储使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedStorage), 0) FROM QuotaUsage u " +
           "WHERE u.period = :period AND u.usageType = 'USER'")
    Long getUserStorageUsageByPeriod(@Param("period") String period);

    /**
     * 统计指定周期的工作空间存储使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedStorage), 0) FROM QuotaUsage u " +
           "WHERE u.period = :period AND u.usageType = 'WORKSPACE'")
    Long getWorkspaceStorageUsageByPeriod(@Param("period") String period);

    /**
     * 统计指定周期的AI操作使用量
     */
    @Query("SELECT COALESCE(SUM(u.usedCopilotActions), 0) FROM QuotaUsage u WHERE u.period = :period")
    Long getTotalCopilotUsageByPeriod(@Param("period") String period);

    /**
     * 统计活跃用户数量（有存储使用的用户）
     */
    @Query("SELECT COUNT(DISTINCT u.targetId) FROM QuotaUsage u " +
           "WHERE u.usageType = 'USER' AND u.usedStorage > 0")
    Long countActiveUsers();

    /**
     * 统计活跃工作空间数量（有存储使用的工作空间）
     */
    @Query("SELECT COUNT(DISTINCT u.targetId) FROM QuotaUsage u " +
           "WHERE u.usageType = 'WORKSPACE' AND u.usedStorage > 0")
    Long countActiveWorkspaces();

    // ==================== 配额重置相关 ====================

    /**
     * 查询需要重置的使用记录
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.resetAt IS NOT NULL AND u.resetAt <= :now")
    List<QuotaUsage> findUsageNeedingReset(@Param("now") LocalDateTime now);

    /**
     * 重置AI操作使用量
     */
    @Modifying
    @Query("UPDATE QuotaUsage u SET u.usedCopilotActions = 0, u.resetAt = :nextResetTime, " +
           "u.period = :newPeriod, u.updatedAt = :now WHERE u.id IN :ids")
    int resetCopilotUsage(@Param("ids") List<Integer> ids,
                         @Param("nextResetTime") LocalDateTime nextResetTime,
                         @Param("newPeriod") String newPeriod,
                         @Param("now") LocalDateTime now);

    /**
     * 批量创建历史记录
     */
    @Modifying
    @Query("INSERT INTO QuotaUsage (usageType, targetId, resourceType, usedStorage, " +
           "usedMembers, usedCopilotActions, usedHistoryRecords, fileCount, documentCount, " +
           "period, resetAt, createdAt, updatedAt) " +
           "SELECT u.usageType, u.targetId, u.resourceType, u.usedStorage, " +
           "u.usedMembers, u.usedCopilotActions, u.usedHistoryRecords, u.fileCount, u.documentCount, " +
           ":historyPeriod, null, :now, :now FROM QuotaUsage u WHERE u.id IN :ids")
    int createHistoryRecords(@Param("ids") List<Integer> ids,
                            @Param("historyPeriod") String historyPeriod,
                            @Param("now") LocalDateTime now);

    // ==================== 高级统计 ====================

    /**
     * 获取存储使用量排行榜（用户）
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.usageType = 'USER' AND u.usedStorage > 0 " +
           "ORDER BY u.usedStorage DESC")
    Page<QuotaUsage> getTopUsersByStorageUsage(Pageable pageable);

    /**
     * 获取存储使用量排行榜（工作空间）
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.usageType = 'WORKSPACE' AND u.usedStorage > 0 " +
           "ORDER BY u.usedStorage DESC")
    Page<QuotaUsage> getTopWorkspacesByStorageUsage(Pageable pageable);

    /**
     * 获取AI操作使用排行榜（指定周期）
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.period = :period AND u.usedCopilotActions > 0 " +
           "ORDER BY u.usedCopilotActions DESC")
    Page<QuotaUsage> getTopCopilotUsageByPeriod(@Param("period") String period, Pageable pageable);

    /**
     * 统计使用量分布
     */
    @Query("SELECT " +
           "CASE " +
           "WHEN u.usedStorage = 0 THEN '未使用' " +
           "WHEN u.usedStorage <= 1073741824 THEN '1GB以下' " +
           "WHEN u.usedStorage <= 10737418240 THEN '1-10GB' " +
           "WHEN u.usedStorage <= 107374182400 THEN '10-100GB' " +
           "ELSE '100GB以上' END, COUNT(u) " +
           "FROM QuotaUsage u WHERE u.usageType = :usageType GROUP BY " +
           "CASE " +
           "WHEN u.usedStorage = 0 THEN 0 " +
           "WHEN u.usedStorage <= 1073741824 THEN 1 " +
           "WHEN u.usedStorage <= 10737418240 THEN 2 " +
           "WHEN u.usedStorage <= 107374182400 THEN 3 " +
           "ELSE 4 END")
    List<Object[]> getStorageUsageDistribution(@Param("usageType") QuotaUsage.UsageType usageType);

    /**
     * 获取配额使用趋势（按月）
     */
    @Query("SELECT u.period, COUNT(DISTINCT u.targetId), SUM(u.usedStorage), SUM(u.usedCopilotActions) " +
           "FROM QuotaUsage u WHERE u.usageType = :usageType " +
           "GROUP BY u.period ORDER BY u.period DESC")
    List<Object[]> getUsageTrend(@Param("usageType") QuotaUsage.UsageType usageType);

    // ==================== 数据清理 ====================

    /**
     * 删除指定时间之前的历史记录
     */
    @Modifying
    @Query("DELETE FROM QuotaUsage u WHERE u.period < :threshold AND u.resetAt IS NULL")
    int cleanupOldHistory(@Param("threshold") String threshold);

    /**
     * 删除无效的使用记录（目标不存在）
     */
    @Modifying
    @Query("DELETE FROM QuotaUsage u WHERE u.usageType = 'USER' AND u.targetId NOT IN " +
           "(SELECT DISTINCT uf.userId FROM UserFeature uf)")
    int cleanupOrphanedUserUsage();

    /**
     * 删除无效的使用记录（工作空间不存在）
     */
    @Modifying
    @Query("DELETE FROM QuotaUsage u WHERE u.usageType = 'WORKSPACE' AND u.targetId NOT IN " +
           "(SELECT DISTINCT w.id FROM Workspace w)")
    int cleanupOrphanedWorkspaceUsage();

    /**
     * 统计需要清理的记录数量
     */
    @Query("SELECT COUNT(u) FROM QuotaUsage u WHERE " +
           "(u.usageType = 'USER' AND u.targetId NOT IN (SELECT DISTINCT uf.userId FROM UserFeature uf)) OR " +
           "(u.usageType = 'WORKSPACE' AND u.targetId NOT IN (SELECT DISTINCT w.id FROM Workspace w)) OR " +
           "(u.period < :threshold AND u.resetAt IS NULL)")
    Long countRecordsNeedingCleanup(@Param("threshold") String threshold);

    // ==================== 配额监控 ====================

    /**
     * 查询存储使用率高的记录
     */
    @Query("SELECT u.targetId, u.usageType, u.usedStorage FROM QuotaUsage u " +
           "WHERE u.usedStorage > :threshold")
    List<Object[]> findHighStorageUsage(@Param("threshold") Long threshold);

    /**
     * 查询AI操作使用异常的记录
     */
    @Query("SELECT u.targetId, u.usageType, u.usedCopilotActions FROM QuotaUsage u " +
           "WHERE u.period = :period AND u.usedCopilotActions > :threshold")
    List<Object[]> findHighCopilotUsage(@Param("period") String period, 
                                       @Param("threshold") Integer threshold);

    /**
     * 统计零使用量的记录
     */
    @Query("SELECT COUNT(u) FROM QuotaUsage u WHERE u.usedStorage = 0 AND " +
           "u.usedMembers = 0 AND u.usedCopilotActions = 0")
    Long countZeroUsageRecords();

    /**
     * 获取最近更新的使用记录
     */
    @Query("SELECT u FROM QuotaUsage u WHERE u.updatedAt > :since ORDER BY u.updatedAt DESC")
    Page<QuotaUsage> findRecentlyUpdated(@Param("since") LocalDateTime since, Pageable pageable);

    // ==================== 聚合查询 ====================

    /**
     * 获取全局使用统计
     */
    @Query("SELECT " +
           "COUNT(DISTINCT CASE WHEN u.usageType = 'USER' THEN u.targetId END), " +
           "COUNT(DISTINCT CASE WHEN u.usageType = 'WORKSPACE' THEN u.targetId END), " +
           "COALESCE(SUM(u.usedStorage), 0), " +
           "COALESCE(SUM(u.usedCopilotActions), 0), " +
           "COALESCE(SUM(u.fileCount), 0), " +
           "COALESCE(SUM(u.documentCount), 0) " +
           "FROM QuotaUsage u")
    Object[] getGlobalUsageStatistics();

    /**
     * 按使用类型统计
     */
    @Query("SELECT u.usageType, " +
           "COUNT(DISTINCT u.targetId), " +
           "COALESCE(SUM(u.usedStorage), 0), " +
           "COALESCE(AVG(u.usedStorage), 0), " +
           "COALESCE(SUM(u.usedCopilotActions), 0) " +
           "FROM QuotaUsage u GROUP BY u.usageType")
    List<Object[]> getUsageStatisticsByType();
}