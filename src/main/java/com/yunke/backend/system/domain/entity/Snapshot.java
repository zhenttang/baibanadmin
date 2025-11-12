package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.yunke.backend.user.domain.entity.User;

@Entity
@Table(
    name = "snapshots",
    indexes = {
        @Index(name = "idx_snapshot_workspace_updated", columnList = "workspace_id, updated_at")
    }
)
@IdClass(Snapshot.SnapshotId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Snapshot {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "id", nullable = false)  // 修改为 'id' 而不是 'guid'
    private String id;

    @Lob
    @Column(name = "`blob`", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] blob;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] state;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column
    @Builder.Default
    private Integer seq = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User updatedByUser;

    // 暂时注释掉embeddings关系避免字段映射问题
    // @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    // @Builder.Default
    // private List<AiWorkspaceEmbedding> embedding = new ArrayList<>();

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SnapshotId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String id;
        
        public void setWorkspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
        }
        
        public void setId(String id) {
            this.id = id;
        }
    }
} 