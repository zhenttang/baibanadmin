package com.yunke.backend.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatsDto {
    private String workspaceId;
    private Long totalDocs;
    private Long totalBlocks;
    private Long indexedDocs;
    private Long indexedBlocks;
    private LocalDateTime lastIndexed;
    private Boolean isIndexing;
    private String status;
    private Long indexSize;
}