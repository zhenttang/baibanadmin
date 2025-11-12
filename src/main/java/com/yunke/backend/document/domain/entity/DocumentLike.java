package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 文档点赞实体类
 */
@Entity
@Table(name = "document_likes")
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentLike {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 文档ID
     */
    @Column(name = "document_id")
    private String documentId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private String userId;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}