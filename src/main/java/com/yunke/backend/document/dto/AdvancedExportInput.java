package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedExportInput {
    private DocExportFormat format;
    private Boolean includeImages;
    private Boolean includeMetadata;
    private Boolean compressOutput;
    private String customTemplate;
    private Map<String, Object> formatOptions;
    private Boolean watermark;
    private String watermarkText;
}