package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_purchases",
    uniqueConstraints = @UniqueConstraint(name = "uk_doc_user", columnNames = {"document_id", "user_id"}),
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_purchased_at", columnList = "purchased_at"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", length = 50, nullable = false)
    private String documentId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @CreationTimestamp
    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
}
