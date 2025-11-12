package com.yunke.backend.workspace.repository;

import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间文档存储库
 */
@Repository
public interface WorkspaceDocRepository extends JpaRepository<WorkspaceDoc, WorkspaceDoc.WorkspaceDocId> {

    /**
     * 根据工作空间ID和文档ID查找文档（带工作空间信息）
     */
    @EntityGraph(attributePaths = {"workspace"})
    Optional<WorkspaceDoc> findByWorkspaceIdAndDocId(String workspaceId, String docId);

    /**
     * 根据工作空间ID查找文档（带工作空间信息，优化N+1查询）
     */
    @EntityGraph(attributePaths = {"workspace"})
    List<WorkspaceDoc> findByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间ID分页查找文档
     */
    Page<WorkspaceDoc> findByWorkspaceId(String workspaceId, Pageable pageable);

    /**
     * 根据工作空间ID和公开状态查找文档
     */
    Page<WorkspaceDoc> findByWorkspaceIdAndPublic_(String workspaceId, Boolean isPublic, Pageable pageable);

    /**
     * 根据关键词搜索文档（标题）
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE " +
           "wd.workspaceId = :workspaceId AND " +
           "(LOWER(wd.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(wd.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<WorkspaceDoc> searchByKeyword(@Param("workspaceId") String workspaceId, 
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    /**
     * 查找公开文档
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.public_ = true")
    Page<WorkspaceDoc> findPublicDocs(Pageable pageable);

    /**
     * 根据工作空间ID查找公开文档
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.public_ = true")
    List<WorkspaceDoc> findPublicDocsByWorkspace(@Param("workspaceId") String workspaceId);

    /**
     * 统计工作空间文档数量
     */
    long countByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间ID和文档ID删除文档
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.docId = :docId")
    void deleteByWorkspaceIdAndDocId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);

    /**
     * 根据工作空间ID和ID删除文档（兼容方法）
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.docId = :id")
    void deleteByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("id") String id);

    /**
     * 检查文档是否存在
     */
    boolean existsByWorkspaceIdAndDocId(String workspaceId, String docId);
    
    /**
     * 根据工作空间ID和文档ID列表查找文档（批量查询，优化N+1查询）
     */
    @EntityGraph(attributePaths = {"workspace"})
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.docId IN :docIds")
    List<WorkspaceDoc> findByWorkspaceIdAndDocIdIn(@Param("workspaceId") String workspaceId, @Param("docIds") List<String> docIds);
    
    /**
     * 根据文档ID查找文档（跨所有工作空间）
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.docId = :docId")
    Optional<WorkspaceDoc> findByDocId(@Param("docId") String docId);
}