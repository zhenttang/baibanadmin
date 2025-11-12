package com.yunke.backend.security.dto.throttle;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流日志条目。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottleLogEntryDto {

    private String id;
    private Instant timestamp;
    private String level;
    private String rule;
    private String clientIp;
    private String requestPath;
    private Integer requestCount;
    private String action;
    private String message;
}
