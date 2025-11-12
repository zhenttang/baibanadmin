package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocMode;
import com.yunke.backend.document.domain.entity.DocRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
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
public class DocDto {
    private String id;
    private String workspaceId;
    private String title;
    private String summary;
    private Boolean isPublic;
    private Boolean blocked;
    private DocRole defaultRole;
    private DocMode mode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Creator and updater information
    private UserDto creator;
    private UserDto updater;
    
    // Permission information for current user
    private DocPermissionsDto permissions;
}