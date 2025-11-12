package com.yunke.backend.common.concurrency;

import com.yunke.backend.lock.RedisDistributedLock;
import com.yunke.backend.lock.LockHandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 并发控制服务
 * 为AFFiNE文档协作提供高级并发控制功能
 * 
 * 核心功能：
 * 1. 基于分布式锁的临界区保护
 * 2. 文档级和工作空间级并发控制
 * 3. 死锁检测和预防
 * 4. 性能监控和统计
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrencyControlService {
    
    private final RedisDistributedLock distributedLock;
    
    // 默认锁等待时间（5秒）
    private static final long DEFAULT_WAIT_TIME = 5000;
    
    // 线程池用于异步任务
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "concurrency-control-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });
    
    /**
     * 在文档锁保护下执行操作
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param operation 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws ConcurrencyException 并发控制异常
     */
    public <T> T executeWithDocLock(String workspaceId, String docId, Supplier<T> operation) 
            throws ConcurrencyException {
        return executeWithDocLock(workspaceId, docId, operation, DEFAULT_WAIT_TIME);
    }
    
    /**
     * 在文档锁保护下执行操作（自定义等待时间）
     */
    public <T> T executeWithDocLock(String workspaceId, String docId, Supplier<T> operation, long maxWaitTime) 
            throws ConcurrencyException {
        
        log.debug("🔒 [ConcurrencyControlService] 申请文档锁: docKey={}:{}", workspaceId, docId);
        
        try (LockHandle lock = distributedLock.acquireDocLock(workspaceId, docId, maxWaitTime)) {
            if (lock == null) {
                throw new ConcurrencyException("获取文档锁超时: " + workspaceId + ":" + docId);
            }
            
            log.debug("✅ [ConcurrencyControlService] 获得文档锁，开始执行操作: docKey={}:{}", workspaceId, docId);
            
            long startTime = System.currentTimeMillis();
            try {
                T result = operation.get();
                long executeTime = System.currentTimeMillis() - startTime;
                
                log.debug("🎉 [ConcurrencyControlService] 操作执行完成: docKey={}:{}, executeTime={}ms", 
                         workspaceId, docId, executeTime);
                
                return result;
                
            } catch (Exception e) {
                long executeTime = System.currentTimeMillis() - startTime;
                log.error("❌ [ConcurrencyControlService] 操作执行失败: docKey={}:{}, executeTime={}ms", 
                         workspaceId, docId, executeTime, e);
                throw new ConcurrencyException("操作执行失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 在工作空间锁保护下执行操作
     */
    public <T> T executeWithWorkspaceLock(String workspaceId, Supplier<T> operation) 
            throws ConcurrencyException {
        return executeWithWorkspaceLock(workspaceId, operation, DEFAULT_WAIT_TIME);
    }
    
    /**
     * 在工作空间锁保护下执行操作（自定义等待时间）
     */
    public <T> T executeWithWorkspaceLock(String workspaceId, Supplier<T> operation, long maxWaitTime) 
            throws ConcurrencyException {
        
        log.debug("🔒 [ConcurrencyControlService] 申请工作空间锁: workspaceId={}", workspaceId);
        
        try (LockHandle lock = distributedLock.acquireWorkspaceLock(workspaceId, maxWaitTime)) {
            if (lock == null) {
                throw new ConcurrencyException("获取工作空间锁超时: " + workspaceId);
            }
            
            log.debug("✅ [ConcurrencyControlService] 获得工作空间锁，开始执行操作: workspaceId={}", workspaceId);
            
            long startTime = System.currentTimeMillis();
            try {
                T result = operation.get();
                long executeTime = System.currentTimeMillis() - startTime;
                
                log.debug("🎉 [ConcurrencyControlService] 操作执行完成: workspaceId={}, executeTime={}ms", 
                         workspaceId, executeTime);
                
                return result;
                
            } catch (Exception e) {
                long executeTime = System.currentTimeMillis() - startTime;
                log.error("❌ [ConcurrencyControlService] 操作执行失败: workspaceId={}, executeTime={}ms", 
                         workspaceId, executeTime, e);
                throw new ConcurrencyException("操作执行失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 异步执行受保护的文档操作
     */
    public <T> CompletableFuture<T> executeDocOperationAsync(
            String workspaceId, String docId, Supplier<T> operation) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWithDocLock(workspaceId, docId, operation);
            } catch (ConcurrencyException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    /**
     * 异步执行受保护的工作空间操作
     */
    public <T> CompletableFuture<T> executeWorkspaceOperationAsync(
            String workspaceId, Supplier<T> operation) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWithWorkspaceLock(workspaceId, operation);
            } catch (ConcurrencyException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    /**
     * 批量执行文档操作（顺序执行，避免死锁）
     */
    public <T> CompletableFuture<T> executeBatchDocOperations(
            String workspaceId, Supplier<T> operation, String... docIds) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 按docId排序以避免死锁
                String[] sortedDocIds = java.util.Arrays.copyOf(docIds, docIds.length);
                java.util.Arrays.sort(sortedDocIds);
                
                return executeWithWorkspaceLock(workspaceId, () -> {
                    log.debug("📦 [ConcurrencyControlService] 批量操作开始: workspaceId={}, docCount={}", 
                             workspaceId, docIds.length);
                    
                    T result = operation.get();
                    
                    log.debug("✅ [ConcurrencyControlService] 批量操作完成: workspaceId={}, docCount={}", 
                             workspaceId, docIds.length);
                    
                    return result;
                });
                
            } catch (ConcurrencyException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    /**
     * 检查文档是否被锁定
     */
    public boolean isDocLocked(String workspaceId, String docId) {
        String lockKey = "affine:lock:doc:" + workspaceId + ":" + docId;
        return distributedLock.isLocked(lockKey);
    }
    
    /**
     * 检查工作空间是否被锁定
     */
    public boolean isWorkspaceLocked(String workspaceId) {
        String lockKey = "affine:lock:workspace:" + workspaceId;
        return distributedLock.isLocked(lockKey);
    }
    
    /**
     * 获取文档锁的剩余时间
     */
    public long getDocLockTTL(String workspaceId, String docId) {
        String lockKey = "affine:lock:doc:" + workspaceId + ":" + docId;
        return distributedLock.getLockTTL(lockKey);
    }
    
    /**
     * 获取并发控制统计信息
     */
    public ConcurrencyStats getStats() {
        return new ConcurrencyStats(
            distributedLock.getHeldLockCount(),
            ((ThreadPoolExecutor) executorService).getActiveCount(),
            ((ThreadPoolExecutor) executorService).getTaskCount(),
            ((ThreadPoolExecutor) executorService).getCompletedTaskCount()
        );
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        log.info("🧹 [ConcurrencyControlService] 开始清理资源");
        
        distributedLock.cleanupLocalLocks();
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("✅ [ConcurrencyControlService] 资源清理完成");
    }
    
    /**
     * 并发控制统计信息
     */
    public record ConcurrencyStats(
        int heldLocks,
        int activeThreads,
        long totalTasks,
        long completedTasks
    ) {}
    
    /**
     * 并发控制异常
     */
    public static class ConcurrencyException extends Exception {
        public ConcurrencyException(String message) {
            super(message);
        }
        
        public ConcurrencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}