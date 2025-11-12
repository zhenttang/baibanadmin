package com.yunke.backend.security.dto.throttle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流系统整体配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottleConfigDto {

    /**
     * 是否开启限流。
     */
    @Builder.Default
    private Boolean enabled = Boolean.FALSE;

    /**
     * 限流器配置集合。
     */
    private Throttlers throttlers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Throttlers {

        /**
         * 默认限流器配置。
         */
        @JsonProperty("default")
        private ThrottlerConfigDto defaultConfig;

        /**
         * 严格限流器配置。
         */
        private ThrottlerConfigDto strict;
    }
}
