package com.yunke.backend.user.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.yunke.backend.system.domain.entity.Feature;

@Entity
@Table(
    name = "user_features",
    indexes = {
        @Index(name = "idx_user_feature_user_id", columnList = "user_id"),
        @Index(name = "idx_user_feature_name", columnList = "name"),
        @Index(name = "idx_user_feature_feature_id", columnList = "feature_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "feature_id", nullable = false)
    private Integer featureId;

    @Column(name = "name", nullable = false)
    @Builder.Default
    private String name = "";

    @Column(name = "type", nullable = false)
    @Builder.Default
    private Integer type = 0;

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
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
} 