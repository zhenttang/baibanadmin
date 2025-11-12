package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新订阅输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionInput {
    private String priceId;
    private Integer quantity;
    private String paymentMethodId;
    private Boolean cancelAtPeriodEnd;
    private Map<String, Object> metadata;
} 