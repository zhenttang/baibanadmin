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
import java.util.Map;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_target_id", columnList = "target_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @Column(name = "stripe_invoice_id", nullable = false)
    private String stripeInvoiceId;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "workspace_id")
    private String workspaceId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal tax;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "last_payment_error", columnDefinition = "TEXT")
    private String lastPaymentError;

    @Column(name = "link", columnDefinition = "TEXT")
    private String link;

    @Column(name = "onetime_subscription_redeemed", nullable = false)
    @Builder.Default
    private Boolean onetimeSubscriptionRedeemed = false;

    @Column(name = "subscription_id")
    private Integer subscriptionId;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 关系映射
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    private Subscription subscription;

    public enum InvoiceStatus {
        DRAFT("draft"),
        OPEN("open"),
        PAID("paid"),
        UNCOLLECTIBLE("uncollectible"),
        VOID("void");

        private final String value;

        InvoiceStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // 业务方法
    public boolean isPaid() {
        return InvoiceStatus.PAID.equals(this.status);
    }

    public boolean isOverdue() {
        return InvoiceStatus.OPEN.equals(this.status) &&
               dueDate != null && dueDate.isBefore(LocalDateTime.now());
    }

    public boolean canBePaid() {
        return InvoiceStatus.OPEN.equals(this.status);
    }

    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsVoid() {
        this.status = InvoiceStatus.VOID;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
        if (tax != null) {
            total = total.add(tax);
        }
        return total;
    }

    public String getId() {
        return stripeInvoiceId;
    }

    public BigDecimal getTotal() {
        return getTotalAmount();
    }
}
