package com.yunke.backend.document.service;

import com.yunke.backend.document.domain.entity.DocRecord;
import com.yunke.backend.document.domain.entity.DocUpdate;
import com.yunke.backend.document.repository.DocRecordRepository;

import com.yunke.backend.document.repository.DocUpdateRepository;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 异步文档合并任务服务 - 基于AFFiNE的异步优先级队列实现
 * 
 * 核心功能：
 * 1. 真正的异步合并任务调度
 * 2. 防重复合并机制
 * 3. 批处理优化
 * 4. 错误重试和恢复
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncDocMergeTaskService {
    
    private final DocRecordRepository docRecordRepository;
    private final DocUpdateRepository docUpdateRepository;
    private final AFFiNEDocMergeService docMergeService;
    private final DocBinaryStorageService binaryStorageService;
    
    // 待处理的合并任务队列
    private final ConcurrentLinkedQueue<MergeTask> pendingTasks = new ConcurrentLinkedQueue<>();
    
    // 正在处理的文档集合，防止重复处理
    private final ConcurrentHashMap<String, AtomicBoolean> processingDocs = new ConcurrentHashMap<>();
    
    // 失败重试计数器
    private final ConcurrentHashMap<String, Integer> retryCounters = new ConcurrentHashMap<>();
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 10;
    
    /**
     * 调度文档合并任务 - 对应AFFiNE的队列调度
     */
    @Async
    public CompletableFuture<Void> scheduleDocMerge(String spaceId, String docId, String reason) {
        String docKey = spaceId + ":" + docId;
        
        // 检查是否已在处理中
        AtomicBoolean processing = processingDocs.computeIfAbsent(docKey, k -> new AtomicBoolean(false));
        if (processing.get()) {
            log.debug("Document merge already in progress: {}", docKey);
            return CompletableFuture.completedFuture(null);
        }
        
        // 添加到待处理队列
        MergeTask task = new MergeTask(spaceId, docId, reason, System.currentTimeMillis());
        pendingTasks.offer(task);
        
        log.info("Scheduled document merge task: {} (reason: {})", docKey, reason);
        
        // 立即尝试处理任务
        return processNextTask();
    }
    
    /**
     * 处理下一个合并任务
     */
    @Async
    public CompletableFuture<Void> processNextTask() {
        MergeTask task = pendingTasks.poll();
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        String docKey = task.getSpaceId() + ":" + task.getDocId();
        AtomicBoolean processing = processingDocs.get(docKey);
        
        if (processing == null || !processing.compareAndSet(false, true)) {
            log.debug("Skipping task for document already being processed: {}", docKey);
            return CompletableFuture.completedFuture(null);
        }
        
        return performDocumentMerge(task)
            .whenComplete((result, error) -> {
                // 无论成功失败都释放锁
                processing.set(false);
                
                if (error != null) {
                    handleMergeError(task, error);
                } else {
                    // 重置重试计数器
                    retryCounters.remove(docKey);
                    log.info("Successfully merged document: {}", docKey);
                }
            });
    }
    
    /**
     * 执行文档合并 - 核心合并逻辑
     */
    @Transactional
    public CompletableFuture<Void> performDocumentMerge(MergeTask task) {
        return CompletableFuture.runAsync(() -> {
            String spaceId = task.getSpaceId();
            String docId = task.getDocId();
            String docKey = spaceId + ":" + docId;
            
            log.info("Starting document merge: {} (reason: {})", docKey, task.getReason());
            
            try {
                // 1. 获取现有快照
                DocRecord existingSnapshot = docRecordRepository
                    .findBySpaceIdAndDocId(spaceId, docId)
                    .orElse(null);

                byte[] existingData = resolveSnapshotData(existingSnapshot);

                // 2. 获取待合并的更新
                List<DocUpdate> pendingUpdates = docUpdateRepository
                    .findUnmergedUpdates(spaceId, docId);
                
                if (pendingUpdates.isEmpty()) {
                    log.debug("No pending updates found for document: {}", docKey);
                    return;
                }
                
                log.info("Found {} pending updates for document: {}", pendingUpdates.size(), docKey);

                List<DocUpdate> hydratedUpdates = pendingUpdates.stream()
                    .map(this::hydrateUpdate)
                    .toList();

                // 3. 执行合并
                byte[] mergedData = docMergeService.safelyMergeUpdates(
                    spaceId, docId, hydratedUpdates, existingData
                ).join();
                
                // 4. 验证合并结果
                if (!docMergeService.validateMergeResult(mergedData, hydratedUpdates)) {
                    throw new RuntimeException("Merge result validation failed");
                }
                
                // 5. 创建新快照
                DocRecord newSnapshot = createNewSnapshot(
                    spaceId, docId, mergedData, pendingUpdates, existingSnapshot
                );
                
                // 6. 保存新快照
                DocRecord savedSnapshot = docRecordRepository.save(newSnapshot);
                
                // 7. 标记更新为已合并
                List<Long> timestamps = pendingUpdates.stream()
                    .map(DocUpdate::getTimestamp)
                    .collect(Collectors.toList());
                
                int markedCount = docUpdateRepository.markUpdatesMerged(spaceId, docId, timestamps);
                
                log.info("Document merge completed: {} ({} updates merged, {} marked)", 
                        docKey, pendingUpdates.size(), markedCount);
                
                // 8. 记录统计信息
                logMergeStatistics(savedSnapshot, hydratedUpdates);
                
            } catch (Exception e) {
                log.error("Document merge failed: {}", docKey, e);
                throw new RuntimeException("Document merge failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 创建新的快照记录
     */
    private DocRecord createNewSnapshot(String spaceId, String docId, byte[] mergedData,
                                       List<DocUpdate> pendingUpdates, DocRecord existingSnapshot) {
        // 获取最新的时间戳和编辑者
        long latestTimestamp = pendingUpdates.stream()
            .mapToLong(DocUpdate::getTimestamp)
            .max()
            .orElse(System.currentTimeMillis());
        
        String latestEditor = pendingUpdates.get(pendingUpdates.size() - 1).getEditorId();
        
        String pointer = binaryStorageService.saveSnapshot(spaceId, docId, mergedData);

        DocRecord newSnapshot = DocRecord.builder()
                .spaceId(spaceId)
                .docId(docId)
                .bin(binaryStorageService.pointerToBytes(pointer))
                .timestamp(latestTimestamp)
                .editorId(latestEditor)
                .build();

        // 如果是更新现有快照，保留ID并释放旧指针
        if (existingSnapshot != null) {
            newSnapshot.setId(existingSnapshot.getId());
            binaryStorageService.deletePointer(existingSnapshot.getBin());
        }

        return newSnapshot;
    }
    
    /**
     * 处理合并错误
     */
    private void handleMergeError(MergeTask task, Throwable error) {
        String docKey = task.getSpaceId() + ":" + task.getDocId();
        int retryCount = retryCounters.compute(docKey, (key, count) -> count == null ? 1 : count + 1);
        
        log.error("Document merge failed: {} (attempt {}/{})", docKey, retryCount, MAX_RETRY_COUNT, error);
        
        if (retryCount < MAX_RETRY_COUNT) {
            // 重新调度任务，增加延迟
            long delay = retryCount * 5000L; // 5秒、10秒、15秒
            
            CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    pendingTasks.offer(task);
                    log.info("Rescheduled failed merge task: {} (retry {})", docKey, retryCount);
                });
        } else {
            log.error("Document merge permanently failed after {} attempts: {}", MAX_RETRY_COUNT, docKey);
            retryCounters.remove(docKey);
        }
    }
    
    // ===== 辅助方法 =====

    private byte[] resolveSnapshotData(DocRecord snapshot) {
        if (snapshot == null || snapshot.getBin() == null) {
            return new byte[0];
        }
        return binaryStorageService.resolvePointer(snapshot.getBin(), snapshot.getSpaceId(), snapshot.getDocId());
    }

    private DocUpdate hydrateUpdate(DocUpdate update) {
        if (update == null) {
            return null;
        }
        byte[] data = binaryStorageService.resolvePointer(update.getBin());
        return DocUpdate.builder()
                .id(update.getId())
                .spaceId(update.getSpaceId())
                .docId(update.getDocId())
                .bin(data)
                .timestamp(update.getTimestamp())
                .editorId(update.getEditorId())
                .createdAt(update.getCreatedAt())
                .merged(update.getMerged())
                .build();
    }

    private byte[] resolveUpdateContent(DocUpdate update) {
        if (update == null || update.getBin() == null) {
            return new byte[0];
        }
        return binaryStorageService.resolvePointer(update.getBin());
    }

    /**
     * 记录合并统计信息
     */
    private void logMergeStatistics(DocRecord snapshot, List<DocUpdate> mergedUpdates) {
        try {
            byte[] snapshotData = resolveSnapshotData(snapshot);
            AFFiNEDocMergeService.DocStats stats = docMergeService.getDocumentStats(snapshotData);

            int totalUpdateSize = mergedUpdates.stream()
                .mapToInt(update -> resolveUpdateContent(update).length)
                .sum();

            log.info("Merge statistics - Doc: {}, Updates: {} ({}→{} bytes), Result: {}",
                    snapshot.getDocId(),
                    mergedUpdates.size(),
                    totalUpdateSize,
                    stats.getBinarySize(),
                    stats);

        } catch (Exception e) {
            log.warn("Failed to collect merge statistics", e);
        }
    }
    
    /**
     * 定期处理队列中的任务 - 确保任务不会积压
     */
    @Scheduled(fixedDelay = 10000) // 每10秒检查一次
    public void processQueuedTasks() {
        if (pendingTasks.isEmpty()) {
            return;
        }
        
        log.debug("Processing queued merge tasks: {} pending", pendingTasks.size());
        
        // 批量处理任务
        for (int i = 0; i < BATCH_SIZE && !pendingTasks.isEmpty(); i++) {
            processNextTask();
        }
    }
    
    /**
     * 获取队列状态 - 用于监控
     */
    public QueueStatus getQueueStatus() {
        int pendingCount = pendingTasks.size();
        int processingCount = (int) processingDocs.values().stream()
            .mapToInt(flag -> flag.get() ? 1 : 0)
            .sum();
        int retryingCount = retryCounters.size();
        
        return new QueueStatus(pendingCount, processingCount, retryingCount);
    }
    
    /**
     * 合并任务类
     */
    public static class MergeTask {
        private final String spaceId;
        private final String docId;
        private final String reason;
        private final long createdAt;
        
        public MergeTask(String spaceId, String docId, String reason, long createdAt) {
            this.spaceId = spaceId;
            this.docId = docId;
            this.reason = reason;
            this.createdAt = createdAt;
        }
        
        public String getSpaceId() { return spaceId; }
        public String getDocId() { return docId; }
        public String getReason() { return reason; }
        public long getCreatedAt() { return createdAt; }
    }
    
    /**
     * 队列状态类
     */
    public static class QueueStatus {
        private final int pendingTasks;
        private final int processingTasks;
        private final int retryingTasks;
        
        public QueueStatus(int pendingTasks, int processingTasks, int retryingTasks) {
            this.pendingTasks = pendingTasks;
            this.processingTasks = processingTasks;
            this.retryingTasks = retryingTasks;
        }
        
        public int getPendingTasks() { return pendingTasks; }
        public int getProcessingTasks() { return processingTasks; }
        public int getRetryingTasks() { return retryingTasks; }
        
        @Override
        public String toString() {
            return String.format("QueueStatus{pending=%d, processing=%d, retrying=%d}",
                               pendingTasks, processingTasks, retryingTasks);
        }
    }
}