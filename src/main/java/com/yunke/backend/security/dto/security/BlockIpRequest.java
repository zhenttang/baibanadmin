package com.yunke.backend.security.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封禁IP请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockIpRequest {
    
    /**
     * IP地址
     */
    @NotBlank(message = "IP地址不能为空")
    private String ip;
    
    /**
     * 封禁原因
     */
    @NotBlank(message = "封禁原因不能为空")
    private String reason;
    
    /**
     * 封禁时长（分钟）
     */
    @Min(value = 1, message = "封禁时长至少1分钟")
    @Builder.Default
    private Integer durationMinutes = 60;
}

