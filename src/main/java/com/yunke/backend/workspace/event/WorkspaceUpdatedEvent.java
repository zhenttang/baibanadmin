package com.yunke.backend.workspace.event;

import com.yunke.backend.workspace.domain.entity.Workspace;

/**
 * 工作空间更新事件
 * 当工作空间信息被更新后发布此事件
 */
public record WorkspaceUpdatedEvent(
    Workspace workspace,
    String updaterUserId
) {
    
    /**
     * 获取工作空间ID
     */
    public String getWorkspaceId() {
        return workspace.getId();
    }
    
    /**
     * 获取工作空间名称
     */
    public String getWorkspaceName() {
        return workspace.getName();
    }
}