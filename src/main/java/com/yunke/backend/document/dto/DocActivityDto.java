package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocActivityDto {
    private String id;
    private String userId;
    private String workspaceId;
    private String docId;
    private String action; // CREATE, UPDATE, DELETE, PUBLISH, etc.
    private LocalDateTime timestamp;
    private String docTitle;
    private String workspaceName;
    
    // Additional context
    private Object metadata; // JSON object with activity details
}