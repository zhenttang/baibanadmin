package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.GanttViewConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 甘特图视图配置Repository
 * 
 * @author AFFiNE Development Team
 */
@Repository
public interface GanttViewConfigRepository extends JpaRepository<GanttViewConfig, String> {
    
    /**
     * 根据工作空间和文档ID查找甘特图配置
     */
    Optional<GanttViewConfig> findByWorkspaceIdAndDocId(String workspaceId, String docId);
    
    /**
     * 根据工作空间ID查找所有甘特图配置
     */
    List<GanttViewConfig> findByWorkspaceId(String workspaceId);
    
    /**
     * 根据文档ID查找甘特图配置
     */
    Optional<GanttViewConfig> findByDocId(String docId);
    
    /**
     * 检查工作空间和文档是否已有甘特图配置
     */
    boolean existsByWorkspaceIdAndDocId(String workspaceId, String docId);
    
    /**
     * 根据工作空间ID删除所有配置
     */
    void deleteByWorkspaceId(String workspaceId);
    
    /**
     * 根据文档ID删除配置
     */
    void deleteByDocId(String docId);
    
    /**
     * 查询指定工作空间中最近更新的甘特图配置
     */
    @Query("SELECT g FROM GanttViewConfig g WHERE g.workspaceId = :workspaceId ORDER BY g.updatedAt DESC")
    List<GanttViewConfig> findRecentByWorkspaceId(@Param("workspaceId") String workspaceId);
    
    /**
     * 查询包含特定时间轴配置的甘特图
     * TODO: 暂时注释掉，避免JSON_EXTRACT类型问题
     */
    // @Query(value = "SELECT * FROM gantt_view_configs g WHERE g.workspace_id = :workspaceId " +
    //        "AND JSON_EXTRACT(g.timeline_config, '$.unit') = :unit", nativeQuery = true)
    // List<GanttViewConfig> findByWorkspaceIdAndTimelineUnit(
    //     @Param("workspaceId") String workspaceId, 
    //     @Param("unit") String unit
    // );
    
    /**
     * 统计工作空间中的甘特图数量
     */
    @Query("SELECT COUNT(g) FROM GanttViewConfig g WHERE g.workspaceId = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") String workspaceId);
    
    /**
     * 查询启用关键路径显示的甘特图
     * TODO: 暂时注释掉，避免JSON_EXTRACT类型问题
     */
    // @Query(value = "SELECT * FROM gantt_view_configs g WHERE g.workspace_id = :workspaceId " +
    //        "AND JSON_EXTRACT(g.display_config, '$.showCriticalPath') = 'true'", nativeQuery = true)
    // List<GanttViewConfig> findByWorkspaceIdAndCriticalPathEnabled(@Param("workspaceId") String workspaceId);
}