package com.yunke.backend.forum.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentDTO {
    private Long id;
    private String postId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String uploaderId;
    private String uploaderName;
    private LocalDateTime createdAt;
}

