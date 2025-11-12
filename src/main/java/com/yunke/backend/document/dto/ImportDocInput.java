package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportDocInput {
    private String title;
    private String summary;
    private byte[] content;
    private DocExportFormat format;
    private String filename;
    private Boolean preserveFormatting;
    private Boolean isPublic;
}