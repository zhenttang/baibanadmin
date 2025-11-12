package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.dto.DocRecord;
import com.yunke.backend.system.domain.entity.Snapshot;
import com.yunke.backend.system.domain.entity.Update;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.system.repository.UpdateRepository;
import com.yunke.backend.document.service.DocWriter;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.document.util.YjsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文档写入器数据库实现
 */
@Service("databaseDocWriter")
@RequiredArgsConstructor
@Slf4j
public class DatabaseDocWriter implements DocWriter {

    private final SnapshotRepository snapshotRepository;
    private final UpdateRepository updateRepository;
    private final YjsUtils yjsUtils;
    private final DocBinaryStorageService binaryStorageService;

    @Override
    @Transactional
    public Mono<Boolean> upsertDoc(DocRecord record) {
        return Mono.fromCallable(() -> {
            try {
                // 转换时间戳为LocalDateTime
                LocalDateTime updatedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(record.getTimestamp()), 
                        ZoneId.systemDefault());
                
                // 检查是否已存在快照
                Optional<Snapshot> existingSnapshot = snapshotRepository.findByWorkspaceIdAndId(
                        record.getSpaceId(), record.getDocId());
                
                if (existingSnapshot.isPresent()) {
                    Snapshot snapshot = existingSnapshot.get();

                    // 只有当新快照的时间戳大于或等于现有快照时才更新
                    if (updatedAt.isAfter(snapshot.getUpdatedAt()) || updatedAt.isEqual(snapshot.getUpdatedAt())) {
                        binaryStorageService.deletePointer(snapshot.getBlob());
                        String pointer = binaryStorageService.saveSnapshot(record.getSpaceId(), record.getDocId(), record.getBlob());
                        snapshot.setBlob(binaryStorageService.pointerToBytes(pointer));
                        snapshot.setUpdatedAt(updatedAt);
                        snapshot.setUpdatedBy(record.getEditorId());
                        snapshotRepository.save(snapshot);
                        log.debug("更新文档快照: workspaceId={}, docId={}", record.getSpaceId(), record.getDocId());
                        return true;
                    } else {
                        log.debug("跳过旧快照更新: workspaceId={}, docId={}", record.getSpaceId(), record.getDocId());
                        return false;
                    }
                } else {
                    // 创建新快照
                    Snapshot snapshot = new Snapshot();
                    snapshot.setWorkspaceId(record.getSpaceId());
                    snapshot.setId(record.getDocId());
                    String pointer = binaryStorageService.saveSnapshot(record.getSpaceId(), record.getDocId(), record.getBlob());
                    snapshot.setBlob(binaryStorageService.pointerToBytes(pointer));
                    snapshot.setCreatedAt(updatedAt);
                    snapshot.setUpdatedAt(updatedAt);
                    snapshot.setCreatedBy(record.getEditorId());
                    snapshot.setUpdatedBy(record.getEditorId());
                    
                    snapshotRepository.save(snapshot);
                    log.debug("创建文档快照: workspaceId={}, docId={}", record.getSpaceId(), record.getDocId());
                    return true;
                }
            } catch (Exception e) {
                log.error("更新文档快照失败: {}", e.getMessage(), e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Integer> pushDocUpdates(String workspaceId, String docId, List<byte[]> updates, String editorId) {
        return Mono.fromCallable(() -> {
            try {
                List<Update> updateEntities = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();

                // 基于数据库中已存在的最大序号继续递增，避免主键冲突或覆盖
                int currentMaxSeq = updateRepository.findMaxSeqByWorkspaceIdAndId(workspaceId, docId);
                int seq = currentMaxSeq + 1;
                for (byte[] blob : updates) {
                    Update update = new Update();
                    update.setWorkspaceId(workspaceId);
                    update.setId(docId);
                    String pointer = binaryStorageService.saveUpdate(workspaceId, docId, seq++, blob);
                    update.setBlob(binaryStorageService.pointerToBytes(pointer));
                    update.setCreatedAt(now);
                    update.setCreatedBy(editorId);

                    updateEntities.add(update);
                    // 每次更新时间增加1毫秒，确保顺序
                    now = now.plusNanos(1_000_000);
                }
                
                List<Update> savedUpdates = updateRepository.saveAll(updateEntities);
                log.debug("推送文档更新: workspaceId={}, docId={}, count={}", workspaceId, docId, savedUpdates.size());
                
                return savedUpdates.size();
            } catch (Exception e) {
                log.error("推送文档更新失败: {}", e.getMessage(), e);
                return 0;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteDoc(String workspaceId, String docId) {
        return Mono.fromCallable(() -> {
            try {
                snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                    .ifPresent(snapshot -> binaryStorageService.deletePointer(snapshot.getBlob()));
                snapshotRepository.deleteByWorkspaceIdAndId(workspaceId, docId);

                updateRepository.findByWorkspaceIdAndId(workspaceId, docId)
                    .forEach(u -> binaryStorageService.deletePointer(u.getBlob()));
                updateRepository.deleteByWorkspaceIdAndId(workspaceId, docId);

                binaryStorageService.deleteDoc(workspaceId, docId);

                log.debug("删除文档: workspaceId={}, docId={}", workspaceId, docId);
                return true;
            } catch (Exception e) {
                log.error("删除文档失败: {}", e.getMessage(), e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<DocRecord> createInitialDoc(String workspaceId, String docId, String editorId) {
        return Mono.fromCallable(() -> {
            try {
                // 创建最小有效的YJS文档
                byte[] initialContent = yjsUtils.createMinimalValidYjsDoc(docId);
                
                // 创建DocRecord
                DocRecord record = new DocRecord();
                record.setSpaceId(workspaceId);
                record.setDocId(docId);
                record.setBlob(initialContent);
                record.setTimestamp(System.currentTimeMillis());
                record.setEditorId(editorId);
                
                // 保存快照
                upsertDoc(record).block();
                
                log.debug("创建初始文档: workspaceId={}, docId={}", workspaceId, docId);
                return record;
            } catch (Exception e) {
                log.error("创建初始文档失败: {}", e.getMessage(), e);
                throw new RuntimeException("创建初始文档失败", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
} 
