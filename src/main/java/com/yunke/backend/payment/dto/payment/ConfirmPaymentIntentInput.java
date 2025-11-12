package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认支付意向输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentIntentInput {
    private String paymentIntentId;
    private String paymentMethodId;
    private Boolean setupFutureUsage;
    private String returnUrl;
} 