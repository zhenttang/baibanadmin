package com.yunke.backend.workspace.domain.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.workspace.domain.entity.Workspace;

@Entity
@Table(
    name = "workspace_features",
    indexes = {
        @Index(name = "idx_workspace_feature_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_workspace_feature_name", columnList = "name"),
        @Index(name = "idx_workspace_feature_feature_id", columnList = "feature_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "feature_id", nullable = false)
    private Integer featureId;

    @Column(name = "name", nullable = false)
    @Builder.Default
    private String name = "";

    @Column(name = "type", nullable = false)
    @Builder.Default
    private Integer type = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    @Builder.Default
    private Map<String, Object> configs = Map.of();

    @Column(nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean activated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Feature feature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Workspace workspace;
} 