package com.yunke.backend.forum.domain.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forums", indexes = {
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_slug", columnList = "slug"),
    @Index(name = "idx_order", columnList = "display_order"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
public class Forum {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    
    @Column(name = "slug", length = 100, nullable = false, unique = true)
    private String slug;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "icon", length = 500)
    private String icon;
    
    @Column(name = "banner", length = 500)
    private String banner;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @Column(name = "post_count")
    private Integer postCount = 0;
    
    @Column(name = "topic_count")
    private Integer topicCount = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_private")
    private Boolean isPrivate = false;
    
    @Column(name = "announcement", columnDefinition = "TEXT")
    private String announcement;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
