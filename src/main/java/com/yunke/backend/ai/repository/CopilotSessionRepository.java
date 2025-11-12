package com.yunke.backend.ai.repository;

import com.yunke.backend.ai.domain.entity.CopilotSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Copilot会话Repository接口
 * 对应Node.js版本的ChatSessionRepository
 */
@Repository
public interface CopilotSessionRepository extends JpaRepository<CopilotSession, String> {

    /**
     * 根据用户ID查找活跃会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' ORDER BY s.updatedAt DESC")
    List<CopilotSession> findActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID和工作空间ID查找会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.userId = :userId AND s.workspaceId = :workspaceId ORDER BY s.updatedAt DESC")
    List<CopilotSession> findByUserIdAndWorkspaceId(@Param("userId") String userId, @Param("workspaceId") String workspaceId);

    /**
     * 根据用户ID查找最近的会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.userId = :userId ORDER BY s.updatedAt DESC")
    List<CopilotSession> findRecentSessionsByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * 根据文档ID查找相关会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.docId = :docId ORDER BY s.updatedAt DESC")
    List<CopilotSession> findByDocId(@Param("docId") String docId);

    /**
     * 查找用户的活跃会话数量
     */
    @Query("SELECT COUNT(s) FROM CopilotSession s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    long countActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * 查找指定时间范围内的会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.userId = :userId AND s.createdAt BETWEEN :startTime AND :endTime ORDER BY s.createdAt DESC")
    List<CopilotSession> findSessionsByUserIdAndTimeRange(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据提供商查找会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.userId = :userId AND s.provider = :provider ORDER BY s.updatedAt DESC")
    List<CopilotSession> findByUserIdAndProvider(@Param("userId") String userId, @Param("provider") CopilotSession.AIProvider provider);

    /**
     * 查找长时间未活动的会话
     */
    @Query("SELECT s FROM CopilotSession s WHERE s.status = 'ACTIVE' AND s.updatedAt < :cutoffTime")
    List<CopilotSession> findInactiveActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 获取用户今天的会话统计
     */
    @Query("SELECT COUNT(s), COALESCE(SUM(s.tokensUsed), 0), COALESCE(SUM(s.messageCount), 0) " +
           "FROM CopilotSession s WHERE s.userId = :userId AND DATE(s.createdAt) = CURRENT_DATE")
    Object[] getTodayUsageStats(@Param("userId") String userId);

    /**
     * 获取工作空间今天的会话统计
     */
    @Query("SELECT COUNT(s), COALESCE(SUM(s.tokensUsed), 0), COALESCE(SUM(s.messageCount), 0) " +
           "FROM CopilotSession s WHERE s.workspaceId = :workspaceId AND DATE(s.createdAt) = CURRENT_DATE")
    Object[] getTodayWorkspaceUsageStats(@Param("workspaceId") String workspaceId);

    /**
     * 清理旧会话（超过指定天数）
     */
    @Query("DELETE FROM CopilotSession s WHERE s.createdAt < :cutoffDate")
    int deleteOldSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 根据状态统计会话数量
     */
    @Query("SELECT s.status, COUNT(s) FROM CopilotSession s WHERE s.userId = :userId GROUP BY s.status")
    List<Object[]> countSessionsByStatus(@Param("userId") String userId);

    /**
     * 查找用户最近使用的提供商
     */
    @Query("SELECT s.provider, COUNT(s) as usage_count FROM CopilotSession s " +
           "WHERE s.userId = :userId GROUP BY s.provider ORDER BY usage_count DESC")
    List<Object[]> findMostUsedProviders(@Param("userId") String userId);
}