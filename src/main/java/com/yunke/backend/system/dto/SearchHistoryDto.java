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
public class SearchHistoryDto {
    private String id;
    private String userId;
    private String query;
    private String workspaceId;
    private LocalDateTime searchedAt;
    private Integer resultCount;
}