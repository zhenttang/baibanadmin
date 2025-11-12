package com.yunke.backend.payment.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 客户数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDto {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String description;
    private Address address;
    private boolean defaultSource;
    private Instant created;
    private List<String> paymentMethods;
    private Map<String, String> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
} 