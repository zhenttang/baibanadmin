package com.yunke.backend.system.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 通用实体收藏记录
 * 使用 entity_type 区分收藏的目标类型（POST / DOCUMENT / ...）
 */
@Entity
@Table(
    name = "entity_collections",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_entity", columnNames = {"user_id", "entity_type", "entity_id"})
    },
    indexes = {
        @Index(name = "idx_user_entity_type", columnList = "user_id, entity_type, created_at")
    }
)
@Data
@EqualsAndHashCode(callSuper = false)
public class EntityCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** 实体类型，例如：POST、DOCUMENT 等 */
    @Column(name = "entity_type", length = 20, nullable = false)
    private String entityType;

    /** 实体ID（帖子ID、文档ID等） */
    @Column(name = "entity_id", length = 100, nullable = false)
    private String entityId;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

