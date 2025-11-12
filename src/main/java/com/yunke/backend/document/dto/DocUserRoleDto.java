package com.yunke.backend.document.dto;

import com.yunke.backend.document.domain.entity.DocRole;
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
public class DocUserRoleDto {
    private String userId;
    private DocRole role;
    private LocalDateTime createdAt;
    
    // User information
    private UserDto user;
}