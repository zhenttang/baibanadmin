package com.yunke.backend.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI会话消息实体
 * 对应Node.js版本的ChatMessage
 */
@Entity
@Table(name = "copilot_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CopilotMessage {

    @Id
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "JSON")
    private List<Map<String, Object>> attachments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "JSON")
    private Map<String, Object> params;

    @Column(name = "tokens")
    private Integer tokens;

    @Column(name = "finish_reason", length = 50)
    private String finishReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private CopilotSession session;

    public enum MessageRole {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system");

        private final String value;

        MessageRole(String value) {
            this.value = value;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() {
            return value;
        }
        
        @com.fasterxml.jackson.annotation.JsonCreator
        public static MessageRole fromString(String value) {
            if (value == null) {
                return null;
            }
            
            // 支持大写枚举名匹配（前端发送的"USER"）
            for (MessageRole role : MessageRole.values()) {
                if (role.name().equalsIgnoreCase(value) || role.getValue().equalsIgnoreCase(value)) {
                    return role;
                }
            }
            
            throw new IllegalArgumentException("Unknown MessageRole: " + value);
        }
    }

    public boolean isUserMessage() {
        return MessageRole.USER.equals(this.role);
    }

    public boolean isAssistantMessage() {
        return MessageRole.ASSISTANT.equals(this.role);
    }

    public boolean isSystemMessage() {
        return MessageRole.SYSTEM.equals(this.role);
    }

    public void addAttachment(String type, String name, String content) {
        Map<String, Object> attachment = Map.of(
                "type", type,
                "name", name,
                "content", content,
                "timestamp", LocalDateTime.now()
        );
        
        if (this.attachments == null) {
            this.attachments = List.of(attachment);
        } else {
            this.attachments = List.copyOf(
                    java.util.stream.Stream.concat(
                            this.attachments.stream(),
                            java.util.stream.Stream.of(attachment)
                    ).toList()
            );
        }
    }

    public void setParam(String key, Object value) {
        if (this.params == null) {
            this.params = new java.util.HashMap<>();
        }
        this.params.put(key, value);
    }

    public Object getParam(String key) {
        return this.params != null ? this.params.get(key) : null;
    }
}