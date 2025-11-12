package com.yunke.backend.document.domain.entity;

import com.yunke.backend.common.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档历史记录实体 - 完全参考AFFiNE的DocHistory实现
 * 
 * 用于存储文档的历史版本，支持版本回滚功能
 * 对应AFFiNE中的 history 表结构
 */
@Entity
@Table(name = "doc_histories", indexes = {
    @Index(name = "idx_history_space_id", columnList = "space_id"),
    @Index(name = "idx_history_doc_id", columnList = "doc_id"),
    @Index(name = "idx_history_timestamp", columnList = "timestamp"),
    @Index(name = "idx_history_space_doc", columnList = "space_id,doc_id"),
    @Index(name = "idx_history_space_doc_time", columnList = "space_id,doc_id,timestamp", unique = true)
})
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DocHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * 空间ID
     */
    @Column(name = "space_id", nullable = false, length = 100)
    private String spaceId;
    
    /**
     * 文档ID
     */
    @Column(name = "doc_id", nullable = false, length = 100)
    private String docId;
    
    /**
     * 历史版本的二进制数据
     * 存储当时的文档快照
     */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] blob;
    
    /**
     * 历史版本时间戳
     * 用于标识该历史版本的创建时间
     */
    @Column(nullable = false)
    private Long timestamp;
    
    /**
     * 创建该历史版本时的编辑者ID
     */
    @Column(name = "editor_id", length = 100)
    private String editorId;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 过期时间
     * 用于自动清理过期的历史记录
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}