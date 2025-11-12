package com.yunke.backend.workspace.service;

import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole.WorkspaceRole;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import com.yunke.backend.workspace.enums.WorkspaceMemberSource;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.time.OffsetDateTime;

/**
 * 工作空间管理服务接口
 */
public interface WorkspaceManagementService {

    // 工作空间基本操作
    /**
     * 创建工作空间
     */
    Mono<Workspace> createWorkspace(String userId, CreateWorkspaceInput input);

    /**
     * 更新工作空间
     */
    Mono<Workspace> updateWorkspace(String workspaceId, String userId, UpdateWorkspaceInput input);

    /**
     * 删除工作空间
     */
    Mono<Boolean> deleteWorkspace(String workspaceId, String userId);

    /**
     * 获取用户的工作空间列表
     */
    Flux<WorkspaceWithRole> getUserWorkspaces(String userId);

    /**
     * 获取工作空间详情
     */
    Mono<WorkspaceWithRole> getWorkspace(String workspaceId, String userId);

    // 成员管理
    /**
     * 批量邀请成员
     */
    Mono<List<InviteResult>> inviteMembers(String workspaceId, String inviterId, List<String> emails, WorkspaceRole role);

    /**
     * 创建邀请链接
     */
    Mono<InviteLink> createInviteLink(String workspaceId, String userId, InviteLinkExpireTime expireTime);

    /**
     * 撤销邀请链接
     */
    Mono<Boolean> revokeInviteLink(String workspaceId, String userId);

    /**
     * 通过邀请链接加入
     */
    Mono<Boolean> acceptInviteByLink(String inviteId, String userId);

    /**
     * 通过邀请ID接受邀请
     */
    Mono<Boolean> acceptInviteById(String inviteId, String userId);

    /**
     * 审批成员
     */
    Mono<Boolean> approveMember(String workspaceId, String adminId, String userId);

    /**
     * 变更成员权限
     */
    Mono<Boolean> grantMember(String workspaceId, String adminId, String userId, WorkspaceRole role);

    /**
     * 移除成员
     */
    Mono<Boolean> revokeMember(String workspaceId, String adminId, String userId);

    /**
     * 离开工作空间
     */
    Mono<Boolean> leaveWorkspace(String workspaceId, String userId);

    /**
     * 转移所有权
     */
    Mono<Boolean> transferOwnership(String workspaceId, String currentOwnerId, String newOwnerId);

    // 成员查询
    /**
     * 获取工作空间成员列表
     */
    Flux<WorkspaceMemberInfo> getWorkspaceMembers(String workspaceId, String userId);

    /**
     * 获取等待处理的邀请
     */
    Flux<WorkspaceMemberInfo> getPendingInvitations(String workspaceId, String userId);

    /**
     * 获取邀请信息
     */
    Mono<InvitationInfo> getInviteInfo(String inviteId);

    // 权限检查
    /**
     * 检查用户权限
     */
    Mono<Boolean> hasWorkspacePermission(String workspaceId, String userId, WorkspaceAction action);

    /**
     * 获取用户在工作空间中的角色
     */
    Mono<WorkspaceRole> getUserWorkspaceRole(String workspaceId, String userId);

    // 席位管理
    /**
     * 分配席位
     */
    Mono<Void> allocateSeats(String workspaceId, int limit);

    /**
     * 检查席位配额
     */
    Mono<SeatQuota> getSeatQuota(String workspaceId);

    // 数据传输对象
    /**
     * 创建工作空间输入
     */
    record CreateWorkspaceInput(
            String name,
            Boolean isPublic,
            Boolean enableAi,
            Boolean enableUrlPreview,
            Boolean enableDocEmbedding
    ) {}

    /**
     * 更新工作空间输入
     */
    record UpdateWorkspaceInput(
            String name,
            Boolean isPublic,
            Boolean enableAi,
            Boolean enableUrlPreview,
            Boolean enableDocEmbedding,
            String avatarKey
    ) {}

    /**
     * 带角色的工作空间信息
     */
    record WorkspaceWithRole(
            Workspace workspace,
            WorkspaceUserRole.WorkspaceRole role,
            WorkspaceMemberStatus status,
            boolean isOwner,
            boolean isAdmin
    ) {}

    /**
     * 邀请结果
     */
    record InviteResult(
            String email,
            boolean success,
            String message,
            String inviteId
    ) {}

    /**
     * 邀请链接
     */
    record InviteLink(
            String inviteId,
            String link,
            OffsetDateTime expireTime
    ) {}

    /**
     * 邀请链接过期时间
     */
    enum InviteLinkExpireTime {
        ONE_DAY(86400),
        THREE_DAYS(259200),
        ONE_WEEK(604800),
        ONE_MONTH(2592000);

        private final long seconds;

        InviteLinkExpireTime(long seconds) {
            this.seconds = seconds;
        }

        public long getSeconds() {
            return seconds;
        }
    }

    /**
     * 工作空间成员信息
     */
    record WorkspaceMemberInfo(
            String userId,
            String email,
            String name,
            String avatarUrl,
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            WorkspaceMemberSource source,
            String inviterId,
            String inviterName,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    /**
     * 邀请信息
     */
    record InvitationInfo(
            String inviteId,
            String workspaceId,
            String workspaceName,
            String inviterName,
            String inviterEmail,
            WorkspaceRole role,
            WorkspaceMemberSource source,
            OffsetDateTime createdAt,
            boolean expired
    ) {}

    /**
     * 席位配额信息
     */
    record SeatQuota(
            int memberLimit,
            int memberCount,
            int pendingCount,
            int availableSeats,
            boolean needMoreSeats
    ) {}

    /**
     * 工作空间操作权限枚举
     */
    enum WorkspaceAction {
        READ,               // 读取
        WRITE,              // 写入
        COMMENT,            // 评论
        DELETE,             // 删除
        MANAGE_USERS,       // 管理用户
        UPDATE_SETTINGS,    // 更新设置
        TRANSFER_OWNER,     // 转移所有权
        CREATE_DOC,         // 创建文档
        SYNC               // 同步
    }
}