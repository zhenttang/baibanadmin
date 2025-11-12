package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 创建退款输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefundInput {
    private String paymentIntentId;
    private String chargeId;
    private BigDecimal amount;
    private String reason;
    private Boolean refundApplicationFee;
    private Boolean reverseTransfer;
    private Map<String, Object> metadata;
} 