package com.yunke.backend.notification.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 邮件队列实体
 * 对应Node.js版本的邮件队列处理
 * 参考: /packages/backend/server/src/core/mail/job.ts
 */
@Entity
@Table(name = "mail_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 邮件类型/模板名称
     */
    @Column(name = "mail_type", nullable = false)
    private String mailType;

    /**
     * 收件人邮箱
     */
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    /**
     * 收件人姓名
     */
    @Column(name = "recipient_name")
    private String recipientName;

    /**
     * 邮件主题
     */
    @Column(name = "subject", nullable = false)
    private String subject;

    /**
     * 邮件参数（JSON格式）
     */
    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    /**
     * 处理状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MailStatus status = MailStatus.PENDING;

    /**
     * 优先级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private MailPriority priority = MailPriority.NORMAL;

    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * 下次重试时间
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * 错误信息
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * 错误堆栈
     */
    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;

    /**
     * 实际发送时间
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 邮件状态枚举
     */
    public enum MailStatus {
        PENDING,    // 等待发送
        PROCESSING, // 正在处理
        SENT,       // 已发送
        FAILED,     // 发送失败
        CANCELLED   // 已取消
    }

    /**
     * 邮件优先级枚举
     */
    public enum MailPriority {
        LOW,        // 低优先级
        NORMAL,     // 普通优先级
        HIGH,       // 高优先级
        URGENT      // 紧急
    }

    // ==================== 辅助方法 ====================

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 设置邮件参数
     */
    @JsonIgnore
    public void setParametersMap(Map<String, Object> parametersMap) {
        try {
            this.parameters = objectMapper.writeValueAsString(parametersMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize parameters", e);
        }
    }

    /**
     * 获取邮件参数
     */
    @JsonIgnore
    public Map<String, Object> getParametersMap() {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(parameters, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize parameters", e);
        }
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        if (this.retryCount >= this.maxRetries) {
            this.status = MailStatus.FAILED;
        }
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries && 
               this.status != MailStatus.SENT && 
               this.status != MailStatus.CANCELLED;
    }

    /**
     * 标记为已发送
     */
    public void markAsSent() {
        this.status = MailStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
        this.errorStack = null;
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage, String errorStack) {
        this.status = MailStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
    }

    /**
     * 计算下次重试时间
     */
    public void calculateNextRetryTime(int baseDelayMinutes) {
        // 指数退避策略
        long delayMinutes = (long) (baseDelayMinutes * Math.pow(2, retryCount));
        this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
    }

    /**
     * 检查是否需要立即处理
     */
    public boolean shouldProcessNow() {
        return this.status == MailStatus.PENDING || 
               (this.status == MailStatus.FAILED && 
                this.canRetry() && 
                (this.nextRetryAt == null || this.nextRetryAt.isBefore(LocalDateTime.now())));
    }

    /**
     * 获取优先级权重（用于排序）
     */
    public int getPriorityWeight() {
        return switch (this.priority) {
            case URGENT -> 4;
            case HIGH -> 3;
            case NORMAL -> 2;
            case LOW -> 1;
        };
    }
    
    /**
     * Builder扩展方法
     */
    public static class MailQueueBuilder {
        /**
         * 设置参数Map
         */
        public MailQueueBuilder parametersMap(Map<String, String> params) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.parameters = mapper.writeValueAsString(params);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize parameters", e);
            }
            return this;
        }
    }
}