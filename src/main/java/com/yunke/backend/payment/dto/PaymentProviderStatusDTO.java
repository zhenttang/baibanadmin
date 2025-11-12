package com.yunke.backend.payment.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProviderStatusDTO {
    private String provider;
    private Boolean enabled;
    private Boolean healthy;
    private String status; // ACTIVE, INACTIVE, ERROR, MAINTENANCE
    private LocalDateTime lastChecked;
    private String errorMessage;
    private Map<String, Object> metrics;
    private ConnectionInfo connectionInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionInfo {
        private Boolean connected;
        private Integer responseTime; // milliseconds
        private String version;
        private String environment;
        private LocalDateTime lastSuccessfulConnection;
    }
}
