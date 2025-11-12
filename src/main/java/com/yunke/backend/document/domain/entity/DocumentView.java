package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_views", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id, viewed_at"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", length = 50, nullable = false)
    private String documentId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "view_duration")
    private Integer viewDuration = 0;

    @CreationTimestamp
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
}
