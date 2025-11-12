package com.yunke.backend.community.repository;

import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.community.enums.CommunityPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 社区文档Repository接口
 * 提供社区文档相关的数据访问方法
 * 
 * @author 开发者C
 */
@Repository
public interface CommunityDocRepository extends JpaRepository<WorkspaceDoc, WorkspaceDoc.WorkspaceDocId> {
    
    /**
     * 根据工作空间获取所有社区文档
     * 按分享时间倒序排列
     * 
     * @param workspaceId 工作空间ID
     * @param pageable 分页参数
     * @return 社区文档分页列表
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true ORDER BY wd.communitySharedAt DESC")
    Page<WorkspaceDoc> findCommunityDocsByWorkspace(@Param("workspaceId") String workspaceId, Pageable pageable);
    
    /**
     * 根据权限级别获取可见的社区文档
     * 按分享时间倒序排列
     * 
     * @param workspaceId 工作空间ID
     * @param permissions 允许的权限级别列表
     * @param pageable 分页参数
     * @return 可见的社区文档分页列表
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true AND wd.communityPermission IN :permissions ORDER BY wd.communitySharedAt DESC")
    Page<WorkspaceDoc> findCommunityDocsByPermissions(
        @Param("workspaceId") String workspaceId, 
        @Param("permissions") List<CommunityPermission> permissions, 
        Pageable pageable
    );
    
    /**
     * 搜索社区文档（根据标题和描述）
     * 支持模糊匹配，按分享时间倒序排列
     * 
     * @param workspaceId 工作空间ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的社区文档分页列表
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true AND (wd.communityTitle LIKE %:keyword% OR wd.communityDescription LIKE %:keyword%) ORDER BY wd.communitySharedAt DESC")
    Page<WorkspaceDoc> searchCommunityDocs(
        @Param("workspaceId") String workspaceId, 
        @Param("keyword") String keyword, 
        Pageable pageable
    );
    
    /**
     * 获取特定文档的社区信息
     * 仅返回已分享到社区的文档
     * 
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @return 社区文档信息，如果未分享则返回empty
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.docId = :docId AND wd.workspaceId = :workspaceId AND wd.communityShared = true")
    Optional<WorkspaceDoc> findCommunityDoc(@Param("docId") String docId, @Param("workspaceId") String workspaceId);
    
    /**
     * 统计工作空间的社区文档总数
     * 
     * @param workspaceId 工作空间ID
     * @return 社区文档数量
     */
    @Query("SELECT COUNT(wd) FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true")
    Long countCommunityDocsByWorkspace(@Param("workspaceId") String workspaceId);
    
    /**
     * 获取最受欢迎的社区文档（按浏览量排序）
     * 
     * @param workspaceId 工作空间ID
     * @param pageable 分页参数
     * @return 按浏览量排序的社区文档分页列表
     */
    @Query("SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true ORDER BY wd.communityViewCount DESC")
    Page<WorkspaceDoc> findPopularCommunityDocs(@Param("workspaceId") String workspaceId, Pageable pageable);
    
    /**
     * 获取最近分享的社区文档
     * 
     * @param workspaceId 工作空间ID
     * @param pageable 分页参数
     * @return 最近分享的社区文档列表
     */
    @Query(value = "SELECT wd FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true ORDER BY wd.communitySharedAt DESC")
    Page<WorkspaceDoc> findRecentCommunityDocs(@Param("workspaceId") String workspaceId, Pageable pageable);
    
    /**
     * 检查文档是否已分享到社区
     * 
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @return true如果已分享，false如果未分享
     */
    @Query("SELECT CASE WHEN COUNT(wd) > 0 THEN true ELSE false END FROM WorkspaceDoc wd WHERE wd.docId = :docId AND wd.workspaceId = :workspaceId AND wd.communityShared = true")
    boolean existsCommunityDoc(@Param("docId") String docId, @Param("workspaceId") String workspaceId);
    
    /**
     * 根据权限类型统计社区文档数量
     * 
     * @param workspaceId 工作空间ID
     * @param permission 权限类型
     * @return 指定权限类型的文档数量
     */
    @Query("SELECT COUNT(wd) FROM WorkspaceDoc wd WHERE wd.workspaceId = :workspaceId AND wd.communityShared = true AND wd.communityPermission = :permission")
    Long countCommunityDocsByPermission(@Param("workspaceId") String workspaceId, @Param("permission") CommunityPermission permission);
}