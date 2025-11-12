package com.yunke.backend.security.dto.throttle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预设限流配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottlePresetDto {

    private String id;
    private String name;
    private String description;
    private ThrottlerConfigDto config;
}
