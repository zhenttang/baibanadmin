package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.GanttOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 甘特图操作日志Repository
 * 
 * @author AFFiNE Development Team
 */
@Repository
public interface GanttOperationLogRepository extends JpaRepository<GanttOperationLog, Long> {
    
    /**
     * 根据工作空间和文档ID查找操作日志（分页）
     */
    Page<GanttOperationLog> findByWorkspaceIdAndDocIdOrderByCreatedAtDesc(
        String workspaceId, String docId, Pageable pageable
    );
    
    /**
     * 根据用户ID查找操作日志
     */
    List<GanttOperationLog> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 根据操作类型查找日志
     */
    List<GanttOperationLog> findByWorkspaceIdAndDocIdAndOperationType(
        String workspaceId, String docId, GanttOperationLog.OperationType operationType
    );
    
    /**
     * 查找指定时间范围内的操作日志
     */
    @Query("SELECT g FROM GanttOperationLog g WHERE g.workspaceId = :workspaceId " +
           "AND g.docId = :docId AND g.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY g.createdAt DESC")
    List<GanttOperationLog> findByTimeRange(
        @Param("workspaceId") String workspaceId,
        @Param("docId") String docId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 查找最近的N条操作日志
     */
    @Query("SELECT g FROM GanttOperationLog g WHERE g.workspaceId = :workspaceId " +
           "AND g.docId = :docId ORDER BY g.createdAt DESC")
    List<GanttOperationLog> findRecentLogs(@Param("workspaceId") String workspaceId, 
                                          @Param("docId") String docId, 
                                          Pageable pageable);
    
    /**
     * 查找用户最近的操作
     */
    @Query("SELECT g FROM GanttOperationLog g WHERE g.workspaceId = :workspaceId " +
           "AND g.docId = :docId AND g.userId = :userId ORDER BY g.createdAt DESC")
    List<GanttOperationLog> findRecentUserLogs(@Param("workspaceId") String workspaceId,
                                              @Param("docId") String docId,
                                              @Param("userId") String userId,
                                              Pageable pageable);
    
    /**
     * 统计操作次数
     */
    @Query("SELECT COUNT(g) FROM GanttOperationLog g WHERE g.workspaceId = :workspaceId " +
           "AND g.docId = :docId AND g.operationType = :operationType")
    long countByOperationType(@Param("workspaceId") String workspaceId,
                             @Param("docId") String docId,
                             @Param("operationType") GanttOperationLog.OperationType operationType);
    
    /**
     * 删除指定时间之前的日志（清理旧日志）
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
    
    /**
     * 删除文档的所有操作日志
     */
    void deleteByDocId(String docId);
    
    /**
     * 删除工作空间的所有操作日志
     */
    void deleteByWorkspaceId(String workspaceId);
    
    /**
     * 查找协作活跃度统计（每日操作次数）
     */
    @Query(value = """
        SELECT DATE(created_at) as operation_date, 
               COUNT(*) as operation_count,
               COUNT(DISTINCT user_id) as active_users
        FROM gantt_operation_logs 
        WHERE workspace_id = :workspaceId 
          AND doc_id = :docId 
          AND created_at >= :startDate
        GROUP BY DATE(created_at)
        ORDER BY operation_date DESC
        """, nativeQuery = true)
    List<Object[]> findDailyActivityStats(@Param("workspaceId") String workspaceId,
                                         @Param("docId") String docId,
                                         @Param("startDate") LocalDateTime startDate);
    
    /**
     * 查找用户操作统计
     */
    @Query(value = """
        SELECT user_id, 
               operation_type,
               COUNT(*) as operation_count
        FROM gantt_operation_logs 
        WHERE workspace_id = :workspaceId 
          AND doc_id = :docId 
          AND created_at >= :startDate
        GROUP BY user_id, operation_type
        ORDER BY operation_count DESC
        """, nativeQuery = true)
    List<Object[]> findUserOperationStats(@Param("workspaceId") String workspaceId,
                                         @Param("docId") String docId,
                                         @Param("startDate") LocalDateTime startDate);
}