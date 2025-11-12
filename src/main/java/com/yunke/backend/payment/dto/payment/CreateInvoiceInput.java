package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 创建发票输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceInput {
    private String customerId;
    private String description;
    private String currency;
    private LocalDateTime dueDate;
    private List<LineItem> items;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        private String description;
        private BigDecimal amount;
        private String currency;
        private Integer quantity;
    }
} 