package com.yunke.backend.payment.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 付费记录实体
 */
@Entity
@Table(name = "payment_records")
@Data
@EqualsAndHashCode(callSuper = false)
public class PaymentRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private String userId;
    
    /**
     * 文档ID
     */
    @Column(name = "document_id")
    private String documentId;
    
    /**
     * 支付金额
     */
    @Column(name = "amount")
    private BigDecimal amount;
    
    /**
     * 支付方式
     */
    @Column(name = "payment_method")
    private String paymentMethod;
    
    /**
     * 交易ID
     */
    @Column(name = "transaction_id")
    private String transactionId;
    
    /**
     * 支付状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 支付状态枚举
     */
    public enum PaymentStatus {
        PENDING("待支付"),
        SUCCESS("支付成功"),
        FAILED("支付失败"),
        REFUNDED("已退款");
        
        private final String description;
        
        PaymentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}