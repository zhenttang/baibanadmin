package com.yunke.backend.workspace.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 工作空间ID映射实体
 * 存储nanoid短格式和UUID格式之间的映射关系
 */
@Entity
@Table(name = "workspace_id_mapping", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "short_id"),
           @UniqueConstraint(columnNames = "uuid_id")
       },
       indexes = {
           @Index(name = "idx_short_id", columnList = "short_id"),
           @Index(name = "idx_uuid_id", columnList = "uuid_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceIdMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 短格式ID (nanoid)
     * 例如: jqTFqzCl20mSOklIJnvKb
     */
    @Column(name = "short_id", nullable = false, unique = true, length = 21)
    private String shortId;
    
    /**
     * UUID格式ID
     * 例如: 09056edd-a08a-40b5-a420-0205a8514ff2
     */
    @Column(name = "uuid_id", nullable = false, unique = true, length = 36)
    private String uuidId;
    
    /**
     * 映射描述（可选）
     */
    @Column(name = "description", length = 255)
    private String description;
    
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
     * 是否启用（软删除标记）
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}