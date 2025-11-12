package com.yunke.backend.payment.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 订阅数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionDto {
    private String id;
    private String customerId;
    private String status; // active, canceled, incomplete, incomplete_expired, past_due, trialing, unpaid
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private Instant canceledAt;
    private Instant startDate;
    private Instant endDate;
    private String defaultPaymentMethod;
    private List<SubscriptionItem> items;
    private Map<String, String> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionItem {
        private String id;
        private String priceId;
        private Integer quantity;
        private Map<String, String> metadata;
    }
} 