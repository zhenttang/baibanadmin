package com.yunke.backend.forum.domain.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_posts", indexes = {
    @Index(name = "idx_forum_id", columnList = "forum_id, created_at"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_sticky_essence", columnList = "is_sticky, is_essence, last_reply_at"),
    @Index(name = "idx_hot_score", columnList = "hot_score"),
    @Index(name = "idx_last_reply", columnList = "last_reply_at")
})
@Data
public class ForumPost {
    
    @Id
    @Column(length = 50)
    private String id;
    
    @Column(name = "forum_id", nullable = false)
    private Long forumId;
    
    @Column(name = "forum_name", length = 100)
    private String forumName;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "user_avatar", length = 500)
    private String userAvatar;
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;
    
    @Column(name = "content_type", length = 20)
    private String contentType = "markdown";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PostStatus status = PostStatus.NORMAL;
    
    @Column(name = "is_sticky")
    private Boolean isSticky = false;
    
    @Column(name = "is_essence")
    private Boolean isEssence = false;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @Column(name = "is_hot")
    private Boolean isHot = false;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    @Column(name = "reply_count")
    private Integer replyCount = 0;
    
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    @Column(name = "collect_count")
    private Integer collectCount = 0;
    
    @Column(name = "hot_score", precision = 10, scale = 2)
    private BigDecimal hotScore = BigDecimal.ZERO;
    
    @Column(name = "quality_score", precision = 3, scale = 2)
    private BigDecimal qualityScore = BigDecimal.ZERO;
    
    @Column(name = "last_reply_at")
    private LocalDateTime lastReplyAt;
    
    @Column(name = "last_reply_user_id", length = 50)
    private String lastReplyUserId;
    
    @Column(name = "last_reply_user_name", length = 100)
    private String lastReplyUserName;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Transient
    private Boolean isLiked;
    
    @Transient
    private Boolean isCollected;
    
    public enum PostStatus {
        NORMAL,
        LOCKED,
        DELETED,
        HIDDEN
    }
}
