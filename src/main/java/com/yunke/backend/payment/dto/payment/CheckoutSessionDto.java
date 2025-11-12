package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 结账会话DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionDto {
    private String id;
    private String customerId;
    private String url;
    private String status;
    private String mode;
    private String clientReferenceId;
    private String paymentStatus;
    private String subscriptionId;
    private String paymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Map<String, Object> metadata;
} 