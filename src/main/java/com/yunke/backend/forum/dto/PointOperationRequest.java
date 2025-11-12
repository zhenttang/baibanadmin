package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointOperationRequest {
    @NotNull
    private Long userId;
    
    @NotNull
    private Integer points;
    
    private String reason; // 加减分原因
}

