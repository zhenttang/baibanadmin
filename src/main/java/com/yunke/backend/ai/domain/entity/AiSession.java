package com.yunke.backend.ai.domain.entity;

import com.yunke.backend.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "ai_sessions_metadata",
    indexes = {
        @Index(name = "idx_ai_session_user_id", columnList = "user_id"),
        @Index(name = "idx_ai_session_user_workspace", columnList = "user_id, workspace_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSession {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "doc_id", nullable = false)
    private String docId;

    @Column(name = "prompt_name", length = 32, nullable = false)
    private String promptName;

    @Column(name = "parent_session_id")
    private String parentSessionId;

    @Column(name = "message_cost", nullable = false)
    @Builder.Default
    private Integer messageCost = 0;

    @Column(name = "token_cost", nullable = false)
    @Builder.Default
    private Integer tokenCost = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_name", referencedColumnName = "name", insertable = false, updatable = false)
    private AiPrompt prompt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiSessionMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiContext> context = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
} 