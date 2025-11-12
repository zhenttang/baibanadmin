package com.yunke.backend.community.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 社区文档实体类
 * 扩展原有文档功能，支持社区分享、付费、关注等特性
 */
@Entity
@Table(name = "community_documents", indexes = {
    @Index(name = "idx_author_id", columnList = "author_id"),
    @Index(name = "idx_category", columnList = "category_id, subcategory_id"),
    @Index(name = "idx_workspace_source", columnList = "workspace_id, source_doc_id"),
    @Index(name = "idx_status_public", columnList = "status, is_public"),
    @Index(name = "idx_published_at", columnList = "published_at"),
    @Index(name = "idx_view_count", columnList = "view_count"),
    @Index(name = "idx_like_count", columnList = "like_count"),
    @Index(name = "idx_is_paid", columnList = "is_paid, price"),
    @Index(name = "idx_featured_sticky", columnList = "is_featured, is_sticky, published_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class CommunityDocument {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "workspace_id", length = 50, nullable = false)
    private String workspaceId;

    @Column(name = "source_doc_id", length = 50, nullable = false)
    private String sourceDocId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Lob
    @Column(name = "content_snapshot", columnDefinition = "LONGTEXT")
    private String contentSnapshot;

    @Column(name = "author_id", length = 50, nullable = false)
    private String authorId;

    @Column(name = "author_name", length = 100, nullable = false)
    private String authorName;

    @Column(name = "author_avatar", length = 500)
    private String authorAvatar;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "subcategory_id")
    private Integer subcategoryId;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "require_follow")
    private Boolean requireFollow = false;

    @Column(name = "require_purchase")
    private Boolean requirePurchase = false;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "free_preview_length")
    private Integer freePreviewLength = 500;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "collect_count")
    private Integer collectCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "share_count")
    private Integer shareCount = 0;

    @Column(name = "purchase_count")
    private Integer purchaseCount = 0;

    @Column(name = "quality_score", precision = 3, scale = 2)
    private BigDecimal qualityScore = BigDecimal.ZERO;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @Column(name = "status", length = 20)
    private String status = "published";

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "is_sticky")
    private Boolean isSticky = false;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 临时字段，不映射到数据库，用于前端显示
    @Transient
    private Boolean isLiked;

    @Transient
    private Boolean isCollected;

    @Transient
    private Boolean isFollowing;

}