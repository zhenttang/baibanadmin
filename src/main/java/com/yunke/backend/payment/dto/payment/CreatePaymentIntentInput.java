package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 创建支付意向输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentInput {
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String paymentMethodId;
    private Boolean setupFutureUsage;
    private Map<String, Object> metadata;
} 