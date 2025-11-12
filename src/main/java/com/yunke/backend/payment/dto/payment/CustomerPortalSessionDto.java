package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户门户会话DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPortalSessionDto {
    private String id;
    private String url;
    private String customerId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
} 