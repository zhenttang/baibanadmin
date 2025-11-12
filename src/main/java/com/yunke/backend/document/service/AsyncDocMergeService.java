package com.yunke.backend.document.service;

import com.yunke.backend.storage.model.DocStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 异步文档合并服务 - 对应开源AFFiNE的JobQueue机制
 * 
 * 核心功能：
 * 1. 调度文档更新的异步合并任务
 * 2. 避免频繁的实时合并操作，提高性能
 * 3. 确保同一文档只有一个合并任务在执行
 * 
 * 注意：为了避免循环依赖，这个服务不直接操作存储适配器，
 * 而是通过触发合并任务，让存储适配器的getDoc方法执行合并逻辑
 */
@Service
public class AsyncDocMergeService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncDocMergeService.class);
    
    @Autowired
    @Qualifier("docMergeTaskScheduler")
    private TaskScheduler taskScheduler;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    // 存储已调度的合并任务，key格式: "spaceId:docId"
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledMerges = new ConcurrentHashMap<>();
    
    /**
     * 调度合并任务（对应开源的queue.add逻辑）
     * 
     * @param spaceId 工作空间ID
     * @param docId 文档ID
     * @param delayMs 延迟时间（毫秒）
     */
    public void scheduleMerge(String spaceId, String docId, long delayMs) {
        String jobId = String.format("doc:merge-pending-updates:%s:%s", spaceId, docId);
        
        logger.debug("调度文档合并任务: jobId={}, 延迟={}ms", jobId, delayMs);
        
        // 取消之前的调度（保持最新的调度）
        ScheduledFuture<?> existingTask = scheduledMerges.get(jobId);
        if (existingTask != null && !existingTask.isDone()) {
            logger.debug("取消之前的合并任务: jobId={}", jobId);
            existingTask.cancel(false);
        }
        
        // 调度新的合并任务
        try {
            ScheduledFuture<?> newTask = taskScheduler.schedule(
                () -> triggerMerge(spaceId, docId),
                Instant.now().plusMillis(delayMs)
            );
            
            scheduledMerges.put(jobId, newTask);
            logger.info("文档合并任务已调度: jobId={}, 延迟={}ms", jobId, delayMs);
            
        } catch (Exception e) {
            logger.error("调度文档合并任务失败: jobId={}", jobId, e);
        }
    }
    
    /**
     * 立即触发文档合并（用于测试或紧急情况）
     */
    public void mergeImmediately(String spaceId, String docId) {
        logger.info("立即触发文档合并: spaceId={}, docId={}", spaceId, docId);
        triggerMerge(spaceId, docId);
    }
    
    /**
     * 取消指定文档的合并任务
     */
    public boolean cancelMerge(String spaceId, String docId) {
        String jobId = String.format("doc:merge-pending-updates:%s:%s", spaceId, docId);
        
        ScheduledFuture<?> task = scheduledMerges.get(jobId);
        if (task != null && !task.isDone()) {
            boolean cancelled = task.cancel(false);
            if (cancelled) {
                scheduledMerges.remove(jobId);
                logger.info("文档合并任务已取消: jobId={}", jobId);
            }
            return cancelled;
        }
        
        return false;
    }
    
    /**
     * 获取待执行的合并任务数量
     */
    public int getPendingMergeCount() {
        return (int) scheduledMerges.values().stream()
            .filter(task -> !task.isDone())
            .count();
    }
    
    /**
     * 触发文档合并
     * 
     * 注意：这里不直接操作存储适配器，而是发布事件让其他组件处理
     * 这样可以避免循环依赖问题
     */
    private void triggerMerge(String spaceId, String docId) {
        String jobId = String.format("doc:merge-pending-updates:%s:%s", spaceId, docId);
        
        logger.info("触发文档合并: spaceId={}, docId={}", spaceId, docId);
        
        try {
            // 发布文档合并事件，让存储适配器或其他组件处理
            DocMergeRequestEvent event = new DocMergeRequestEvent(spaceId, docId);
            applicationEventPublisher.publishEvent(event);
            
            logger.info("✅ 文档合并事件已发布: spaceId={}, docId={}", spaceId, docId);
            
        } catch (Exception e) {
            logger.error("文档合并触发失败: spaceId={}, docId={}", spaceId, docId, e);
            
        } finally {
            // 清理调度记录
            scheduledMerges.remove(jobId);
            logger.debug("合并任务记录已清理: jobId={}", jobId);
        }
    }
    
    /**
     * 清理已完成的任务（定期清理方法）
     */
    public void cleanupCompletedTasks() {
        int initialSize = scheduledMerges.size();
        
        scheduledMerges.entrySet().removeIf(entry -> entry.getValue().isDone());
        
        int cleanedCount = initialSize - scheduledMerges.size();
        if (cleanedCount > 0) {
            logger.debug("清理了{}个已完成的合并任务", cleanedCount);
        }
    }
    
    /**
     * 获取服务状态信息（用于监控）
     */
    public String getServiceStatus() {
        int totalTasks = scheduledMerges.size();
        int pendingTasks = getPendingMergeCount();
        int completedTasks = totalTasks - pendingTasks;
        
        return String.format("AsyncDocMergeService状态: 总任务=%d, 待执行=%d, 已完成=%d", 
                           totalTasks, pendingTasks, completedTasks);
    }
    
    /**
     * 文档合并请求事件
     */
    public static class DocMergeRequestEvent {
        private final String spaceId;
        private final String docId;
        private final long timestamp;
        
        public DocMergeRequestEvent(String spaceId, String docId) {
            this.spaceId = spaceId;
            this.docId = docId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getSpaceId() {
            return spaceId;
        }
        
        public String getDocId() {
            return docId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("DocMergeRequestEvent{spaceId='%s', docId='%s', timestamp=%d}", 
                               spaceId, docId, timestamp);
        }
    }
}