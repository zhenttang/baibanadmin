package com.yunke.backend.community.service.impl;

import com.yunke.backend.community.dto.CommunityDocDto;
import com.yunke.backend.community.repository.CommunityDocRepository;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;

import com.yunke.backend.community.enums.CommunityPermission;

import com.yunke.backend.user.repository.UserRepository;

import com.yunke.backend.community.service.CommunityService;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDocUserRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.repository.WorkspaceDocUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * 社区服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommunityServiceImpl implements CommunityService {
    
    private final CommunityDocRepository communityDocRepository;
    private final WorkspaceManagementService workspaceManagementService;
    private final UserService userService;
    private final WorkspaceDocUserRoleRepository workspaceDocUserRoleRepository;
    private final UserRepository userRepository;
    
    @Override
    public Mono<Boolean> shareDocToCommunity(String docId, String workspaceId, String userId,
                                           CommunityPermission permission, String title, String description) {
        log.info("分享文档到社区: docId={}, workspaceId={}, userId={}, permission={}", 
                docId, workspaceId, userId, permission);
        
        return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                .flatMap(role -> {
                    // 检查权限：外部用户不能分享
                    if (role == WorkspaceUserRole.WorkspaceRole.EXTERNAL) {
                        return Mono.error(new IllegalArgumentException("外部用户无权分享文档到社区"));
                    }
                    
                    return Mono.fromCallable(() -> {
                        Optional<WorkspaceDoc> docOpt = communityDocRepository.findById(
                            new WorkspaceDoc.WorkspaceDocId(workspaceId, docId)
                        );
                        if (docOpt.isEmpty()) {
                            throw new IllegalArgumentException("文档不存在");
                        }
                        
                        WorkspaceDoc doc = docOpt.get();
                        if (!doc.getWorkspaceId().equals(workspaceId)) {
                            throw new IllegalArgumentException("文档不属于指定工作空间");
                        }
                        
                        // 执行分享操作
                        doc.shareToCommunity(permission, title, description);
                        communityDocRepository.save(doc);
                        
                        log.info("文档分享到社区成功: docId={}", docId);
                        return true;
                    });
                });
    }
    
    @Override
    public Mono<Page<CommunityDocDto>> getCommunityDocs(String workspaceId, String userId,
                                                       int page, int size, String search) {
        log.info("获取社区文档列表: workspaceId={}, userId={}, page={}, size={}", 
                workspaceId, userId, page, size);
        
        return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                .flatMap(role -> {
                    List<CommunityPermission> visiblePermissions = getUserVisiblePermissions(role.toString());
                    
                    return Mono.fromCallable(() -> {
                        Pageable pageable = PageRequest.of(page, size);
                        Page<WorkspaceDoc> docsPage;
                        
                        if (search != null && !search.trim().isEmpty()) {
                            docsPage = communityDocRepository.searchCommunityDocs(workspaceId, search.trim(), pageable);
                        } else {
                            docsPage = communityDocRepository.findCommunityDocsByPermissions(workspaceId, visiblePermissions, pageable);
                        }
                        
                        return docsPage.map(this::convertToDto);
                    });
                });
    }
    
    @Override
    public Mono<Boolean> unshareDocFromCommunity(String docId, String workspaceId, String userId) {
        log.info("取消文档社区分享: docId={}, workspaceId={}, userId={}", docId, workspaceId, userId);
        
        return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                .flatMap(role -> {
                    // 检查权限：至少需要协作者权限
                    if (role == WorkspaceUserRole.WorkspaceRole.EXTERNAL) {
                        return Mono.error(new IllegalArgumentException("外部用户无权取消分享"));
                    }
                    
                    return Mono.fromCallable(() -> {
                        Optional<WorkspaceDoc> docOpt = communityDocRepository.findById(
                            new WorkspaceDoc.WorkspaceDocId(workspaceId, docId)
                        );
                        if (docOpt.isEmpty()) {
                            throw new IllegalArgumentException("文档不存在");
                        }
                        
                        WorkspaceDoc doc = docOpt.get();
                        doc.unshareFromCommunity();
                        communityDocRepository.save(doc);
                        
                        log.info("取消文档社区分享成功: docId={}", docId);
                        return true;
                    });
                });
    }
    
    @Override
    public Mono<Boolean> updateCommunityPermission(String docId, String workspaceId, String userId, 
                                                  CommunityPermission permission) {
        log.info("更新文档社区权限: docId={}, workspaceId={}, userId={}, permission={}", 
                docId, workspaceId, userId, permission);
        
        return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                .flatMap(role -> {
                    // 检查权限：至少需要协作者权限
                    if (role == WorkspaceUserRole.WorkspaceRole.EXTERNAL) {
                        return Mono.error(new IllegalArgumentException("外部用户无权修改社区权限"));
                    }
                    
                    return Mono.fromCallable(() -> {
                        Optional<WorkspaceDoc> docOpt = communityDocRepository.findById(
                            new WorkspaceDoc.WorkspaceDocId(workspaceId, docId)
                        );
                        if (docOpt.isEmpty()) {
                            throw new IllegalArgumentException("文档不存在");
                        }
                        
                        WorkspaceDoc doc = docOpt.get();
                        if (!Boolean.TRUE.equals(doc.getCommunityShared())) {
                            throw new IllegalArgumentException("文档未分享到社区");
                        }
                        
                        doc.updateCommunityPermission(permission);
                        communityDocRepository.save(doc);
                        
                        log.info("更新文档社区权限成功: docId={}, permission={}", docId, permission);
                        return true;
                    });
                });
    }
    
    @Override
    public Mono<Boolean> incrementViewCount(String docId, String workspaceId) {
        log.debug("增加文档浏览次数: docId={}, workspaceId={}", docId, workspaceId);
        
        return Mono.fromCallable(() -> {
            Optional<WorkspaceDoc> docOpt = communityDocRepository.findById(
                new WorkspaceDoc.WorkspaceDocId(workspaceId, docId)
            );
            if (docOpt.isEmpty()) {
                log.warn("文档不存在，无法增加浏览次数: docId={}", docId);
                return false;
            }
            
            WorkspaceDoc doc = docOpt.get();
            if (!Boolean.TRUE.equals(doc.getCommunityShared())) {
                log.warn("文档未分享到社区，无法增加浏览次数: docId={}", docId);
                return false;
            }
            
            doc.incrementViewCount();
            communityDocRepository.save(doc);
            
            log.debug("增加文档浏览次数成功: docId={}, 当前浏览次数={}", docId, doc.getCommunityViewCount());
            return true;
        });
    }
    
    @Override
    public Mono<Boolean> canUserViewCommunityDoc(String docId, String workspaceId, String userId) {
        log.debug("检查用户查看社区文档权限: docId={}, workspaceId={}, userId={}", docId, workspaceId, userId);
        
        return workspaceManagementService.getUserWorkspaceRole(workspaceId, userId)
                .flatMap(role -> {
                    return Mono.fromCallable(() -> {
                        Optional<WorkspaceDoc> docOpt = communityDocRepository.findCommunityDoc(docId, workspaceId);
                        if (docOpt.isEmpty()) {
                            return false;
                        }
                        
                        WorkspaceDoc doc = docOpt.get();
                        CommunityPermission permission = doc.getCommunityPermission();
                        
                        List<CommunityPermission> visiblePermissions = getUserVisiblePermissions(role.toString());
                        boolean canView = visiblePermissions.contains(permission);
                        
                        log.debug("用户查看权限检查结果: userId={}, docId={}, userRole={}, docPermission={}, canView={}", 
                                userId, docId, role, permission, canView);
                        
                        return canView;
                    });
                });
    }
    
    @Override
    public List<CommunityPermission> getUserVisiblePermissions(String workspaceRole) {
        return switch (workspaceRole) {
            case "OWNER", "ADMIN" -> List.of(
                CommunityPermission.PUBLIC, 
                CommunityPermission.COLLABORATOR, 
                CommunityPermission.ADMIN
            );
            case "COLLABORATOR" -> List.of(
                CommunityPermission.PUBLIC, 
                CommunityPermission.COLLABORATOR
            );
            case "EXTERNAL" -> List.of(CommunityPermission.PUBLIC);
            default -> List.of();
        };
    }
    
    /**
     * 转换WorkspaceDoc为CommunityDocDto
     */
    private CommunityDocDto convertToDto(WorkspaceDoc doc) {
        String authorId = null;
        String authorName = "未知用户";

        try {
            List<WorkspaceDocUserRole> owners = workspaceDocUserRoleRepository
                .findByWorkspaceIdAndDocIdAndType(doc.getWorkspaceId(), doc.getDocId(), 10);

            if (!owners.isEmpty()) {
                WorkspaceDocUserRole owner = owners.get(0);
                authorId = owner.getUserId();

                User author = userRepository.findById(authorId).orElse(null);
                if (author != null) {
                    authorName = author.getName();
                }
            }
        } catch (Exception e) {
            log.warn("获取文档作者信息失败: docId={}, error={}", doc.getDocId(), e.getMessage());
        }

        return new CommunityDocDto(
            doc.getDocId(),
            doc.getCommunityTitle(),
            doc.getCommunityDescription(),
            authorId,
            authorName,
            doc.getCommunitySharedAt(),
            doc.getCommunityViewCount(),
            doc.getCommunityPermission(),
            doc.getWorkspaceId()
        );
    }
}