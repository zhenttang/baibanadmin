package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyDocInput {
    private String newTitle;
    private Boolean copyPermissions;
    private Boolean copyHistory;
    private Boolean preserveStructure;
}