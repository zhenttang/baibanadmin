package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocStatsDto {
    private String workspaceId;
    private Long totalDocs;
    private Long publicDocs;
    private Long privateDocs;
    private Long blockedDocs;
    private Long totalSnapshots;
    private Long totalUpdates;
    private Long totalSize; // in bytes
}