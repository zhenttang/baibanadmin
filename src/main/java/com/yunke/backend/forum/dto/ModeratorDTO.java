package com.yunke.backend.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ModeratorDTO {
    private Long id;
    private Long forumId;
    private String forumName;
    private String userId;
    private String userName;
    private String role; // CHIEF / DEPUTY
    // 存储为 JSON（对象或数组），与服务端解析逻辑保持一致
    private String permissions;
    private String appointedBy;
    private LocalDateTime appointedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime updatedAt;
}
