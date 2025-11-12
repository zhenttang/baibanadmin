package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_comments", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id, created_at"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_parent_id", columnList = "parent_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", length = 50, nullable = false)
    private String documentId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    @Column(name = "user_avatar", length = 500)
    private String userAvatar;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "is_author")
    private Boolean isAuthor = false;

    @Column(name = "status", length = 20)
    private String status = "normal";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
