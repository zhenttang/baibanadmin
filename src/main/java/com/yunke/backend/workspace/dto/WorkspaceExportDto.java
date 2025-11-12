package com.yunke.backend.workspace.dto;

import com.yunke.backend.document.domain.entity.DocExportFormat;
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
public class WorkspaceExportDto {
    private String workspaceId;
    private String workspaceName;
    private DocExportFormat format;
    private byte[] content;
    private String filename;
    private Long size;
    private Integer docCount;
    private List<String> includedDocs;
    private LocalDateTime exportedAt;
    private String exportedBy;
    private String checksum;
}