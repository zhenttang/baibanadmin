package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocInput {
    private String title;
    private String summary;
    private DocMode mode;
    private Boolean blocked;
}