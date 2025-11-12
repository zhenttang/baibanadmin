package com.yunke.backend.forum.domain.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_replies", indexes = {
    @Index(name = "idx_post_id", columnList = "post_id, floor"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
public class ForumReply {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "user_avatar", length = 500)
    private String userAvatar;
    
    @Column(name = "floor", nullable = false)
    private Integer floor;
    
    @Column(name = "parent_id")
    private Long parentId = 0L;
    
    @Column(name = "reply_to_user_id", length = 50)
    private String replyToUserId;
    
    @Column(name = "reply_to_user_name", length = 100)
    private String replyToUserName;
    
    @Lob
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "content_type", length = 20)
    private String contentType = "markdown";
    
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    @Column(name = "is_author")
    private Boolean isAuthor = false;
    
    @Column(name = "is_best_answer")
    private Boolean isBestAnswer = false;
    
    @Column(name = "status", length = 20)
    private String status = "normal";
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Transient
    private Boolean isLiked;
}
