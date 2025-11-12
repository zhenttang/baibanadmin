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
 * 发票DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private String id;
    private String customerId;
    private String subscriptionId;
    private String number;
    private String status;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private String description;
    private String receiptUrl;
    private String hostedInvoiceUrl;
    private List<InvoiceLineItem> lineItems;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLineItem {
        private String id;
        private String description;
        private BigDecimal amount;
        private String currency;
        private Integer quantity;
        private String priceId;
        private String productId;
        private Map<String, Object> metadata;
    }
} 