package com.yunke.backend.document.dto;

import com.yunke.backend.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocMetaDto {
    private String docId;
    private String workspaceId;
    private String title;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Editor information
    private UserDto createdByUser;
    private UserDto updatedByUser;
}