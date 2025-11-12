package com.yunke.backend.payment.dto;

import com.yunke.backend.payment.dto.payment.InvoiceDto;
import com.yunke.backend.payment.domain.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订阅DTO
 * 对应Node.js版本的UserSubscriptionType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {

    private String id;
    private String userId;
    private String workspaceId;
    private String targetId;
    private Subscription.SubscriptionPlan plan;
    private Subscription.BillingInterval recurring;
    private String variant;
    private Integer quantity;
    private String stripeSubscriptionId;
    private String stripeScheduleId;
    private Subscription.SubscriptionStatus status;
    private LocalDateTime start;
    private LocalDateTime end;
    private LocalDateTime nextBillAt;
    private LocalDateTime canceledAt;
    private Boolean cancelAtPeriodEnd;
    private String cancelReason;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private BigDecimal amount;
    private String currency;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 业务字段
    private Boolean isActive;
    private Boolean isTrialing;
    private Boolean isCanceled;
    private Boolean isPastDue;
    private Long daysUntilRenewal;
    private List<String> availableFeatures;
    private List<InvoiceDto> recentInvoices;

    // 嵌套DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSubscriptionInput {
        private String targetId;
        private Subscription.SubscriptionPlan plan;
        private Subscription.BillingInterval recurring;
        private String variant;
        private Integer quantity;
        private String customerId;
        private String priceId;
        private LocalDateTime trialEnd;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSubscriptionInput {
        private Subscription.SubscriptionPlan plan;
        private Subscription.BillingInterval recurring;
        private Integer quantity;
        private Boolean cancelAtPeriodEnd;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelSubscriptionInput {
        private Boolean cancelAtPeriodEnd;
        private String reason;
        private Boolean immediately;
    }
}
