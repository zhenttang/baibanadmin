package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 通用实体点赞记录
 * 统一存储帖子/回复/文档等点赞关系，避免为每种类型建立独立表
 */
@Entity
@Table(name = "entity_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_entity", columnNames = {"user_id", "entity_type", "entity_id"})
        },
        indexes = {
                @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_user", columnList = "user_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = false)
public class EntityLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 20, nullable = false)
    private EntityType entityType;

    // 统一使用字符串存储，保证对UUID/数值ID的兼容
    @Column(name = "entity_id", length = 100, nullable = false)
    private String entityId;

    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EntityType {
        POST,
        REPLY,
        DOCUMENT
    }
}

