package com.yunke.backend.document.dto;

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
public class ImportPreviewDto {
    private String filename;
    private DocExportFormat detectedFormat;
    private String title;
    private String summary;
    private Long fileSize;
    private Integer estimatedPages;
    private Boolean hasImages;
    private Boolean hasFormatting;
    private List<String> warnings;
    private List<String> suggestions;
}