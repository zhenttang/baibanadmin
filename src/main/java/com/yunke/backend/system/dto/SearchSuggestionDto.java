package com.yunke.backend.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionDto {
    private String text;
    private String type;
    private Double score;
    private String docId;
    private String workspaceId;
}