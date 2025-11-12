package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.dto.CopyDocInput;
import com.yunke.backend.document.dto.DocDto;
import com.yunke.backend.document.dto.DuplicateDocInput;
import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.document.service.DocCopyService;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;

import com.yunke.backend.workspace.repository.WorkspaceDocUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 文档复制服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocCopyServiceImpl implements DocCopyService {

    private final WorkspaceDocRepository workspaceDocRepository;
    private final WorkspaceDocUserRoleRepository docUserRoleRepository;
    private final PermissionService permissionService;

    @Override
    public Mono<DocDto> duplicateDoc(String workspaceId, String docId, String userId) {
        return permissionService.hasDocPermission(workspaceId, docId, userId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to duplicate doc")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // Get source document
                        WorkspaceDoc sourceDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                                .orElseThrow(() -> new RuntimeException("Source document not found"));
                        
                        // Generate new title
                        String newTitle = "Copy of " + sourceDoc.getTitle();
                        
                        // Create duplicate document
                        String newDocId = UUID.randomUUID().toString();
                        WorkspaceDoc duplicateDoc = WorkspaceDoc.builder()
                                .workspaceId(workspaceId)
                                .docId(newDocId)
                                .title(newTitle)
                                .summary(sourceDoc.getSummary())
                                .mode(sourceDoc.getMode())
                                .defaultRole(sourceDoc.getDefaultRole())
                                .build();
                        
                        WorkspaceDoc savedDoc = workspaceDocRepository.save(duplicateDoc);
                        
                        // Copy latest snapshot
                        copyLatestSnapshot(workspaceId, docId, newDocId, userId);
                        
                        // Copy permissions if requested
                        copyDocPermissions(workspaceId, docId, newDocId);
                        
                        log.info("Document duplicated: {}/{} -> {}", workspaceId, docId, newDocId);
                        
                        return convertToDto(savedDoc);
                    })
                );
    }

    @Override
    public Mono<DocDto> duplicateDoc(String workspaceId, String docId, DuplicateDocInput input, String userId) {
        return permissionService.hasDocPermission(workspaceId, docId, userId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to duplicate doc")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // Get source document
                        WorkspaceDoc sourceDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                                .orElseThrow(() -> new RuntimeException("Source document not found"));
                        
                        // Generate new title with prefix if provided
                        String prefix = input.getPrefix() != null ? input.getPrefix() + " " : "Copy of ";
                        String newTitle = prefix + sourceDoc.getTitle();
                        
                        // Create duplicate document
                        String newDocId = UUID.randomUUID().toString();
                        WorkspaceDoc duplicateDoc = WorkspaceDoc.builder()
                                .workspaceId(workspaceId)
                                .docId(newDocId)
                                .title(newTitle)
                                .summary(sourceDoc.getSummary())
                                .mode(sourceDoc.getMode())
                                .defaultRole(sourceDoc.getDefaultRole())
                                .build();
                        
                        WorkspaceDoc savedDoc = workspaceDocRepository.save(duplicateDoc);
                        
                        // Copy latest snapshot
                        copyLatestSnapshot(workspaceId, docId, newDocId, userId);
                        
                        // Copy permissions if requested
                        if (input.getCopyPermissions()) {
                            copyDocPermissions(workspaceId, docId, workspaceId, newDocId);
                        }
                        
                        // Copy history if requested
                        if (input.getCopyHistory()) {
                            copyDocHistory(workspaceId, docId, workspaceId, newDocId, userId);
                        }
                        
                        log.info("Document duplicated with options: {}/{} -> {}", workspaceId, docId, newDocId);
                        
                        return convertToDto(savedDoc);
                    })
                );
    }

    @Override
    public Mono<DocDto> copyDoc(String sourceWorkspaceId, String sourceDocId, String targetWorkspaceId, String userId) {
        return permissionService.hasDocPermission(sourceWorkspaceId, sourceDocId, userId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to copy source doc")))
                .flatMap(hasSourcePermission -> 
                    permissionService.hasWorkspacePermission(targetWorkspaceId, userId)
                        .filter(Boolean::booleanValue)
                        .switchIfEmpty(Mono.error(new RuntimeException("No permission to copy to target workspace")))
                        .flatMap(hasTargetPermission ->
                            Mono.fromCallable(() -> {
                                // Get source document
                                WorkspaceDoc sourceDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(sourceWorkspaceId, sourceDocId)
                                        .orElseThrow(() -> new RuntimeException("Source document not found"));
                                
                                // Generate new title (same as source)
                                String newTitle = sourceDoc.getTitle();
                                
                                // Create new document in target workspace
                                String newDocId = UUID.randomUUID().toString();
                                WorkspaceDoc newDoc = WorkspaceDoc.builder()
                                        .workspaceId(targetWorkspaceId)
                                        .docId(newDocId)
                                        .title(newTitle)
                                        .summary(sourceDoc.getSummary())
                                        .mode(sourceDoc.getMode())
                                        .defaultRole(sourceDoc.getDefaultRole())
                                        .build();
                                
                                WorkspaceDoc savedDoc = workspaceDocRepository.save(newDoc);
                                
                                // Copy latest snapshot
                                copyLatestSnapshot(sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId, userId);
                                
                                log.info("Document copied: {}/{} -> {}/{}", 
                                        sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId);
                                
                                return convertToDto(savedDoc);
                            })
                        )
                );
    }

    @Override
    public Mono<DocDto> copyDocToWorkspace(String sourceWorkspaceId, String sourceDocId, String targetWorkspaceId,
                                           CopyDocInput input, String userId) {
        return permissionService.hasDocPermission(sourceWorkspaceId, sourceDocId, userId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to copy source doc")))
                .flatMap(hasSourcePermission -> 
                    permissionService.hasWorkspacePermission(targetWorkspaceId, userId)
                        .filter(Boolean::booleanValue)
                        .switchIfEmpty(Mono.error(new RuntimeException("No permission to copy to target workspace")))
                        .flatMap(hasTargetPermission ->
                            Mono.fromCallable(() -> {
                                // Get source document
                                WorkspaceDoc sourceDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(sourceWorkspaceId, sourceDocId)
                                        .orElseThrow(() -> new RuntimeException("Source document not found"));
                                
                                // Generate new title (same as source)
                                String newTitle = sourceDoc.getTitle();
                                
                                // Create new document in target workspace
                                String newDocId = UUID.randomUUID().toString();
                                WorkspaceDoc newDoc = WorkspaceDoc.builder()
                                        .workspaceId(targetWorkspaceId)
                                        .docId(newDocId)
                                        .title(newTitle)
                                        .summary(sourceDoc.getSummary())
                                        .mode(sourceDoc.getMode())
                                        .defaultRole(sourceDoc.getDefaultRole())
                                        .build();
                                
                                WorkspaceDoc savedDoc = workspaceDocRepository.save(newDoc);
                                
                                // Copy latest snapshot
                                copyLatestSnapshot(sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId, userId);
                                
                                // Copy permissions if requested
                                if (input.getCopyPermissions()) {
                                    copyDocPermissions(sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId);
                                }
                                
                                // Copy history if requested
                                if (input.getCopyHistory()) {
                                    copyDocHistory(sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId, userId);
                                }
                                
                                // Preserve structure if requested
                                if (input.getPreserveStructure()) {
                                    preserveDocStructure(sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId, userId);
                                }
                                
                                log.info("Document copied with options: {}/{} -> {}/{}", 
                                        sourceWorkspaceId, sourceDocId, targetWorkspaceId, newDocId);
                                
                                return convertToDto(savedDoc);
                            })
                        )
                );
    }

    // Helper methods
    private void copyLatestSnapshot(String sourceWorkspaceId, String sourceDocId, String targetDocId, String userId) {
        copyLatestSnapshot(sourceWorkspaceId, sourceDocId, sourceWorkspaceId, targetDocId, userId);
    }
    
    private void copyLatestSnapshot(String sourceWorkspaceId, String sourceDocId, 
                                   String targetWorkspaceId, String targetDocId, String userId) {
        // Implementation would depend on how snapshots are stored
        log.info("Copying latest snapshot from {}/{} to {}/{}", 
                sourceWorkspaceId, sourceDocId, targetWorkspaceId, targetDocId);
        // Actual implementation would retrieve and copy the snapshot
    }
    
    private void copyDocPermissions(String workspaceId, String sourceDocId, String targetDocId) {
        copyDocPermissions(workspaceId, sourceDocId, workspaceId, targetDocId);
    }

    private void copyDocPermissions(String sourceWorkspaceId, String sourceDocId, 
                                   String targetWorkspaceId, String targetDocId) {
        // Implementation would depend on permission model
        log.info("Copying permissions from {}/{} to {}/{}", 
                sourceWorkspaceId, sourceDocId, targetWorkspaceId, targetDocId);
        // Actual implementation would copy permissions
    }
    
    private void copyDocHistory(String workspaceId, String sourceDocId, String targetDocId, String userId) {
        copyDocHistory(workspaceId, sourceDocId, workspaceId, targetDocId, userId);
    }

    private void copyDocHistory(String sourceWorkspaceId, String sourceDocId, 
                               String targetWorkspaceId, String targetDocId, String userId) {
        // Implementation would depend on history model
        log.info("Copying history from {}/{} to {}/{}", 
                sourceWorkspaceId, sourceDocId, targetWorkspaceId, targetDocId);
        // Actual implementation would copy history records
    }
    
    private void preserveDocStructure(String sourceWorkspaceId, String sourceDocId, 
                                     String targetWorkspaceId, String targetDocId, String userId) {
        // Implementation would depend on document structure model
        log.info("Preserving structure from {}/{} to {}/{}", 
                sourceWorkspaceId, sourceDocId, targetWorkspaceId, targetDocId);
        // Actual implementation would preserve structure
    }
    
    private DocDto convertToDto(WorkspaceDoc doc) {
        return DocDto.builder()
                .id(doc.getDocId())
                .title(doc.getTitle())
                .summary(doc.getSummary())
                .build();
    }
}
