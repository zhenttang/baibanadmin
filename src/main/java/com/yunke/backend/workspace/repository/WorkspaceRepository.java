package com.yunke.backend.workspace.repository;

import com.yunke.backend.workspace.domain.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间存储库
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    
    /**
     * 根据ID查找工作空间（带特性信息）
     */
    @EntityGraph(attributePaths = {"features"})
    @Override
    Optional<Workspace> findById(String id);
    
    /**
     * 根据ID查找工作空间（带权限信息）
     */
    @Query("SELECT w FROM Workspace w " +
           "LEFT JOIN FETCH w.features " +
           "LEFT JOIN FETCH w.permissions " +
           "WHERE w.id = :id")
    Optional<Workspace> findByIdWithPermissions(@Param("id") String id);

    /**
     * 根据用户ID查找工作空间
     */
    @Query("SELECT w FROM Workspace w " +
           "JOIN WorkspaceMember wm ON w.id = wm.workspaceId " +
           "WHERE wm.userId = :userId AND wm.status = 'Accepted'")
    List<Workspace> findByUserId(@Param("userId") String userId);

    /**
     * 根据关键词搜索工作空间（名称）
     */
    @Query("SELECT w FROM Workspace w WHERE " +
           "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Workspace> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 查找公开工作空间
     */
    @Query("SELECT w FROM Workspace w WHERE w.public_ = true")
    Page<Workspace> findPublicWorkspaces(Pageable pageable);

    /**
     * 根据创建时间范围查找工作空间
     */
    @Query("SELECT w FROM Workspace w WHERE w.createdAt BETWEEN :startDate AND :endDate")
    List<Workspace> findByCreatedAtBetween(@Param("startDate") java.time.Instant startDate, 
                                          @Param("endDate") java.time.Instant endDate);

    /**
     * 统计用户拥有的工作空间数量
     */
    @Query("SELECT COUNT(w) FROM Workspace w " +
           "JOIN WorkspaceMember wm ON w.id = wm.workspaceId " +
           "WHERE wm.userId = :userId AND wm.status = 'Accepted'")
    long countByUserId(@Param("userId") String userId);
    
    /**
     * 批量查找工作空间（优化N+1查询）
     */
    @Query("SELECT w FROM Workspace w WHERE w.id IN :workspaceIds")
    List<Workspace> findByIds(@Param("workspaceIds") List<String> workspaceIds);
    
    /**
     * 查找用户的工作空间（使用JOIN FETCH优化，避免N+1查询）
     */
    @Query("SELECT DISTINCT w FROM Workspace w " +
           "JOIN FETCH w.features " +
           "JOIN WorkspaceUserRole wur ON w.id = wur.workspaceId " +
           "WHERE wur.userId = :userId AND wur.status = 'ACCEPTED'")
    List<Workspace> findUserWorkspacesWithFeatures(@Param("userId") String userId);
}