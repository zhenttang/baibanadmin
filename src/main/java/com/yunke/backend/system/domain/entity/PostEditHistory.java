package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_edit_history", indexes = {
        @Index(name = "idx_post_id", columnList = "post_id, edited_at"),
        @Index(name = "idx_editor_id", columnList = "editor_id")
})
@Data
public class PostEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;

    @Column(name = "old_title", length = 200)
    private String oldTitle;

    @Lob
    @Column(name = "old_content", columnDefinition = "LONGTEXT")
    private String oldContent;

    @Column(name = "editor_id", length = 50)
    private String editorId;

    @Column(name = "editor_name", length = 100)
    private String editorName;

    @CreationTimestamp
    @Column(name = "edited_at", nullable = false, updatable = false)
    private LocalDateTime editedAt;
}

