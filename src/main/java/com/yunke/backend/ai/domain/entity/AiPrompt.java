package com.yunke.backend.ai.domain.entity;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "ai_prompts_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 32, nullable = false, unique = true)
    private String name;

    @Column
    private String action;

    @Column(nullable = false)
    private String model;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "optional_models", columnDefinition = "text[]")
    @Builder.Default
    private List<String> optionalModels = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> config;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean modified = false;

    @OneToMany(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiPromptMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiSession> sessions = new ArrayList<>();
} 