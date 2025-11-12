package com.yunke.backend.user.domain.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_points", 
    uniqueConstraints = @UniqueConstraint(columnNames = "user_id"),
    indexes = {
        @Index(name = "idx_level", columnList = "level, total_points"),
        @Index(name = "idx_reputation", columnList = "reputation")
    }
)
@Data
public class UserPoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", length = 50, nullable = false, unique = true)
    private String userId;
    
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;
    
    @Column(name = "current_points", nullable = false)
    private Integer currentPoints = 0;
    
    @Column(name = "level", nullable = false)
    private Integer level = 1;
    
    @Column(name = "post_count", nullable = false)
    private Integer postCount = 0;
    
    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0;
    
    @Column(name = "like_received_count", nullable = false)
    private Integer likeReceivedCount = 0;
    
    @Column(name = "essence_count", nullable = false)
    private Integer essenceCount = 0;
    
    @Column(name = "reputation", nullable = false)
    private Integer reputation = 0;
    
    @Column(name = "last_sign_in_date")
    private LocalDateTime lastSignInDate;
    
    @Column(name = "continuous_sign_in_days")
    private Integer continuousSignInDays = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
