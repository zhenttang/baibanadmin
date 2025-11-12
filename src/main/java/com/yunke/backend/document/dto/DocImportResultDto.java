package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocImportResultDto {
    private String filename;
    private Boolean success;
    private String error;
    private DocDto importedDoc;
    private DocExportFormat detectedFormat;
    private Long fileSize;
    private LocalDateTime importedAt;
    private String warnings;
    private String docId;
    private String title;
    private String workspaceId;
}