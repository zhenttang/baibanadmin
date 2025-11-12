package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.document.domain.entity.DocMode;
import com.yunke.backend.document.domain.entity.DocRole;
import com.yunke.backend.document.dto.*;
import com.yunke.backend.document.service.DocImportExportService;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.security.util.PermissionUtils;
import com.yunke.backend.security.constants.PermissionActions;
import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.system.domain.entity.*;
import com.yunke.backend.system.repository.*;

import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.dto.WorkspaceExportDto;
import com.yunke.backend.workspace.dto.WorkspaceExportInput;
import com.yunke.backend.workspace.dto.WorkspaceImportResultDto;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocImportExportServiceImpl implements DocImportExportService {

    private final WorkspaceDocRepository workspaceDocRepository;
    private final SnapshotRepository snapshotRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PermissionService permissionService;
    private final DocBinaryStorageService binaryStorageService;

    public Mono<DocImportResultDto> importDoc(String workspaceId, String userId, String filename, 
                                           InputStream content, String contentType) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.DOC_CREATE,
                () -> Mono.fromCallable(() -> {
                        // 创建新文档
                        String docId = UUID.randomUUID().toString();
                        String title = getFilenameWithoutExtension(filename);
                        
                        WorkspaceDoc newDoc = WorkspaceDoc.builder()
                                .workspaceId(workspaceId)
                                .docId(docId)
                                .title(title)
                                .summary("Imported document")
                                .mode(0) // Page mode
                                .defaultRole(30) // Manager
                                .build();
                        
                        WorkspaceDoc savedDoc = workspaceDocRepository.save(newDoc);
                        
                        // ✅ 验证元数据已保存成功
                        Optional<WorkspaceDoc> verifyDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
                        if (verifyDoc.isEmpty()) {
                            log.error("❌ [DOC-IMPORT] 元数据保存失败，但未抛出异常: docId={}", docId);
                            throw new RuntimeException("Failed to create document metadata");
                        }
                        
                        // 处理导入内容
                        try {
                            byte[] contentBytes = content.readAllBytes();

                            String pointer = binaryStorageService.saveSnapshot(workspaceId, docId, contentBytes);

                            // 创建快照
                            Snapshot snapshot = Snapshot.builder()
                                    .id(docId)
                                    .workspaceId(workspaceId)
                                    .blob(binaryStorageService.pointerToBytes(pointer))
                                    .createdAt(LocalDateTime.now())
                                    .createdBy(userId)
                                    .build();

                            snapshotRepository.save(snapshot);
                            
                            log.info("Document imported: {} in workspace: {}", docId, workspaceId);
                            
                            return DocImportResultDto.builder()
                                    .docId(docId)
                                    .title(title)
                                    .workspaceId(workspaceId)
                                    .success(true)
                                    .build();
                        } catch (IOException e) {
                            log.error("Failed to import document", e);
                            throw new RuntimeException("Failed to import document: " + e.getMessage());
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<List<DocImportResultDto>> batchImportDocs(String workspaceId, List<ImportDocInput> inputs, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.DOC_CREATE,
                () -> Mono.fromCallable(() -> {
                        List<DocImportResultDto> results = new ArrayList<>();
                        
                        for (ImportDocInput input : inputs) {
                            try {
                                DocDto importedDoc = smartImportDoc(workspaceId, input, userId).block();
                                
                                results.add(DocImportResultDto.builder()
                                        .filename(input.getFilename())
                                        .success(true)
                                        .importedDoc(importedDoc)
                                        .detectedFormat(input.getFormat())
                                        .fileSize(input.getContent() != null ? (long) input.getContent().length : 0L)
                                        .importedAt(LocalDateTime.now())
                                        .build());
                                
                            } catch (Exception e) {
                                log.error("Failed to import document: {}", input.getFilename(), e);
                                
                                results.add(DocImportResultDto.builder()
                                        .filename(input.getFilename())
                                        .success(false)
                                        .error(e.getMessage())
                                        .detectedFormat(input.getFormat())
                                        .fileSize(input.getContent() != null ? (long) input.getContent().length : 0L)
                                        .importedAt(LocalDateTime.now())
                                        .build());
                            }
                        }
                        
                        return results;
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<DocExportDto> advancedExportDoc(String workspaceId, String docId, AdvancedExportInput input, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_READ,
                () -> Mono.fromCallable(() -> {
                    WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
                        
                        // Get latest snapshot content
                        var snapshotOpt = snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
                        byte[] rawContent = snapshotOpt.map(this::resolveSnapshotContent).orElse(new byte[0]);
                        
                        // Apply advanced processing
                        byte[] processedContent = processExportContent(rawContent, input);
                        
                        // Generate filename with options
                        String filename = generateAdvancedFilename(doc.getTitle(), input);
                        
                        return DocExportDto.builder()
                                .docId(docId)
                                .title(doc.getTitle())
                                .format(input.getFormat())
                                .content(processedContent)
                                .contentType(input.getFormat().getContentType())
                                .filename(filename)
                                .size((long) processedContent.length)
                                .exportedAt(LocalDateTime.now())
                                .exportedBy(userId)
                                .build();
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<List<DocExportDto>> batchExportDocs(String workspaceId, List<String> docIds, DocExportFormat format, String userId) {
        return Mono.fromCallable(() -> {
            List<DocExportDto> results = new ArrayList<>();
            
            for (String docId : docIds) {
                try {
                    AdvancedExportInput input = AdvancedExportInput.builder()
                            .format(format)
                            .includeImages(true)
                            .includeMetadata(true)
                            .compressOutput(false)
                            .build();
                    
                    DocExportDto exported = advancedExportDoc(workspaceId, docId, input, userId).block();
                    results.add(exported);
                    
                } catch (Exception e) {
                    log.error("Failed to export document: {}", docId, e);
                    // Continue with other documents
                }
            }
            
            return results;
        });
    }

    @Override
    public Mono<WorkspaceExportDto> exportWorkspace(String workspaceId, WorkspaceExportInput input, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                "Workspace.Export",
                () -> Mono.fromCallable(() -> {
                    Workspace workspace = workspaceRepository.findById(workspaceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
                        
                        // Get documents to export
                        List<WorkspaceDoc> docs;
                        if (input.getIncludeDocs() != null) {
                            docs = workspaceDocRepository.findByWorkspaceIdAndDocIdIn(workspaceId, input.getIncludeDocs());
                        } else {
                            docs = workspaceDocRepository.findByWorkspaceId(workspaceId);
                        }
                        
                        // Filter private docs if needed
                        if (input.getIncludePrivateDocs() == null || !input.getIncludePrivateDocs()) {
                            docs = docs.stream().filter(WorkspaceDoc::getIsPublic).toList();
                        }
                        
                        // Create export package
                        byte[] exportContent = createWorkspaceExportPackage(docs, input);
                        String checksum = calculateChecksum(exportContent);
                        
                        String filename = workspace.getName() + "_export." + input.getFormat().getExtension();
                        
                        return WorkspaceExportDto.builder()
                                .workspaceId(workspaceId)
                                .workspaceName(workspace.getName())
                                .format(input.getFormat())
                                .content(exportContent)
                                .filename(filename)
                                .size((long) exportContent.length)
                                .docCount(docs.size())
                                .includedDocs(docs.stream().map(WorkspaceDoc::getDocId).toList())
                                .exportedAt(LocalDateTime.now())
                                .exportedBy(userId)
                                .checksum(checksum)
                                .build();
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    @Transactional
    public Mono<WorkspaceImportResultDto> importWorkspaceFromZip(String workspaceId, byte[] zipContent, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                "Workspace.Import",
                () -> Mono.fromCallable(() -> {
                        List<DocImportResultDto> results = new ArrayList<>();
                        List<String> warnings = new ArrayList<>();
                        int totalFiles = 0;
                        int successfulImports = 0;
                        int failedImports = 0;
                        
                        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
                            ZipEntry entry;
                            while ((entry = zis.getNextEntry()) != null) {
                                if (!entry.isDirectory()) {
                                    totalFiles++;
                                    
                                    try {
                                        byte[] content = zis.readAllBytes();
                                        String filename = entry.getName();
                                        
                                        // Extract title from filename
                                        String title = extractTitleFromFilename(filename);
                                        
                                        ImportDocInput input = ImportDocInput.builder()
                                                .title(title)
                                                .filename(filename)
                                                .content(content)
                                                .preserveFormatting(true)
                                                .build();
                                        
                                        DocDto imported = smartImportDoc(workspaceId, input, userId).block();
                                        
                                        results.add(DocImportResultDto.builder()
                                                .filename(filename)
                                                .success(true)
                                                .importedDoc(imported)
                                                .fileSize((long) content.length)
                                                .importedAt(LocalDateTime.now())
                                                .build());
                                        
                                        successfulImports++;
                                        
                                    } catch (Exception e) {
                                        log.error("Failed to import file from ZIP: {}", entry.getName(), e);
                                        
                                        results.add(DocImportResultDto.builder()
                                                .filename(entry.getName())
                                                .success(false)
                                                .error(e.getMessage())
                                                .importedAt(LocalDateTime.now())
                                                .build());
                                        
                                        failedImports++;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to process ZIP file", e);
                        }
                        
                        return WorkspaceImportResultDto.builder()
                                .workspaceId(workspaceId)
                                .success(failedImports == 0)
                                .totalFiles(totalFiles)
                                .successfulImports(successfulImports)
                                .failedImports(failedImports)
                                .results(results)
                                .importedAt(LocalDateTime.now())
                                .warnings(warnings)
                                .build();
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<DocExportDto> convertDocFormat(String workspaceId, String docId, DocExportFormat sourceFormat, DocExportFormat targetFormat, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_READ,
                () -> Mono.fromCallable(() -> {
                    // Get document content
                    var snapshotOpt = snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
                    byte[] content = snapshotOpt.map(this::resolveSnapshotContent).orElse(new byte[0]);
                        
                    // Convert format
                    byte[] convertedContent = convertFormat(content, sourceFormat, targetFormat);
                    
                    WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
                        
                        String filename = doc.getTitle() + "." + targetFormat.getExtension();
                        
                        return DocExportDto.builder()
                                .docId(docId)
                                .title(doc.getTitle())
                                .format(targetFormat)
                                .content(convertedContent)
                                .contentType(targetFormat.getContentType())
                                .filename(filename)
                                .size((long) convertedContent.length)
                                .exportedAt(LocalDateTime.now())
                                .exportedBy(userId)
                                .build();
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }
    
    public Mono<DocExportDto> exportDoc(String workspaceId, String docId, DocExportFormat format, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_READ,
                () -> Mono.fromCallable(() -> {
                    WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                            .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
                        
                        // 获取文档内容
                        var snapshotOpt = snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
                        byte[] content = snapshotOpt.map(this::resolveSnapshotContent).orElse(new byte[0]);
                        
                        // 处理内容（简单示例）
                        byte[] processedContent = switch (format) {
                            case HTML -> content;
                            case PDF -> content;
                            case MARKDOWN -> content;
                            case TEXT -> content;
                            case DOCX -> content;
                            case JSON -> content;
                            default -> content;
                        };
                        
                        String filename = doc.getTitle() + "." + format.getExtension();
                        
                        return DocExportDto.builder()
                                .docId(docId)
                                .title(doc.getTitle())
                                .format(format)
                                .content(processedContent)
                                .contentType(format.getContentType())
                                .filename(filename)
                                .size((long) processedContent.length)
                                .exportedAt(LocalDateTime.now())
                                .exportedBy(userId)
                                .build();
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }
    
    public Mono<byte[]> exportDocs(String workspaceId, List<String> docIds, DocExportFormat format, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> Mono.fromCallable(() -> {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ZipOutputStream zipOut = new ZipOutputStream(outputStream);
                        
                        for (String docId : docIds) {
                            try {
                                // 检查文档访问权限
                                boolean canAccess = permissionService.checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                                        .block();
                                if (!canAccess) {
                                    log.warn("User {} has no access to doc {} in workspace {}", userId, docId, workspaceId);
                                    continue;
                                }
                                
                                WorkspaceDoc doc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                                        .orElse(null);
                                if (doc == null) {
                                    log.warn("Document not found: {} in workspace {}", docId, workspaceId);
                                    continue;
                                }
                                
                                // 获取文档内容
                                var snapshotOpt = snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
                                byte[] content = snapshotOpt.map(this::resolveSnapshotContent).orElse(new byte[0]);
                                
                                // 处理内容
                                byte[] processedContent = processExportContent(content, 
                                        AdvancedExportInput.builder()
                                                .format(format)
                                                .includeImages(true)
                                                .build());
                                
                                String filename = doc.getTitle() + "." + format.getExtension();
                                ZipEntry zipEntry = new ZipEntry(filename);
                                zipOut.putNextEntry(zipEntry);
                                zipOut.write(processedContent);
                                zipOut.closeEntry();
                                
                            } catch (Exception e) {
                                log.error("Error exporting document {}: {}", docId, e.getMessage(), e);
                                // Continue with other documents
                            }
                        }
                        
                        zipOut.close();
                        return outputStream.toByteArray();
                    })
                    .subscribeOn(Schedulers.boundedElastic()));
    }

    public List<String> getSupportedImportFormats() {
        // 返回支持的导入格式名称列表
        return Arrays.asList(
            "markdown", 
            "html", 
            "docx", 
            "json"
        );
    }

    public List<DocExportFormat> getSupportedExportFormats() {
        // 返回支持的导出格式枚举列表
        return Arrays.asList(
            DocExportFormat.MARKDOWN,
            DocExportFormat.HTML,
            DocExportFormat.PDF,
            DocExportFormat.DOCX,
            DocExportFormat.JSON
        );
    }

    public Mono<ImportPreviewDto> previewImport(ImportDocInput input) {
        return Mono.fromCallable(() -> {
            DocExportFormat detectedFormat = detectFormat(input.getFilename(), input.getContent());
            List<String> warnings = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();
            
            // Analyze content
            int estimatedPages = estimatePages(input.getContent());
            boolean hasImages = detectImages(input.getContent(), detectedFormat);
            boolean hasFormatting = detectFormatting(input.getContent(), detectedFormat);
            
            // Add warnings and suggestions
            if (input.getContent().length > 10 * 1024 * 1024) { // 10MB
                warnings.add("Large file size may impact performance");
            }
            
            if (hasImages && detectedFormat == DocExportFormat.MARKDOWN) {
                suggestions.add("Consider using HTML format to preserve image formatting");
            }
            
            String title = input.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = extractTitleFromFilename(input.getFilename());
                suggestions.add("Title extracted from filename: " + title);
            }
            
            return ImportPreviewDto.builder()
                    .filename(input.getFilename())
                    .detectedFormat(detectedFormat)
                    .title(title)
                    .summary("Imported document")
                    .fileSize((long) input.getContent().length)
                    .estimatedPages(estimatedPages)
                    .hasImages(hasImages)
                    .hasFormatting(hasFormatting)
                    .warnings(warnings)
                    .suggestions(suggestions)
                    .build();
        });
    }

    /**
     * 智能导入文档，根据文档格式自动处理
     * @param workspaceId 工作空间ID
     * @param input 导入输入参数
     * @param userId 用户ID
     * @return 导入的文档
     */
    public Mono<DocDto> smartImportDoc(String workspaceId, ImportDocInput input, String userId) {
        return Mono.fromCallable(() -> {
            // 创建新文档
            String docId = UUID.randomUUID().toString();
            String title = input.getTitle() != null ? input.getTitle() : extractTitleFromFilename(input.getFilename());
            
            WorkspaceDoc newDoc = WorkspaceDoc.builder()
                    .workspaceId(workspaceId)
                    .docId(docId)
                    .title(title)
                    .summary("Imported document: " + input.getFilename())
                    .mode(0) // Page mode
                    .defaultRole(30) // Manager角色
                    .public_(input.getIsPublic() != null ? input.getIsPublic() : false)
                    .build();
            
            WorkspaceDoc savedDoc = workspaceDocRepository.save(newDoc);
            
            // ✅ 验证元数据已保存成功（双重检查）
            Optional<WorkspaceDoc> verifyDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
            if (verifyDoc.isEmpty()) {
                log.error("❌ [DOC-IMPORT] 元数据保存失败，但未抛出异常: docId={}", docId);
                throw new RuntimeException("Failed to create document metadata");
            }
            
            // 处理导入内容
            DocExportFormat detectedFormat = detectFormat(input.getFilename(), input.getContent());
            byte[] processedContent = processImportContent(input.getContent(), detectedFormat, input.getPreserveFormatting());
            
            // ✅ 使用二进制存储服务保存快照（与 WorkspaceDocServiceImpl 保持一致）
            String pointer = binaryStorageService.saveSnapshot(workspaceId, docId, processedContent);
            Snapshot snapshot = Snapshot.builder()
                    .workspaceId(workspaceId)
                    .id(docId)  // 使用id而不是docId
                    .blob(binaryStorageService.pointerToBytes(pointer))
                    .createdAt(LocalDateTime.now())
                    .createdBy(userId)
                    .build();

            snapshotRepository.save(snapshot);
            
            log.info("Document imported using smart import: {} in workspace: {}", docId, workspaceId);
            
            return convertToDto(savedDoc);
        });
    }

    // Helper methods
    private DocExportFormat detectFormat(String filename, byte[] content) {
        if (filename != null) {
            String ext = getFileExtension(filename).toLowerCase();
            return switch (ext) {
                case "md", "markdown" -> DocExportFormat.MARKDOWN;
                case "html", "htm" -> DocExportFormat.HTML;
                case "pdf" -> DocExportFormat.PDF;
                case "docx" -> DocExportFormat.DOCX;
                case "json" -> DocExportFormat.JSON;
                default -> DocExportFormat.MARKDOWN; // Default
            };
        }
        
        // Try to detect from content
        if (content != null && content.length > 0) {
            String contentStart = new String(content, 0, Math.min(content.length, 1000)).toLowerCase();
            if (contentStart.contains("<html") || contentStart.contains("<!doctype html")) {
                return DocExportFormat.HTML;
            } else if (contentStart.startsWith("{") && contentStart.contains("\"")) {
                return DocExportFormat.JSON;
            }
        }
        
        return DocExportFormat.MARKDOWN; // Default fallback
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
    
    private byte[] processImportContent(byte[] content, DocExportFormat format, Boolean preserveFormatting) {
        // Simplified content processing
        return content;
    }

    private byte[] processExportContent(byte[] content, AdvancedExportInput input) {
        // Apply advanced processing options
        byte[] processed = content;
        
        if (input.getWatermark() != null && input.getWatermark()) {
            processed = addWatermark(processed, input.getWatermarkText());
        }
        
        if (input.getCompressOutput() != null && input.getCompressOutput()) {
            processed = compressContent(processed);
        }
        
        return processed;
    }

    private byte[] resolveSnapshotContent(Snapshot snapshot) {
        if (snapshot == null || snapshot.getBlob() == null) {
            return new byte[0];
        }
        return binaryStorageService.resolvePointer(snapshot.getBlob(), snapshot.getWorkspaceId(), snapshot.getId());
    }
    
    private String generateAdvancedFilename(String title, AdvancedExportInput input) {
        String filename = title;
        
        if (input.getWatermark() != null && input.getWatermark()) {
            filename += "_watermarked";
        }
        
        filename += "." + input.getFormat().getExtension();
        return filename;
    }
    
    private byte[] createWorkspaceExportPackage(List<WorkspaceDoc> docs, WorkspaceExportInput input) {
        // Simplified implementation - would create ZIP or other package format
        return "Workspace export package".getBytes();
    }
    
    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "checksum_error";
        }
    }
    
    private String extractTitleFromFilename(String filename) {
        String name = filename;
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            name = name.substring(0, lastDot);
        }
        return name.replaceAll("[_-]", " ");
    }
    
    private String getFilenameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
    
    private byte[] convertFormat(byte[] content, DocExportFormat sourceFormat, DocExportFormat targetFormat) {
        // Simplified format conversion
        return content;
    }
    
    private int estimatePages(byte[] content) {
        // Rough estimation: 500 characters per page
        return Math.max(1, content.length / 500);
    }
    
    private boolean detectImages(byte[] content, DocExportFormat format) {
        if (format == DocExportFormat.HTML) {
            String contentStr = new String(content);
            return contentStr.contains("<img") || contentStr.contains("data:image");
        }
        return false;
    }
    
    private boolean detectFormatting(byte[] content, DocExportFormat format) {
        if (format == DocExportFormat.MARKDOWN) {
            String contentStr = new String(content);
            return contentStr.contains("**") || contentStr.contains("*") || contentStr.contains("#");
        } else if (format == DocExportFormat.HTML) {
            String contentStr = new String(content);
            return contentStr.contains("<b>") || contentStr.contains("<i>") || contentStr.contains("<h");
        }
        return false;
    }
    
    private byte[] addWatermark(byte[] content, String watermarkText) {
        // Simplified watermark implementation
        if (watermarkText != null) {
            String watermark = "\n\n<!-- Watermark: " + watermarkText + " -->\n";
            byte[] watermarkBytes = watermark.getBytes();
            byte[] result = new byte[content.length + watermarkBytes.length];
            System.arraycopy(content, 0, result, 0, content.length);
            System.arraycopy(watermarkBytes, 0, result, content.length, watermarkBytes.length);
            return result;
        }
        return content;
    }
    
    private byte[] compressContent(byte[] content) {
        // Simplified compression (would use actual compression library)
        return content;
    }
    
    private DocDto convertToDto(WorkspaceDoc doc) {
        return DocDto.builder()
                .id(doc.getDocId())
                .workspaceId(doc.getWorkspaceId())
                .title(doc.getTitle())
                .summary(doc.getSummary())
                .isPublic(doc.getIsPublic())
                .blocked(doc.getBlocked())
                .defaultRole(DocRole.fromValue(doc.getDefaultRole()))
                .mode(DocMode.fromValue(doc.getMode()))
                .build();
    }
}
