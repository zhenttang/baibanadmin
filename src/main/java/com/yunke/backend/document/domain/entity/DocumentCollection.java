package com.yunke.backend.document.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 文档收藏实体类
 */
@Entity
@Table(name = "document_collections")
@Data
@EqualsAndHashCode(callSuper = false)
public class DocumentCollection {
    
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
     * 收藏夹名称
     */
    @Column(name = "collection_name", length = 100)
    private String collectionName;

    /**
     * 收藏夹ID
     */
    @Column(name = "folder_id")
    private Integer folderId;

    /**
     * 收藏备注
     */
    @Column(name = "notes", length = 500)
    private String notes;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}