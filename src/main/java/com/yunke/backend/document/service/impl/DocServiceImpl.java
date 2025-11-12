package com.yunke.backend.document.service.impl;

import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.common.dto.PaginationInput;
import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.document.domain.entity.DocMode;
import com.yunke.backend.document.domain.entity.DocRole;
import com.yunke.backend.document.dto.*;
import com.yunke.backend.document.service.DocCopyService;
import com.yunke.backend.document.service.DocImportExportService;
import com.yunke.backend.document.service.DocVersionService;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.security.util.PermissionUtils;
import com.yunke.backend.security.constants.PermissionActions;
import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.system.domain.entity.*;
import com.yunke.backend.system.repository.*;
import com.yunke.backend.document.service.DocService;

import com.yunke.backend.system.service.SearchService;

import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.domain.entity.WorkspaceDocUserRole;
import com.yunke.backend.workspace.dto.WorkspaceExportDto;
import com.yunke.backend.workspace.dto.WorkspaceExportInput;
import com.yunke.backend.workspace.dto.WorkspaceImportResultDto;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.workspace.repository.WorkspaceDocUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    private final WorkspaceDocRepository workspaceDocRepository;
    private final SnapshotRepository snapshotRepository;
    private final UpdateRepository updateRepository;
    private final WorkspaceDocUserRoleRepository docUserRoleRepository;
    private final PermissionService permissionService;
    private final SearchService searchService;
    private final DocVersionService docVersionService;
    private final DocCopyService docCopyService;
    private final DocImportExportService docImportExportService;
    private final DocBinaryStorageService binaryStorageService;

    @Override
    @Transactional
    public Mono<DocDto> createDoc(String workspaceId, CreateDocInput input, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.DOC_CREATE,
                () -> {
                    // Create workspace doc metadata
                    String resolvedDocId = input.getDocId() != null && !input.getDocId().isBlank()
                            ? input.getDocId()
                            : UUID.randomUUID().toString();

                    WorkspaceDoc workspaceDoc = WorkspaceDoc.builder()
                            .workspaceId(workspaceId)
                            .docId(resolvedDocId)
                            .title(input.getTitle())
                            .summary(input.getSummary())
                            .mode(input.getMode() != null ? input.getMode().ordinal() : 0)
                            .public_(false)
                            .blocked(false)
                            .defaultRole(30) // Manager
                            .build();
                    
                    return Mono.fromCallable(() -> workspaceDocRepository.save(workspaceDoc))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(savedDoc -> {
                                // ✅ 确保元数据已保存成功
                                // 验证元数据是否存在（双重检查）
                                Optional<WorkspaceDoc> verifyDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, resolvedDocId);
                                if (verifyDoc.isEmpty()) {
                                    log.error("❌ [DOC-CREATE] 元数据保存失败，但未抛出异常: docId={}", resolvedDocId);
                                    return Mono.error(new RuntimeException("Failed to create document metadata"));
                                }
                                
                                // Create initial snapshot if content provided
                                if (input.getInitialContent() != null) {
                                    // ✅ 使用二进制存储服务保存快照（与 WorkspaceDocServiceImpl 保持一致）
                                    String pointer = binaryStorageService.saveSnapshot(workspaceId, resolvedDocId, input.getInitialContent());
                                    
                                    Snapshot snapshot = Snapshot.builder()
                                            .workspaceId(workspaceId)
                                            .id(resolvedDocId)
                                            .blob(binaryStorageService.pointerToBytes(pointer))
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .createdBy(userId)
                                            .updatedBy(userId)
                                            .seq(0)
                                            .build();
                                    
                                    return Mono.fromCallable(() -> snapshotRepository.save(snapshot))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .map(snap -> savedDoc);
                                } else {
                                    return Mono.just(savedDoc);
                                }
                            })
                            .map(this::convertToDto);
                });
    }

    @Override
    public Mono<DocDto> getDoc(String workspaceId, String docId, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_READ,
                () -> Mono.fromCallable(() -> workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(docOpt -> docOpt
                                .map(this::convertToDto)
                                .map(Mono::just)
                                .orElse(Mono.error(new ResourceNotFoundException("Document", docId)))));
    }

    @Override
    @Transactional
    public Mono<DocDto> updateDoc(String workspaceId, String docId, UpdateDocInput input, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_UPDATE,
                () -> Mono.fromCallable(() -> workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(docOpt -> {
                            if (docOpt.isEmpty()) {
                                return Mono.error(new ResourceNotFoundException("Document", docId));
                            }
                            
                            WorkspaceDoc doc = docOpt.get();
                            if (input.getTitle() != null) doc.setTitle(input.getTitle());
                            if (input.getSummary() != null) doc.setSummary(input.getSummary());
                            if (input.getMode() != null) doc.setMode(input.getMode().ordinal());
                            if (input.getBlocked() != null) doc.setBlocked(input.getBlocked());
                            
                            return Mono.fromCallable(() -> workspaceDocRepository.save(doc))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .map(this::convertToDto);
                        }));
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteDoc(String workspaceId, String docId, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_DELETE,
                () -> Mono.fromCallable(() -> {
                    // Delete doc metadata
                    workspaceDocRepository.deleteByWorkspaceIdAndDocId(workspaceId, docId);
                    
                    // Delete snapshots
                    snapshotRepository.deleteByWorkspaceIdAndId(workspaceId, docId);
                    
                    // Delete updates
                    updateRepository.deleteByWorkspaceIdAndId(workspaceId, docId);
                    
                    // Delete doc user roles
                    docUserRoleRepository.deleteByWorkspaceIdAndDocId(workspaceId, docId);
                    
                    // Remove from search index
                    searchService.deleteDoc(workspaceId, docId);
                    
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<DocDto> restoreDoc(String workspaceId, String docId, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_RESTORE,
                () -> Mono.error(new RuntimeException("Restore functionality not implemented")));
    }

    @Override
    public Mono<DocPermissionsDto> getDocPermissions(String workspaceId, String docId, String userId) {
        return permissionService.getDocPermissions(workspaceId, docId, userId);
    }

    @Override
    @Transactional
    public Mono<Boolean> updateDocDefaultRole(String workspaceId, String docId, DocRole role, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_UPDATE,
                () -> Mono.fromCallable(() -> {
                    WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
                    
                    doc.setDefaultRole(Integer.valueOf((short)role.getValue()));
                    workspaceDocRepository.save(doc);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<DocDto> publishDoc(String workspaceId, String docId, PublicDocMode mode, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_PUBLISH,
                () -> Mono.fromCallable(() -> {
                    WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
                    
                    doc.setIsPublic(true);
                    WorkspaceDoc savedDoc = workspaceDocRepository.save(doc);
                    return convertToDto(savedDoc);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<DocDto> revokePublicDoc(String workspaceId, String docId, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_PUBLISH,
                () -> Mono.fromCallable(() -> {
                    WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
                    
                    doc.setIsPublic(false);
                    WorkspaceDoc savedDoc = workspaceDocRepository.save(doc);
                    return convertToDto(savedDoc);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<Boolean> grantDocUserRoles(String workspaceId, String docId, List<String> userIds, DocRole role, String operatorId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, operatorId, 
                PermissionActions.DOC_USERS_MANAGE,
                () -> Mono.fromCallable(() -> {
                    userIds.forEach(userId -> {
                        WorkspaceDocUserRole docUserRole = WorkspaceDocUserRole.builder()
                                .workspaceId(workspaceId)
                                .docId(docId)
                                .userId(userId)
                                .type((short)role.getValue())
                                .createdAt(LocalDateTime.now())
                                .build();
                        
                        docUserRoleRepository.save(docUserRole);
                    });
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<Boolean> revokeDocUserRole(String workspaceId, String docId, String userId, String operatorId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, operatorId, 
                PermissionActions.DOC_USERS_MANAGE,
                () -> Mono.fromCallable(() -> {
                    docUserRoleRepository.deleteByWorkspaceIdAndDocIdAndUserId(workspaceId, docId, userId);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<Boolean> updateDocUserRole(String workspaceId, String docId, String userId, DocRole role, String operatorId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, operatorId, 
                PermissionActions.DOC_USERS_MANAGE,
                () -> Mono.fromCallable(() -> {
                    WorkspaceDocUserRole docUserRole = docUserRoleRepository.findByWorkspaceIdAndDocIdAndUserId(workspaceId, docId, userId)
                            .orElseThrow(() -> new ResourceNotFoundException("DocUserRole", userId));
                    
                    docUserRole.setType((short)role.getValue());
                    docUserRoleRepository.save(docUserRole);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<PaginatedResponse<DocUserRoleDto>> getDocUserRoles(String workspaceId, String docId, PaginationInput pagination, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_USERS_READ,
                () -> Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(
                            pagination.getOffset() / pagination.getFirst(),
                            pagination.getFirst(),
                            Sort.by("createdAt").descending()
                    );
                    
                    var page = docUserRoleRepository.findByWorkspaceIdAndDocId(workspaceId, docId, pageable);
                    
                    List<DocUserRoleDto> content = page.getContent().stream()
                            .map(this::convertDocUserRoleToDto)
                            .toList();
                    
                    return PaginatedResponse.of(
                            content,
                            page.getTotalElements(),
                            pagination.getPage(),
                            pagination.getSize()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<PaginatedResponse<DocDto>> getWorkspaceDocs(String workspaceId, PaginationInput pagination, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(
                            pagination.getOffset() / pagination.getFirst(),
                            pagination.getFirst(),
                            Sort.by("title").ascending()
                    );
                    
                    var page = workspaceDocRepository.findByWorkspaceId(workspaceId, pageable);
                    
                    List<DocDto> content = page.getContent().stream()
                            .map(this::convertToDto)
                            .toList();
                    
                    return PaginatedResponse.of(
                            content,
                            page.getTotalElements(),
                            pagination.getPage(),
                            pagination.getSize()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<PaginatedResponse<DocDto>> searchDocs(String workspaceId, String query, PaginationInput pagination, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> searchService.searchDocs(workspaceId, query, pagination, userId));
    }

    @Override
    public Mono<PaginatedResponse<DocDto>> getPublicDocs(String workspaceId, PaginationInput pagination) {
        return Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(
                    pagination.getPage(), 
                    pagination.getSize(), 
                    Sort.by(Sort.Direction.DESC, "updatedAt"));
            
            Page<WorkspaceDoc> docsPage = workspaceDocRepository.findByWorkspaceIdAndPublic_(workspaceId, true, pageable);
            
            List<DocDto> docs = docsPage.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            return PaginatedResponse.of(
                    docs,
                    docsPage.getTotalElements(),
                    pagination.getPage(),
                    pagination.getSize()
            );
        });
    }

    // 高级功能实现 - 委托给专门的服务
    @Override
    public Mono<PaginatedResponse<DocHistoryDto>> getDocHistories(String workspaceId, String docId, PaginationInput pagination, String userId) {
        return docVersionService.getDocHistories(workspaceId, docId, pagination, userId);
    }

    @Override
    public Mono<LocalDateTime> recoverDocToVersion(String workspaceId, String docId, LocalDateTime timestamp, String userId) {
        return docVersionService.recoverDocToVersion(workspaceId, docId, timestamp, userId);
    }

    @Override
    public Mono<DocDiffDto> getDocDiff(String workspaceId, String docId, byte[] stateVector, String userId) {
        return docVersionService.getDocDiff(workspaceId, docId, stateVector, userId);
    }

    @Override
    public Mono<SnapshotDto> getDocSnapshot(String workspaceId, String docId, String userId) {
        return docVersionService.getLatestSnapshot(workspaceId, docId, userId);
    }

    @Override
    public Mono<Boolean> saveDocSnapshot(String workspaceId, String docId, SaveSnapshotInput input, String userId) {
        return docVersionService.createSnapshot(workspaceId, docId, input.getBlob(), input.getState(), userId)
                .map(snapshot -> true);
    }

    @Override
    public Mono<Boolean> applyDocUpdate(String workspaceId, String docId, ApplyUpdateInput input, String userId) {
        return docVersionService.applyDocUpdate(workspaceId, docId, input.getUpdate(), userId);
    }

    @Override
    public Flux<UpdateDto> getDocUpdates(String workspaceId, String docId, LocalDateTime since, String userId) {
        return docVersionService.getDocUpdates(workspaceId, docId, since, userId);
    }

    @Override
    public Mono<DocMetaDto> getDocMeta(String workspaceId, String docId, String userId) {
        return getDoc(workspaceId, docId, userId)
                .map(doc -> DocMetaDto.builder()
                        .docId(doc.getId())
                        .workspaceId(doc.getWorkspaceId())
                        .title(doc.getTitle())
                        .summary(doc.getSummary())
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .createdBy(doc.getCreatedBy())
                        .updatedBy(doc.getUpdatedBy())
                        .build());
    }

    @Override
    public Mono<DocMetaDto> updateDocMeta(String workspaceId, String docId, UpdateDocMetaInput input, String userId) {
        UpdateDocInput updateInput = UpdateDocInput.builder()
                .title(input.getTitle())
                .summary(input.getSummary())
                .build();
        
        return updateDoc(workspaceId, docId, updateInput, userId)
                .map(doc -> DocMetaDto.builder()
                        .docId(doc.getId())
                        .workspaceId(doc.getWorkspaceId())
                        .title(doc.getTitle())
                        .summary(doc.getSummary())
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .createdBy(doc.getCreatedBy())
                        .updatedBy(doc.getUpdatedBy())
                        .build());
    }

    @Override
    public Mono<DocDto> copyDoc(String sourceWorkspaceId, String sourceDocId, String targetWorkspaceId, String userId) {
        CopyDocInput input = CopyDocInput.builder()
                .copyPermissions(false)
                .copyHistory(false)
                .preserveStructure(true)
                .build();
        
        return docCopyService.copyDocToWorkspace(sourceWorkspaceId, sourceDocId, targetWorkspaceId, input, userId);
    }

    @Override
    public Mono<DocDto> duplicateDoc(String workspaceId, String docId, String userId) {
        DuplicateDocInput input = DuplicateDocInput.builder()
                .prefix("Copy of")
                .copyPermissions(false)
                .copyHistory(false)
                .build();
        
        return docCopyService.duplicateDoc(workspaceId, docId, input, userId);
    }

    @Override
    public Mono<DocStatsDto> getWorkspaceDocStats(String workspaceId, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> Mono.fromCallable(() -> {
                    long totalDocs = workspaceDocRepository.countByWorkspaceId(workspaceId);
                    long publicDocs = workspaceDocRepository.findByWorkspaceIdAndPublic_(workspaceId, true, 
                            org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
                    long privateDocs = totalDocs - publicDocs;
                    long totalSnapshots = snapshotRepository.countByWorkspaceId(workspaceId);
                    
                    // Calculate total size (simplified)
                    List<Snapshot> snapshots = snapshotRepository.findByWorkspaceId(workspaceId);
                    long totalSize = snapshots.stream()
                            .mapToLong(this::resolveSnapshotSize)
                            .sum();
                    
                    return DocStatsDto.builder()
                            .workspaceId(workspaceId)
                            .totalDocs(totalDocs)
                            .publicDocs(publicDocs)
                            .privateDocs(privateDocs)
                            .blockedDocs(0L) // Would need additional query
                            .totalSnapshots(totalSnapshots)
                            .totalUpdates(0L) // Would need additional query
                            .totalSize(totalSize)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<PaginatedResponse<DocActivityDto>> getUserDocActivity(String userId, PaginationInput pagination) {
        // Simplified implementation - would need activity tracking in real implementation
        return Mono.just(PaginatedResponse.of(java.util.Collections.emptyList(), 0L, 0, 10));
    }

    // ==================== 高级导入导出功能实现 ====================
    
    @Override
    public Mono<DocDto> smartImportDoc(String workspaceId, ImportDocInput input, String userId) {
        return docImportExportService.smartImportDoc(workspaceId, input, userId);
    }

    @Override
    public Mono<List<DocImportResultDto>> batchImportDocs(String workspaceId, List<ImportDocInput> inputs, String userId) {
        return docImportExportService.batchImportDocs(workspaceId, inputs, userId);
    }

    @Override
    public Mono<DocExportDto> advancedExportDoc(String workspaceId, String docId, AdvancedExportInput input, String userId) {
        return docImportExportService.advancedExportDoc(workspaceId, docId, input, userId);
    }

    private long resolveSnapshotSize(Snapshot snapshot) {
        if (snapshot == null || snapshot.getBlob() == null) {
            return 0L;
        }
        return binaryStorageService.resolvePointer(snapshot.getBlob(), snapshot.getWorkspaceId(), snapshot.getId()).length;
    }

    @Override
    public Mono<List<DocExportDto>> batchExportDocs(String workspaceId, List<String> docIds, DocExportFormat format, String userId) {
        return docImportExportService.batchExportDocs(workspaceId, docIds, format, userId);
    }

    @Override
    public Mono<WorkspaceExportDto> exportWorkspace(String workspaceId, WorkspaceExportInput input, String userId) {
        return docImportExportService.exportWorkspace(workspaceId, input, userId);
    }

    @Override
    public Mono<WorkspaceImportResultDto> importWorkspaceFromZip(String workspaceId, byte[] zipContent, String userId) {
        return docImportExportService.importWorkspaceFromZip(workspaceId, zipContent, userId);
    }

    @Override
    public Mono<DocExportDto> convertDocFormat(String workspaceId, String docId, DocExportFormat sourceFormat, DocExportFormat targetFormat, String userId) {
        return docImportExportService.convertDocFormat(workspaceId, docId, sourceFormat, targetFormat, userId);
    }

    @Override
    public Mono<ImportPreviewDto> previewImport(ImportDocInput input) {
        return docImportExportService.previewImport(input);
    }

    // Helper methods
    private DocDto convertToDto(WorkspaceDoc workspaceDoc) {
        return DocDto.builder()
                .id(workspaceDoc.getDocId())
                .workspaceId(workspaceDoc.getWorkspaceId())
                .title(workspaceDoc.getTitle())
                .summary(workspaceDoc.getSummary())
                .isPublic(workspaceDoc.getIsPublic())
                .blocked(workspaceDoc.getBlocked())
                .defaultRole(DocRole.fromValue(workspaceDoc.getDefaultRole()))
                .mode(DocMode.fromValue(workspaceDoc.getMode()))
                .build();
    }

    private DocUserRoleDto convertDocUserRoleToDto(WorkspaceDocUserRole docUserRole) {
        return DocUserRoleDto.builder()
                .userId(docUserRole.getUserId())
                .role(DocRole.fromValue(docUserRole.getType()))
                .createdAt(docUserRole.getCreatedAt())
                .build();
    }

    /**
     * 转换DocRole为DocUserRoleDto
     */
    private DocUserRoleDto convertToDocUserRoleDto(String userId, DocRole role) {
        return DocUserRoleDto.builder()
                .userId(userId)
                .role(role)
                .build();
    }
}
