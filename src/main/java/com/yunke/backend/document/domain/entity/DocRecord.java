package com.yunke.backend.document.domain.entity;

import com.yunke.backend.common.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档记录实体 - 完全参考AFFiNE的DocRecord实现
 * 
 * 对应AFFiNE中的 DocRecord interface:
 * - spaceId: string
 * - docId: string  
 * - bin: Uint8Array (YJS编码的文档数据)
 * - timestamp: number
 * - editor?: string
 */
@Entity
@Table(name = "doc_snapshots", indexes = {
    @Index(name = "idx_doc_space_id", columnList = "space_id"),
    @Index(name = "idx_doc_doc_id", columnList = "doc_id"), 
    @Index(name = "idx_doc_timestamp", columnList = "timestamp"),
    @Index(name = "idx_doc_space_doc", columnList = "space_id,doc_id", unique = true)
})
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DocRecord extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * 空间ID - 对应AFFiNE的spaceId
     * 可以是workspace ID或userspace ID
     */
    @Column(name = "space_id", nullable = false, length = 100)
    private String spaceId;
    
    /**
     * 文档ID - 对应AFFiNE的docId  
     * YJS文档的唯一标识符
     */
    @Column(name = "doc_id", nullable = false, length = 100)
    private String docId;
    
    /**
     * YJS编码的文档数据 - 对应AFFiNE的bin: Uint8Array
     * 存储YJS encodeStateAsUpdate的结果
     */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] bin;
    
    /**
     * 文档时间戳 - 对应AFFiNE的timestamp
     * 用于版本控制和同步
     */
    @Column(nullable = false)
    private Long timestamp;
    
    /**
     * 编辑者ID - 对应AFFiNE的editor?
     * 最后编辑该文档的用户ID
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
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 检查文档数据是否为空
     * 参考AFFiNE的isEmptyBin方法
     */
    public boolean isEmptyBin() {
        if (bin == null || bin.length == 0) {
            return true;
        }
        // 0x0 for state vector
        if (bin.length == 1 && bin[0] == 0) {
            return true;
        }
        // 0x00 for update  
        if (bin.length == 2 && bin[0] == 0 && bin[1] == 0) {
            return true;
        }
        return false;
    }
}