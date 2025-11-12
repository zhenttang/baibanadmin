package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 价格DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDto {
    private String id;
    private String productId;
    private String nickname;
    private BigDecimal unitAmount;
    private String currency;
    private String type; // one_time, recurring
    private String interval; // day, week, month, year
    private Integer intervalCount;
    private Boolean active;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
} 