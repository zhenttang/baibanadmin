package com.yunke.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String name;
    private String username;
    private String email;
    private String avatarUrl;
    private String locale;
    private Boolean emailVerified;
    private Boolean mfaEnabled;
    private Boolean isAdmin;
    private Map<String, Object> preferences;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 