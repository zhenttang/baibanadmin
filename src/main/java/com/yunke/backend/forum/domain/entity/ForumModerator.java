package com.yunke.backend.forum.domain.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_moderators", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"forum_id", "user_id"}),
    indexes = {
        @Index(name = "idx_forum_id", columnList = "forum_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
    }
)
@Data
public class ForumModerator {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "forum_id", nullable = false)
    private Long forumId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private ModeratorRole role = ModeratorRole.DEPUTY;
    
    @Column(name = "permissions", columnDefinition = "JSON")
    private String permissions;
    
    @Column(name = "appointed_by", length = 50)
    private String appointedBy;
    
    @CreationTimestamp
    @Column(name = "appointed_at")
    private LocalDateTime appointedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ModeratorRole {
        CHIEF,
        DEPUTY
    }
}
