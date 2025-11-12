package com.yunke.backend.security.dto.throttle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流测试结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottleTestResultDto {

    private Boolean success;
    private String message;
    private String details;
    private Integer testRequests;
    private Integer blockedRequests;
}
