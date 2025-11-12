package com.yunke.backend.security.dto.throttle;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个限流器的配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottlerConfigDto {

    /**
     * 时间窗口（秒）。
     */
    private Integer ttl;

    /**
     * 时间窗口内允许的最大请求数。
     */
    private Integer limit;

    /**
     * 需要忽略限流的 User-Agent 列表。
     */
    @Builder.Default
    private List<String> ignoreUserAgents = new ArrayList<>();

    /**
     * 允许跳过限流的条件表达式。
     */
    private String skipIf;

    /**
     * 触发限流后的阻断时长（秒）。
     */
    private Integer blockDuration;
}
