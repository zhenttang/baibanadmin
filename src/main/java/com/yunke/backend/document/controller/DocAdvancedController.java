package com.yunke.backend.document.controller;

import com.yunke.backend.document.dto.*;
import com.yunke.backend.workspace.dto.WorkspaceImportResultDto;
import com.yunke.backend.workspace.dto.WorkspaceExportInput;
import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.document.service.DocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

/**
 * 文档高级功能控制器
 * 提供导入导出、格式转换、批量操作等高级功能
 */
@Slf4j
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/docs/advanced")
@RequiredArgsConstructor
public class DocAdvancedController {

    private final DocService docService;

    // ==================== 导入功能 ====================

    /**
     * 智能导入文档
     */
    @PostMapping("/import/smart")
    public Mono<ResponseEntity<DocDto>> smartImportDoc(
            @PathVariable String workspaceId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "metadata", required = false) ImportDocInput metadata,
            Principal principal) {
        
        return Mono.fromCallable(() -> {
            try {
                ImportDocInput input = metadata != null ? metadata : new ImportDocInput();
                input.setFilename(file.getOriginalFilename());
                input.setContent(file.getBytes());
                
                if (input.getTitle() == null || input.getTitle().trim().isEmpty()) {
                    input.setTitle(extractTitleFromFilename(file.getOriginalFilename()));
                }
                
                return input;
            } catch (Exception e) {
                throw new RuntimeException("Failed to process uploaded file", e);
            }
        })
        .flatMap(input -> docService.smartImportDoc(workspaceId, input, principal.getName()))
        .map(doc -> ResponseEntity.ok(doc))
        .onErrorResume(e -> {
            log.error("Failed to smart import document", e);
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * 批量导入文档
     */
    @PostMapping("/import/batch")
    public Mono<ResponseEntity<List<DocImportResultDto>>> batchImportDocs(
            @PathVariable String workspaceId,
            @RequestPart("files") List<MultipartFile> files,
            Principal principal) {
        
        return Mono.fromCallable(() -> {
            try {
                return files.stream().map(file -> {
                    try {
                        ImportDocInput input = ImportDocInput.builder()
                                .title(extractTitleFromFilename(file.getOriginalFilename()))
                                .filename(file.getOriginalFilename())
                                .content(file.getBytes())
                                .preserveFormatting(true)
                                .build();
                        return input;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to process file: " + file.getOriginalFilename(), e);
                    }
                }).toList();
            } catch (Exception e) {
                throw new RuntimeException("Failed to process uploaded files", e);
            }
        })
        .flatMap(inputs -> docService.batchImportDocs(workspaceId, inputs, principal.getName()))
        .map(results -> ResponseEntity.ok(results))
        .onErrorResume(e -> {
            log.error("Failed to batch import documents", e);
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * 预览导入
     */
    @PostMapping("/import/preview")
    public Mono<ResponseEntity<ImportPreviewDto>> previewImport(
            @RequestPart("file") MultipartFile file) {
        
        return Mono.fromCallable(() -> {
            try {
                ImportDocInput input = ImportDocInput.builder()
                        .filename(file.getOriginalFilename())
                        .content(file.getBytes())
                        .build();
                return input;
            } catch (Exception e) {
                throw new RuntimeException("Failed to process uploaded file", e);
            }
        })
        .flatMap(input -> docService.previewImport(input))
        .map(preview -> ResponseEntity.ok(preview))
        .onErrorResume(e -> {
            log.error("Failed to preview import", e);
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * 导入工作空间ZIP包
     */
    @PostMapping("/import/workspace")
    public Mono<ResponseEntity<WorkspaceImportResultDto>> importWorkspaceFromZip(
            @PathVariable String workspaceId,
            @RequestPart("file") MultipartFile zipFile,
            Principal principal) {
        
        return Mono.fromCallable(() -> {
            try {
                return zipFile.getBytes();
            } catch (Exception e) {
                throw new RuntimeException("Failed to process ZIP file", e);
            }
        })
        .flatMap(zipContent -> docService.importWorkspaceFromZip(workspaceId, zipContent, principal.getName()))
        .map(result -> ResponseEntity.ok(result))
        .onErrorResume(e -> {
            log.error("Failed to import workspace from ZIP", e);
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    // ==================== 导出功能 ====================

    /**
     * 高级导出文档
     */
    @PostMapping("/{docId}/export/advanced")
    public Mono<ResponseEntity<byte[]>> advancedExportDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody AdvancedExportInput input,
            Principal principal) {
        
        return docService.advancedExportDoc(workspaceId, docId, input, principal.getName())
                .map(export -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + export.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(export.getContentType()))
                        .contentLength(export.getSize())
                        .body(export.getContent()))
                .onErrorResume(e -> {
                    log.error("Failed to export document", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 批量导出文档
     */
    @PostMapping("/export/batch")
    public Mono<ResponseEntity<List<DocExportDto>>> batchExportDocs(
            @PathVariable String workspaceId,
            @RequestBody BatchExportRequest request,
            Principal principal) {
        
        return docService.batchExportDocs(workspaceId, request.getDocIds(), request.getFormat(), principal.getName())
                .map(exports -> ResponseEntity.ok(exports))
                .onErrorResume(e -> {
                    log.error("Failed to batch export documents", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 导出整个工作空间
     */
    @PostMapping("/export/workspace")
    public Mono<ResponseEntity<byte[]>> exportWorkspace(
            @PathVariable String workspaceId,
            @RequestBody WorkspaceExportInput input,
            Principal principal) {
        
        return docService.exportWorkspace(workspaceId, input, principal.getName())
                .map(export -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + export.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(export.getSize())
                        .body(export.getContent()))
                .onErrorResume(e -> {
                    log.error("Failed to export workspace", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    // ==================== 格式转换 ====================

    /**
     * 转换文档格式
     */
    @PostMapping("/{docId}/convert")
    public Mono<ResponseEntity<byte[]>> convertDocFormat(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody FormatConversionRequest request,
            Principal principal) {
        
        return docService.convertDocFormat(workspaceId, docId, 
                request.getSourceFormat(), request.getTargetFormat(), principal.getName())
                .map(export -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + export.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(export.getContentType()))
                        .contentLength(export.getSize())
                        .body(export.getContent()))
                .onErrorResume(e -> {
                    log.error("Failed to convert document format", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    // ==================== 信息查询 ====================

    /**
     * 获取支持的导入格式
     */
    @GetMapping("/import/formats")
    public Mono<ResponseEntity<List<ImportFormatInfoDto>>> getSupportedImportFormats() {
        return docService.previewImport(new ImportDocInput()) // Use service method indirectly
                .then(Mono.fromCallable(() -> {
                    // Return hardcoded supported formats for now
                    return List.of(
                            ImportFormatInfoDto.builder()
                                    .format(DocExportFormat.MARKDOWN)
                                    .name("Markdown")
                                    .description("Plain text with markdown formatting")
                                    .fileExtensions(List.of("md", "markdown"))
                                    .mimeTypes(List.of("text/markdown"))
                                    .supportsImages(true)
                                    .supportsFormatting(true)
                                    .supportsMetadata(false)
                                    .build(),
                            ImportFormatInfoDto.builder()
                                    .format(DocExportFormat.HTML)
                                    .name("HTML")
                                    .description("HyperText Markup Language")
                                    .fileExtensions(List.of("html", "htm"))
                                    .mimeTypes(List.of("text/html"))
                                    .supportsImages(true)
                                    .supportsFormatting(true)
                                    .supportsMetadata(true)
                                    .build(),
                            ImportFormatInfoDto.builder()
                                    .format(DocExportFormat.DOCX)
                                    .name("Microsoft Word")
                                    .description("Microsoft Word document")
                                    .fileExtensions(List.of("docx"))
                                    .mimeTypes(List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                                    .supportsImages(true)
                                    .supportsFormatting(true)
                                    .supportsMetadata(true)
                                    .build()
                    );
                }))
                .map(formats -> ResponseEntity.ok(formats));
    }

    /**
     * 获取支持的导出格式
     */
    @GetMapping("/export/formats")
    public Mono<ResponseEntity<List<ExportFormatInfoDto>>> getSupportedExportFormats() {
        return Mono.fromCallable(() -> List.of(
                ExportFormatInfoDto.builder()
                        .format(DocExportFormat.MARKDOWN)
                        .name("Markdown")
                        .description("Plain text with markdown formatting")
                        .fileExtension("md")
                        .mimeType("text/markdown")
                        .supportsImages(true)
                        .supportsFormatting(true)
                        .supportsCustomTemplates(false)
                        .availableOptions(List.of("includeImages", "includeMetadata"))
                        .build(),
                ExportFormatInfoDto.builder()
                        .format(DocExportFormat.PDF)
                        .name("PDF")
                        .description("Portable Document Format")
                        .fileExtension("pdf")
                        .mimeType("application/pdf")
                        .supportsImages(true)
                        .supportsFormatting(true)
                        .supportsCustomTemplates(true)
                        .availableOptions(List.of("includeImages", "watermark", "customTemplate"))
                        .build(),
                ExportFormatInfoDto.builder()
                        .format(DocExportFormat.HTML)
                        .name("HTML")
                        .description("HyperText Markup Language")
                        .fileExtension("html")
                        .mimeType("text/html")
                        .supportsImages(true)
                        .supportsFormatting(true)
                        .supportsCustomTemplates(true)
                        .availableOptions(List.of("includeImages", "includeMetadata", "customTemplate"))
                        .build(),
                ExportFormatInfoDto.builder()
                        .format(DocExportFormat.DOCX)
                        .name("Microsoft Word")
                        .description("Microsoft Word document")
                        .fileExtension("docx")
                        .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        .supportsImages(true)
                        .supportsFormatting(true)
                        .supportsCustomTemplates(false)
                        .availableOptions(List.of("includeImages", "includeMetadata"))
                        .build()
        ))
        .map(formats -> ResponseEntity.ok(formats));
    }

    // ==================== 辅助类 ====================

    public static class BatchExportRequest {
        private List<String> docIds;
        private DocExportFormat format;

        // Getters and setters
        public List<String> getDocIds() { return docIds; }
        public void setDocIds(List<String> docIds) { this.docIds = docIds; }
        public DocExportFormat getFormat() { return format; }
        public void setFormat(DocExportFormat format) { this.format = format; }
    }

    public static class FormatConversionRequest {
        private DocExportFormat sourceFormat;
        private DocExportFormat targetFormat;

        // Getters and setters
        public DocExportFormat getSourceFormat() { return sourceFormat; }
        public void setSourceFormat(DocExportFormat sourceFormat) { this.sourceFormat = sourceFormat; }
        public DocExportFormat getTargetFormat() { return targetFormat; }
        public void setTargetFormat(DocExportFormat targetFormat) { this.targetFormat = targetFormat; }
    }

    // ==================== 辅助方法 ====================

    private String extractTitleFromFilename(String filename) {
        if (filename == null) return "Untitled";
        
        String name = filename;
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            name = name.substring(0, lastDot);
        }
        return name.replaceAll("[_-]", " ");
    }
}