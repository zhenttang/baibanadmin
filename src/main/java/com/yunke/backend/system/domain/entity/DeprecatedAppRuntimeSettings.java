package com.yunke.backend.system.domain.entity;


import com.yunke.backend.system.enums.RuntimeConfigType;
import com.yunke.backend.user.domain.entity.User;
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
import java.util.Map;

@Entity
@Table(name = "deprecated_app_runtime_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeprecatedAppRuntimeSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RuntimeConfigType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_updated_by_user_id")
    private String lastUpdatedByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by_user_id", insertable = false, updatable = false)
    private User lastUpdatedByUser;
} 