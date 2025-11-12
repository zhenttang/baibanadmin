package com.yunke.backend.workspace.service.impl;

import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.common.exception.ResourceNotFoundException;

import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole.WorkspaceRole;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import com.yunke.backend.workspace.enums.WorkspaceMemberSource;

import com.yunke.backend.workspace.event.WorkspaceCreatedEvent;
import com.yunke.backend.workspace.event.WorkspaceUpdatedEvent;
import com.yunke.backend.workspace.repository.WorkspaceRepository;

import com.yunke.backend.workspace.repository.WorkspaceUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceManagementService;
import com.yunke.backend.workspace.service.WorkspaceManagementService.WorkspaceWithRole;
import com.yunke.backend.workspace.service.WorkspaceManagementService.WorkspaceAction;
import com.yunke.backend.workspace.service.WorkspaceManagementService.InviteResult;
import com.yunke.backend.workspace.service.WorkspaceManagementService.InviteLink;
import com.yunke.backend.workspace.service.WorkspaceManagementService.InviteLinkExpireTime;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.security.constants.PermissionActions;
import com.yunke.backend.security.util.PermissionUtils;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.notification.service.MailService;
import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * 工作空间管理服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceManagementServiceImpl implements WorkspaceManagementService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceUserRoleRepository workspaceUserRoleRepository;
    private final PermissionService permissionService;
    private final UserService userService;
    private final MailService mailService;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // Redis键前缀
    private static final String INVITE_LINK_PREFIX = "workspace:inviteLink:";

    @Override
    @Transactional
    public Mono<Workspace> createWorkspace(String userId, CreateWorkspaceInput input) {
        log.info("=== 后端 WorkspaceManagementService.createWorkspace 开始 ===");
        log.info("创建工作空间请求参数: userId={}, input={}", userId, input);
        
        return createWorkspaceEntity(input)
                .flatMap(workspace -> assignOwnerRole(workspace, userId))
                .doOnSuccess(workspace -> publishWorkspaceCreatedEvent(workspace, userId))
                .doOnError(error -> {
                    log.error("=== 后端 WorkspaceManagementService.createWorkspace 失败 ===");
                    log.error("创建工作空间失败，用户: {}, 错误: {}", userId, error.getMessage(), error);
                });
    }
    
    /**
     * 创建工作空间实体并保存到数据库
     * 
     * @param input 创建工作空间输入参数
     * @return Mono<Workspace> 保存后的工作空间实体
     */
    private Mono<Workspace> createWorkspaceEntity(CreateWorkspaceInput input) {
        return Mono.fromCallable(() -> {
            log.info("开始创建工作空间实体");
            
            // 生成UUID
            String generatedId = UUID.randomUUID().toString();
            log.info("🔍 [WORKSPACE-CREATE] 生成的UUID: '{}'", generatedId);
            
            // 创建工作空间
            Workspace workspace = Workspace.builder()
                    .id(generatedId)
                    .name(input.name())
                    .public_(input.isPublic() != null ? input.isPublic() : false)
                    .enableAi(input.enableAi() != null ? input.enableAi() : true)
                    .enableUrlPreview(input.enableUrlPreview() != null ? input.enableUrlPreview() : false)
                    .enableDocEmbedding(input.enableDocEmbedding() != null ? input.enableDocEmbedding() : true)
                    .avatarKey(generateDefaultAvatarKey(input.name())) // 🔧 设置默认头像key
                    .build();

            log.info("🔍 [WORKSPACE-CREATE] 工作空间实体创建完成:");
            log.info("  📋 ID: '{}'", workspace.getId());
            log.info("  📋 Name: '{}'", workspace.getName());
            log.info("  📋 Public: {}", workspace.getPublic());
            log.info("  📋 EnableAi: {}", workspace.isEnableAi());
            log.info("  📋 AvatarKey: '{}'", workspace.getAvatarKey()); // 🔧 新增日志
            log.info("开始保存工作空间到数据库");
            
            Workspace savedWorkspace = workspaceRepository.save(workspace);
            log.info("🎉 [WORKSPACE-CREATE] 工作空间保存到数据库成功!");
            log.info("  ✅ 保存后的ID: '{}'", savedWorkspace.getId());
            log.info("  ✅ 保存后的Name: '{}'", savedWorkspace.getName());
            log.info("  ✅ 保存后的CreatedAt: {}", savedWorkspace.getCreatedAt());
            
            return savedWorkspace;
        })
        .subscribeOn(Schedulers.boundedElastic()); // 确保事务在同一线程执行
    }
    
    /**
     * 为创建者分配所有者角色
     * 
     * @param workspace 工作空间实体
     * @param userId 用户ID
     * @return Mono<Workspace> 原工作空间实体
     */
    private Mono<Workspace> assignOwnerRole(Workspace workspace, String userId) {
        log.info("开始为创建者添加所有者角色");
        log.info("  🔍 [DEBUG] workspace.getId()='{}'", workspace.getId());
        log.info("  🔍 [DEBUG] userId='{}'", userId);
        
        // 添加创建者为所有者
        WorkspaceUserRole ownerRole = new WorkspaceUserRole();
        ownerRole.setWorkspaceId(workspace.getId());
        ownerRole.setUserId(userId);
        ownerRole.setType(WorkspaceRole.OWNER);
        ownerRole.setStatus(convertToEntityStatus(com.yunke.backend.workspace.enums.WorkspaceMemberStatus.ACCEPTED));
        ownerRole.setSource(convertToEntitySource(com.yunke.backend.workspace.enums.WorkspaceMemberSource.EMAIL));

        log.info("创建工作空间用户角色: workspaceId={}, userId={}, role={}", 
                workspace.getId(), userId, WorkspaceUserRole.WorkspaceRole.OWNER);
        log.info("  🔍 [DEBUG] WorkspaceUserRole实体详情:");
        log.info("    - workspaceId: '{}'", ownerRole.getWorkspaceId());
        log.info("    - userId: '{}'", ownerRole.getUserId());
        log.info("    - type: {}", ownerRole.getType());
        log.info("    - status: {}", ownerRole.getStatus());
        log.info("    - source: {}", ownerRole.getSource());

        return Mono.fromCallable(() -> {
            log.info("🔍 [CRITICAL] 开始保存WorkspaceUserRole到数据库...");
            try {
                WorkspaceUserRole savedRole = workspaceUserRoleRepository.save(ownerRole);
                log.info("✅ [CRITICAL] 工作空间用户角色保存成功! savedRole.id={}", savedRole.getId());
                log.info("  📋 保存后的数据: workspaceId='{}', userId='{}', type={}, status={}", 
                        savedRole.getWorkspaceId(), savedRole.getUserId(), 
                        savedRole.getType(), savedRole.getStatus());
                return savedRole;
            } catch (Exception e) {
                log.error("❌ [CRITICAL] 工作空间用户角色保存失败!", e);
                log.error("  🔍 尝试保存的数据: workspaceId='{}', userId='{}', type={}", 
                        ownerRole.getWorkspaceId(), ownerRole.getUserId(), ownerRole.getType());
                throw e;
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // 确保事务在同一线程执行
        .thenReturn(workspace);
    }
    
    /**
     * 发布工作空间创建事件
     * 
     * @param workspace 工作空间实体
     * @param userId 用户ID
     */
    private void publishWorkspaceCreatedEvent(Workspace workspace, String userId) {
        log.info("=== 后端 WorkspaceManagementService.createWorkspace 成功完成 ===");
        log.info("工作空间创建成功: ID={}, 名称={}", workspace.getId(), workspace.getName());
        
        // 🏠 [EVENT-DRIVEN] 发布工作空间创建事件
        // 事件监听器将异步处理根文档创建，不阻塞主流程
        log.info("🔔 [EVENT-DRIVEN] 发布工作空间创建事件，将触发根文档创建");
        WorkspaceCreatedEvent event = new WorkspaceCreatedEvent(workspace, userId);
        eventPublisher.publishEvent(event);
        
        log.info("=== 后端 WorkspaceManagementService.createWorkspace 结束 ===");
    }

    @Override
    @Transactional
    public Mono<Workspace> updateWorkspace(String workspaceId, String userId, UpdateWorkspaceInput input) {
        log.info("Updating workspace: {} by user: {}", workspaceId, userId);

        return PermissionUtils.requireWorkspacePermission(
                permissionService, workspaceId, userId, PermissionActions.UPDATE_SETTINGS,
                () -> Mono.fromCallable(() -> workspaceRepository.findById(workspaceId))
                        .subscribeOn(Schedulers.boundedElastic()) // 确保事务在同一线程执行
                        .flatMap(optionalWorkspace -> {
                            if (optionalWorkspace.isEmpty()) {
                                return Mono.error(new ResourceNotFoundException("Workspace", workspaceId));
                            }

                            Workspace workspace = optionalWorkspace.get();
                            
                            // 更新字段
                            if (input.name() != null) {
                                workspace.setName(input.name());
                            }
                            if (input.isPublic() != null) {
                                workspace.setPublic_(input.isPublic());
                            }
                            if (input.enableAi() != null) {
                                workspace.setEnableAi(input.enableAi());
                            }
                            if (input.enableUrlPreview() != null) {
                                workspace.setEnableUrlPreview(input.enableUrlPreview());
                            }
                            if (input.enableDocEmbedding() != null) {
                                workspace.setEnableDocEmbedding(input.enableDocEmbedding());
                            }
                            if (input.avatarKey() != null) {
                                workspace.setAvatarKey(input.avatarKey());
                            }

                            return Mono.fromCallable(() -> workspaceRepository.save(workspace))
                                    .subscribeOn(Schedulers.boundedElastic()); // 确保事务在同一线程执行
                        })
                )
                .doOnSuccess(workspace -> {
                    log.info("Workspace updated successfully: {}", workspaceId);
                    eventPublisher.publishEvent(new WorkspaceUpdatedEvent(workspace, userId));
                })
                .doOnError(error -> log.error("Failed to update workspace: {}", workspaceId, error));
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteWorkspace(String workspaceId, String userId) {
        log.info("Deleting workspace: {} by user: {}", workspaceId, userId);

        return PermissionUtils.requireWorkspaceDelete(permissionService, workspaceId, userId,
                () -> Mono.fromCallable(() -> {
                    // 删除工作空间（级联删除关联数据）
                    workspaceRepository.deleteById(workspaceId);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())) // 确保事务在同一线程执行
                .doOnSuccess(result -> {
                    log.info("Workspace deleted successfully: {}", workspaceId);
                    eventPublisher.publishEvent(new WorkspaceDeletedEvent(workspaceId, userId));
                })
                .doOnError(error -> log.error("Failed to delete workspace: {}", workspaceId, error));
    }

    @Override
    public Flux<WorkspaceWithRole> getUserWorkspaces(String userId) {
        log.debug("Getting workspaces for user: {}", userId);

        // 优化：使用批量查询避免N+1问题
        return Mono.fromCallable(() -> {
                    List<WorkspaceUserRole> userRoles = workspaceUserRoleRepository.findUserActiveWorkspaces(userId);
                    List<String> workspaceIds = userRoles.stream()
                            .map(WorkspaceUserRole::getWorkspaceId)
                            .distinct()
                            .toList();
                    
                    // 批量查询工作空间
                    List<Workspace> workspaces = workspaceRepository.findByIds(workspaceIds);
                    
                    // 创建工作空间ID到工作空间的映射
                    java.util.Map<String, Workspace> workspaceMap = workspaces.stream()
                            .collect(java.util.stream.Collectors.toMap(Workspace::getId, w -> w));
                    
                    // 组合结果
                    return userRoles.stream()
                            .map(userRole -> {
                                Workspace workspace = workspaceMap.get(userRole.getWorkspaceId());
                                if (workspace == null) {
                                    return null;
                                }
                                return new WorkspaceWithRole(
                                        workspace,
                                        userRole.getType(),
                                        convertStatus(userRole.getStatus()),
                                        userRole.getType().hasOwnerPermission(),
                                        userRole.getType().hasAdminPermission()
                                );
                            })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .doOnComplete(() -> log.debug("Retrieved workspaces for user: {}", userId));
    }

    @Override
    public Mono<WorkspaceWithRole> getWorkspace(String workspaceId, String userId) {
        log.debug("Getting workspace: {} for user: {}", workspaceId, userId);

        return PermissionUtils.requireWorkspaceRead(permissionService, workspaceId, userId,
                () -> Mono.fromCallable(() -> workspaceRepository.findById(workspaceId))
                        .subscribeOn(Schedulers.boundedElastic()) // 确保事务在同一线程执行
                        .flatMap(optionalWorkspace -> {
                            if (optionalWorkspace.isEmpty()) {
                                return Mono.error(new ResourceNotFoundException("Workspace", workspaceId));
                            }

                            Workspace workspace = optionalWorkspace.get();
                            
                            return getUserWorkspaceRole(workspaceId, userId)
                            .map(role -> {
                                WorkspaceUserRole userRole = workspaceUserRoleRepository
                                        .findByWorkspaceIdAndUserId(workspaceId, userId)
                                        .orElse(null);

                                WorkspaceMemberStatus status = userRole != null ? 
                                        convertStatus(userRole.getStatus()) : WorkspaceMemberStatus.PENDING;

                                // 🔧 [CRITICAL-DEBUG] 增加调试日志查看boolean值计算过程
                                boolean isOwnerFlag = role == WorkspaceRole.OWNER;
                                boolean isAdminFlag = role.hasAdminPermission();
                                
                                log.info("🎯🎯🎯 [CRITICAL-DEBUG] WorkspaceWithRole创建过程:");
                                log.info("  📋 role={}", role);
                                log.info("  📋 role == WorkspaceRole.OWNER: {}", isOwnerFlag);
                                log.info("  📋 role.hasAdminPermission(): {}", isAdminFlag);
                                log.info("  📋 status={}", status);

                                WorkspaceWithRole result = new WorkspaceWithRole(
                                        workspace,
                                        role,
                                        status,
                                        isOwnerFlag,
                                        isAdminFlag
                                );
                                
                                log.info("  ✅ 创建的WorkspaceWithRole: isOwner={}, isAdmin={}", 
                                        result.isOwner(), result.isAdmin());
                                        
                                return result;
                            })
                            .switchIfEmpty(
                                // 外部用户访问公开工作空间
                                Mono.just(new WorkspaceWithRole(
                                        workspace,
                                        WorkspaceRole.EXTERNAL,
                                        WorkspaceMemberStatus.ACCEPTED,
                                        false,
                                        false
                                ))
                            );
                        })
                );
    }

    @Override
    @Transactional
    public Mono<List<InviteResult>> inviteMembers(String workspaceId, String inviterId, 
                                                 List<String> emails, WorkspaceRole role) {
        log.info("Inviting {} members to workspace: {} by user: {}", emails.size(), workspaceId, inviterId);

        return PermissionUtils.requireWorkspaceManageUsers(permissionService, workspaceId, inviterId,
                () -> Flux.fromIterable(emails)
                        .flatMap(email -> inviteSingleMember(workspaceId, inviterId, email, role))
                        .collectList())
                .doOnSuccess(results -> {
                    long successCount = results.stream().mapToLong(r -> r.success() ? 1 : 0).sum();
                    log.info("Invited {}/{} members successfully to workspace: {}", 
                            successCount, emails.size(), workspaceId);
                    
                    eventPublisher.publishEvent(new MembersInvitedEvent(workspaceId, inviterId, results));
                })
                .doOnError(error -> log.error("Failed to invite members to workspace: {}", workspaceId, error));
    }

    private Mono<InviteResult> inviteSingleMember(String workspaceId, String inviterId, 
                                                 String email, WorkspaceRole role) {
        return userService.findByEmail(email)
                .flatMap(user -> {
                    // 检查是否已经是成员或已被邀请
                    if (workspaceUserRoleRepository.isEmailAlreadyInvited(workspaceId, email)) {
                        return Mono.just(new InviteResult(email, false, "Already invited or member", null));
                    }

                    // 创建邀请
                    WorkspaceUserRole invitation = new WorkspaceUserRole();
                    invitation.setWorkspaceId(workspaceId);
                    invitation.setUserId(user.getId());
                    invitation.setType(role);
                    invitation.setStatus(convertToEntityStatus(WorkspaceMemberStatus.PENDING));
                    invitation.setSource(convertToEntitySource(WorkspaceMemberSource.EMAIL));
                    invitation.setInviterId(inviterId);

                    return Mono.fromCallable(() -> workspaceUserRoleRepository.save(invitation))
                            .flatMap(savedInvitation -> {
                                // 发送邀请邮件
                                return sendInvitationEmail(workspaceId, inviterId, email, savedInvitation.getId())
                                        .thenReturn(new InviteResult(email, true, "Invited successfully", savedInvitation.getId()));
                            });
                })
                .switchIfEmpty(
                    // 用户不存在，创建临时用户并邀请
                    createTempUserAndInvite(workspaceId, inviterId, email, role)
                )
                .onErrorReturn(new InviteResult(email, false, "Failed to invite", null));
    }

    private Mono<InviteResult> createTempUserAndInvite(String workspaceId, String inviterId, 
                                                      String email, WorkspaceRole role) {
        // 创建临时用户的逻辑
        return Mono.fromCallable(() -> userService.createTempUser(email))
                .flatMap(tempUser -> {
                    WorkspaceUserRole invitation = new WorkspaceUserRole();
                    invitation.setWorkspaceId(workspaceId);
                    invitation.setUserId(tempUser.getId());
                    invitation.setType(role);
                    invitation.setStatus(convertToEntityStatus(WorkspaceMemberStatus.PENDING));
                    invitation.setSource(convertToEntitySource(WorkspaceMemberSource.EMAIL));
                    invitation.setInviterId(inviterId);

                    return Mono.fromCallable(() -> workspaceUserRoleRepository.save(invitation))
                            .flatMap(savedInvitation -> {
                                return sendInvitationEmail(workspaceId, inviterId, email, savedInvitation.getId())
                                        .thenReturn(new InviteResult(email, true, "Invited successfully", savedInvitation.getId()));
                            });
                });
    }

    private Mono<Void> sendInvitationEmail(String workspaceId, String inviterId, String email, String inviteId) {
        // 发送邮件的逻辑
        return mailService.sendWorkspaceInvitation(workspaceId, inviterId, email, inviteId);
    }

    @Override
    public Mono<InviteLink> createInviteLink(String workspaceId, String userId, InviteLinkExpireTime expireTime) {
        log.info("Creating invite link for workspace: {} by user: {}", workspaceId, userId);

        return PermissionUtils.requireWorkspaceManageUsers(permissionService, workspaceId, userId,
                () -> Mono.fromCallable(() -> {
                    String inviteId = UUID.randomUUID().toString();
                    OffsetDateTime expireAt = OffsetDateTime.now().plusSeconds(expireTime.getSeconds());
                    return new InviteLinkDataWithKey(inviteId, expireAt);
                })
                .flatMap(data -> {
                    String key = INVITE_LINK_PREFIX + data.inviteId;
                    InviteLinkData linkData = new InviteLinkData(workspaceId, userId, data.expireAt);
                    return redisTemplate.opsForValue()
                        .set(key, linkData, Duration.ofSeconds(expireTime.getSeconds()))
                        .thenReturn(new InviteLink(data.inviteId, "/invite/" + data.inviteId, data.expireAt));
                }))
                .doOnSuccess(inviteLink -> log.info("Invite link created: {} for workspace: {}", 
                        inviteLink.inviteId(), workspaceId))
                .doOnError(error -> log.error("Failed to create invite link for workspace: {}", workspaceId, error));
    }

    @Override
    public Mono<Boolean> revokeInviteLink(String workspaceId, String userId) {
        log.info("Revoking invite link for workspace: {} by user: {}", workspaceId, userId);

        return PermissionUtils.requireWorkspaceManageUsers(permissionService, workspaceId, userId,
                () -> Mono.fromCallable(() -> {
                    // 删除Redis中的邀请链接（这里简化处理，实际需要找到对应的key）
                    // 实际实现中可能需要维护一个工作空间到邀请链接的映射
                    return true;
                }))
                .doOnSuccess(result -> log.info("Invite link revoked for workspace: {}", workspaceId))
                .doOnError(error -> log.error("Failed to revoke invite link for workspace: {}", workspaceId, error));
    }

    /**
     * 通过邀请链接接受工作空间邀请
     * 
     * @param inviteId 邀请链接ID
     * @param userId 接受邀请的用户ID
     * @return 是否成功接受邀请
     * @deprecated 功能待实现，当前返回false
     * @see #acceptInviteById(String, String)
     */
    @Deprecated
    @Override
    public Mono<Boolean> acceptInviteByLink(String inviteId, String userId) {
        // TODO: 实现通过邀请链接接受邀请的逻辑
        // 需要：1. 从Redis获取邀请链接信息 2. 验证邀请是否有效 3. 创建或更新WorkspaceUserRole
        return Mono.just(false);
    }

    /**
     * 通过邀请ID接受工作空间邀请
     * 
     * @param inviteId 邀请ID
     * @param userId 接受邀请的用户ID
     * @return 是否成功接受邀请
     * @deprecated 功能待实现，当前返回false
     * @see #acceptInviteByLink(String, String)
     */
    @Deprecated
    @Override
    public Mono<Boolean> acceptInviteById(String inviteId, String userId) {
        // TODO: 实现通过邀请ID接受邀请的逻辑
        // 需要：1. 查询WorkspaceUserRole获取邀请信息 2. 验证邀请状态 3. 更新状态为ACCEPTED
        return Mono.just(false);
    }

    @Override
    public Mono<Boolean> approveMember(String workspaceId, String adminId, String userId) {
        log.info("Approving member: {} in workspace: {} by user: {}", userId, workspaceId, adminId);

        return PermissionUtils.requireWorkspaceManageUsers(permissionService, workspaceId, adminId,
                () -> Mono.fromCallable(() -> {
                    // 更新用户角色状态
                    Optional<WorkspaceUserRole> userRole = 
                            workspaceUserRoleRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
                    
                    if (userRole.isEmpty()) {
                        return false;
                    }
                    
                    WorkspaceUserRole role = userRole.get();
                    role.setStatus(convertToEntityStatus(WorkspaceMemberStatus.ACCEPTED));
                    workspaceUserRoleRepository.save(role);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())) // 确保事务在同一线程执行
                .doOnSuccess(success -> {
                    if (success) {
                        log.info("Member approved successfully: {} in workspace: {}", userId, workspaceId);
                        // 发送事件
                    }
                })
                .doOnError(error -> log.error("Failed to approve member: {}", userId, error));
    }

    /**
     * 授予成员特定角色权限
     * 
     * @param workspaceId 工作空间ID
     * @param adminId 执行操作的管理员ID
     * @param userId 被授予权限的用户ID
     * @param role 要授予的角色
     * @return 是否成功授予权限
     * @deprecated 功能待实现，当前返回false
     */
    @Deprecated
    @Override
    public Mono<Boolean> grantMember(String workspaceId, String adminId, String userId, WorkspaceRole role) {
        // TODO: 实现授予成员权限的逻辑
        // 需要：1. 检查管理员权限 2. 查找或创建WorkspaceUserRole 3. 更新角色类型
        return Mono.just(false);
    }

    /**
     * 撤销成员权限（移除成员）
     * 
     * @param workspaceId 工作空间ID
     * @param adminId 执行操作的管理员ID
     * @param userId 被撤销权限的用户ID
     * @return 是否成功撤销权限
     * @deprecated 功能待实现，当前返回false
     */
    @Deprecated
    @Override
    public Mono<Boolean> revokeMember(String workspaceId, String adminId, String userId) {
        // TODO: 实现撤销成员权限的逻辑
        // 需要：1. 检查管理员权限 2. 删除WorkspaceUserRole记录
        return Mono.just(false);
    }

    /**
     * 用户主动离开工作空间
     * 
     * @param workspaceId 工作空间ID
     * @param userId 离开的用户ID
     * @return 是否成功离开
     * @deprecated 功能待实现，当前返回false
     */
    @Deprecated
    @Override
    public Mono<Boolean> leaveWorkspace(String workspaceId, String userId) {
        // TODO: 实现用户离开工作空间的逻辑
        // 需要：1. 检查是否为所有者（所有者不能离开） 2. 删除WorkspaceUserRole记录
        return Mono.just(false);
    }

    /**
     * 转移工作空间所有权
     * 
     * @param workspaceId 工作空间ID
     * @param currentOwnerId 当前所有者ID
     * @param newOwnerId 新所有者ID
     * @return 是否成功转移所有权
     * @deprecated 功能待实现，当前返回false
     */
    @Deprecated
    @Override
    public Mono<Boolean> transferOwnership(String workspaceId, String currentOwnerId, String newOwnerId) {
        // TODO: 实现转移所有权的逻辑
        // 需要：1. 验证当前用户是所有者 2. 更新两个用户的角色（当前所有者->ADMIN，新所有者->OWNER）
        return Mono.just(false);
    }

    /**
     * 获取工作空间成员列表
     * 
     * @param workspaceId 工作空间ID
     * @param userId 查询用户ID（用于权限检查）
     * @return 成员信息列表
     * @deprecated 功能待实现，当前返回空列表
     */
    @Deprecated
    @Override
    public Flux<WorkspaceMemberInfo> getWorkspaceMembers(String workspaceId, String userId) {
        // TODO: 实现获取成员列表的逻辑
        // 需要：1. 检查读取权限 2. 查询WorkspaceUserRole 3. 关联用户信息并转换为WorkspaceMemberInfo
        return Flux.empty();
    }

    /**
     * 获取待处理的邀请列表
     * 
     * @param workspaceId 工作空间ID
     * @param userId 查询用户ID（用于权限检查）
     * @return 待处理邀请列表
     * @deprecated 功能待实现，当前返回空列表
     */
    @Deprecated
    @Override
    public Flux<WorkspaceMemberInfo> getPendingInvitations(String workspaceId, String userId) {
        // TODO: 实现获取待处理邀请的逻辑
        // 需要：1. 检查读取权限 2. 查询状态为PENDING的WorkspaceUserRole 3. 转换为WorkspaceMemberInfo
        return Flux.empty();
    }

    /**
     * 获取邀请信息
     * 
     * @param inviteId 邀请ID
     * @return 邀请信息
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<InvitationInfo> getInviteInfo(String inviteId) {
        // TODO: 实现获取邀请信息的逻辑
        // 需要：1. 查询WorkspaceUserRole 2. 关联工作空间和邀请者信息 3. 转换为InvitationInfo
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> hasWorkspacePermission(String workspaceId, String userId, WorkspaceAction action) {
        String actionStr = action != null ? action.name().toLowerCase() : PermissionActions.READ;
        return permissionService.checkWorkspacePermission(workspaceId, userId, actionStr);
    }

    @Override
    public Mono<WorkspaceRole> getUserWorkspaceRole(String workspaceId, String userId) {
        log.info("💫💫💫 [CRITICAL-DEBUG] WorkspaceManagementServiceImpl.getUserWorkspaceRole被调用!!!");
        log.info("  📋 参数: workspaceId='{}', userId='{}'", workspaceId, userId);
        log.info("  🔍 workspaceId格式: 长度={}, 包含连字符={}", 
                workspaceId != null ? workspaceId.length() : 0, 
                workspaceId != null ? workspaceId.contains("-") : false);
        log.info("  🔍 userId格式: 长度={}, 包含连字符={}", 
                userId != null ? userId.length() : 0, 
                userId != null ? userId.contains("-") : false);
        
        return Mono.fromCallable(() -> {
            log.info("  🔍 调用workspaceUserRoleRepository.getUserWorkspaceRole...");
            Optional<WorkspaceRole> roleOpt = workspaceUserRoleRepository.getUserWorkspaceRole(workspaceId, userId);
            log.info("  📋 Repository查询结果: role存在={}", roleOpt.isPresent());
            if (roleOpt.isPresent()) {
                log.info("  ✅ 找到用户角色: {}", roleOpt.get());
            } else {
                log.warn("  ⚠️ 未找到用户角色，可能是权限配置问题");
            }
            return roleOpt;
        }).flatMap(optionalRole -> {
            if (optionalRole.isPresent()) {
                WorkspaceRole role = optionalRole.get();
                log.info("✅ [CRITICAL-DEBUG] 成功获取用户工作空间角色: userId='{}', workspaceId='{}', role={}", userId, workspaceId, role);
                return Mono.just(role);
            } else {
                log.error("❌ [CRITICAL-DEBUG] 用户在工作空间中没有角色: userId='{}', workspaceId='{}'", userId, workspaceId);
                log.error("  🔍 这通常意味着workspace_user_roles表中没有对应记录");
                log.error("  🔍 请检查工作空间创建时是否正确插入了权限记录");
                return Mono.empty();
            }
        }).doOnError(error -> {
            log.error("❌ [CRITICAL-DEBUG] 获取用户工作空间角色时发生异常: userId='{}', workspaceId='{}'", userId, workspaceId, error);
        });
    }

    /**
     * 分配工作空间席位
     * 
     * @param workspaceId 工作空间ID
     * @param limit 席位限制数量
     * @return 操作完成信号
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<Void> allocateSeats(String workspaceId, int limit) {
        // TODO: 实现席位分配逻辑
        // 需要：1. 更新工作空间的席位限制 2. 可能需要处理超额成员的降级
        return Mono.empty();
    }

    /**
     * 获取工作空间席位配额信息
     * 
     * @param workspaceId 工作空间ID
     * @return 席位配额信息
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<SeatQuota> getSeatQuota(String workspaceId) {
        // TODO: 实现获取席位配额的逻辑
        // 需要：1. 查询工作空间的席位配置 2. 统计当前使用的席位数量 3. 返回配额信息
        return Mono.empty();
    }

    // 内部数据类
    private record InviteLinkData(String workspaceId, String inviterId, OffsetDateTime expireAt) {}
    private record InviteLinkDataWithKey(String inviteId, OffsetDateTime expireAt) {}

    // 事件类
    public static class WorkspaceCreatedEvent {
        private final Workspace workspace;
        private final String userId;

        public WorkspaceCreatedEvent(Workspace workspace, String userId) {
            this.workspace = workspace;
            this.userId = userId;
        }

        public Workspace getWorkspace() { return workspace; }
        public String getUserId() { return userId; }
    }

    public static class WorkspaceUpdatedEvent {
        private final Workspace workspace;
        private final String userId;

        public WorkspaceUpdatedEvent(Workspace workspace, String userId) {
            this.workspace = workspace;
            this.userId = userId;
        }

        public Workspace getWorkspace() { return workspace; }
        public String getUserId() { return userId; }
    }

    public static class WorkspaceDeletedEvent {
        private final String workspaceId;
        private final String userId;

        public WorkspaceDeletedEvent(String workspaceId, String userId) {
            this.workspaceId = workspaceId;
            this.userId = userId;
        }

        public String getWorkspaceId() { return workspaceId; }
        public String getUserId() { return userId; }
    }

    public static class MembersInvitedEvent {
        private final String workspaceId;
        private final String inviterId;
        private final List<InviteResult> results;

        public MembersInvitedEvent(String workspaceId, String inviterId, List<InviteResult> results) {
            this.workspaceId = workspaceId;
            this.inviterId = inviterId;
            this.results = results;
        }

        public String getWorkspaceId() { return workspaceId; }
        public String getInviterId() { return inviterId; }
        public List<InviteResult> getResults() { return results; }
    }

    /**
     * 生成默认头像key
     * 基于工作空间名称的首字母生成默认头像标识
     */
    private String generateDefaultAvatarKey(String workspaceName) {
        if (workspaceName == null || workspaceName.trim().isEmpty()) {
            return "default-workspace"; // 默认头像key
        }
        
        // 获取首字母，转为大写
        String firstChar = workspaceName.trim().substring(0, 1).toUpperCase();
        
        // 生成简单的头像key，可以后续扩展为更复杂的逻辑
        return "workspace-" + firstChar.toLowerCase();
    }

    /**
     * 将实体枚举转换为服务枚举
     */
    private WorkspaceMemberStatus convertStatus(WorkspaceUserRole.WorkspaceMemberStatus entityStatus) {
        if (entityStatus == null) {
            return WorkspaceMemberStatus.PENDING;
        }
        switch (entityStatus) {
            case ACCEPTED:
                return WorkspaceMemberStatus.ACCEPTED;
            case PENDING:
                return WorkspaceMemberStatus.PENDING;
            case REJECTED:
                return WorkspaceMemberStatus.PENDING; // REJECTED 映射到 PENDING
            default:
                return WorkspaceMemberStatus.PENDING;
        }
    }

    /**
     * 将服务枚举转换为实体枚举
     */
    private WorkspaceUserRole.WorkspaceMemberStatus convertToEntityStatus(WorkspaceMemberStatus status) {
        if (status == null) {
            return WorkspaceUserRole.WorkspaceMemberStatus.PENDING;
        }
        switch (status) {
            case ACCEPTED:
                return WorkspaceUserRole.WorkspaceMemberStatus.ACCEPTED;
            case PENDING:
            case UNDER_REVIEW:
            case ALLOCATING_SEAT:
            case NEED_MORE_SEAT:
            case NEED_MORE_SEAT_AND_REVIEW:
                return WorkspaceUserRole.WorkspaceMemberStatus.PENDING;
            default:
                return WorkspaceUserRole.WorkspaceMemberStatus.PENDING;
        }
    }

    /**
     * 将实体枚举转换为服务枚举
     */
    private WorkspaceMemberSource convertSource(WorkspaceUserRole.WorkspaceMemberSource entitySource) {
        if (entitySource == null) {
            return WorkspaceMemberSource.EMAIL;
        }
        switch (entitySource) {
            case EMAIL:
                return WorkspaceMemberSource.EMAIL;
            case LINK:
                return WorkspaceMemberSource.LINK;
            case SELF_JOIN:
                return WorkspaceMemberSource.LINK; // SELF_JOIN 映射到 LINK
            default:
                return WorkspaceMemberSource.EMAIL;
        }
    }

    /**
     * 将服务枚举转换为实体枚举
     */
    private WorkspaceUserRole.WorkspaceMemberSource convertToEntitySource(WorkspaceMemberSource source) {
        if (source == null) {
            return WorkspaceUserRole.WorkspaceMemberSource.EMAIL;
        }
        switch (source) {
            case EMAIL:
                return WorkspaceUserRole.WorkspaceMemberSource.EMAIL;
            case LINK:
                return WorkspaceUserRole.WorkspaceMemberSource.LINK;
            default:
                return WorkspaceUserRole.WorkspaceMemberSource.EMAIL;
        }
    }
}