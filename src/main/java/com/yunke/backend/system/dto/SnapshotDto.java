package com.yunke.backend.system.dto;

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
public class SnapshotDto {
    private String workspaceId;
    private String id;
    private byte[] blob;
    private byte[] state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Integer seq;
    
    // User information
    private UserDto creator;
    private UserDto updater;
}