package com.yunke.backend.workspace.repository;


import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole.WorkspaceRole;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工作空间用户角色数据访问接口
 */
@Repository
public interface WorkspaceUserRoleRepository extends JpaRepository<WorkspaceUserRole, String> {

    /**
     * 根据工作空间ID查找所有成员
     */
    List<WorkspaceUserRole> findByWorkspaceId(String workspaceId);

    /**
     * 根据用户ID查找所有工作空间
     */
    List<WorkspaceUserRole> findByUserId(String userId);

    /**
     * 根据工作空间ID和用户ID查找关系
     */
    Optional<WorkspaceUserRole> findByWorkspaceIdAndUserId(String workspaceId, String userId);

    /**
     * 根据状态查找成员
     */
    List<WorkspaceUserRole> findByWorkspaceIdAndStatus(String workspaceId, WorkspaceMemberStatus status);

    /**
     * 根据角色查找成员
     */
    List<WorkspaceUserRole> findByWorkspaceIdAndType(String workspaceId, WorkspaceRole type);

    /**
     * 查找工作空间的活跃成员
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.status = 'ACCEPTED'")
    List<WorkspaceUserRole> findActiveMembers(@Param("workspaceId") String workspaceId);

    /**
     * 查找等待处理的邀请
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.status IN ('PENDING', 'UNDER_REVIEW', 'ALLOCATING_SEAT')")
    List<WorkspaceUserRole> findPendingInvitations(@Param("workspaceId") String workspaceId);

    /**
     * 查找用户的活跃工作空间
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.userId = :userId AND wur.status = 'ACCEPTED'")
    List<WorkspaceUserRole> findUserActiveWorkspaces(@Param("userId") String userId);

    /**
     * 统计工作空间成员数
     */
    @Query("SELECT COUNT(wur) FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.status = 'ACCEPTED'")
    long countActiveMembers(@Param("workspaceId") String workspaceId);

    /**
     * 统计等待中的邀请数
     */
    @Query("SELECT COUNT(wur) FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.status IN ('PENDING', 'UNDER_REVIEW', 'ALLOCATING_SEAT')")
    long countPendingInvitations(@Param("workspaceId") String workspaceId);

    /**
     * 查找工作空间所有者
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.type = 'OWNER' AND wur.status = 'ACCEPTED'")
    Optional<WorkspaceUserRole> findWorkspaceOwner(@Param("workspaceId") String workspaceId);

    /**
     * 查找工作空间管理员（包括所有者）
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.type IN ('OWNER', 'ADMIN') AND wur.status = 'ACCEPTED'")
    List<WorkspaceUserRole> findWorkspaceAdmins(@Param("workspaceId") String workspaceId);

    /**
     * 检查用户是否为工作空间成员
     */
    @Query("SELECT CASE WHEN COUNT(wur) > 0 THEN true ELSE false END FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.userId = :userId AND wur.status = 'ACCEPTED'")
    boolean isWorkspaceMember(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /**
     * 检查用户是否为工作空间管理员
     */
    @Query("SELECT CASE WHEN COUNT(wur) > 0 THEN true ELSE false END FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.userId = :userId AND wur.type IN ('OWNER', 'ADMIN') AND wur.status = 'ACCEPTED'")
    boolean isWorkspaceAdmin(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /**
     * 检查用户是否为工作空间所有者
     */
    @Query("SELECT CASE WHEN COUNT(wur) > 0 THEN true ELSE false END FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.userId = :userId AND wur.type = 'OWNER' AND wur.status = 'ACCEPTED'")
    boolean isWorkspaceOwner(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /**
     * 更新成员状态
     */
    @Modifying
    @Query("UPDATE WorkspaceUserRole wur SET wur.status = :status, wur.updatedAt = :updatedAt WHERE wur.id = :id")
    int updateMemberStatus(@Param("id") String id, @Param("status") WorkspaceMemberStatus status, @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 更新成员角色
     */
    @Modifying
    @Query("UPDATE WorkspaceUserRole wur SET wur.type = :role, wur.updatedAt = :updatedAt WHERE wur.workspaceId = :workspaceId AND wur.userId = :userId")
    int updateMemberRole(@Param("workspaceId") String workspaceId, @Param("userId") String userId, 
                        @Param("role") WorkspaceRole role, @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 批量更新状态（用于席位分配）
     */
    @Modifying
    @Query("UPDATE WorkspaceUserRole wur SET wur.status = :newStatus, wur.updatedAt = :updatedAt WHERE wur.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<String> ids, @Param("newStatus") WorkspaceMemberStatus newStatus, @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 删除工作空间成员
     */
    @Modifying
    @Query("DELETE FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.userId = :userId")
    int removeWorkspaceMember(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /**
     * 查找需要分配席位的成员（按创建时间排序）
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.status IN ('ALLOCATING_SEAT', 'NEED_MORE_SEAT') ORDER BY wur.createdAt ASC")
    List<WorkspaceUserRole> findMembersNeedingSeatAllocation(@Param("workspaceId") String workspaceId);

    /**
     * 查找指定邀请人的邀请记录
     */
    List<WorkspaceUserRole> findByWorkspaceIdAndInviterId(String workspaceId, String inviterId);

    /**
     * 查找过期的待处理邀请
     */
    @Query("SELECT wur FROM WorkspaceUserRole wur WHERE wur.status IN ('PENDING', 'UNDER_REVIEW') AND wur.createdAt < :expireTime")
    List<WorkspaceUserRole> findExpiredPendingInvitations(@Param("expireTime") OffsetDateTime expireTime);

    /**
     * 获取用户在工作空间中的角色
     */
    @Query("SELECT wur.type FROM WorkspaceUserRole wur WHERE wur.workspaceId = :workspaceId AND wur.userId = :userId AND wur.status = 'ACCEPTED'")
    Optional<WorkspaceRole> getUserWorkspaceRole(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /**
     * 检查邮箱是否已被邀请
     */
    @Query("SELECT CASE WHEN COUNT(wur) > 0 THEN true ELSE false END FROM WorkspaceUserRole wur JOIN User u ON wur.userId = u.id WHERE wur.workspaceId = :workspaceId AND u.email = :email AND wur.status IN ('PENDING', 'UNDER_REVIEW', 'ALLOCATING_SEAT', 'ACCEPTED')")
    boolean isEmailAlreadyInvited(@Param("workspaceId") String workspaceId, @Param("email") String email);
}