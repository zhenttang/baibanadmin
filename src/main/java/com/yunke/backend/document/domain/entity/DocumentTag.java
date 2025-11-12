package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_tags", indexes = {
    @Index(name = "idx_use_count", columnList = "use_count")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 30, nullable = false, unique = true)
    private String name;

    @Column(name = "slug", length = 30, nullable = false, unique = true)
    private String slug;

    @Column(name = "color", length = 20)
    private String color = "#999999";

    @Column(name = "use_count")
    private Integer useCount = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
