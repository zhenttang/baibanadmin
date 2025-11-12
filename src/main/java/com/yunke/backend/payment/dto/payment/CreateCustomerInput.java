package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建客户输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerInput {
    private String name;
    private String email;
    private String phone;
    private String description;
    private Address address;
    private String taxId;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
} 