package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.GanttTaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 甘特图任务依赖关系Repository
 * 
 * @author AFFiNE Development Team
 */
@Repository
public interface GanttTaskDependencyRepository extends JpaRepository<GanttTaskDependency, Long> {
    
    /**
     * 根据工作空间和文档ID查找所有依赖关系
     */
    List<GanttTaskDependency> findByWorkspaceIdAndDocId(String workspaceId, String docId);
    
    /**
     * 根据前置任务ID查找依赖关系
     */
    List<GanttTaskDependency> findByFromTaskId(String fromTaskId);
    
    /**
     * 根据后续任务ID查找依赖关系
     */
    List<GanttTaskDependency> findByToTaskId(String toTaskId);
    
    /**
     * 查找特定的依赖关系
     */
    Optional<GanttTaskDependency> findByWorkspaceIdAndDocIdAndFromTaskIdAndToTaskId(
        String workspaceId, String docId, String fromTaskId, String toTaskId
    );
    
    /**
     * 检查是否存在特定的依赖关系
     */
    boolean existsByWorkspaceIdAndDocIdAndFromTaskIdAndToTaskId(
        String workspaceId, String docId, String fromTaskId, String toTaskId
    );
    
    /**
     * 删除指定任务的所有依赖关系（作为前置任务或后续任务）
     */
    void deleteByFromTaskIdOrToTaskId(String fromTaskId, String toTaskId);
    
    /**
     * 根据文档ID删除所有依赖关系
     */
    void deleteByDocId(String docId);
    
    /**
     * 根据工作空间ID删除所有依赖关系
     */
    void deleteByWorkspaceId(String workspaceId);
    
    /**
     * 查找任务的所有前置依赖（该任务依赖的其他任务）
     */
    @Query("SELECT d FROM GanttTaskDependency d WHERE d.workspaceId = :workspaceId " +
           "AND d.docId = :docId AND d.toTaskId = :taskId")
    List<GanttTaskDependency> findPredecessorsByTask(
        @Param("workspaceId") String workspaceId,
        @Param("docId") String docId,
        @Param("taskId") String taskId
    );
    
    /**
     * 查找任务的所有后续依赖（依赖该任务的其他任务）
     */
    @Query("SELECT d FROM GanttTaskDependency d WHERE d.workspaceId = :workspaceId " +
           "AND d.docId = :docId AND d.fromTaskId = :taskId")
    List<GanttTaskDependency> findSuccessorsByTask(
        @Param("workspaceId") String workspaceId,
        @Param("docId") String docId,
        @Param("taskId") String taskId
    );
    
    /**
     * 检测循环依赖 - 使用递归CTE查询
     * 这是一个复杂的查询，用于检测是否存在循环依赖
     */
    @Query(value = """
        WITH RECURSIVE dependency_path AS (
            SELECT from_task_id, to_task_id, 1 as depth
            FROM gantt_task_dependencies 
            WHERE workspace_id = :workspaceId AND doc_id = :docId
            
            UNION ALL
            
            SELECT d.from_task_id, dp.to_task_id, dp.depth + 1
            FROM gantt_task_dependencies d
            JOIN dependency_path dp ON d.to_task_id = dp.from_task_id
            WHERE dp.depth < 100 
              AND d.workspace_id = :workspaceId 
              AND d.doc_id = :docId
        )
        SELECT COUNT(*) > 0 FROM dependency_path 
        WHERE from_task_id = to_task_id
        """, nativeQuery = true)
    boolean hasCircularDependency(@Param("workspaceId") String workspaceId, @Param("docId") String docId);
    
    /**
     * 检测添加新依赖是否会产生循环
     */
    @Query(value = """
        WITH RECURSIVE dependency_path AS (
            -- 包含现有依赖关系
            SELECT from_task_id, to_task_id, 1 as depth
            FROM gantt_task_dependencies 
            WHERE workspace_id = :workspaceId AND doc_id = :docId
            
            UNION ALL
            
            -- 包含要添加的新依赖关系
            SELECT :fromTaskId, :toTaskId, 1 as depth
            
            UNION ALL
            
            SELECT d.from_task_id, dp.to_task_id, dp.depth + 1
            FROM gantt_task_dependencies d
            JOIN dependency_path dp ON d.to_task_id = dp.from_task_id
            WHERE dp.depth < 100 
              AND d.workspace_id = :workspaceId 
              AND d.doc_id = :docId
        )
        SELECT COUNT(*) > 0 FROM dependency_path 
        WHERE from_task_id = to_task_id
        """, nativeQuery = true)
    boolean wouldCreateCircularDependency(
        @Param("workspaceId") String workspaceId, 
        @Param("docId") String docId,
        @Param("fromTaskId") String fromTaskId, 
        @Param("toTaskId") String toTaskId
    );
    
    /**
     * 查找所有灵活的依赖关系（可以自动调整的）
     */
    @Query("SELECT d FROM GanttTaskDependency d WHERE d.workspaceId = :workspaceId " +
           "AND d.docId = :docId AND d.isFlexible = true")
    List<GanttTaskDependency> findFlexibleDependencies(
        @Param("workspaceId") String workspaceId,
        @Param("docId") String docId
    );
    
    /**
     * 统计文档中的依赖关系数量
     */
    @Query("SELECT COUNT(d) FROM GanttTaskDependency d WHERE d.workspaceId = :workspaceId AND d.docId = :docId")
    long countByWorkspaceIdAndDocId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);
    
    /**
     * 查找特定类型的依赖关系
     */
    List<GanttTaskDependency> findByWorkspaceIdAndDocIdAndDependencyType(
        String workspaceId, String docId, GanttTaskDependency.DependencyType dependencyType
    );
    
    /**
     * 查找有延迟的依赖关系
     */
    @Query("SELECT d FROM GanttTaskDependency d WHERE d.workspaceId = :workspaceId " +
           "AND d.docId = :docId AND d.lagDays != 0")
    List<GanttTaskDependency> findDependenciesWithLag(
        @Param("workspaceId") String workspaceId,
        @Param("docId") String docId
    );
}