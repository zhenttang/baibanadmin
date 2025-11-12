package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消订阅输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelSubscriptionInput {
    private Boolean cancelAtPeriodEnd;
    private String reason;
    private Boolean immediately;
} 