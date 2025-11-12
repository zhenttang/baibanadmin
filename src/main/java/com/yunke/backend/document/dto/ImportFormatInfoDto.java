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
public class ImportFormatInfoDto {
    private DocExportFormat format;
    private String name;
    private String description;
    private List<String> fileExtensions;
    private List<String> mimeTypes;
    private Boolean supportsImages;
    private Boolean supportsFormatting;
    private Boolean supportsMetadata;
}