package com.yunke.backend.workspace.domain.entity;

import com.yunke.backend.community.enums.CommunityPermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "workspace_pages")
@IdClass(WorkspaceDoc.WorkspaceDocId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDoc {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "page_id", nullable = false)
    private String docId;

    @Column(name = "public", nullable = false)
    @Builder.Default
    private boolean public_ = false;

    @Column(name = "default_role", nullable = false, columnDefinition = "SMALLINT")
    @Builder.Default
    private Integer defaultRole = 30; // Manager角色

    @Column(name = "mode", nullable = false, columnDefinition = "SMALLINT")
    @Builder.Default
    private Integer mode = 0; // Page/Edgeless模式

    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String summary;
    
    @Column(name = "public_mode", length = 20)
    private String publicMode; // page/edgeless
    
    @Column(name = "public_permission", length = 50)
    private String publicPermission; // read-only/append-only

  /**
   * Optional bitmask for default permissions of workspace members on this doc.
   * If null, server will fall back to legacy defaultRole mapping.
   */
  @Column(name = "default_permission_mask")
  private Integer defaultPermissionMask;
    
    // 注意：根据技术文档要求，移除binary_data字段
    // 文档内容现在存储在snapshots表中，此表仅存储元数据

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", insertable = false, updatable = false)
    private Workspace workspace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // 社区功能相关字段
    @Column(name = "community_shared")
    @Builder.Default
    private Boolean communityShared = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_permission")
    private CommunityPermission communityPermission;

    @Column(name = "community_shared_at")
    private LocalDateTime communitySharedAt;

    @Column(name = "community_title", length = 200)
    private String communityTitle;

    @Column(name = "community_description", columnDefinition = "TEXT")
    private String communityDescription;

    @Column(name = "community_view_count")
    @Builder.Default
    private Integer communityViewCount = 0;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceDocId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String docId;
    }

    /**
     * 获取文档ID
     */
    public String getId() {
        return docId;
    }
    
    /**
     * 获取文档ID
     */
    public String getDocId() {
        return docId;
    }
    
    /**
     * 设置文档ID
     */
    public void setId(String id) {
        this.docId = id;
    }
    
    /**
     * 获取是否公开
     */
    public Boolean getIsPublic() {
        return public_;
    }
    
    /**
     * 获取是否公开
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
     * 设置是否公开
     */
    public void setIsPublic(boolean isPublic) {
        this.public_ = isPublic;
    }
    
    /**
     * 获取是否被封禁
     */
    public Boolean getBlocked() {
        return blocked;
    }
    
    /**
     * 获取创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt != null ? LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()) : null;
    }
    
    /**
     * 获取更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt != null ? LocalDateTime.ofInstant(updatedAt, ZoneId.systemDefault()) : null;
    }
    
    /**
     * 设置创建时间
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 设置更新时间
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 获取公开模式
     */
    public String getPublicMode() {
        return publicMode;
    }
    
    /**
     * 设置公开模式
     */
    public void setPublicMode(String publicMode) {
        this.publicMode = publicMode;
    }
    
    /**
     * 获取公开权限
     */
    public String getPublicPermission() {
        return publicPermission;
    }
    
    /**
     * 设置公开权限
     */
    public void setPublicPermission(String publicPermission) {
        this.publicPermission = publicPermission;
    }
    
    /**
     * 检查文档是否已分享到社区
     */
    public boolean isSharedToCommunity() {
        return Boolean.TRUE.equals(communityShared);
    }

    /**
     * 分享文档到社区
     */
    public void shareToCommunity(CommunityPermission permission, String title, String description) {
        this.communityShared = true;
        this.communityPermission = permission;
        this.communityTitle = title;
        this.communityDescription = description;
        this.communitySharedAt = LocalDateTime.now();
    }

    /**
     * 取消文档在社区的分享
     */
    public void unshareFromCommunity() {
        this.communityShared = false;
        this.communityPermission = null;
        this.communitySharedAt = null;
        this.communityTitle = null;
        this.communityDescription = null;
    }

    /**
     * 增加社区浏览次数
     */
    public void incrementViewCount() {
        this.communityViewCount = (this.communityViewCount == null ? 0 : this.communityViewCount) + 1;
    }

    /**
     * 更新社区权限
     */
    public void updateCommunityPermission(CommunityPermission permission) {
        if (Boolean.TRUE.equals(communityShared)) {
            this.communityPermission = permission;
        }
    }

    /**
     * 更新社区信息
     */
    public void updateCommunityInfo(String title, String description) {
        if (Boolean.TRUE.equals(communityShared)) {
            this.communityTitle = title;
            this.communityDescription = description;
        }
    }
    
    /**
     * 文档模式枚举
     */
    public enum DocMode {
        PAGE(0),
        EDGELESS(1);
        
        private final int value;
        
        DocMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static DocMode fromValue(int value) {
            for (DocMode mode : DocMode.values()) {
                if (mode.value == value) {
                    return mode;
                }
            }
            return PAGE; // 默认为页面模式
        }
    }
} 