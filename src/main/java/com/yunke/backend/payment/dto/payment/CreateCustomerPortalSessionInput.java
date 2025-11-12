package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建客户门户会话输入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerPortalSessionInput {
    private String customerId;
    private String returnUrl;
} 