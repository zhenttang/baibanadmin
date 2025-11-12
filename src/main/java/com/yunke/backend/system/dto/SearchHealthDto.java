package com.yunke.backend.system.dto;

import com.yunke.backend.search.enums.SearchProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHealthDto {
    private Boolean isHealthy;
    private SearchProviderType providerType;
    private String status;
    private LocalDateTime lastChecked;
    private Long responseTime;
    private String version;
    private String errorMessage;
}