package com.yunke.backend.ai.domain.entity;

import com.yunke.backend.system.domain.entity.Snapshot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_workspace_embeddings")
@IdClass(AiWorkspaceEmbedding.AiWorkspaceEmbeddingId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiWorkspaceEmbedding {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Id
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", columnDefinition = "real[]")
    private float[] embedding;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "workspace_id", referencedColumnName = "workspace_id", insertable = false, updatable = false),
        @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Snapshot snapshot;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiWorkspaceEmbeddingId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String id;
        private Integer chunkIndex;
    }
} 