package com.yunke.backend.workspace.event;

import com.yunke.backend.workspace.domain.entity.Workspace;

/**
 * 工作空间创建事件
 * 当工作空间成功创建后发布此事件
 */
public record WorkspaceCreatedEvent(
    Workspace workspace,
    String creatorUserId
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