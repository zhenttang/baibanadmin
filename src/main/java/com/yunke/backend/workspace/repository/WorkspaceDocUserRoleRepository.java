package com.yunke.backend.workspace.repository;


import com.yunke.backend.workspace.domain.entity.WorkspaceDocUserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceDocUserRoleRepository extends JpaRepository<WorkspaceDocUserRole, WorkspaceDocUserRole.WorkspaceDocUserRoleId> {

    /**
     * 根据工作空间ID、文档ID和用户ID查找权限
     */
    Optional<WorkspaceDocUserRole> findByWorkspaceIdAndDocIdAndUserId(String workspaceId, String docId, String userId);

    /**
     * 根据工作空间ID和文档ID查找所有用户权限
     */
    Page<WorkspaceDocUserRole> findByWorkspaceIdAndDocId(String workspaceId, String docId, Pageable pageable);

    /**
     * 根据工作空间ID和文档ID查找所有用户权限列表
     */
    List<WorkspaceDocUserRole> findByWorkspaceIdAndDocId(String workspaceId, String docId);

    /**
     * 根据用户ID查找所有文档权限
     */
    List<WorkspaceDocUserRole> findByUserId(String userId);

    /**
     * 根据工作空间ID和用户ID查找权限
     */
    List<WorkspaceDocUserRole> findByWorkspaceIdAndUserId(String workspaceId, String userId);

    /**
     * 根据工作空间ID查找所有文档权限
     */
    List<WorkspaceDocUserRole> findByWorkspaceId(String workspaceId);

    /**
     * 根据角色类型查找权限
     */
    @Query("SELECT dur FROM WorkspaceDocUserRole dur WHERE dur.type = :roleType")
    List<WorkspaceDocUserRole> findByRoleType(@Param("roleType") Integer roleType);

    /**
     * 根据工作空间ID、文档ID和角色类型查找权限
     */
    @Query("SELECT dur FROM WorkspaceDocUserRole dur WHERE dur.workspaceId = :workspaceId AND dur.docId = :docId AND dur.type = :roleType")
    List<WorkspaceDocUserRole> findByWorkspaceIdAndDocIdAndType(@Param("workspaceId") String workspaceId,
                                                                @Param("docId") String docId,
                                                                @Param("roleType") Integer roleType);

    /**
     * 检查用户是否有文档权限
     */
    boolean existsByWorkspaceIdAndDocIdAndUserId(String workspaceId, String docId, String userId);

    /**
     * 统计文档的用户权限数量
     */
    long countByWorkspaceIdAndDocId(String workspaceId, String docId);

    /**
     * 统计用户的文档权限数量
     */
    long countByUserId(String userId);

    /**
     * 根据工作空间ID、文档ID和用户ID删除权限
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceIdAndDocIdAndUserId(String workspaceId, String docId, String userId);

    /**
     * 根据工作空间ID和文档ID删除所有权限
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceIdAndDocId(String workspaceId, String docId);

    /**
     * 根据工作空间ID删除所有权限
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceId(String workspaceId);

    /**
     * 根据用户ID删除所有权限
     */
    @Modifying
    @Transactional
    void deleteByUserId(String userId);

    /**
     * 批量更新用户权限角色
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceDocUserRole dur SET dur.type = :newRoleType WHERE dur.workspaceId = :workspaceId AND dur.docId = :docId AND dur.userId IN :userIds")
    void updateUserRolesBatch(@Param("workspaceId") String workspaceId,
                             @Param("docId") String docId,
                             @Param("userIds") List<String> userIds,
                             @Param("newRoleType") Integer newRoleType);

    /**
     * 查找拥有特定权限级别以上的用户
     */
    @Query("SELECT dur FROM WorkspaceDocUserRole dur WHERE dur.workspaceId = :workspaceId AND dur.docId = :docId AND dur.type >= :minRoleType")
    List<WorkspaceDocUserRole> findUsersWithMinimumRole(@Param("workspaceId") String workspaceId,
                                                        @Param("docId") String docId,
                                                        @Param("minRoleType") Integer minRoleType);
}