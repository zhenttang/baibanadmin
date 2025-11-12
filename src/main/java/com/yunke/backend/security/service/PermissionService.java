package com.yunke.backend.security.service;

import com.yunke.backend.document.dto.DocPermissionsDto;
import com.yunke.backend.workspace.domain.entity.WorkspacePagePermission;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Unified permission service interface.
 * - Legacy boolean/role-style methods are preserved for compatibility
 * - Bitmask-based method resolveEffectiveDocMask is added for fine-grained controls
 */
public interface PermissionService {

    /**
     * Resolve effective permission mask for a user on a doc.
     * Aggregates workspace role, per-doc grants and public link mode.
     * @param workspaceId workspace id
     * @param docId document id
     * @param userId user id (nullable for anonymous)
     * @return effective bitmask
     */
    int resolveEffectiveDocMask(String workspaceId, String docId, String userId);

    /**
     * 检查文档权限
     */
    Mono<Boolean> checkDocPermission(String workspaceId, String docId, String userId, String action);

    /**
     * 检查工作空间权限
     */
    Mono<Boolean> checkWorkspacePermission(String workspaceId, String userId, String action);

    /**
     * 检查用户是否有文档权限
     */
    Mono<Boolean> hasDocPermission(String workspaceId, String docId, String userId);

    /**
     * 检查用户是否有工作空间权限
     */
    Mono<Boolean> hasWorkspacePermission(String workspaceId, String userId);

    /**
     * 检查用户是否有工作空间访问权限
     */
    boolean hasWorkspaceAccess(String userId, String workspaceId);

    /**
     * 检查用户是否有工作空间管理权限
     */
    boolean hasWorkspaceManagePermission(String userId, String workspaceId);

    /**
     * 检查用户是否有页面访问权限
     */
    boolean hasPageAccess(String userId, String pageId);

    /**
     * 检查用户是否有页面编辑权限
     */
    boolean hasPageEditPermission(String userId, String pageId);

    /**
     * 设置页面权限
     */
    WorkspacePagePermission setPagePermission(String pageId, String userId, String permission);

    /**
     * 移除页面权限
     */
    void removePagePermission(String pageId, String userId);

    /**
     * 获取页面权限列表
     */
    List<WorkspacePagePermission> getPagePermissions(String pageId);

    /**
     * 获取用户的页面权限
     */
    Optional<WorkspacePagePermission> getUserPagePermission(String pageId, String userId);

    /**
     * 获取文档权限
     */
    Mono<DocPermissionsDto> getDocPermissions(String workspaceId, String docId, String userId);

    /**
     * 权限类型枚举
     */
    enum PermissionType {
        READ,
        write,
        admin
    }

    /**
     * 资源类型枚举
     */
    enum ResourceType {
        workspace,
        page,
        doc
    }
}