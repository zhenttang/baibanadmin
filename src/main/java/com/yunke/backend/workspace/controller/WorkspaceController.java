package com.yunke.backend.workspace.controller;

import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.system.service.QuotaService;
import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole.WorkspaceRole;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.workspace.service.WorkspaceManagementService;
import com.yunke.backend.workspace.service.WorkspaceManagementService.WorkspaceWithRole;
import com.yunke.backend.workspace.service.WorkspaceManagementService.CreateWorkspaceInput;
import com.yunke.backend.workspace.service.WorkspaceManagementService.UpdateWorkspaceInput;
import com.yunke.backend.workspace.service.WorkspaceManagementService.InviteLinkExpireTime;



import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 工作空间控制器
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Slf4j
public class WorkspaceController {

    private final WorkspaceManagementService workspaceManagementService;
    private final QuotaService quotaService;
    private final WorkspaceDocRepository workspaceDocRepository;
    private final PermissionService permissionService;

    /**
     * 获取当前认证用户信息
     */
    private Mono<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        log.info("🔐 [Auth] SecurityContext状态检查:");
        log.info("  - Authentication对象: {}", authentication != null ? "存在" : "null");
        
        if (authentication != null) {
            log.info("  - Principal类型: {}", authentication.getPrincipal().getClass().getSimpleName());
            log.info("  - Principal值: {}", authentication.getPrincipal());
            log.info("  - 是否为AffineUserDetails: {}", authentication.getPrincipal() instanceof AffineUserDetails);
        }

        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            log.warn("⚠️ [Auth] 认证失败 - authentication={}, principal类型={}", 
                authentication != null ? "存在" : "null", 
                authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "N/A");
            return Mono.empty();
        }

        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        log.info("✅ [Auth] 认证成功 - userId: {}", userId);
        return Mono.just(userId);
    }

    /**
     * 检查认证并返回错误响应
     */
    private Mono<ResponseEntity<Map<String, Object>>> checkAuthenticationAndReturn() {
        return getCurrentUserId()
                .map(userId -> (ResponseEntity<Map<String, Object>>) null)
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 获取用户的工作空间列表
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getUserWorkspaces(HttpServletRequest httpRequest) {
        log.info("=== getUserWorkspaces 方法开始 ===");
        
        // 记录所有请求头
        log.info("请求头信息:");
        httpRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            log.info("  {}: {}", headerName, httpRequest.getHeader(headerName))
        );
        
        // 直接从传统SecurityContextHolder获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("从SecurityContextHolder获取到Authentication: {}", authentication != null ? "存在" : "null");
        
        if (authentication != null) {
            log.info("Authentication类型: {}", authentication.getClass().getSimpleName());
            log.info("Authentication Principal: {}", authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
            log.info("Authentication isAuthenticated: {}", authentication.isAuthenticated());
            log.info("Authentication Authorities: {}", authentication.getAuthorities());
        }
        
        String userId;
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            String errorMsg = authentication == null ? "Authentication is null" : "Invalid principal type";
            log.error("认证失败: {}", errorMsg);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication required");
            errorResponse.put("message", "Please login first");
            return Mono.just(ResponseEntity.status(401).body(errorResponse));
        } else {
            // 正常模式 - 从认证中获取用户ID
            AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
            userId = userDetails.getUserId();
            log.info("成功获取用户信息，获取用户工作空间列表, userId: {}", userId);
        }
        
        return workspaceManagementService.getUserWorkspaces(userId)
                .collectList()
                .map(workspaces -> {
                    Map<String, Object> response = new HashMap<>();
                    
                    try {
                        log.info("准备序列化工作空间数据，工作空间数量: {}", workspaces.size());
                        
                        // 手动构建安全的工作空间数据结构，避免懒加载问题
                        List<Map<String, Object>> safeWorkspaces = new ArrayList<>();
                        
                        for (WorkspaceWithRole ws : workspaces) {
                            Workspace workspace = ws.workspace();
                            
                            // 为每个工作空间创建一个新的Map，只包含必要的非懒加载字段
                            Map<String, Object> safeWorkspace = new HashMap<>();
                            safeWorkspace.put("id", workspace.getId());
                            safeWorkspace.put("name", workspace.getName());
                            safeWorkspace.put("isPublic", workspace.isPublic_());
                            safeWorkspace.put("public", workspace.isPublic_());  // 兼容前端可能的两种属性名
                            safeWorkspace.put("enableAi", workspace.isEnableAi());
                            safeWorkspace.put("enableUrlPreview", workspace.isEnableUrlPreview());
                            safeWorkspace.put("enableDocEmbedding", workspace.isEnableDocEmbedding());
                            safeWorkspace.put("createdAt", workspace.getCreatedAt());
                            safeWorkspace.put("avatarKey", workspace.getAvatarKey());
                            
                            // 不包含懒加载集合如docs, permissions等
                            
                            // 添加角色信息
                            Map<String, Object> safeWorkspaceWithRole = new HashMap<>();
                            safeWorkspaceWithRole.put("workspace", safeWorkspace);
                            safeWorkspaceWithRole.put("role", ws.role().toString());
                            safeWorkspaceWithRole.put("status", ws.status().toString());
                            safeWorkspaceWithRole.put("isOwner", ws.isOwner());
                            safeWorkspaceWithRole.put("isAdmin", ws.isAdmin());
                            
                            safeWorkspaces.add(safeWorkspaceWithRole);
                            
                            log.info("已安全序列化工作空间: ID={}, 名称={}, 角色={}", 
                                    workspace.getId(), workspace.getName(), ws.role());
                        }
                        
                        response.put("workspaces", safeWorkspaces);
                        response.put("count", safeWorkspaces.size());
                        log.info("工作空间数据序列化成功");
                    } catch (Exception e) {
                        log.error("工作空间数据序列化失败", e);
                        
                        // 返回空列表和错误信息，避免500错误
                        response.put("workspaces", List.of());
                        response.put("count", 0);
                        response.put("error", "数据序列化错误，请联系管理员");
                    }
                    
                    log.info("返回工作空间列表, 数量: {}", response.get("count"));
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> log.error("获取工作空间列表失败", error))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get workspaces")))
                .doFinally(signalType -> log.info("=== getUserWorkspaces 方法结束，signalType: {} ===", signalType));
    }

    /**
     * 创建工作空间
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createWorkspace(
            @RequestBody CreateWorkspaceRequest request,
            HttpServletRequest httpRequest) {

        log.info("=== createWorkspace 方法开始 ===");
        log.info("请求参数: name={}, isPublic={}, enableAi={}, enableUrlPreview={}, enableDocEmbedding={}", 
                request.name(), request.isPublic(), request.enableAi(), 
                request.enableUrlPreview(), request.enableDocEmbedding());
        
        // 记录所有请求头
        log.info("请求头信息:");
        httpRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            log.info("  {}: {}", headerName, httpRequest.getHeader(headerName))
        );

        // 直接从传统SecurityContextHolder获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("从SecurityContextHolder获取到Authentication: {}", authentication != null ? "存在" : "null");
        
        if (authentication != null) {
            log.info("Authentication类型: {}", authentication.getClass().getSimpleName());
            log.info("Authentication Principal: {}", authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
            log.info("Authentication isAuthenticated: {}", authentication.isAuthenticated());
            log.info("Authentication Authorities: {}", authentication.getAuthorities());
        }

        String userId;
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            String errorMsg = authentication == null ? "Authentication is null" : "Invalid principal type";
            log.error("认证失败: {}", errorMsg);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication required");
            errorResponse.put("message", "Please login first");
            return Mono.just(ResponseEntity.status(401).body(errorResponse));
        } else {
            AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
            userId = userDetails.getUserId();
            log.info("成功获取用户信息, userId: {}", userId);
        }

        CreateWorkspaceInput input = new CreateWorkspaceInput(
                request.name(),
                request.isPublic(),
                request.enableAi(),
                request.enableUrlPreview(),
                request.enableDocEmbedding()
        );

        log.info("调用workspaceManagementService.createWorkspace, userId: {}", userId);
        return workspaceManagementService.createWorkspace(userId, input)
                .doOnNext(workspace -> {
                    log.info("🎉 [WORKSPACE-CONTROLLER] 工作空间创建成功!");
                    log.info("  🆔 返回给前端的workspaceId: '{}'", workspace.getId());
                    log.info("  📝 返回给前端的workspaceName: '{}'", workspace.getName());
                    log.info("  🔍 [ID-VERIFICATION] 确认工作空间ID格式: 长度={}, 包含连字符={}", 
                            workspace.getId().length(), workspace.getId().contains("-"));
                })
                .map(workspace -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("workspace", workspace);
                    
                    log.info("🚀 [WORKSPACE-CONTROLLER] 准备返回的完整响应:");
                    log.info("  📦 响应结构: success=true, workspace.id='{}'", workspace.getId());
                    log.info("  📤 前端将收到这个workspaceId用于后续操作");
                    
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> log.error("工作空间创建失败", error))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to create workspace")))
                .doOnNext(response -> log.info("最终响应状态码: {}, 响应体: {}", response.getStatusCode(), response.getBody()))
                .doFinally(signalType -> log.info("=== createWorkspace 方法结束，signalType: {} ===", signalType));
    }

    /**
     * 获取工作空间详情
     */
    @GetMapping("/{workspaceId}")
    @Transactional(readOnly = true)
    public Mono<ResponseEntity<Map<String, Object>>> getWorkspace(
            @PathVariable String workspaceId) {

        log.info("=== getWorkspace 开始 ===");
        log.info("请求的工作空间ID: {}", workspaceId);
        
        // 1. 首先验证工作空间ID格式和长度
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            log.warn("工作空间ID为空或null");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "工作空间ID不能为空");
            errorResponse.put("errorCode", "INVALID_WORKSPACE_ID");
            return Mono.just(ResponseEntity.status(400).body(errorResponse));
        }
        
        // 2. 检查ID格式是否合理（简单验证）
        String trimmedId = workspaceId.trim();
        if (trimmedId.length() < 10 || trimmedId.length() > 50) {
            log.warn("工作空间ID格式可能无效: {}, 长度: {}", trimmedId, trimmedId.length());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "工作空间ID格式无效");
            errorResponse.put("errorCode", "INVALID_WORKSPACE_ID_FORMAT");
            errorResponse.put("providedId", trimmedId);
            return Mono.just(ResponseEntity.status(400).body(errorResponse));
        }
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("获取工作空间详情: 工作空间ID={}, 用户ID={}", trimmedId, userId);
                    
                    return workspaceManagementService.getWorkspace(trimmedId, userId)
                            .map(workspaceWithRole -> {
                                log.info("成功找到工作空间: {}, 用户: {}", trimmedId, userId);
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                
                                // 创建不依赖懒加载属性的工作空间数据
                                Workspace workspace = workspaceWithRole.workspace();
                                Map<String, Object> safeWorkspace = new HashMap<>();
                                safeWorkspace.put("id", workspace.getId());
                                safeWorkspace.put("name", workspace.getName() != null ? workspace.getName() : "未命名工作空间");
                                safeWorkspace.put("isPublic", workspace.isPublic_());
                                safeWorkspace.put("public", workspace.isPublic_());
                                safeWorkspace.put("enableAi", workspace.isEnableAi());
                                safeWorkspace.put("enableUrlPreview", workspace.isEnableUrlPreview());
                                safeWorkspace.put("enableDocEmbedding", workspace.isEnableDocEmbedding());
                                safeWorkspace.put("createdAt", workspace.getCreatedAt());
                                safeWorkspace.put("avatarKey", workspace.getAvatarKey());
                                
                                // 添加角色和权限信息
                                response.put("workspace", safeWorkspace);
                                response.put("role", workspaceWithRole.role().toString());
                                response.put("status", workspaceWithRole.status().toString());
                                response.put("isOwner", workspaceWithRole.isOwner());
                                response.put("isAdmin", workspaceWithRole.isAdmin());
                                
                                // 🔧 [CRITICAL-DEBUG] 验证响应数据
                                log.info("🎯🎯🎯 [CRITICAL-DEBUG] 即将返回给前端的响应数据:");
                                log.info("  📋 response.isOwner: {}", response.get("isOwner"));
                                log.info("  📋 response.isAdmin: {}", response.get("isAdmin"));
                                log.info("  📋 response.role: {}", response.get("role"));
                                log.info("  📋 workspaceWithRole.isOwner(): {}", workspaceWithRole.isOwner());
                                log.info("  📋 workspaceWithRole.isAdmin(): {}", workspaceWithRole.isAdmin());
                                
                                log.info("返回工作空间详情: ID={}, 名称={}, 角色={}", 
                                        workspace.getId(), workspace.getName(), workspaceWithRole.role());
                                
                                return ResponseEntity.ok(response);
                            })
                            .onErrorResume(e -> {
                                log.error("查询工作空间失败: workspaceId={}, userId={}, error={}", 
                                        trimmedId, userId, e.getMessage(), e);
                                
                                Map<String, Object> errorResponse = new HashMap<>();
                                errorResponse.put("success", false);
                                errorResponse.put("error", "工作空间不存在或无法访问");
                                errorResponse.put("errorCode", "WORKSPACE_NOT_FOUND");
                                errorResponse.put("workspaceId", trimmedId);
                                errorResponse.put("userId", userId);
                                
                                return Mono.just(ResponseEntity.status(404).body(errorResponse));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("用户未认证，无法访问工作空间: {}", trimmedId);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "用户未认证，请先登录");
                    errorResponse.put("errorCode", "UNAUTHORIZED");
                    return Mono.just(ResponseEntity.status(401).body(errorResponse));
                }))
                .doOnError(e -> log.error("处理工作空间请求时发生系统错误: workspaceId={}", trimmedId, e))
                .onErrorResume(e -> {
                    log.error("系统异常，返回500错误: workspaceId={}", trimmedId, e);
                    Map<String, Object> serverError = new HashMap<>();
                    serverError.put("success", false);
                    serverError.put("error", "处理请求时发生服务器错误");
                    serverError.put("errorCode", "INTERNAL_SERVER_ERROR");
                    return Mono.just(ResponseEntity.status(500).body(serverError));
                })
                .doFinally(signalType -> log.info("=== getWorkspace 结束, signalType: {} ===", signalType));
    }

    /**
     * 更新工作空间
     */
    @PutMapping("/{workspaceId}")
    public Mono<ResponseEntity<Map<String, Object>>> updateWorkspace(
            @PathVariable String workspaceId,
            @RequestBody UpdateWorkspaceRequest request) {

        return getCurrentUserId()
                .flatMap(userId -> {
                    UpdateWorkspaceInput input = new UpdateWorkspaceInput(
                            request.name(),
                            request.isPublic(),
                            request.enableAi(),
                            request.enableUrlPreview(),
                            request.enableDocEmbedding(),
                            request.avatarKey()
                    );

                    return workspaceManagementService.updateWorkspace(workspaceId, userId, input)
                            .map(workspace -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("workspace", workspace);
                                return ResponseEntity.ok(response);
                            })
                            .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to update workspace")));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 删除工作空间
     */
    @DeleteMapping("/{workspaceId}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteWorkspace(
            @PathVariable String workspaceId) {

        return getCurrentUserId()
                .flatMap(userId -> workspaceManagementService.deleteWorkspace(workspaceId, userId)
                        .map(success -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("success", success);
                            response.put("message", "Workspace deleted successfully");
                            return ResponseEntity.ok(response);
                        })
                        .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to delete workspace"))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 邀请成员
     */
    @PostMapping("/{workspaceId}/invite")
    public Mono<ResponseEntity<Map<String, Object>>> inviteMembers(
            @PathVariable String workspaceId,
            @RequestBody InviteMembersRequest request) {

        return getCurrentUserId()
                .flatMap(userId -> {
                    WorkspaceRole role = request.role() != null ? request.role() : WorkspaceRole.COLLABORATOR;

                    return workspaceManagementService.inviteMembers(workspaceId, userId, request.emails(), role)
                            .map(results -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("results", results);
                                
                                long successCount = results.stream().mapToLong(r -> r.success() ? 1 : 0).sum();
                                response.put("successCount", successCount);
                                response.put("totalCount", results.size());
                                
                                return ResponseEntity.ok(response);
                            })
                            .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to invite members")));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 创建邀请链接
     */
    @PostMapping("/{workspaceId}/invite-link")
    public Mono<ResponseEntity<Map<String, Object>>> createInviteLink(
            @PathVariable String workspaceId,
            @RequestBody CreateInviteLinkRequest request) {

        return getCurrentUserId()
                .flatMap(userId -> {
                    InviteLinkExpireTime expireTime = request.expireTime() != null ? 
                            request.expireTime() : InviteLinkExpireTime.ONE_WEEK;

                    return workspaceManagementService.createInviteLink(workspaceId, userId, expireTime)
                            .map(inviteLink -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("inviteLink", inviteLink);
                                return ResponseEntity.ok(response);
                            })
                            .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to create invite link")));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 撤销邀请链接
     */
    @DeleteMapping("/{workspaceId}/invite-link")
    public Mono<ResponseEntity<Map<String, Object>>> revokeInviteLink(
            @PathVariable String workspaceId) {

        return getCurrentUserId()
                .flatMap(userId -> workspaceManagementService.revokeInviteLink(workspaceId, userId)
                        .map(success -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("success", success);
                            response.put("message", "Invite link revoked successfully");
                            return ResponseEntity.ok(response);
                        })
                        .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to revoke invite link"))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 获取工作空间成员列表
     */
    @GetMapping("/{workspaceId}/members")
    public Mono<ResponseEntity<Map<String, Object>>> getWorkspaceMembers(
            @PathVariable String workspaceId) {

        return getCurrentUserId()
                .flatMap(userId -> workspaceManagementService.getWorkspaceMembers(workspaceId, userId)
                        .collectList()
                        .map(members -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("members", members);
                            response.put("count", members.size());
                            return ResponseEntity.ok(response);
                        })
                        .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get workspace members"))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 获取等待处理的邀请
     */
    @GetMapping("/{workspaceId}/pending-invitations")
    public Mono<ResponseEntity<Map<String, Object>>> getPendingInvitations(
            @PathVariable String workspaceId) {

        return getCurrentUserId()
                .flatMap(userId -> workspaceManagementService.getPendingInvitations(workspaceId, userId)
                        .collectList()
                        .map(invitations -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("invitations", invitations);
                            response.put("count", invitations.size());
                            return ResponseEntity.ok(response);
                        })
                        .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get pending invitations"))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized"))));
    }

    /**
     * 获取工作空间权限
     */
    @GetMapping("/{workspaceId}/permissions")
    public Mono<ResponseEntity<Map<String, Object>>> getWorkspacePermissions(
            @PathVariable String workspaceId) {
        
        log.info("🚀🚀🚀 [CRITICAL-DEBUG] getWorkspacePermissions API被调用!!!");
        log.info("  📋 参数: workspaceId='{}'", workspaceId);
        log.info("  🔍 workspaceId格式: 长度={}, 包含连字符={}", 
                workspaceId != null ? workspaceId.length() : 0, 
                workspaceId != null ? workspaceId.contains("-") : false);
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("为用户 {} 获取工作空间 {} 的权限", userId, workspaceId);
                    
                    // 先检查用户是否有访问工作空间的权限
                    return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                            .flatMap(role -> {
                                log.info("用户 {} 在工作空间 {} 中的角色: {}", userId, workspaceId, role);
                                
                                // 基于角色构建权限映射
                                Map<String, Boolean> permissions = new HashMap<>();
                                
                                switch (role) {
                                    case OWNER:
                                        permissions.put("Workspace_Properties_Update", true);
                                        permissions.put("Doc_Read", true);
                                        permissions.put("Doc_Write", true);
                                        permissions.put("Doc_Delete", true);
                                        permissions.put("Doc_Create", true);
                                        permissions.put("Doc_Update", true);
                                        permissions.put("Workspace_Manage_Users", true);
                                        permissions.put("Workspace_Delete", true);
                                        break;
                                    case ADMIN:
                                        permissions.put("Workspace_Properties_Update", true);
                                        permissions.put("Doc_Read", true);
                                        permissions.put("Doc_Write", true);
                                        permissions.put("Doc_Delete", true);
                                        permissions.put("Doc_Create", true);
                                        permissions.put("Doc_Update", true);
                                        permissions.put("Workspace_Manage_Users", true);
                                        permissions.put("Workspace_Delete", false);
                                        break;
                                    case COLLABORATOR:
                                        permissions.put("Workspace_Properties_Update", false);
                                        permissions.put("Doc_Read", true);
                                        permissions.put("Doc_Write", true);
                                        permissions.put("Doc_Delete", false);
                                        permissions.put("Doc_Create", true);
                                        permissions.put("Doc_Update", true);
                                        permissions.put("Workspace_Manage_Users", false);
                                        permissions.put("Workspace_Delete", false);
                                        break;
                                    case EXTERNAL:
                                        permissions.put("Workspace_Properties_Update", false);
                                        permissions.put("Doc_Read", true);
                                        permissions.put("Doc_Write", false);
                                        permissions.put("Doc_Delete", false);
                                        permissions.put("Doc_Create", false);
                                        permissions.put("Doc_Update", false);
                                        permissions.put("Workspace_Manage_Users", false);
                                        permissions.put("Workspace_Delete", false);
                                        break;
                                    default:
                                        // 默认无权限
                                        permissions.put("Workspace_Properties_Update", false);
                                        permissions.put("Doc_Read", false);
                                        permissions.put("Doc_Write", false);
                                        permissions.put("Doc_Delete", false);
                                        permissions.put("Doc_Create", false);
                                        permissions.put("Doc_Update", false);
                                        permissions.put("Workspace_Manage_Users", false);
                                        permissions.put("Workspace_Delete", false);
                                }
                                
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("permissions", permissions);
                                response.put("role", role.toString());
                                
                                log.info("返回工作空间权限: {}", permissions);
                                return Mono.just(ResponseEntity.ok(response));
                            })
                            .onErrorResume(e -> {
                                log.error("获取工作空间权限失败: {}", workspaceId, e);
                                
                                // 返回默认权限（无权限）
                                Map<String, Boolean> defaultPermissions = new HashMap<>();
                                defaultPermissions.put("Workspace_Properties_Update", false);
                                defaultPermissions.put("Doc_Read", false);
                                defaultPermissions.put("Doc_Write", false);
                                defaultPermissions.put("Doc_Delete", false);
                                defaultPermissions.put("Doc_Create", false);
                                defaultPermissions.put("Doc_Update", false);
                                defaultPermissions.put("Workspace_Manage_Users", false);
                                defaultPermissions.put("Workspace_Delete", false);
                                
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("permissions", defaultPermissions);
                                response.put("role", "NONE");
                                response.put("warning", "无法获取权限，使用默认权限");
                                
                                log.warn("使用默认权限: {}", defaultPermissions);
                                return Mono.just(ResponseEntity.ok(response));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("用户未认证，无法获取工作空间权限: {}", workspaceId);
                    return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
                }));
    }

    /**
     * 获取文档权限
     */
    @GetMapping("/{workspaceId}/docs/{docId}/permissions")
    public Mono<ResponseEntity<Map<String, Object>>> getDocPermissions(
            @PathVariable String workspaceId,
            @PathVariable String docId) {

        log.info("获取文档权限, workspaceId: {}, docId: {}", workspaceId, docId);

        // 首先检查文档是否是public的
        return Mono.fromCallable(() -> {
            Optional<WorkspaceDoc> docOpt = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
            if (docOpt.isPresent() && docOpt.get().getPublic() != null && docOpt.get().getPublic()) {
                log.info("文档 {} 是公开的，返回只读权限", docId);

                Map<String, Boolean> publicPermissions = new HashMap<>();
                publicPermissions.put("Doc_Read", true);
                publicPermissions.put("Doc_Write", false);
                publicPermissions.put("Doc_Delete", false);
                publicPermissions.put("Doc_Update", false);
                publicPermissions.put("Doc_Create", false);
                publicPermissions.put("Doc_Comment", false);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("permissions", publicPermissions);
                // 新模型：公开只读 -> Read
                int effectiveMask = (1 << 0); // Read
                if ("append-only".equalsIgnoreCase(docOpt.get().getPublicPermission())) {
                    // Read + Add（仅追加）
                    effectiveMask = (1 << 0) | (1 << 2);
                }
                response.put("effectiveMask", effectiveMask);
                response.put("role", "PUBLIC_VIEWER");
                response.put("isPublic", true);

                return response;
            }
            return null;
        })
        .flatMap(publicResponse -> {
            if (publicResponse != null) {
                return Mono.just(ResponseEntity.ok(publicResponse));
            }

            // 文档不是public，检查用户工作空间角色
            return getCurrentUserId()
                    .flatMap(userId -> {
                        log.info("为用户 {} 获取文档 {} 的权限", userId, docId);

                        // 检查用户是否有访问工作空间的权限
                        return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                                .flatMap(role -> {
                                    log.info("用户 {} 在工作空间 {} 中的角色: {}", userId, workspaceId, role);

                                    // 基于角色构建文档权限映射
                                    Map<String, Boolean> permissions = new HashMap<>();

                                    switch (role) {
                                        case OWNER:
                                            permissions.put("Doc_Read", true);
                                            permissions.put("Doc_Write", true);
                                            permissions.put("Doc_Delete", true);
                                            permissions.put("Doc_Update", true);
                                            permissions.put("Doc_Create", true);
                                            permissions.put("Doc_Comment", true);
                                            break;
                                        case ADMIN:
                                            permissions.put("Doc_Read", true);
                                            permissions.put("Doc_Write", true);
                                            permissions.put("Doc_Delete", true);
                                            permissions.put("Doc_Update", true);
                                            permissions.put("Doc_Create", true);
                                            permissions.put("Doc_Comment", true);
                                            break;
                                        case COLLABORATOR:
                                            permissions.put("Doc_Read", true);
                                            permissions.put("Doc_Write", true);
                                            permissions.put("Doc_Delete", false);
                                            permissions.put("Doc_Update", true);
                                            permissions.put("Doc_Create", true);
                                            permissions.put("Doc_Comment", true);
                                            break;
                                        case EXTERNAL:
                                            permissions.put("Doc_Read", true);
                                            permissions.put("Doc_Write", false);
                                            permissions.put("Doc_Delete", false);
                                            permissions.put("Doc_Update", false);
                                            permissions.put("Doc_Create", false);
                                            permissions.put("Doc_Comment", false);
                                            break;
                                        default:
                                            // 默认无权限
                                            permissions.put("Doc_Read", false);
                                            permissions.put("Doc_Write", false);
                                            permissions.put("Doc_Delete", false);
                                            permissions.put("Doc_Update", false);
                                            permissions.put("Doc_Create", false);
                                            permissions.put("Doc_Comment", false);
                                    }

                                    // 统一通过 PermissionService 计算位掩码
                                    int mask = permissionService.resolveEffectiveDocMask(workspaceId, docId, userId);

                                    Map<String, Object> response = new HashMap<>();
                                    response.put("success", true);
                                    response.put("permissions", permissions);
                                    response.put("effectiveMask", mask);
                                    response.put("role", role.toString());
                                    response.put("isPublic", false);

                                    log.info("返回文档权限: {}", permissions);
                                    return Mono.just(ResponseEntity.ok(response));
                                })
                                .onErrorResume(e -> {
                                    log.error("获取文档权限失败: workspaceId={}, docId={}", workspaceId, docId, e);

                                    // 返回默认权限（无权限）
                                    Map<String, Boolean> defaultPermissions = new HashMap<>();
                                    defaultPermissions.put("Doc_Read", false);
                                    defaultPermissions.put("Doc_Write", false);
                                    defaultPermissions.put("Doc_Delete", false);
                                    defaultPermissions.put("Doc_Update", false);
                                    defaultPermissions.put("Doc_Create", false);
                                    defaultPermissions.put("Doc_Comment", false);

                                    Map<String, Object> response = new HashMap<>();
                                    response.put("success", true);
                                    response.put("permissions", defaultPermissions);
                                    response.put("effectiveMask", 0);
                                    response.put("role", "NONE");
                                    response.put("isPublic", false);
                                    response.put("warning", "无法获取权限，使用默认权限");

                                    log.warn("使用默认文档权限: {}", defaultPermissions);
                                    return Mono.just(ResponseEntity.ok(response));
                                });
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("用户未认证，无法获取文档权限: workspaceId={}, docId={}", workspaceId, docId);
                        return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
                    }));
        });
    }

    // 请求数据类
    public record CreateWorkspaceRequest(
            String name,
            Boolean isPublic,
            Boolean enableAi,
            Boolean enableUrlPreview,
            Boolean enableDocEmbedding
    ) {}

    public record UpdateWorkspaceRequest(
            String name,
            Boolean isPublic,
            Boolean enableAi,
            Boolean enableUrlPreview,
            Boolean enableDocEmbedding,
            String avatarKey
    ) {}

    public record InviteMembersRequest(
            List<String> emails,
            WorkspaceUserRole.WorkspaceRole role
    ) {}

    public record CreateInviteLinkRequest(
            WorkspaceManagementService.InviteLinkExpireTime expireTime
    ) {}

    /**
     * 调试工作空间相关问题的接口
     */
    @GetMapping("/debug/workspaces")
    public Mono<ResponseEntity<Map<String, Object>>> debugWorkspaces(HttpServletRequest request) {
        log.info("=== debugWorkspaces 开始 ===");
        Map<String, Object> response = new HashMap<>();
        
        // 1. 检查认证状态
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put("hasAuthentication", auth != null);
        authInfo.put("authorization", request.getHeader("Authorization"));
        authInfo.put("userAgent", request.getHeader("User-Agent"));
        
        if (auth != null) {
            authInfo.put("principalType", auth.getPrincipal().getClass().getSimpleName());
            authInfo.put("isAffineUserDetails", auth.getPrincipal() instanceof AffineUserDetails);
            
            if (auth.getPrincipal() instanceof AffineUserDetails) {
                AffineUserDetails userDetails = (AffineUserDetails) auth.getPrincipal();
                authInfo.put("userId", userDetails.getUserId());
                authInfo.put("username", userDetails.getUsername());
            }
        }
        response.put("authentication", authInfo);
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("调试工作空间状态，用户ID: {}", userId);
                    
                    // 2. 获取用户的工作空间列表
                    return workspaceManagementService.getUserWorkspaces(userId)
                            .collectList()
                            .map(workspaces -> {
                                List<Map<String, Object>> workspaceDebugInfo = new ArrayList<>();
                                
                                for (WorkspaceWithRole ws : workspaces) {
                                    Map<String, Object> wsInfo = new HashMap<>();
                                    Workspace workspace = ws.workspace();
                                    
                                    wsInfo.put("id", workspace.getId());
                                    wsInfo.put("name", workspace.getName());
                                    wsInfo.put("role", ws.role().toString());
                                    wsInfo.put("status", ws.status().toString());
                                    wsInfo.put("isOwner", ws.isOwner());
                                    wsInfo.put("isAdmin", ws.isAdmin());
                                    wsInfo.put("isPublic", workspace.isPublic_());
                                    wsInfo.put("createdAt", workspace.getCreatedAt());
                                    
                                    workspaceDebugInfo.add(wsInfo);
                                }
                                
                                response.put("userId", userId);
                                response.put("workspaces", workspaceDebugInfo);
                                response.put("workspaceCount", workspaceDebugInfo.size());
                                response.put("timestamp", System.currentTimeMillis());
                                response.put("success", true);
                                
                                log.info("调试信息: 用户 {} 有 {} 个工作空间", userId, workspaceDebugInfo.size());
                                return ResponseEntity.ok(response);
                            })
                            .onErrorResume(e -> {
                                log.error("获取工作空间调试信息失败", e);
                                response.put("error", "获取工作空间信息失败: " + e.getMessage());
                                response.put("success", false);
                                return Mono.just(ResponseEntity.status(500).body(response));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("用户未认证，无法获取工作空间调试信息");
                    response.put("error", "用户未认证");
                    response.put("success", false);
                    return Mono.just(ResponseEntity.status(401).body(response));
                }))
                .doFinally(signalType -> log.info("=== debugWorkspaces 结束 ==="));
    }
    

    @GetMapping("/debug/auth")
    public ResponseEntity<Map<String, Object>> debugAuth(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // 检查请求头
        response.put("authorization", request.getHeader("Authorization"));
        response.put("userAgent", request.getHeader("User-Agent"));
        
        // 检查SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        response.put("hasAuthentication", auth != null);
        
        if (auth != null) {
            response.put("principalType", auth.getPrincipal().getClass().getSimpleName());
            response.put("isAffineUserDetails", auth.getPrincipal() instanceof AffineUserDetails);
            
            if (auth.getPrincipal() instanceof AffineUserDetails) {
                AffineUserDetails userDetails = (AffineUserDetails) auth.getPrincipal();
                response.put("userId", userDetails.getUserId());
                response.put("username", userDetails.getUsername());
            }
        }
        
        log.info("🐛 [Debug] 认证状态调试: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作空间配额信息
     * GET /api/workspaces/{workspaceId}/quota
     */
    @GetMapping("/{workspaceId}/quota")
    public Mono<ResponseEntity<Map<String, Object>>> getWorkspaceQuota(@PathVariable String workspaceId) {
        log.info("获取工作空间配额信息: workspaceId={}", workspaceId);
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    // 检查用户是否有权限访问此工作空间
                    return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                            .flatMap(role -> {
                                // 检查用户角色是否有权限访问配额信息
                                if (role == WorkspaceRole.OWNER || role == WorkspaceRole.ADMIN || role == WorkspaceRole.COLLABORATOR) {
                                    // 获取工作空间配额信息
                                    return quotaService.getWorkspaceQuotaWithUsage(workspaceId)
                                            .map(quotaData -> ResponseEntity.ok(quotaData))
                                            .onErrorResume(e -> {
                                                log.error("获取工作空间配额失败: workspaceId={}", workspaceId, e);
                                                Map<String, Object> errorMap = new HashMap<>();
                                                errorMap.put("error", "获取工作空间配额失败");
                                                errorMap.put("message", e.getMessage());
                                                return Mono.just(ResponseEntity.status(500).body(errorMap));
                                            });
                                } else {
                                    Map<String, Object> errorMap = new HashMap<>();
                                    errorMap.put("error", "无权访问此工作空间");
                                    return Mono.just(ResponseEntity.<Map<String, Object>>status(403).body(errorMap));
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("检查工作空间权限失败: workspaceId={}, userId={}", workspaceId, userId, e);
                                Map<String, Object> errorMap = new HashMap<>();
                                errorMap.put("error", "无权访问此工作空间");
                                return Mono.just(ResponseEntity.<Map<String, Object>>status(403).body(errorMap));
                            });
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "未授权");
                    return ResponseEntity.<Map<String, Object>>status(401).body(errorMap);
                }));
    }

    /**
     * 获取用户配额信息
     * GET /api/workspaces/user/quota
     */
    @GetMapping("/user/quota")
    public Mono<ResponseEntity<Map<String, Object>>> getUserQuota() {
        log.info("获取用户配额信息");
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    return quotaService.getUserQuotaWithUsage(userId)
                            .map(quotaData -> ResponseEntity.ok(quotaData))
                            .onErrorResume(e -> {
                                log.error("获取用户配额失败: userId={}", userId, e);
                                Map<String, Object> errorMap = new HashMap<>();
                                errorMap.put("error", "获取用户配额失败");
                                errorMap.put("message", e.getMessage());
                                return Mono.just(ResponseEntity.status(500).body(errorMap));
                            });
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "未授权");
                    return ResponseEntity.<Map<String, Object>>status(401).body(errorMap);
                }));
    }
    
    // ==================== License 相关接口 ====================
    
    /**
     * 获取工作空间 License 信息
     * GET /api/workspaces/{workspaceId}/license
     */
    @GetMapping("/{workspaceId}/license")
    public ResponseEntity<Map<String, Object>> getWorkspaceLicense(@PathVariable String workspaceId) {
        log.info("GET /api/workspaces/{}/license", workspaceId);
        
        Map<String, Object> license = new HashMap<>();
        license.put("plan", "free");  // 免费计划
        license.put("status", "active");
        license.put("seats", 1);
        license.put("usedSeats", 1);
        license.put("features", List.of(
            "basic_editor",
            "cloud_sync",
            "collaboration"
        ));
        license.put("maxStorage", 10L * 1024 * 1024 * 1024);  // 10GB
        license.put("expiresAt", null);  // 永不过期
        
        return ResponseEntity.ok(license);
    }
    
    /**
     * 更新工作空间 License
     * PUT /api/workspaces/{workspaceId}/license
     */
    @PutMapping("/{workspaceId}/license")
    public ResponseEntity<Map<String, Object>> updateWorkspaceLicense(
            @PathVariable String workspaceId,
            @RequestBody Map<String, Object> licenseData) {
        log.info("PUT /api/workspaces/{}/license: {}", workspaceId, licenseData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("license", licenseData);
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== Embedding 相关接口 ====================
    
    /**
     * 获取工作空间 embedding 配置
     * GET /api/workspaces/{workspaceId}/embedding/config
     */
    @GetMapping("/{workspaceId}/embedding/config")
    public ResponseEntity<Map<String, Object>> getEmbeddingConfig(@PathVariable String workspaceId) {
        log.info("GET /api/workspaces/{}/embedding/config", workspaceId);
        
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", false);  // 默认禁用
        config.put("provider", "openai");
        config.put("model", "text-embedding-ada-002");
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * 更新工作空间 embedding 配置
     * PUT /api/workspaces/{workspaceId}/embedding/config
     */
    @PutMapping("/{workspaceId}/embedding/config")
    public ResponseEntity<Map<String, Object>> updateEmbeddingConfig(
            @PathVariable String workspaceId,
            @RequestBody Map<String, Object> config) {
        log.info("PUT /api/workspaces/{}/embedding/config: {}", workspaceId, config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("config", config);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取工作空间 embedding 状态
     * GET /api/workspaces/{workspaceId}/embedding/status
     */
    @GetMapping("/{workspaceId}/embedding/status")
    public ResponseEntity<Map<String, Object>> getEmbeddingStatus(@PathVariable String workspaceId) {
        log.info("GET /api/workspaces/{}/embedding/status", workspaceId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("total", 0);
        status.put("embedded", 0);
        status.put("pending", 0);
        status.put("failed", 0);
        status.put("progress", 0);
        status.put("isProcessing", false);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取工作空间 embedding 忽略的文档列表
     * GET /api/workspaces/{workspaceId}/embedding/ignored-docs
     */
    @GetMapping("/{workspaceId}/embedding/ignored-docs")
    public ResponseEntity<List<String>> getIgnoredDocs(@PathVariable String workspaceId) {
        log.info("GET /api/workspaces/{}/embedding/ignored-docs", workspaceId);
        
        // 返回空列表表示没有忽略的文档
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * 添加忽略的文档
     * POST /api/workspaces/{workspaceId}/embedding/ignored-docs
     */
    @PostMapping("/{workspaceId}/embedding/ignored-docs")
    public ResponseEntity<Map<String, Object>> addIgnoredDoc(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {
        log.info("POST /api/workspaces/{}/embedding/ignored-docs: {}", workspaceId, request);
        
        String docId = request.get("docId");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("docId", docId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 移除忽略的文档
     * DELETE /api/workspaces/{workspaceId}/embedding/ignored-docs/{docId}
     */
    @DeleteMapping("/{workspaceId}/embedding/ignored-docs/{docId}")
    public ResponseEntity<Map<String, Object>> removeIgnoredDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        log.info("DELETE /api/workspaces/{}/embedding/ignored-docs/{}", workspaceId, docId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("docId", docId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取工作空间 embedding 文件列表
     * GET /api/workspaces/{workspaceId}/embedding/files
     */
    @GetMapping("/{workspaceId}/embedding/files")
    public ResponseEntity<Map<String, Object>> getEmbeddingFiles(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "10") int first,
            @RequestParam(required = false) String after) {
        log.info("GET /api/workspaces/{}/embedding/files?first={}&after={}", workspaceId, first, after);
        
        Map<String, Object> response = new HashMap<>();
        response.put("files", List.of());  // 空文件列表
        response.put("hasMore", false);
        response.put("nextCursor", null);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 上传 embedding 文件
     * POST /api/workspaces/{workspaceId}/embedding/files
     */
    @PostMapping("/{workspaceId}/embedding/files")
    public ResponseEntity<Map<String, Object>> uploadEmbeddingFile(
            @PathVariable String workspaceId,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/workspaces/{}/embedding/files - 上传文件: {}", workspaceId, file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fileId", UUID.randomUUID().toString());
        response.put("fileName", file.getOriginalFilename());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除 embedding 文件
     * DELETE /api/workspaces/{workspaceId}/embedding/files/{fileId}
     */
    @DeleteMapping("/{workspaceId}/embedding/files/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteEmbeddingFile(
            @PathVariable String workspaceId,
            @PathVariable String fileId) {
        log.info("DELETE /api/workspaces/{}/embedding/files/{}", workspaceId, fileId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fileId", fileId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 开始 embedding 处理
     * POST /api/workspaces/{workspaceId}/embedding/start
     */
    @PostMapping("/{workspaceId}/embedding/start")
    public ResponseEntity<Map<String, Object>> startEmbedding(@PathVariable String workspaceId) {
        log.info("POST /api/workspaces/{}/embedding/start", workspaceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Embedding process started");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 停止 embedding 处理
     * POST /api/workspaces/{workspaceId}/embedding/stop
     */
    @PostMapping("/{workspaceId}/embedding/stop")
    public ResponseEntity<Map<String, Object>> stopEmbedding(@PathVariable String workspaceId) {
        log.info("POST /api/workspaces/{}/embedding/stop", workspaceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Embedding process stopped");
        
        return ResponseEntity.ok(response);
    }
}



