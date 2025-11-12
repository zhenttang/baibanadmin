package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 税费计算输入参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationInput {
    private Long amount;
    private String currency;
    private String countryCode;
    private String postalCode;
    private String state;
    private String city;
} 