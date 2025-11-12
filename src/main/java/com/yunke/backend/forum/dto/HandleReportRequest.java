package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HandleReportRequest {
    @NotBlank
    private String status; // RESOLVED 或 REJECTED

    private String handleNote; // 处理备注
}

