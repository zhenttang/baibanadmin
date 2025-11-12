package com.yunke.backend.workspace.service;

import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceMember;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * 工作空间服务接口
 */
public interface WorkspaceService {

    /**
     * 创建工作空间（简化版）
     */
    Mono<Workspace> createWorkspace(String name, String userId);
    
    /**
     * 创建工作空间（带公开选项）
     */
    Mono<Workspace> createWorkspace(String name, String userId, boolean isPublic);

    /**
     * 根据ID查找工作空间
     */
    Optional<Workspace> findById(String id);

    /**
     * 更新工作空间
     */
    Workspace updateWorkspace(Workspace workspace);

    /**
     * 删除工作空间
     */
    void deleteWorkspace(String id);

    /**
     * 获取用户的工作空间列表
     */
    List<Workspace> getUserWorkspaces(String userId);
    
    /**
     * 获取用户的默认工作空间（通常是第一个或最后创建的）
     */
    Optional<String> getUserDefaultWorkspace(String userId);

    /**
     * 分页获取工作空间
     */
    Page<Workspace> findAll(Pageable pageable);

    /**
     * 搜索工作空间
     */
    List<Workspace> searchWorkspaces(String keyword);

    /**
     * 邀请用户加入工作空间
     */
    WorkspaceMember inviteUser(String workspaceId, String inviterUserId, String invitedEmail);

    /**
     * 接受工作空间邀请
     */
    WorkspaceMember acceptInvitation(String workspaceId, String userId);

    /**
     * 拒绝工作空间邀请
     */
    void rejectInvitation(String workspaceId, String userId);

    /**
     * 移除工作空间成员
     */
    void removeMember(String workspaceId, String userId);

    /**
     * 获取工作空间成员列表
     */
    List<WorkspaceMember> getWorkspaceMembers(String workspaceId);

    /**
     * 获取工作空间成员
     */
    Optional<WorkspaceMember> getWorkspaceMember(String workspaceId, String userId);

    /**
     * 检查用户是否是工作空间成员
     */
    boolean isMember(String workspaceId, String userId);

    /**
     * 检查用户是否是工作空间所有者
     */
    boolean isOwner(String workspaceId, String userId);

    /**
     * 检查用户是否有工作空间访问权限
     */
    boolean hasAccess(String workspaceId, String userId);

    /**
     * 获取工作空间统计信息
     */
    WorkspaceStats getWorkspaceStats(String workspaceId);

    /**
     * 工作空间统计信息
     */
    record WorkspaceStats(
            int memberCount,
            int docCount,
            long storageUsed,
            int activeCollaborators
    ) {}
}