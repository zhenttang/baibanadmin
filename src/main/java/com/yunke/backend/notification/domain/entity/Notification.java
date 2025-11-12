package com.yunke.backend.notification.domain.entity;

import com.yunke.backend.notification.enums.NotificationLevel;
import com.yunke.backend.notification.enums.NotificationType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import com.yunke.backend.user.domain.entity.User;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_id, created_at"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_read", columnList = "read")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Column(name = "recipient_id", nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationLevel level;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
} 