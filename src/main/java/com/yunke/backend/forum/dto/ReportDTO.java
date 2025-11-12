package com.yunke.backend.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportDTO {
    private Long id;
    private String targetType; // POST, REPLY, USER
    private String targetId;
    private Long reporterId;
    private String reporterName;
    private String reason; // SPAM, ILLEGAL_CONTENT, HARASSMENT, etc.
    private String description;
    private String status; // PENDING, RESOLVED, REJECTED
    private Long handlerId;
    private String handlerName;
    private String handleNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}

