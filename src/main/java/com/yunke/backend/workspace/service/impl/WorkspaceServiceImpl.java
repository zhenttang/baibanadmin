package com.yunke.backend.workspace.service.impl;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceMember;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import com.yunke.backend.workspace.repository.WorkspaceRepository;
import com.yunke.backend.workspace.repository.WorkspaceMemberRepository;
import com.yunke.backend.workspace.repository.WorkspaceUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceService;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.monitor.MetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import reactor.core.publisher.Mono;

/**
 * 工作空间服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserService userService;
    private final MetricsCollector metricsCollector;
    private final WorkspaceUserRoleRepository workspaceUserRoleRepository;

    @Override
    public Mono<Workspace> createWorkspace(String name, String userId) {
        return createWorkspace(name, userId, false);
    }
    
    /**
     * 创建工作空间（带公开选项）
     */
    public Mono<Workspace> createWorkspace(String name, String userId, boolean isPublic) {
        log.info("Creating workspace: {} for user: {}, public: {}", name, userId, isPublic);
        
        return Mono.fromCallable(() -> {
            // 创建工作空间
            Workspace workspace = new Workspace();
            workspace.setId(UUID.randomUUID().toString());
            workspace.setName(name);
            workspace.setPublic(isPublic);
            // 使用当前时间作为创建时间，由@CreationTimestamp自动处理
            workspace.setCreatedBy(userId);
            workspace.setUpdatedBy(userId);
            
            Workspace savedWorkspace = workspaceRepository.save(workspace);
            
            // 创建所有者权限
            WorkspaceUserRole ownerRole = WorkspaceUserRole.builder()
                    .id(UUID.randomUUID().toString())
                    .workspaceId(savedWorkspace.getId())
                    .userId(userId)
                    .type(WorkspaceUserRole.WorkspaceRole.OWNER)
                    .status(WorkspaceUserRole.WorkspaceMemberStatus.ACCEPTED)
                    .source(WorkspaceUserRole.WorkspaceMemberSource.SELF_JOIN)
                    .build();
            
            workspaceUserRoleRepository.save(ownerRole);
            
            return savedWorkspace;
        });
    }

    @Override
    @Cacheable(value = "workspaces", key = "#id")
    public Optional<Workspace> findById(String id) {
        log.debug("Finding workspace by ID: {}", id);
        return workspaceRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "workspaces", key = "#workspace.id")
    public Workspace updateWorkspace(Workspace workspace) {
        log.info("Updating workspace: {}", workspace.getId());
        
        Optional<Workspace> existingWorkspace = workspaceRepository.findById(workspace.getId());
        if (existingWorkspace.isEmpty()) {
            throw new IllegalArgumentException("Workspace not found: " + workspace.getId());
        }
        
        Workspace current = existingWorkspace.get();
        
        // 更新允许修改的字段
        if (workspace.getName() != null) {
            current.setName(workspace.getName());
        }
        
        if (workspace.getPublic() != null) {
            current.setPublic(workspace.getPublic());
        }
        
        current.setUpdatedAt(Instant.now());
        
        Workspace updatedWorkspace = workspaceRepository.save(current);
        
        // 记录指标
        metricsCollector.recordWorkspaceOperation("update", updatedWorkspace.getId());
        
        log.info("Workspace updated successfully: {}", updatedWorkspace.getId());
        return updatedWorkspace;
    }

    @Override
    @Transactional
    @CacheEvict(value = "workspaces", key = "#id")
    public void deleteWorkspace(String id) {
        log.info("Deleting workspace: {}", id);
        
        if (!workspaceRepository.existsById(id)) {
            throw new IllegalArgumentException("Workspace not found: " + id);
        }
        
        // 删除所有成员
        memberRepository.deleteByWorkspaceId(id);
        
        // 删除工作空间
        workspaceRepository.deleteById(id);
        
        // 记录指标
        metricsCollector.recordWorkspaceOperation("delete", id);
        
        log.info("Workspace deleted successfully: {}", id);
    }

    @Override
    @Cacheable(value = "workspaces", key = "'user:' + #userId")
    public List<Workspace> getUserWorkspaces(String userId) {
        log.debug("Getting workspaces for user: {}", userId);
        return workspaceRepository.findByUserId(userId);
    }
    
    @Override
    @Cacheable(value = "workspaces", key = "'default:' + #userId")
    public Optional<String> getUserDefaultWorkspace(String userId) {
        log.debug("Getting default workspace for user: {}", userId);
        List<Workspace> workspaces = getUserWorkspaces(userId);
        
        if (workspaces.isEmpty()) {
            log.debug("No workspaces found for user: {}", userId);
            return Optional.empty();
        }
        
        // 返回用户第一个工作空间作为默认工作空间
        // 实际项目中可能需要存储用户的偏好设置
        String defaultWorkspaceId = workspaces.get(0).getId();
        log.debug("Default workspace for user {}: {}", userId, defaultWorkspaceId);
        return Optional.of(defaultWorkspaceId);
    }

    @Override
    public Page<Workspace> findAll(Pageable pageable) {
        log.debug("Finding all workspaces with pagination");
        return workspaceRepository.findAll(pageable);
    }

    @Override
    public List<Workspace> searchWorkspaces(String keyword) {
        log.debug("Searching workspaces with keyword: {}", keyword);
        return workspaceRepository.searchByKeyword(keyword);
    }

    @Override
    @Transactional
    public WorkspaceMember inviteUser(String workspaceId, String inviterUserId, String invitedEmail) {
        log.info("Inviting user to workspace: {} -> {}", invitedEmail, workspaceId);
        
        // 检查工作空间是否存在
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new IllegalArgumentException("Workspace not found: " + workspaceId);
        }
        
        // 检查邀请者是否是工作空间成员
        if (!isMember(workspaceId, inviterUserId)) {
            throw new IllegalArgumentException("Inviter is not a member of the workspace");
        }
        
        // 查找被邀请用户
        User invitedUser = userService.findByEmail(invitedEmail)
                .block(); // 在同步方法中使用block是可以接受的
        
        if (invitedUser == null) {
            throw new IllegalArgumentException("Invited user not found: " + invitedEmail);
        }
        
        String invitedUserId = invitedUser.getId();
        
        // 检查用户是否已经是成员
        if (isMember(workspaceId, invitedUserId)) {
            throw new IllegalArgumentException("User is already a member of the workspace");
        }
        
        // 创建邀请记录
        WorkspaceMember member = new WorkspaceMember();
        member.setId(UUID.randomUUID().toString());
        member.setWorkspaceId(workspaceId);
        member.setUserId(invitedUserId);
        member.setStatus(WorkspaceMemberStatus.PENDING);
        member.setCreatedAt(Instant.now());
        member.setUpdatedAt(Instant.now());
        
        WorkspaceMember savedMember = memberRepository.save(member);
        
        log.info("User invited successfully: {} -> {}", invitedEmail, workspaceId);
        return savedMember;
    }

    @Override
    @Transactional
    public WorkspaceMember acceptInvitation(String workspaceId, String userId) {
        log.info("Accepting invitation: {} -> {}", userId, workspaceId);
        
        Optional<WorkspaceMember> memberOpt = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Invitation not found");
        }
        
        WorkspaceMember member = memberOpt.get();
        if (member.getStatus() != WorkspaceMemberStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is not pending");
        }
        
        member.setStatus(WorkspaceMemberStatus.ACCEPTED);
        member.setAcceptedAt(Instant.now());
        member.setUpdatedAt(Instant.now());
        
        WorkspaceMember updatedMember = memberRepository.save(member);
        
        log.info("Invitation accepted successfully: {} -> {}", userId, workspaceId);
        return updatedMember;
    }

    @Override
    @Transactional
    public void rejectInvitation(String workspaceId, String userId) {
        log.info("Rejecting invitation: {} -> {}", userId, workspaceId);
        
        Optional<WorkspaceMember> memberOpt = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Invitation not found");
        }
        
        WorkspaceMember member = memberOpt.get();
        if (member.getStatus() != WorkspaceMemberStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is not pending");
        }
        
        memberRepository.delete(member);
        
        log.info("Invitation rejected successfully: {} -> {}", userId, workspaceId);
    }

    @Override
    @Transactional
    public void removeMember(String workspaceId, String userId) {
        log.info("Removing member from workspace: {} -> {}", userId, workspaceId);
        
        Optional<WorkspaceMember> memberOpt = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member not found");
        }
        
        memberRepository.delete(memberOpt.get());
        
        log.info("Member removed successfully: {} -> {}", userId, workspaceId);
    }

    @Override
    @Cacheable(value = "workspaces", key = "'members:' + #workspaceId")
    public List<WorkspaceMember> getWorkspaceMembers(String workspaceId) {
        log.debug("Getting members for workspace: {}", workspaceId);
        return memberRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    @Cacheable(value = "workspaces", key = "'member:' + #workspaceId + ':' + #userId")
    public Optional<WorkspaceMember> getWorkspaceMember(String workspaceId, String userId) {
        log.debug("Getting member for workspace: {} -> {}", userId, workspaceId);
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
    }

    @Override
    public boolean isMember(String workspaceId, String userId) {
        // 🔧 [CRITICAL-FIX] 修复权限检查 - 查询workspace_user_roles表而不是workspace_members表
        // 因为工作空间创建时只创建了workspace_user_roles记录，没有创建workspace_members记录
        log.debug("🔍 [isMember] 检查用户是否为工作空间成员: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        boolean isUserRole = workspaceUserRoleRepository.isWorkspaceMember(workspaceId, userId);
        
        log.debug("📋 [isMember] workspace_user_roles表查询结果: {}", isUserRole);
        
        // 为了兼容性，也检查老的workspace_members表（如果有数据的话）
        boolean isMember = memberRepository.existsByWorkspaceIdAndUserIdAndStatus(
            workspaceId, userId, WorkspaceMemberStatus.ACCEPTED);
        
        log.debug("📋 [isMember] workspace_members表查询结果: {}", isMember);
        
        boolean result = isUserRole || isMember;
        log.info("✅ [isMember] 最终结果: userId='{}', workspaceId='{}', isMember={}", userId, workspaceId, result);
        
        return result;
    }

    @Override
    public boolean isOwner(String workspaceId, String userId) {
        // 🔧 [CRITICAL-FIX] 修复所有者检查 - 查询workspace_user_roles表而不是workspace_members表  
        log.debug("🔍 [isOwner] 检查用户是否为工作空间所有者: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        // 直接查询workspace_user_roles表中的OWNER角色
        boolean isOwnerRole = workspaceUserRoleRepository.isWorkspaceOwner(workspaceId, userId);
        
        log.debug("📋 [isOwner] workspace_user_roles表查询结果: {}", isOwnerRole);
        
        // 为了兼容性，也检查老的workspace_members表中的第一个成员（创建者）
        Optional<WorkspaceMember> firstMember = memberRepository.findFirstByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
        boolean isFirstMember = firstMember.isPresent() && firstMember.get().getUserId().equals(userId);
        
        log.debug("📋 [isOwner] workspace_members表第一个成员检查结果: {}", isFirstMember);
        
        boolean result = isOwnerRole || isFirstMember;
        log.info("✅ [isOwner] 最终结果: userId='{}', workspaceId='{}', isOwner={}", userId, workspaceId, result);
        
        return result;
    }

    @Override
    public boolean hasAccess(String workspaceId, String userId) {
        log.info("🎯🎯🎯 [CRITICAL-DEBUG] WorkspaceServiceImpl.hasAccess被调用!!!");
        log.info("  📋 参数: workspaceId='{}', userId='{}'", workspaceId, userId);
        
        // 检查是否是成员
        log.info("  🔍 步骤1: 检查用户是否为工作空间成员...");
        boolean isMember = isMember(workspaceId, userId);
        log.info("  📋 isMember()结果: {}", isMember);
        
        if (isMember) {
            log.info("✅ [CRITICAL-DEBUG] 用户是工作空间成员，返回true");
            return true;
        }
        
        // 检查是否是公开工作空间
        log.info("  🔍 步骤2: 检查是否为公开工作空间...");
        try {
            Optional<Workspace> workspace = findById(workspaceId);
            log.info("  📋 findById()结果: workspace存在={}", workspace.isPresent());
            
            if (workspace.isPresent()) {
                boolean isPublic = workspace.get().getPublic();
                log.info("  📋 工作空间public属性: {}", isPublic);
                
                boolean result = isPublic;
                log.info("✅ [CRITICAL-DEBUG] 最终结果: workspaceId='{}', userId='{}', hasAccess={}", workspaceId, userId, result);
                return result;
            } else {
                log.warn("⚠️ [CRITICAL-DEBUG] 工作空间不存在: workspaceId='{}'", workspaceId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ [CRITICAL-DEBUG] 检查工作空间public属性时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public WorkspaceStats getWorkspaceStats(String workspaceId) {
        log.debug("Getting stats for workspace: {}", workspaceId);
        
        int memberCount = memberRepository.countByWorkspaceIdAndStatus(workspaceId, WorkspaceMemberStatus.ACCEPTED);
        
        // 其他统计信息需要在实现文档管理后完善
        return new WorkspaceStats(memberCount, 0, 0L, 0);
    }
}