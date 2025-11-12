package com.yunke.backend.workspace.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.Workspace;

/**
 * 工作空间用户角色实体
 */
@Entity
@Table(name = "workspace_user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceUserRole {

    @Id
    private String id;

    @Column(name = "workspace_id")
    private String workspaceId;
    
    @Column(name = "user_id") 
    private String userId;

    @Enumerated(EnumType.STRING)
    private WorkspaceRole type;

    @Enumerated(EnumType.STRING)
    private WorkspaceMemberStatus status;

    @Enumerated(EnumType.STRING)
    private WorkspaceMemberSource source;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant acceptedAt;
    private String inviterId;  // 添加邀请者ID字段

    // JPA 关系映射
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", insertable = false, updatable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", insertable = false, updatable = false)
    private User inviter;
    
    /**
     * 创建前自动生成ID
     */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        
        this.updatedAt = Instant.now();
    }
    
    /**
     * 更新前设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * 获取角色
     */
    public WorkspaceRole getRole() {
        return type;
    }

    /**
     * 工作空间角色枚举
     */
    public enum WorkspaceRole {
        OWNER,
        ADMIN,
        COLLABORATOR,
        EXTERNAL;

        /**
         * 检查是否具有所有者权限
         */
        public boolean hasOwnerPermission() {
            return this == OWNER;
        }

        /**
         * 检查是否具有管理员权限
         */
        public boolean hasAdminPermission() {
            return this == OWNER || this == ADMIN;
        }

        /**
         * 检查是否具有协作者权限
         */
        public boolean hasCollaboratorPermission() {
            return this == OWNER || this == ADMIN || this == COLLABORATOR;
        }
    }

    /**
     * 工作空间成员状态枚举
     */
    public enum WorkspaceMemberStatus {
        ACCEPTED,  // 已接受邀请
        PENDING,   // 待接受邀请
        REJECTED   // 已拒绝邀请
    }

    /**
     * 工作空间成员来源枚举
     */
    public enum WorkspaceMemberSource {
        EMAIL,      // 邮件邀请
        LINK,       // 链接邀请
        SELF_JOIN   // 自主加入
    }
} 