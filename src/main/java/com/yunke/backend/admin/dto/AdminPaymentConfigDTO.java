package com.yunke.backend.admin.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentConfigDTO {
    private String provider;
    private Boolean enabled;
    private Map<String, String> configuration;
    private String webhookUrl;
    private String publicKey;
    private String environment; // sandbox, production
    private Map<String, Object> features;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentProviderConfig {
        private String name;
        private String displayName;
        private Boolean supported;
        private Map<String, ConfigField> requiredFields;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigField {
        private String type; // text, password, select, boolean
        private String label;
        private String description;
        private Boolean required;
        private String defaultValue;
        private String[] options; // for select type
    }
}
