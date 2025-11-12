package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建订阅输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionInput {
    private String customerId;
    private String priceId;
    private Integer quantity;
    private String paymentMethodId;
    private LocalDateTime trialEnd;
    private Boolean cancelAtPeriodEnd;
    private Map<String, Object> metadata;
} 