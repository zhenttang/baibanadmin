package com.yunke.backend.workspace.dto;

import com.yunke.backend.document.dto.DocImportResultDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceImportResultDto {
    private String workspaceId;
    private Boolean success;
    private String error;
    private Integer totalFiles;
    private Integer successfulImports;
    private Integer failedImports;
    private List<DocImportResultDto> results;
    private LocalDateTime importedAt;
    private List<String> warnings;
}