package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档版本DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocVersionDto {
    private String id;
    private String docId;
    private String workspaceId;
    private String version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private byte[] content;
    private byte[] state;
} 