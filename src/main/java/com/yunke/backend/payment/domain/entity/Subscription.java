package com.yunke.backend.payment.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_subscription_target_plan", columnNames = {"target_id", "plan"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "workspace_id")
    private String workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring", nullable = false, length = 20)
    private BillingInterval recurring;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", length = 20)
    private BillingInterval billingInterval;

    @Column(name = "variant", length = 20)
    private String variant;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;

    @Column(name = "stripe_schedule_id")
    private String stripeScheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime start;

    @Column(name = "end_time")
    private LocalDateTime end;

    @Column(name = "next_bill_at")
    private LocalDateTime nextBillAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "trial_start")
    private LocalDateTime trialStart;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 关系映射 - 注意：这里使用targetId作为外键，可以关联到User或其他实体
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    public enum SubscriptionPlan {
        FREE("free"),
        PRO("pro"),
        TEAM("team"),
        ENTERPRISE("enterprise");

        private final String value;

        SubscriptionPlan(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SubscriptionStatus {
        INCOMPLETE("incomplete"),
        INCOMPLETE_EXPIRED("incomplete_expired"),
        TRIALING("trialing"),
        ACTIVE("active"),
        PAST_DUE("past_due"),
        CANCELED("canceled"),
        UNPAID("unpaid"),
        PAUSED("paused");

        private final String value;

        SubscriptionStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum BillingInterval {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year");

        private final String value;

        BillingInterval(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // 业务方法
    public boolean isActive() {
        return SubscriptionStatus.ACTIVE.equals(this.status) || 
               SubscriptionStatus.TRIALING.equals(this.status);
    }

    public boolean isTrialing() {
        return SubscriptionStatus.TRIALING.equals(this.status) &&
               trialEnd != null && trialEnd.isAfter(LocalDateTime.now());
    }

    public boolean isCanceled() {
        return SubscriptionStatus.CANCELED.equals(this.status) ||
               canceledAt != null;
    }

    public boolean isPastDue() {
        return SubscriptionStatus.PAST_DUE.equals(this.status);
    }

    public void cancel(boolean immediately) {
        if (immediately) {
            this.status = SubscriptionStatus.CANCELED;
            this.canceledAt = LocalDateTime.now();
            this.end = LocalDateTime.now();
        } else {
            // 设置在期间结束时取消
            if (this.metadata == null) {
                this.metadata = new java.util.HashMap<>();
            }
            this.metadata.put("cancel_at_period_end", true);
        }
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.canceledAt = null;
        if (this.metadata != null) {
            this.metadata.remove("cancel_at_period_end");
        }
    }

    public long getDaysUntilRenewal() {
        if (nextBillAt == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), nextBillAt).toDays();
    }

    public boolean hasFeature(String featureName) {
        return switch (this.plan) {
            case FREE -> isFreeFeature(featureName);
            case PRO -> isProFeature(featureName);
            case TEAM -> isTeamFeature(featureName);
            case ENTERPRISE -> true; // Enterprise has all features
        };
    }

    private boolean isFreeFeature(String featureName) {
        return switch (featureName) {
            case "basic_docs", "basic_storage", "basic_search" -> true;
            default -> false;
        };
    }

    private boolean isProFeature(String featureName) {
        return switch (featureName) {
            case "basic_docs", "basic_storage", "basic_search",
                 "advanced_search", "ai_copilot", "unlimited_docs" -> true;
            default -> false;
        };
    }

    private boolean isTeamFeature(String featureName) {
        return switch (featureName) {
            case "basic_docs", "basic_storage", "basic_search",
                 "advanced_search", "ai_copilot", "unlimited_docs",
                 "team_collaboration", "advanced_permissions" -> true;
            default -> false;
        };
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
} 