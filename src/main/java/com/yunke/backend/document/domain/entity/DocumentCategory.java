package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_categories", indexes = {
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_sort_order", columnList = "sort_order")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "parent_id")
    private Integer parentId = 0;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "slug", length = 50, nullable = false, unique = true)
    private String slug;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
