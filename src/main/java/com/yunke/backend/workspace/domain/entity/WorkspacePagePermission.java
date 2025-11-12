package com.yunke.backend.workspace.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 工作空间页面权限实体
 */
@Entity
@Table(name = "workspace_page_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspacePagePermission {
    
    @Id
    private String id;
    
    private String workspaceId;
    private String pageId;
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private PagePermission permission;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * 页面权限枚举
     */
    public enum PagePermission {
        OWNER,    // 所有者
        EDITOR,   // 编辑者
        VIEWER,   // 查看者
        NONE      // 无权限
    }
} 