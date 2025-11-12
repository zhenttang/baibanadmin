package com.yunke.backend.payment.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AFFiNE支付订单实体
 */
@Entity
@Table(name = "affine_payment_orders", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_jeepay_order_no", columnList = "jeepay_order_no"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AFFiNEPaymentOrder {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    /**
     * 工作空间ID(可选)
     */
    @Column(name = "workspace_id", length = 36)
    private String workspaceId;
    
    /**
     * Jeepay订单号
     */
    @Column(name = "jeepay_order_no", length = 64)
    private String jeepayOrderNo;
    
    /**
     * 订阅计划类型
     */
    @Column(name = "plan_type", length = 32)
    private String planType;
    
    /**
     * 支付金额(分)
     */
    @Column(name = "amount", nullable = false)
    private Long amount;
    
    /**
     * 支付方式
     */
    @Column(name = "payment_method", length = 32)
    private String paymentMethod;
    
    /**
     * 商品标题
     */
    @Column(name = "subject", length = 255)
    private String subject;
    
    /**
     * 商品描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 支付状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    /**
     * 支付数据类型
     */
    @Column(name = "pay_data_type", length = 32)
    private String payDataType;
    
    /**
     * 支付数据
     */
    @Column(name = "pay_data", columnDefinition = "TEXT")
    private String payData;
    
    /**
     * 支付URL
     */
    @Column(name = "pay_url", length = 500)
    private String payUrl;
    
    /**
     * 二维码URL
     */
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;
    
    /**
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
    
    /**
     * 支付完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}