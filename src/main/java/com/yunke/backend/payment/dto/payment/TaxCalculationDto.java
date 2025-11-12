package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 税费计算结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationDto {
    private Long amount;
    private Long taxAmount;
    private String currency;
    private String countryCode;
    private List<TaxItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxItem {
        private String type;
        private String description;
        private Long amount;
        private Double rate;
    }
    
    public static class TaxCalculationDtoBuilder {
        public TaxCalculationDtoBuilder amount(Long amount) {
            this.amount = amount;
            return this;
        }
    }
} 