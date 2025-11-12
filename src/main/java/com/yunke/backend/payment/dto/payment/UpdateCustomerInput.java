package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新客户输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerInput {
    private String name;
    private String email;
    private String phone;
    private String description;
    private Map<String, Object> metadata;
    private Address address;
    private String taxId;
    
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