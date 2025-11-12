package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_attachments", indexes = {
        @Index(name = "idx_post_id", columnList = "post_id, created_at"),
        @Index(name = "idx_uploader_id", columnList = "uploader_id")
})
@Data
public class PostAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "file_name", length = 200, nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 50, nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploader_id", length = 50, nullable = false)
    private String uploaderId;

    @Column(name = "uploader_name", length = 100)
    private String uploaderName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

