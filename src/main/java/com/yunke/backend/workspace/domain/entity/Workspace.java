package com.yunke.backend.workspace.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.yunke.backend.ai.domain.entity.AiWorkspaceIgnoredDocs;
import com.yunke.backend.ai.domain.entity.AiWorkspaceFiles;
import com.yunke.backend.storage.domain.entity.Blob;

@Entity
@Table(name = "workspaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Column(name = "sid", unique = true, nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sid;

    @Id
    private String id;

    @Column(name = "public", nullable = false)
    private boolean public_;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "enable_ai", nullable = false)
    @Builder.Default
    private boolean enableAi = true;

    @Column(name = "enable_url_preview", nullable = false)
    @Builder.Default
    private boolean enableUrlPreview = false;

    @Column(name = "enable_doc_embedding", nullable = false)
    @Builder.Default
    private boolean enableDocEmbedding = true;

    @Column
    private String name;

    @Column(name = "avatar_key")
    private String avatarKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean indexed = false;

    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * 获取是否公开（is前缀方法）
     */
    public Boolean getIsPublic() {
        return public_;
    }
    
    /**
     * 获取是否公开（无is前缀方法）
     */
    public Boolean getPublic() {
        return public_;
    }
    
    /**
     * 设置是否公开
     */
    public void setPublic(boolean isPublic) {
        this.public_ = isPublic;
    }
    
    /**
     * 设置更新时间
     */
    public void setUpdatedAt(Instant updatedAt) {
        // 转换Instant到LocalDateTime
        this.createdAt = updatedAt != null ? 
                LocalDateTime.ofInstant(updatedAt, ZoneId.systemDefault()) : null;
    }
    
    /**
     * 设置创建者
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * 设置更新者
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // 关系映射
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<WorkspaceFeature> features = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<WorkspaceDoc> docs = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<WorkspaceUserRole> permissions = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<WorkspaceDocUserRole> docPermissions = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Blob> blobs = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<AiWorkspaceIgnoredDocs> ignoredDocs = new ArrayList<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<AiWorkspaceFiles> embedFiles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
} 