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
public class UpdateDto {
    private String workspaceId;
    private String id;
    private LocalDateTime createdAt;
    private byte[] blob;
    private String createdBy;
    private Integer seq;
    
    // User information
    private UserDto creator;
}