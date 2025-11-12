package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.Workspace;

@Entity
@Table(name = "updates")
@IdClass(Update.UpdateId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Update {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Id
    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Lob
    @Column(name = "`blob`", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] blob;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Workspace workspace;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String id;
        private Integer seq;
    }
} 