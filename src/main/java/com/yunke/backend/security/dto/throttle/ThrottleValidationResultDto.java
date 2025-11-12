package com.yunke.backend.security.dto.throttle;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流配置验证结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrottleValidationResultDto {

    @Builder.Default
    private Boolean valid = Boolean.TRUE;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
