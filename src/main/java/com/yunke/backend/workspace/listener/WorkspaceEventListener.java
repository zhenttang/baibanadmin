package com.yunke.backend.workspace.listener;

import com.yunke.backend.workspace.event.WorkspaceCreatedEvent;
import com.yunke.backend.document.service.RootDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 工作空间事件监听器
 * 处理工作空间相关的事件，如创建根文档等
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceEventListener {

    private final RootDocumentService rootDocumentService;

    /**
     * 监听工作空间创建事件，自动创建根文档
     * 使用现有的通用任务调度器进行异步处理
     */
    @EventListener
    @Async("generalTaskScheduler")
    public void handleWorkspaceCreated(WorkspaceCreatedEvent event) {
        String workspaceId = event.workspace().getId();
        String creatorUserId = event.creatorUserId();
        
        log.info("🏠 [WORKSPACE-EVENT] 收到工作空间创建事件: workspaceId='{}', creator='{}'", 
                workspaceId, creatorUserId);
        
        try {
            // 为新创建的工作空间创建根文档
            // 在Spring异步方法中，直接调用block()来同步执行Reactor操作
            Boolean created = rootDocumentService.createRootDocument(workspaceId, creatorUserId)
                    .block(); // 同步等待结果
            
            if (created != null && created) {
                log.info("🎉 [WORKSPACE-EVENT] 根文档创建成功: workspaceId='{}', name='{}'", 
                        workspaceId, event.workspace().getName());
            } else {
                log.warn("⚠️ [WORKSPACE-EVENT] 根文档创建跳过（已存在）: workspaceId='{}', name='{}'", 
                        workspaceId, event.workspace().getName());
            }
                    
        } catch (Exception e) {
            log.error("❌ [WORKSPACE-EVENT] 根文档创建失败: workspaceId='{}', name='{}', error={}", 
                    workspaceId, event.workspace().getName(), e.getMessage(), e);
        }
    }
}