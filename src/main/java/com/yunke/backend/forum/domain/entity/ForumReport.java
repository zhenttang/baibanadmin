package com.yunke.backend.forum.domain.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_reports", indexes = {
    @Index(name = "idx_target", columnList = "target_type, target_id"),
    @Index(name = "idx_reporter", columnList = "reporter_id"),
    @Index(name = "idx_status", columnList = "status, created_at"),
    @Index(name = "idx_handler", columnList = "handler_id")
})
@Data
public class ForumReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false)
    private TargetType targetType;
    
    @Column(name = "target_id", length = 50, nullable = false)
    private String targetId;
    
    @Column(name = "reporter_id", length = 50, nullable = false)
    private String reporterId;
    
    @Column(name = "reporter_name", length = 100)
    private String reporterName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50, nullable = false)
    private ReportReason reason;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "handler_id", length = 50)
    private String handlerId;
    
    @Column(name = "handler_name", length = 100)
    private String handlerName;
    
    @Column(name = "handler_note", columnDefinition = "TEXT")
    private String handlerNote;
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TargetType {
        POST,
        REPLY,
        USER
    }
    
    public enum ReportReason {
        SPAM,
        ILLEGAL_CONTENT,
        HARASSMENT,
        PORNOGRAPHY,
        VIOLENCE,
        OTHER
    }
    
    public enum ReportStatus {
        PENDING,
        RESOLVED,
        REJECTED
    }
}
