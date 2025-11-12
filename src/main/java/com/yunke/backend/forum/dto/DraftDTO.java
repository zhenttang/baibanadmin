package com.yunke.backend.forum.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DraftDTO {
    private Long id;
    private String userId;
    private Long forumId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

