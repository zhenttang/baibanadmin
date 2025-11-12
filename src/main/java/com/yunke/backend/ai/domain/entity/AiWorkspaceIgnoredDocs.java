package com.yunke.backend.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.yunke.backend.workspace.domain.entity.Workspace;

@Entity
@Table(name = "ai_workspace_ignored_docs")
@IdClass(AiWorkspaceIgnoredDocs.AiWorkspaceIgnoredDocsId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiWorkspaceIgnoredDocs {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "doc_id", nullable = false)
    private String docId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Workspace workspace;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiWorkspaceIgnoredDocsId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String docId;
    }
} 