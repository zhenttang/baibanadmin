package com.yunke.backend.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI会话实体
 * 对应Node.js版本的ChatSession
 */
@Entity
@Table(name = "copilot_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CopilotSession {

    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(name = "doc_id", length = 36)
    private String docId;

    @Column(name = "title", length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private AIProvider provider;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "message_count")
    private Integer messageCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "JSON")
    private Map<String, Object> config;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public enum AIProvider {
        OPENAI("openai"),
        ANTHROPIC("anthropic"),
        GOOGLE("google"),
        AZURE_OPENAI("azure_openai"),
        OLLAMA("ollama");

        private final String value;

        AIProvider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SessionStatus {
        ACTIVE("active"),
        FINISHED("finished"),
        CANCELLED("cancelled"),
        ERROR("error");

        private final String value;

        SessionStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public void markFinished() {
        this.status = SessionStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = SessionStatus.CANCELLED;
        this.finishedAt = LocalDateTime.now();
    }

    public void markError() {
        this.status = SessionStatus.ERROR;
        this.finishedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return SessionStatus.ACTIVE.equals(this.status);
    }

    public void incrementTokenUsage(int tokens) {
        this.tokensUsed = (this.tokensUsed != null ? this.tokensUsed : 0) + tokens;
    }

    public void incrementMessageCount() {
        this.messageCount = (this.messageCount != null ? this.messageCount : 0) + 1;
    }
}