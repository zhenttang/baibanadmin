package com.yunke.backend.system.domain.entity;

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
@Table(name = "app_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    @Id
    @Column(nullable = false)
    private String id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> value;

    @Column(name = "last_updated_by")
    private String lastUpdatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 关系映射
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User lastUpdatedByUser;
} 