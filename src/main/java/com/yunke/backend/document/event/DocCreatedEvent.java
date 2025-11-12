package com.yunke.backend.document.event;

import org.springframework.context.ApplicationEvent;

/**
 * 文档创建事件
 * 当文档创建成功时触发
 */
public class DocCreatedEvent extends ApplicationEvent {

    private final String workspaceId;
    private final String docId;
    private final String editorId;

    public DocCreatedEvent(Object source, String workspaceId, String docId, String editorId) {
        super(source);
        this.workspaceId = workspaceId;
        this.docId = docId;
        this.editorId = editorId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getDocId() {
        return docId;
    }

    public String getEditorId() {
        return editorId;
    }
} 