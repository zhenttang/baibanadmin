package com.yunke.backend.document.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 文档事件监听器
 * 处理文档相关的事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocEventListener {

    /**
     * 处理文档创建事件
     * 
     * @param event 文档创建事件
     */
    @EventListener
    public void handleDocCreatedEvent(DocCreatedEvent event) {
        log.info("文档创建事件: workspaceId={}, docId={}, editorId={}", 
                event.getWorkspaceId(), event.getDocId(), event.getEditorId());
        
        // 这里可以添加额外的处理逻辑，例如：
        // 1. 发送通知
        // 2. 更新统计信息
        // 3. 触发索引更新
        // 4. 记录审计日志
        // 等等
    }
} 