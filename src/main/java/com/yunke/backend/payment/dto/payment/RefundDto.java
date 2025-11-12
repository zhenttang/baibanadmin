package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 退款DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto {
    private String id;
    private String paymentIntentId;
    private String chargeId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
} 