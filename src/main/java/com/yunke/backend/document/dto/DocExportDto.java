package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocExportDto {
    private String docId;
    private String title;
    private DocExportFormat format;
    private byte[] content;
    private String contentType;
    private String filename;
    private Long size;
    private LocalDateTime exportedAt;
    private String exportedBy;
}