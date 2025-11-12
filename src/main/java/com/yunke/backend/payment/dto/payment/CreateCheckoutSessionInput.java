package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建结账会话输入
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckoutSessionInput {
    private String priceId;
    private String customerId;
    private String successUrl;
    private String cancelUrl;
    private String workspaceId;
    private String userId;
    private String couponId;
    private Integer quantity;
    private String mode;
} 