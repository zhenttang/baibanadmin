package com.yunke.backend.document.domain.entity;

import com.yunke.backend.common.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档更新实体 - 完全参考AFFiNE的DocUpdate实现
 * 
 * 对应AFFiNE中的 DocUpdate interface:
 * - bin: Uint8Array (YJS更新数据)
 * - timestamp: number
 * - editor?: string
 * 
 * 用于存储文档的增量更新，在合并到快照前的临时存储
 */
@Entity
@Table(name = "doc_updates", indexes = {
    @Index(name = "idx_update_space_id", columnList = "space_id"),
    @Index(name = "idx_update_doc_id", columnList = "doc_id"),
    @Index(name = "idx_update_timestamp", columnList = "timestamp"),
    @Index(name = "idx_update_space_doc", columnList = "space_id,doc_id"),
    @Index(name = "idx_update_seq", columnList = "space_id,doc_id,timestamp")
})
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DocUpdate extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * 空间ID - 对应文档所属的空间
     */
    @Column(name = "space_id", nullable = false, length = 100)
    private String spaceId;
    
    /**
     * 文档ID - 对应要更新的文档
     */
    @Column(name = "doc_id", nullable = false, length = 100) 
    private String docId;
    
    /**
     * YJS更新数据 - 对应AFFiNE的bin: Uint8Array
     * 存储YJS增量更新的二进制数据
     */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] bin;
    
    /**
     * 更新时间戳 - 对应AFFiNE的timestamp
     * 用于排序和合并更新
     */
    @Column(nullable = false)
    private Long timestamp;
    
    /**
     * 编辑者ID - 对应AFFiNE的editor?
     * 执行此次更新的用户ID
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
     * 是否已合并到快照
     * 用于标记该更新是否已经被合并到DocRecord中
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean merged = false;
    
    /**
     * 检查更新数据是否为空
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