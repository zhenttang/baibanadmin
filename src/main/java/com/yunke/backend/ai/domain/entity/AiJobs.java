package com.yunke.backend.ai.domain.entity;


import com.yunke.backend.ai.enums.AiJobType;
import com.yunke.backend.ai.enums.AiJobStatus;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.Workspace;
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

@Entity
@Table(name = "ai_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiJobs {

    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "workspace_id")
    private String workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiJobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> input;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> output;

    @Column
    private String error;

    @Column(name = "worker_id")
    private String workerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Workspace workspace;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = AiJobStatus.PENDING;
        }
    }
} 