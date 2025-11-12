package com.yunke.backend.document.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, length = 50)
    private String documentId;

    @Column(name = "reporter_id", nullable = false, length = 50)
    private String reporterId;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "reviewer_id", length = 50)
    private String reviewerId;

    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    @Column(name = "review_result", length = 20)
    private String reviewResult;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
