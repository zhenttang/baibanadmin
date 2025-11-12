package com.yunke.backend.document.service;

import com.yunke.backend.document.crdt.*;
import com.yunke.backend.document.domain.entity.DocUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * YJS兼容的文档合并服务
 * 
 * 提供真正的YJS CRDT算法实现
 * 用于替换DocStorageService中的简化mergeUpdates方法
 */
@Service
@Slf4j
public class YjsMergeService {
    
    /**
     * 合并多个YJS更新到一个文档快照
     * 
     * @param updates 需要合并的更新列表
     * @param existingSnapshot 现有快照数据（可为null）
     * @return 合并后的文档二进制数据
     */
    public byte[] mergeUpdates(List<DocUpdate> updates, byte[] existingSnapshot) {
        if (updates.isEmpty()) {
            return existingSnapshot != null ? existingSnapshot : new byte[0];
        }
        
        log.debug("Merging {} updates with existing snapshot", updates.size());
        
        try {
            // 创建临时YDoc进行合并操作
            YDoc tempDoc = new YDoc("temp-merge-" + System.currentTimeMillis());
            
            // 如果有现有快照，先应用到文档
            if (existingSnapshot != null && existingSnapshot.length > 0) {
                applySnapshotToDoc(tempDoc, existingSnapshot);
            }
            
            // 按时间戳排序更新，确保因果一致性
            List<DocUpdate> sortedUpdates = new ArrayList<>(updates);
            sortedUpdates.sort((u1, u2) -> Long.compare(u1.getTimestamp(), u2.getTimestamp()));
            
            // 逐个应用更新
            for (DocUpdate update : sortedUpdates) {
                applyUpdateToDoc(tempDoc, update);
            }
            
            // 编码最终状态为快照
            return tempDoc.encodeStateAsUpdate(new StateVector());
            
        } catch (Exception e) {
            log.error("Failed to merge updates using YJS algorithm", e);
            // 降级到简单合并策略
            return fallbackMerge(updates, existingSnapshot);
        }
    }
    
    /**
     * 将快照数据应用到YDoc
     */
    private void applySnapshotToDoc(YDoc doc, byte[] snapshotData) {
        try {
            doc.applyUpdate(snapshotData, "snapshot");
            log.debug("Applied existing snapshot to doc, size: {} bytes", snapshotData.length);
        } catch (Exception e) {
            log.warn("Failed to apply existing snapshot, starting with empty doc", e);
        }
    }
    
    /**
     * 将单个更新应用到YDoc
     */
    private void applyUpdateToDoc(YDoc doc, DocUpdate update) {
        try {
            String origin = update.getEditorId() != null ? update.getEditorId() : "unknown";
            doc.applyUpdate(update.getBin(), origin);
            
            log.debug("Applied update: docId={}, timestamp={}, size={} bytes", 
                     update.getDocId(), update.getTimestamp(), update.getBin().length);
                     
        } catch (Exception e) {
            log.error("Failed to apply update: docId={}, timestamp={}", 
                     update.getDocId(), update.getTimestamp(), e);
            // 继续处理其他更新，不中断整个合并过程
        }
    }
    
    /**
     * 计算文档差异 - 对应AFFiNE的getDocDiff
     */
    public DiffResult calculateDiff(byte[] docSnapshot, byte[] clientStateVector) {
        try {
            // 创建临时文档
            YDoc doc = new YDoc("diff-" + System.currentTimeMillis());
            
            // 应用快照
            if (docSnapshot != null && docSnapshot.length > 0) {
                doc.applyUpdate(docSnapshot, "diff");
            }
            
            // 解码客户端状态向量
            StateVector clientState = clientStateVector != null ? 
                StateVector.decode(clientStateVector) : new StateVector();
            
            // 计算缺失的更新
            byte[] missingUpdate = doc.encodeStateAsUpdate(clientState);
            
            // 获取服务器状态向量
            StateVector serverState = doc.getStateVector();
            byte[] serverStateBytes = serverState.encode();
            
            return new DiffResult(missingUpdate, serverStateBytes, System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("Failed to calculate document diff", e);
            // 返回完整文档作为差异
            return new DiffResult(
                docSnapshot != null ? docSnapshot : new byte[0],
                new byte[]{0}, // 空状态向量
                System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 验证更新数据的完整性
     */
    public boolean validateUpdate(byte[] updateData) {
        if (updateData == null || updateData.length == 0) {
            return false;
        }
        
        try {
            UpdateDecoder decoder = new UpdateDecoder(updateData);
            return decoder.validate();
        } catch (Exception e) {
            log.warn("Update validation failed", e);
            return false;
        }
    }
    
    /**
     * 检查更新是否为空
     */
    public boolean isEmptyUpdate(byte[] updateData) {
        if (updateData == null || updateData.length == 0) {
            return true;
        }
        
        // YJS空更新的特征
        if (updateData.length == 1 && updateData[0] == 0) {
            return true;
        }
        
        if (updateData.length == 2 && updateData[0] == 0 && updateData[1] == 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 压缩多个更新为单个更新
     */
    public byte[] compressUpdates(List<byte[]> updates) {
        if (updates.isEmpty()) {
            return new byte[0];
        }
        
        if (updates.size() == 1) {
            return updates.get(0);
        }
        
        try {
            YDoc tempDoc = new YDoc("compress-" + System.currentTimeMillis());
            
            // 按顺序应用所有更新
            for (byte[] update : updates) {
                if (!isEmptyUpdate(update)) {
                    tempDoc.applyUpdate(update, "compress");
                }
            }
            
            // 返回压缩后的状态
            return tempDoc.encodeStateAsUpdate(new StateVector());
            
        } catch (Exception e) {
            log.error("Failed to compress updates", e);
            return fallbackCompress(updates);
        }
    }
    
    /**
     * 降级合并策略（当YJS合并失败时使用）
     */
    private byte[] fallbackMerge(List<DocUpdate> updates, byte[] existingSnapshot) {
        log.warn("Using fallback merge strategy");
        
        if (updates.isEmpty()) {
            return existingSnapshot != null ? existingSnapshot : new byte[0];
        }
        
        // 简单策略：返回最新的更新
        DocUpdate latestUpdate = updates.stream()
            .max((u1, u2) -> Long.compare(u1.getTimestamp(), u2.getTimestamp()))
            .orElse(updates.get(updates.size() - 1));
            
        return latestUpdate.getBin();
    }
    
    /**
     * 降级压缩策略
     */
    private byte[] fallbackCompress(List<byte[]> updates) {
        log.warn("Using fallback compress strategy");
        
        // 返回最后一个非空更新
        for (int i = updates.size() - 1; i >= 0; i--) {
            byte[] update = updates.get(i);
            if (!isEmptyUpdate(update)) {
                return update;
            }
        }
        
        return new byte[0];
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
     * 获取文档的统计信息
     */
    public DocStats getDocStats(byte[] docSnapshot) {
        if (docSnapshot == null || docSnapshot.length == 0) {
            return new DocStats(0, 0, 0, 0);
        }
        
        try {
            YDoc doc = new YDoc("stats-" + System.currentTimeMillis());
            doc.applyUpdate(docSnapshot, "stats");
            
            int totalSize = doc.size();
            int clientCount = doc.getStateVector().size();
            int typeCount = doc.getShare().size();
            
            return new DocStats(docSnapshot.length, totalSize, clientCount, typeCount);
            
        } catch (Exception e) {
            log.error("Failed to get doc stats", e);
            return new DocStats(docSnapshot.length, 0, 0, 0);
        }
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