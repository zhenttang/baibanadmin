package com.yunke.backend.security.dto.throttle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流运行时统计信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottleStatsDto {

    private Boolean enabled;
    private Integer activeThrottlers;
    private Long totalRequests;
    private Long blockedRequests;
    private Integer requestsPerMinute;
}
