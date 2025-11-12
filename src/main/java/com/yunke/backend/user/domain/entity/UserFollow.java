package com.yunke.backend.user.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 */
@Entity
@Table(name = "user_follows")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserFollow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关注者ID
     */
    @Column(name = "follower_id")
    private String followerId;
    
    /**
     * 被关注者ID
     */
    @Column(name = "following_id")
    private String followingId;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}