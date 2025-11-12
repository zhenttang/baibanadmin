package com.yunke.backend.workspace.dto;

import com.yunke.backend.document.domain.entity.DocExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceExportInput {
    private DocExportFormat format;
    private List<String> includeDocs; // null = all docs
    private Boolean includePrivateDocs;
    private Boolean includeMetadata;
    private Boolean includePermissions;
    private Boolean compressOutput;
}