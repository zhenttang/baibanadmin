package com.yunke.backend.security.service.impl;

import com.yunke.backend.document.dto.DocPermissionsDto;
import com.yunke.backend.document.enums.DocPermission;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.domain.entity.WorkspaceDocUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspacePagePermission;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.workspace.repository.WorkspaceDocUserRoleRepository;
import com.yunke.backend.workspace.repository.WorkspacePagePermissionRepository;
import com.yunke.backend.workspace.repository.WorkspaceUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 统一权限服务实现
 * 提供文档、工作空间、页面的统一权限检查和管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final WorkspaceUserRoleRepository workspaceUserRoleRepository;
    private final WorkspaceDocUserRoleRepository workspaceDocUserRoleRepository;
    private final WorkspaceDocRepository workspaceDocRepository;
    private final WorkspaceService workspaceService;
    private final WorkspacePagePermissionRepository pagePermissionRepository;

    /**
     * 将工作空间角色转换为权限位掩码
     */
    private static int roleToMask(WorkspaceUserRole.WorkspaceRole role) {
        if (role == null) {
            return 0;
        }
        switch (role) {
            case OWNER:
            case ADMIN:
                return DocPermission.Read.bit | DocPermission.Comment.bit | DocPermission.Add.bit |
                        DocPermission.Modify.bit | DocPermission.Delete.bit | DocPermission.Export.bit |
                        DocPermission.Share.bit | DocPermission.Invite.bit | DocPermission.Manage.bit;
            case COLLABORATOR:
                return DocPermission.Read.bit | DocPermission.Comment.bit | DocPermission.Add.bit |
                        DocPermission.Modify.bit | DocPermission.Export.bit;
            case EXTERNAL:
                return DocPermission.Read.bit;
            default:
                return 0;
        }
    }

    /**
     * 将权限动作字符串转换为 DocPermission 枚举
     */
    private static DocPermission actionToPermission(String action) {
        if (action == null || action.isBlank()) {
            return DocPermission.Read;
        }
        String lowerAction = action.toLowerCase().trim();
        return switch (lowerAction) {
            case "read", "view" -> DocPermission.Read;
            case "comment" -> DocPermission.Comment;
            case "add", "create", "write" -> DocPermission.Add;
            case "modify", "edit", "update" -> DocPermission.Modify;
            case "delete", "remove" -> DocPermission.Delete;
            case "export" -> DocPermission.Export;
            case "share" -> DocPermission.Share;
            case "invite" -> DocPermission.Invite;
            case "manage", "admin" -> DocPermission.Manage;
            default -> DocPermission.Read;
        };
    }

    @Override
    public int resolveEffectiveDocMask(String workspaceId, String docId, String userId) {
        int mask = 0;

        // 1) Public link (anonymous) baseline
        Optional<WorkspaceDoc> docOpt = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
        if (docOpt.isPresent()) {
            WorkspaceDoc doc = docOpt.get();
            if (Boolean.TRUE.equals(doc.getPublic())) {
                mask = DocPermission.Read.bit;
                if ("append-only".equalsIgnoreCase(doc.getPublicPermission())) {
                    mask |= DocPermission.Add.bit;
                }
            }
            // default permission mask for members (optional)
            if (doc.getDefaultPermissionMask() != null) {
                // we do not merge here—member grants and workspace roles will override below
            }
        }

        if (userId == null || userId.isBlank()) {
            return mask; // anonymous users only get public permissions
        }

        // 2) Workspace role baseline
        WorkspaceUserRole.WorkspaceRole role = workspaceUserRoleRepository
                .getUserWorkspaceRole(workspaceId, userId)
                .orElse(null);
        if (role != null) {
            mask = roleToMask(role);
        }

        // 3) Per-doc override (grant)
        Optional<WorkspaceDocUserRole> grantOpt = workspaceDocUserRoleRepository
                .findByWorkspaceIdAndDocIdAndUserId(workspaceId, docId, userId);
        if (grantOpt.isPresent()) {
            WorkspaceDocUserRole grant = grantOpt.get();
            if (grant.getPermissionMask() != null) {
                mask = grant.getPermissionMask();
            }
        }

        return mask;
    }

    @Override
    public Mono<Boolean> checkDocPermission(String workspaceId, String docId, String userId, String action) {
        return Mono.fromCallable(() -> {
            try {
                // 获取有效权限掩码
                int mask = resolveEffectiveDocMask(workspaceId, docId, userId);
                
                // 将动作转换为权限枚举
                DocPermission requiredPermission = actionToPermission(action);
                
                // 检查是否有权限
                boolean hasPermission = DocPermission.has(mask, requiredPermission);
                
                log.debug("权限检查 - workspaceId: {}, docId: {}, userId: {}, action: {}, hasPermission: {}", 
                        workspaceId, docId, userId, action, hasPermission);
                
                return hasPermission;
            } catch (Exception e) {
                log.error("权限检查失败 - workspaceId: {}, docId: {}, userId: {}, action: {}", 
                        workspaceId, docId, userId, action, e);
                return false;
            }
        });
    }

    @Override
    public Mono<Boolean> checkWorkspacePermission(String workspaceId, String userId, String action) {
        return Mono.fromCallable(() -> {
            try {
                // 检查用户是否有工作空间访问权限
                if (!hasWorkspaceAccess(userId, workspaceId)) {
                    log.debug("用户无工作空间访问权限 - workspaceId: {}, userId: {}", workspaceId, userId);
                    return false;
                }
                
                // 获取用户在工作空间的角色
                Optional<WorkspaceUserRole.WorkspaceRole> roleOpt = workspaceUserRoleRepository
                        .getUserWorkspaceRole(workspaceId, userId);
                
                if (roleOpt.isEmpty()) {
                    log.debug("用户无工作空间角色 - workspaceId: {}, userId: {}", workspaceId, userId);
                    return false;
                }
                
                WorkspaceUserRole.WorkspaceRole role = roleOpt.get();
                
                // 根据动作和角色判断权限
                String lowerAction = action != null ? action.toLowerCase().trim() : "read";
                boolean hasPermission = switch (lowerAction) {
                    case "read", "view" -> true; // 所有有访问权限的用户都可以读取
                    case "write", "edit", "modify", "create" -> 
                        role == WorkspaceUserRole.WorkspaceRole.OWNER || 
                        role == WorkspaceUserRole.WorkspaceRole.ADMIN || 
                        role == WorkspaceUserRole.WorkspaceRole.COLLABORATOR;
                    case "delete", "remove" -> 
                        role == WorkspaceUserRole.WorkspaceRole.OWNER || 
                        role == WorkspaceUserRole.WorkspaceRole.ADMIN;
                    case "manage", "admin", "invite", "share" -> 
                        role == WorkspaceUserRole.WorkspaceRole.OWNER || 
                        role == WorkspaceUserRole.WorkspaceRole.ADMIN;
                    default -> true; // 默认允许读取
                };
                
                log.debug("工作空间权限检查 - workspaceId: {}, userId: {}, action: {}, role: {}, hasPermission: {}", 
                        workspaceId, userId, action, role, hasPermission);
                
                return hasPermission;
            } catch (Exception e) {
                log.error("工作空间权限检查失败 - workspaceId: {}, userId: {}, action: {}", 
                        workspaceId, userId, action, e);
                return false;
            }
        });
    }

    @Override
    public Mono<Boolean> hasDocPermission(String workspaceId, String docId, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "read");
    }

    @Override
    public Mono<Boolean> hasWorkspacePermission(String workspaceId, String userId) {
        return checkWorkspacePermission(workspaceId, userId, "read");
    }

    @Override
    @Cacheable(value = "permissions", key = "'workspace:' + #userId + ':' + #workspaceId")
    public boolean hasWorkspaceAccess(String userId, String workspaceId) {
        log.info("🔐 [WORKSPACE-ACCESS] 检查工作空间访问权限: userId='{}', workspaceId='{}'", userId, workspaceId);
        boolean hasAccess = workspaceService.hasAccess(workspaceId, userId);
        if (hasAccess) {
            log.info("🔐 [WORKSPACE-ACCESS] ✅ 用户有工作空间访问权限: userId='{}', workspaceId='{}'", userId, workspaceId);
        } else {
            log.warn("🔐 [WORKSPACE-ACCESS] ❌ 用户无工作空间访问权限: userId='{}', workspaceId='{}'", userId, workspaceId);
        }
        return hasAccess;
    }

    @Override
    @Cacheable(value = "permissions", key = "'workspace_manage:' + #userId + ':' + #workspaceId")
    public boolean hasWorkspaceManagePermission(String userId, String workspaceId) {
        return workspaceService.isOwner(workspaceId, userId) || workspaceService.isMember(workspaceId, userId);
    }

    @Override
    @Cacheable(value = "permissions", key = "'page:' + #userId + ':' + #pageId")
    public boolean hasPageAccess(String userId, String pageId) {
        return getUserPagePermission(pageId, userId).isPresent();
    }

    @Override
    @Cacheable(value = "permissions", key = "'page_edit:' + #userId + ':' + #pageId")
    public boolean hasPageEditPermission(String userId, String pageId) {
        return getUserPagePermission(pageId, userId)
                .map(p -> p.getPermission() == WorkspacePagePermission.PagePermission.EDITOR
                        || p.getPermission() == WorkspacePagePermission.PagePermission.OWNER)
                .orElse(false);
    }

    @Override
    @Transactional
    public WorkspacePagePermission setPagePermission(String pageId, String userId, String permission) {
        WorkspacePagePermission.PagePermission pagePermissionEnum = WorkspacePagePermission.PagePermission.valueOf(permission.toUpperCase());
        WorkspacePagePermission pagePermission = pagePermissionRepository.findByPageIdAndUserId(pageId, userId)
                .orElseGet(() -> WorkspacePagePermission.builder()
                        .id(UUID.randomUUID().toString())
                        .pageId(pageId)
                        .userId(userId)
                        .createdAt(Instant.now())
                        .build());
        pagePermission.setPermission(pagePermissionEnum);
        pagePermission.setUpdatedAt(Instant.now());
        return pagePermissionRepository.save(pagePermission);
    }

    @Override
    @Transactional
    public void removePagePermission(String pageId, String userId) {
        pagePermissionRepository.findByPageIdAndUserId(pageId, userId)
                .ifPresent(pagePermissionRepository::delete);
    }

    @Override
    @Cacheable(value = "permissions", key = "'page_permissions:' + #pageId")
    public List<WorkspacePagePermission> getPagePermissions(String pageId) {
        return pagePermissionRepository.findByPageId(pageId);
    }

    @Override
    @Cacheable(value = "permissions", key = "'user_page_permission:' + #pageId + ':' + #userId")
    public Optional<WorkspacePagePermission> getUserPagePermission(String pageId, String userId) {
        return pagePermissionRepository.findByPageIdAndUserId(pageId, userId);
    }

    @Override
    public Mono<DocPermissionsDto> getDocPermissions(String workspaceId, String docId, String userId) {
        return Mono.fromCallable(() -> {
            int mask = resolveEffectiveDocMask(workspaceId, docId, userId);
            
            return DocPermissionsDto.builder()
                    .docRead(DocPermission.has(mask, DocPermission.Read))
                    .docCopy(DocPermission.has(mask, DocPermission.Read)) // Copy requires read
                    .docDuplicate(DocPermission.has(mask, DocPermission.Add))
                    .docTrash(DocPermission.has(mask, DocPermission.Delete))
                    .docRestore(DocPermission.has(mask, DocPermission.Manage))
                    .docDelete(DocPermission.has(mask, DocPermission.Delete))
                    .docUpdate(DocPermission.has(mask, DocPermission.Modify))
                    .docPublish(DocPermission.has(mask, DocPermission.Share))
                    .docTransferOwner(DocPermission.has(mask, DocPermission.Manage))
                    .docPropertiesRead(DocPermission.has(mask, DocPermission.Read))
                    .docPropertiesUpdate(DocPermission.has(mask, DocPermission.Modify))
                    .docUsersRead(DocPermission.has(mask, DocPermission.Read))
                    .docUsersManage(DocPermission.has(mask, DocPermission.Manage))
                    .build();
        });
    }
}

