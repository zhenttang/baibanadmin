package com.yunke.backend.document.service;

import com.yunke.backend.document.crdt.*;
import com.yunke.backend.document.domain.entity.DocUpdate;
import com.yunke.backend.document.domain.entity.DocRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 真正的YJS文档合并服务 - 完全基于AFFiNE实现
 * 
 * 核心功能：
 * 1. 正确的YJS二进制数据编码/解码
 * 2. 可靠的异步合并任务
 * 3. 数据完整性验证
 * 4. 强大的错误处理和重试
 */
@Service
@Slf4j
public class AFFiNEDocMergeService {
    
    // 文档级别的锁，防止并发合并冲突
    private final ConcurrentHashMap<String, ReentrantLock> docLocks = new ConcurrentHashMap<>();
    
    /**
     * 合并多个YJS更新到一个文档快照 - 核心算法
     * 完全参考AFFiNE的squash和mergeUpdates实现
     */
    public CompletableFuture<byte[]> mergeUpdatesAsync(List<DocUpdate> updates, byte[] existingSnapshot) {
        return CompletableFuture.supplyAsync(() -> {
            if (updates.isEmpty()) {
                return existingSnapshot != null ? existingSnapshot : new byte[0];
            }
            
            log.debug("Merging {} updates with existing snapshot using AFFiNE algorithm", updates.size());
            
            try {
                // 1. 过滤空更新
                List<byte[]> validUpdates = new ArrayList<>();
                if (existingSnapshot != null && !isEmptyUpdate(existingSnapshot)) {
                    validUpdates.add(existingSnapshot);
                }
                
                for (DocUpdate update : updates) {
                    if (!isEmptyUpdate(update.getBin())) {
                        validUpdates.add(update.getBin());
                    }
                }
                
                // 2. 快速返回单个更新的情况
                if (validUpdates.isEmpty()) {
                    return new byte[0];
                }
                if (validUpdates.size() == 1) {
                    return validUpdates.get(0);
                }
                
                // 3. 使用YJS CRDT算法进行真正的合并
                return performCRDTMerge(validUpdates);
                
            } catch (Exception e) {
                log.error("CRDT merge failed, this should not happen in production", e);
                throw new RuntimeException("Critical: YJS merge algorithm failed", e);
            }
        });
    }
    
    /**
     * 执行CRDT合并 - AFFiNE核心算法
     */
    private byte[] performCRDTMerge(List<byte[]> updates) {
        log.debug("Performing CRDT merge for {} updates", updates.size());
        
        // 创建临时YDoc进行合并
        String tempDocId = "merge-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        YDoc tempDoc = new YDoc(tempDocId);
        
        try {
            // 在单个事务中应用所有更新，确保原子性
            tempDoc.transact("merge", () -> {
                for (byte[] update : updates) {
                    validateUpdate(update); // 验证更新完整性
                    tempDoc.applyUpdate(update, "merge");
                    log.trace("Applied update of size {} bytes", update.length);
                }
            });
            
            // 编码最终状态
            StateVector emptyStateVector = new StateVector();
            byte[] finalState = tempDoc.encodeStateAsUpdate(emptyStateVector);
            
            log.debug("CRDT merge completed: {} updates -> {} bytes", 
                     updates.size(), finalState.length);
            
            return finalState;
            
        } catch (Exception e) {
            // 合并失败是严重错误，必须抛出异常
            log.error("CRDT merge failed for {} updates", updates.size(), e);
            throw new RuntimeException("YJS CRDT merge failed", e);
        } finally {
            // 清理临时文档
            tempDoc.destroy();
        }
    }
    
    /**
     * 计算文档差异 - 对应AFFiNE的getDocDiff
     */
    public DiffResult calculateDocDiff(byte[] docSnapshot, byte[] clientStateVector) {
        if (docSnapshot == null || docSnapshot.length == 0) {
            return new DiffResult(new byte[0], new byte[]{0}, System.currentTimeMillis());
        }
        
        try {
            String tempDocId = "diff-" + System.currentTimeMillis();
            YDoc tempDoc = new YDoc(tempDocId);
            
            try {
                // 应用服务器快照
                tempDoc.applyUpdate(docSnapshot, "diff");
                
                // 解码客户端状态向量
                StateVector clientState = clientStateVector != null && clientStateVector.length > 0 ?
                    StateVector.decode(clientStateVector) : new StateVector();
                
                // 计算客户端缺失的更新
                byte[] missingUpdate = tempDoc.encodeStateAsUpdate(clientState);
                
                // 获取服务器当前状态向量
                StateVector serverState = tempDoc.getStateVector();
                byte[] serverStateBytes = serverState.encode();
                
                log.debug("Calculated diff: missing={} bytes, serverState={} bytes", 
                         missingUpdate.length, serverStateBytes.length);
                
                return new DiffResult(missingUpdate, serverStateBytes, System.currentTimeMillis());
                
            } finally {
                tempDoc.destroy();
            }
            
        } catch (Exception e) {
            log.error("Failed to calculate document diff", e);
            // 返回完整文档作为差异（安全降级）
            return new DiffResult(docSnapshot, new byte[]{0}, System.currentTimeMillis());
        }
    }
    
    /**
     * 验证YJS更新的完整性 - 基于AFFiNE的验证逻辑
     */
    public void validateUpdate(byte[] updateData) {
        if (updateData == null) {
            throw new IllegalArgumentException("Update data cannot be null");
        }
        
        if (updateData.length == 0) {
            throw new IllegalArgumentException("Update data cannot be empty");
        }
        
        try {
            // 尝试解码更新以验证格式正确性
            UpdateDecoder decoder = new UpdateDecoder(updateData);
            if (!decoder.validate()) {
                throw new IllegalArgumentException("Invalid YJS update format");
            }
            
        } catch (Exception e) {
            log.error("Update validation failed for {} bytes", updateData.length, e);
            throw new IllegalArgumentException("Invalid YJS update data", e);
        }
    }
    
    /**
     * 检查更新是否为空 - AFFiNE的isEmptyUpdate实现
     */
    public boolean isEmptyUpdate(byte[] updateData) {
        if (updateData == null || updateData.length == 0) {
            return true;
        }
        
        // YJS空更新的特征模式
        if (updateData.length == 2 && updateData[0] == 0 && updateData[1] == 0) {
            return true;
        }
        
        // 只包含空状态向量的更新
        if (updateData.length == 1 && updateData[0] == 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 压缩多个更新为单个更新 - AFFiNE压缩算法
     */
    public CompletableFuture<byte[]> compressUpdates(List<byte[]> updates) {
        return CompletableFuture.supplyAsync(() -> {
            if (updates.isEmpty()) {
                return new byte[0];
            }
            
            if (updates.size() == 1) {
                return updates.get(0);
            }
            
            // 过滤空更新
            List<byte[]> validUpdates = updates.stream()
                .filter(update -> !isEmptyUpdate(update))
                .toList();
            
            if (validUpdates.isEmpty()) {
                return new byte[0];
            }
            
            if (validUpdates.size() == 1) {
                return validUpdates.get(0);
            }
            
            return performCRDTMerge(validUpdates);
        });
    }
    
    /**
     * 获取文档锁 - 防止并发合并冲突
     */
    private ReentrantLock getDocLock(String docKey) {
        return docLocks.computeIfAbsent(docKey, k -> new ReentrantLock());
    }
    
    /**
     * 线程安全的文档合并 - 带锁保护
     */
    public CompletableFuture<byte[]> safelyMergeUpdates(String spaceId, String docId, 
                                                       List<DocUpdate> updates, byte[] existingSnapshot) {
        return CompletableFuture.supplyAsync(() -> {
            String docKey = spaceId + ":" + docId;
            ReentrantLock lock = getDocLock(docKey);
            
            lock.lock();
            try {
                log.debug("Acquired lock for document merge: {}", docKey);
                
                // 执行实际合并
                return mergeUpdatesAsync(updates, existingSnapshot).join();
                
            } finally {
                lock.unlock();
                log.debug("Released lock for document merge: {}", docKey);
                
                // 清理不再使用的锁（可选优化）
                if (!lock.hasQueuedThreads()) {
                    docLocks.remove(docKey, lock);
                }
            }
        });
    }
    
    /**
     * 验证合并结果的完整性
     */
    public boolean validateMergeResult(byte[] mergedData, List<DocUpdate> originalUpdates) {
        if (isEmptyUpdate(mergedData) && !originalUpdates.isEmpty()) {
            log.warn("Merge result is empty but had {} input updates", originalUpdates.size());
            return false;
        }
        
        try {
            validateUpdate(mergedData);
            return true;
        } catch (Exception e) {
            log.error("Merge result validation failed", e);
            return false;
        }
    }
    
    /**
     * 获取文档统计信息 - 用于调试和监控
     */
    public DocStats getDocumentStats(byte[] docSnapshot) {
        if (isEmptyUpdate(docSnapshot)) {
            return new DocStats(0, 0, 0, 0);
        }
        
        try {
            String tempDocId = "stats-" + System.currentTimeMillis();
            YDoc tempDoc = new YDoc(tempDocId);
            
            try {
                tempDoc.applyUpdate(docSnapshot, "stats");
                
                int binarySize = docSnapshot.length;
                int logicalSize = tempDoc.size();
                int clientCount = tempDoc.getStateVector().size();
                int typeCount = tempDoc.getShare().size();
                
                return new DocStats(binarySize, logicalSize, clientCount, typeCount);
                
            } finally {
                tempDoc.destroy();
            }
            
        } catch (Exception e) {
            log.error("Failed to get document stats", e);
            return new DocStats(docSnapshot.length, 0, 0, 0);
        }
    }
    
    /**
     * 差异结果类
     */
    public static class DiffResult {
        private final byte[] missing;
        private final byte[] state;
        private final long timestamp;
        
        public DiffResult(byte[] missing, byte[] state, long timestamp) {
            this.missing = missing;
            this.state = state;
            this.timestamp = timestamp;
        }
        
        public byte[] getMissing() { return missing; }
        public byte[] getState() { return state; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 文档统计信息类
     */
    public static class DocStats {
        private final int binarySize;
        private final int logicalSize;
        private final int clientCount;
        private final int typeCount;
        
        public DocStats(int binarySize, int logicalSize, int clientCount, int typeCount) {
            this.binarySize = binarySize;
            this.logicalSize = logicalSize;
            this.clientCount = clientCount;
            this.typeCount = typeCount;
        }
        
        public int getBinarySize() { return binarySize; }
        public int getLogicalSize() { return logicalSize; }
        public int getClientCount() { return clientCount; }
        public int getTypeCount() { return typeCount; }
        
        @Override
        public String toString() {
            return String.format("DocStats{binary=%d, logical=%d, clients=%d, types=%d}",
                               binarySize, logicalSize, clientCount, typeCount);
        }
    }
}