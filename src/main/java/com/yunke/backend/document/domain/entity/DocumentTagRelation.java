package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_tag_relations",
    uniqueConstraints = @UniqueConstraint(name = "uk_doc_tag", columnNames = {"document_id", "tag_id"}),
    indexes = {
        @Index(name = "idx_tag_id", columnList = "tag_id")
    }
)
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentTagRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", length = 50, nullable = false)
    private String documentId;

    @Column(name = "tag_id", nullable = false)
    private Integer tagId;

    @Column(name = "entity_type", length = 20, nullable = false)
    private String entityType = "DOCUMENT"; // DOCUMENT or POST

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
