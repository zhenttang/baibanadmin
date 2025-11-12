package com.yunke.backend.workspace.repository;

import com.yunke.backend.workspace.domain.entity.WorkspaceMember;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间成员存储库
 */
@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {

    /**
     * 根据工作空间ID查找成员
     */
    List<WorkspaceMember> findByWorkspaceId(String workspaceId);

    /**
     * 根据用户ID查找成员关系
     */
    List<WorkspaceMember> findByUserId(String userId);

    /**
     * 根据工作空间ID和用户ID查找成员
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(String workspaceId, String userId);

    /**
     * 检查用户是否是工作空间成员
     */
    boolean existsByWorkspaceIdAndUserIdAndStatus(String workspaceId, String userId, WorkspaceMemberStatus status);

    /**
     * 根据工作空间ID和状态统计成员数量
     */
    int countByWorkspaceIdAndStatus(String workspaceId, WorkspaceMemberStatus status);

    /**
     * 根据工作空间ID和状态查找成员
     */
    List<WorkspaceMember> findByWorkspaceIdAndStatus(String workspaceId, WorkspaceMemberStatus status);

    /**
     * 查找工作空间的第一个成员（通常是创建者）
     */
    Optional<WorkspaceMember> findFirstByWorkspaceIdOrderByCreatedAtAsc(String workspaceId);

    /**
     * 删除工作空间的所有成员
     */
    @Modifying
    @Query("DELETE FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId")
    int deleteByWorkspaceId(@Param("workspaceId") String workspaceId);

    /**
     * 查找用户的待处理邀请
     */
    List<WorkspaceMember> findByUserIdAndStatus(String userId, WorkspaceMemberStatus status);

    /**
     * 根据用户ID和角色查找成员关系
     */
    List<WorkspaceMember> findByUserIdAndRole(String userId, String role);

    /**
     * 根据工作空间ID和角色查找成员
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndRole(String workspaceId, String role);

    /**
     * 统计工作空间成员总数
     */
    int countByWorkspaceId(String workspaceId);
}