package com.yunke.backend.workspace.repository;

import com.yunke.backend.workspace.domain.entity.WorkspacePagePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间页面权限存储库
 */
@Repository
public interface WorkspacePagePermissionRepository extends JpaRepository<WorkspacePagePermission, String> {

    /**
     * 根据页面ID查找权限
     */
    List<WorkspacePagePermission> findByPageId(String pageId);

    /**
     * 根据用户ID查找权限
     */
    List<WorkspacePagePermission> findByUserId(String userId);

    /**
     * 根据页面ID和用户ID查找权限
     */
    Optional<WorkspacePagePermission> findByPageIdAndUserId(String pageId, String userId);

    /**
     * 根据页面ID和权限类型查找
     */
    List<WorkspacePagePermission> findByPageIdAndPermission(String pageId, String permission);

    /**
     * 检查用户是否有页面权限
     */
    boolean existsByPageIdAndUserId(String pageId, String userId);

    /**
     * 删除页面的所有权限
     */
    @Modifying
    @Query("DELETE FROM WorkspacePagePermission wpp WHERE wpp.pageId = :pageId")
    int deleteByPageId(@Param("pageId") String pageId);

    /**
     * 删除用户的所有权限
     */
    @Modifying
    @Query("DELETE FROM WorkspacePagePermission wpp WHERE wpp.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 统计页面权限数量
     */
    long countByPageId(String pageId);

    /**
     * 统计用户权限数量
     */
    long countByUserId(String userId);
}