package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank
    private String targetType; // POST, REPLY, USER

    @NotBlank
    private String targetId;

    @NotBlank
    private String reason; // SPAM, ILLEGAL_CONTENT, HARASSMENT, etc.

    private String description; // 详细描述
}

