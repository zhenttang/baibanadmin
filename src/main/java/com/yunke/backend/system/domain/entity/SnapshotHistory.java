package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.yunke.backend.user.domain.entity.User;

@Entity
@Table(name = "snapshot_histories")
@IdClass(SnapshotHistory.SnapshotHistoryId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotHistory {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "`id`", nullable = false)
    private String id;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Lob
    @Column(name = "`blob`", nullable = false)
    private byte[] blob;

    @Lob
    @Column(name = "`state`")
    private byte[] state;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "created_by")
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotHistoryId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String id;
        private Long timestamp;
    }
} 