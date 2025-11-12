package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.dto.DocRecord;
import com.yunke.backend.system.domain.entity.Snapshot;
import com.yunke.backend.system.domain.entity.Update;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.system.repository.UpdateRepository;
import com.yunke.backend.document.service.DocReader;
import com.yunke.backend.document.service.YjsServiceClient;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.document.util.YjsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 数据库文档读取器
 * 直接从数据库读取文档快照和更新
 */
@Service("databaseDocReader")
@Primary
@RequiredArgsConstructor
@Slf4j
public class DatabaseDocReader implements DocReader {

    private final SnapshotRepository snapshotRepository;
    private final UpdateRepository updateRepository;
    private final YjsServiceClient yjsServiceClient;  // 🔥 使用YJS微服务（正确的CRDT实现）
    private final DocBinaryStorageService binaryStorageService;
    
    @Override
    public Mono<Optional<DocRecord>> getDoc(String workspaceId, String docId) {
        return Mono.fromCallable(() -> {
            try {
                // 首先获取最新快照
                Optional<Snapshot> snapshotOpt = snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId);
                
                if (snapshotOpt.isEmpty()) {
                    log.debug("未找到文档快照: workspaceId={}, docId={}", workspaceId, docId);
                    return Optional.<DocRecord>empty();
                }
                
                Snapshot snapshot = snapshotOpt.get();
                byte[] snapshotBytes = binaryStorageService.resolvePointer(snapshot.getBlob(), workspaceId, docId);

                // 获取快照之后的所有更新
                List<Update> updates = updateRepository.findByWorkspaceIdAndDocIdAfterTimestamp(
                        workspaceId, docId, snapshot.getUpdatedAt());

                byte[] finalBlob;
                long finalTimestamp;
                String finalEditorId;

                if (updates.isEmpty()) {
                    // 没有新的更新，直接使用快照
                    finalBlob = snapshotBytes;
                    finalTimestamp = snapshot.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) * 1000;
                    finalEditorId = snapshot.getUpdatedBy();
                } else {
                    // 有新的更新，需要合并
                    log.debug("发现 {} 个待合并的更新，开始合并文档: workspaceId={}, docId={}", 
                            updates.size(), workspaceId, docId);

                    finalBlob = mergeUpdates(snapshotBytes, updates);

                    // 使用最新更新的时间戳和编辑者
                    Update latestUpdate = updates.get(updates.size() - 1);
                    finalTimestamp = latestUpdate.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000;
                    finalEditorId = latestUpdate.getCreatedBy();
                }
                
                DocRecord docRecord = DocRecord.builder()
                        .spaceId(workspaceId)
                        .docId(docId)
                        .blob(finalBlob)
                        .timestamp(finalTimestamp)
                        .editorId(finalEditorId)
                        .build();
                
                log.debug("成功从数据库加载文档: workspaceId={}, docId={}, size={}, timestamp={}", 
                        workspaceId, docId, finalBlob.length, finalTimestamp);
                
                return Optional.of(docRecord);
                
            } catch (Exception e) {
                log.error("从数据库读取文档失败: workspaceId={}, docId={}", workspaceId, docId, e);
                return Optional.<DocRecord>empty();
            }
        });
    }
    
    @Override
    public Mono<Optional<byte[]>> getDocSnapshot(String workspaceId, String docId) {
        return Mono.fromCallable(() -> {
            return snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                    .map(snapshot -> binaryStorageService.resolvePointer(snapshot.getBlob(), workspaceId, docId));
        });
    }
    
    @Override
    public Mono<byte[]> getDocUpdates(String workspaceId, String docId, LocalDateTime since) {
        return Mono.fromCallable(() -> {
            try {
                List<Update> updates = updateRepository.findByWorkspaceIdAndDocIdAfterTimestamp(
                        workspaceId, docId, since);
                
                if (updates.isEmpty()) {
                    return new byte[0];
                }
                
                // 合并所有更新为单一的二进制数据
                return mergeUpdatesToBinary(updates);
                
            } catch (Exception e) {
                log.error("获取文档更新失败: workspaceId={}, docId={}, since={}", 
                        workspaceId, docId, since, e);
                return new byte[0];
            }
        });
    }
    
    @Override
    public Mono<byte[]> getDocDiff(String workspaceId, String docId, byte[] stateVector) {
        return Mono.fromCallable(() -> {
            try {
                // 获取最新的文档状态
                Optional<DocRecord> docRecordOpt = getDoc(workspaceId, docId).block();
                
                if (docRecordOpt.isEmpty()) {
                    return new byte[0];
                }
                
                byte[] currentBlob = docRecordOpt.get().getBlob();

                // 🔥 使用YJS微服务计算差异（正确的CRDT算法）
                return yjsServiceClient.diffUpdate(currentBlob, stateVector);
                
            } catch (Exception e) {
                log.error("计算文档差异失败: workspaceId={}, docId={}", workspaceId, docId, e);
                return new byte[0];
            }
        });
    }
    
    @Override
    public Mono<Boolean> docExists(String workspaceId, String docId) {
        return Mono.fromCallable(() -> {
            return snapshotRepository.existsByWorkspaceIdAndId(workspaceId, docId);
        });
    }
    
    @Override
    public Mono<Optional<LocalDateTime>> getDocLastModified(String workspaceId, String docId) {
        return Mono.fromCallable(() -> {
            // 首先检查是否有未合并的更新
            Optional<Update> latestUpdate = updateRepository.findLatestByWorkspaceIdAndDocId(workspaceId, docId);
            
            if (latestUpdate.isPresent()) {
                return Optional.of(latestUpdate.get().getCreatedAt());
            }
            
            // 没有更新，检查快照的更新时间
            return snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                    .map(Snapshot::getUpdatedAt);
        });
    }
    
    // ==================== 私有方法 ====================

    /**
     * 合并快照和更新列表 - 使用YJS微服务
     */
    private byte[] mergeUpdates(byte[] snapshotBlob, List<Update> updates) {
        try {
            // 构建需要合并的所有更新（包括快照）
            List<byte[]> allUpdates = new ArrayList<>();
            allUpdates.add(snapshotBlob);

            for (Update update : updates) {
                allUpdates.add(binaryStorageService.resolvePointer(update.getBlob()));
            }

            // 使用YJS微服务进行合并
            log.debug("🔧 使用YJS微服务合并 {} 个更新（包括快照）", allUpdates.size());
            byte[] merged = yjsServiceClient.mergeUpdates(allUpdates);

            log.debug("✅ YJS微服务合并成功: size={}B", merged.length);
            return merged;

        } catch (Exception e) {
            log.error("❌ 使用YJS微服务合并失败，返回原始快照", e);
            return snapshotBlob; // 返回原始快照
        }
    }
    
    /**
     * 将多个更新合并为单一二进制数据 - 使用YJS微服务
     */
    private byte[] mergeUpdatesToBinary(List<Update> updates) {
        try {
            List<byte[]> updateBlobs = new ArrayList<>();
            for (Update update : updates) {
                updateBlobs.add(binaryStorageService.resolvePointer(update.getBlob()));
            }

            // 使用YJS微服务进行合并
            return yjsServiceClient.mergeUpdates(updateBlobs);

        } catch (Exception e) {
            log.error("❌ 合并更新为二进制数据失败", e);
            return new byte[0];
        }
    }
}
