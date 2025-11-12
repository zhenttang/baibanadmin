package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付意向DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentDto {
    private String id;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String clientSecret;
    private String paymentMethodId;
    private String description;
    private Boolean captured;
    private LocalDateTime createdAt;
    private LocalDateTime capturedAt;
    private Map<String, Object> metadata;
} 