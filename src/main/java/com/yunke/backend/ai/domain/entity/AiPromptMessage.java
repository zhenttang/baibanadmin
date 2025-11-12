package com.yunke.backend.ai.domain.entity;



import com.yunke.backend.ai.enums.AiPromptRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
    name = "ai_prompts_messages", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_prompt_message_idx", columnNames = {"prompt_id", "idx"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptMessage {

    @Column(name = "prompt_id", nullable = false)
    private Integer promptId;

    @Column(nullable = false)
    private Integer idx;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiPromptRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> attachments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> params;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AiPrompt prompt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
} 