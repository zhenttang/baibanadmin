package com.yunke.backend.document.service.impl;

import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.common.dto.PaginationInput;
import com.yunke.backend.document.dto.DocDiffDto;
import com.yunke.backend.document.dto.DocHistoryDto;
import com.yunke.backend.document.dto.DocVersionDto;
import com.yunke.backend.document.dto.DocVersionStatsDto;
import com.yunke.backend.document.service.DocVersionService;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.system.domain.entity.*;
import com.yunke.backend.system.repository.*;

import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocVersionServiceImpl implements DocVersionService {

    private final SnapshotRepository snapshotRepository;
    private final UpdateRepository updateRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final DocBinaryStorageService binaryStorageService;

    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional
    public Mono<DocVersionDto> createVersion(String workspaceId, String docId, byte[] content, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Update")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to create snapshot")))
                .flatMap(hasPermission -> {
                    // Create new snapshot
                    return Mono.fromCallable(() -> {
                        snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                                .ifPresent(existing -> binaryStorageService.deletePointer(existing.getBlob()));

                        byte[] pointerBytes = storeSnapshotContent(workspaceId, docId, content);
                        Snapshot snapshot = Snapshot.builder()
                                .workspaceId(workspaceId)
                                .id(docId)
                                .blob(pointerBytes)
                                .state(new byte[0]) // 默认空状态
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .createdBy(userId)
                                .updatedBy(userId)
                                .seq(getNextSequenceNumber(workspaceId, docId))
                                .build();

                        return snapshotRepository.save(snapshot);
                    })
                    .map(this::convertToVersionDto);
                });
    }

    @Override
    public Mono<DocVersionDto> getVersion(String workspaceId, String docId, String versionId, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to read snapshot")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId))
                            .flatMap(snapshotOpt -> snapshotOpt
                                    .map(this::convertToVersionDto)
                                    .map(Mono::just)
                                    .orElse(Mono.empty()))
                );
    }

    @Override
    public Mono<PaginatedResponse<DocVersionDto>> getVersions(String workspaceId, String docId, PaginationInput pagination, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to read doc histories")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        Pageable pageable = PageRequest.of(
                                pagination.getPage(),
                                pagination.getSize(),
                                Sort.by("updatedAt").descending()
                        );
                        
                        var page = snapshotRepository.findByWorkspaceIdAndUpdatedAtBetween(
                                workspaceId,
                                LocalDateTime.now().minusYears(1), // 1 year history
                                LocalDateTime.now(),
                                pageable
                        );
                        
                        List<DocVersionDto> content = page.getContent().stream()
                                .map(this::convertToVersionDto)
                                .toList();
                        
                        return PaginatedResponse.of(
                                content,
                                page.getTotalElements(),
                                pagination.getPage(),
                                pagination.getSize()
                        );
                    })
                );
    }

    public Mono<LocalDateTime> recoverDocToVersion(String workspaceId, String docId, LocalDateTime timestamp, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Restore")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to restore doc")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // Find snapshot closest to the timestamp
                        var snapshots = snapshotRepository.findByWorkspaceIdAndUpdatedAtBetween(
                                workspaceId,
                                timestamp.minusHours(1),
                                timestamp.plusHours(1),
                                PageRequest.of(0, 1, Sort.by("updatedAt").ascending())
                        );
                        
                        if (snapshots.isEmpty()) {
                            throw new RuntimeException("No snapshot found for timestamp: " + timestamp);
                        }
                        
                        Snapshot targetSnapshot = snapshots.getContent().get(0);

                        byte[] restoredContent = resolveSnapshotContent(targetSnapshot);
                        snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                                .ifPresent(existing -> binaryStorageService.deletePointer(existing.getBlob()));

                        // Create new snapshot with restored content
                        byte[] pointerBytes = storeSnapshotContent(workspaceId, docId, restoredContent);
                        Snapshot restoredSnapshot = Snapshot.builder()
                                .workspaceId(workspaceId)
                                .id(docId)
                                .blob(pointerBytes)
                                .state(targetSnapshot.getState())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .createdBy(userId)
                                .updatedBy(userId)
                                .seq(getNextSequenceNumber(workspaceId, docId))
                                .build();

                        snapshotRepository.save(restoredSnapshot);
                        
                        log.info("Document restored: {}/{} to version at {}", workspaceId, docId, timestamp);
                        return LocalDateTime.now();
                    })
                );
    }

    public Mono<DocDiffDto> getDocDiff(String workspaceId, String docId, byte[] stateVector, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to get doc diff")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // Get latest snapshot
                        var snapshotOpt = snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
                        if (snapshotOpt.isEmpty()) {
                            throw new RuntimeException("No snapshot found for document");
                        }
                        
                        Snapshot snapshot = snapshotOpt.get();
                        
                        // Calculate diff (simplified implementation)
                        byte[] missing = calculateMissingUpdates(resolveSnapshotContent(snapshot), stateVector);
                        
                        return DocDiffDto.builder()
                                .missing(missing)
                                .state(snapshot.getState())
                                .timestamp(snapshot.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                                .build();
                    })
                );
    }

    public Mono<Boolean> applyDocUpdate(String workspaceId, String docId, byte[] update, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Update")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to apply update")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // Create update record
                        int seq = getNextUpdateSequence(workspaceId, docId);
                        byte[] pointerBytes = storeUpdateContent(workspaceId, docId, seq, update);
                        Update updateRecord = Update.builder()
                                .workspaceId(workspaceId)
                                .id(docId)
                                .createdAt(LocalDateTime.now())
                                .blob(pointerBytes)
                                .createdBy(userId)
                                .seq(seq)
                                .build();

                        updateRepository.save(updateRecord);
                        
                        log.debug("Applied update to document: {}/{}", workspaceId, docId);
                        return true;
                    })
                );
    }

    public Flux<UpdateDto> getDocUpdates(String workspaceId, String docId, LocalDateTime since, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to get doc updates")))
                .flatMapMany(hasPermission -> {
                    return Flux.fromIterable(
                            updateRepository.findByWorkspaceIdAndIdAndCreatedAtAfter(
                                    workspaceId, docId, since)
                    ).map(this::convertToUpdateDto);
                });
    }

    @Override
    public Mono<PaginatedResponse<DocHistoryDto>> getDocHistories(String workspaceId, String docId, PaginationInput pagination, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to read doc histories")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        Pageable pageable = PageRequest.of(
                                pagination.getPage(),
                                pagination.getSize(),
                                Sort.by("updatedAt").descending()
                        );
                        
                        var page = snapshotRepository.findByWorkspaceIdAndUpdatedAtBetween(
                                workspaceId,
                                LocalDateTime.now().minusYears(1), // 1 year history
                                LocalDateTime.now(),
                                pageable
                        );
                        
                        List<DocHistoryDto> content = page.getContent().stream()
                                .filter(snapshot -> snapshot.getId().equals(docId))
                                .map(this::convertToHistoryDto)
                                .toList();
                        
                        return PaginatedResponse.of(
                                content,
                                content.size(),
                                pagination.getPage(),
                                pagination.getSize()
                        );
                    })
                );
    }

    @Override
    public Mono<SnapshotDto> getLatestSnapshot(String workspaceId, String docId, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to read snapshot")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId))
                            .flatMap(snapshotOpt -> snapshotOpt
                                    .map(this::convertToSnapshotDto)
                                    .map(Mono::just)
                                    .orElse(Mono.empty()))
                );
    }

    @Override
    @Transactional
    public Mono<SnapshotDto> createSnapshot(String workspaceId, String docId, byte[] blob, byte[] state, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Update")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to create snapshot")))
                .flatMap(hasPermission -> {
                    // Create new snapshot
                    return Mono.fromCallable(() -> {
                        snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                                .ifPresent(existing -> binaryStorageService.deletePointer(existing.getBlob()));

                        byte[] pointerBytes = storeSnapshotContent(workspaceId, docId, blob);
                        Snapshot snapshot = Snapshot.builder()
                                .workspaceId(workspaceId)
                                .id(docId)
                                .blob(pointerBytes)
                                .state(state)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .createdBy(userId)
                                .updatedBy(userId)
                                .seq(getNextSequenceNumber(workspaceId, docId))
                                .build();

                        return snapshotRepository.save(snapshot);
                    })
                    .map(this::convertToSnapshotDto);
                });
    }

    public Mono<Boolean> mergeUpdatesToSnapshot(String workspaceId, String docId, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Update")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to merge updates")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // Get latest snapshot
                        var snapshotOpt = snapshotRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
                        if (snapshotOpt.isEmpty()) {
                            return false;
                        }
                        
                        Snapshot latestSnapshot = snapshotOpt.get();
                        
                        // Get all updates since last snapshot
                        List<Update> updates = updateRepository.findByWorkspaceIdAndIdAndCreatedAtAfter(
                                workspaceId, docId, latestSnapshot.getUpdatedAt()
                        );
                        
                        if (updates.isEmpty()) {
                            return false;
                        }
                        
                        // Merge updates into snapshot content (simplified)
                        byte[] mergedContent = mergeUpdates(resolveSnapshotContent(latestSnapshot), updates);
                        
                        // Create new snapshot with merged content
                        binaryStorageService.deletePointer(latestSnapshot.getBlob());
                        byte[] pointerBytes = storeSnapshotContent(workspaceId, docId, mergedContent);
                        Snapshot newSnapshot = Snapshot.builder()
                                .workspaceId(workspaceId)
                                .id(docId)
                                .blob(pointerBytes)
                                .state(latestSnapshot.getState())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .createdBy(userId)
                                .updatedBy(userId)
                                .seq(getNextSequenceNumber(workspaceId, docId))
                                .build();

                        snapshotRepository.save(newSnapshot);
                        
                        log.info("Merged {} updates into snapshot for doc: {}/{}", updates.size(), workspaceId, docId);
                        return true;
                    })
                );
    }

    public Mono<Long> cleanupExpiredVersions(String workspaceId, int retentionDays) {
        return Mono.fromCallable(() -> {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
            
            // Delete expired snapshots
            List<Snapshot> expiredSnapshots = snapshotRepository.findExpiredSnapshots(expireTime);
            long deletedSnapshots = expiredSnapshots.size();
            expiredSnapshots.forEach(snapshot -> binaryStorageService.deletePointer(snapshot.getBlob()));
            snapshotRepository.deleteExpiredSnapshots(expireTime);
            
            // Delete expired updates
            List<Update> expiredUpdates = updateRepository.findExpiredUpdates(expireTime);
            long deletedUpdates = expiredUpdates.size();
            expiredUpdates.forEach(update -> binaryStorageService.deletePointer(update.getBlob()));
            updateRepository.deleteExpiredUpdates(expireTime);
            
            long totalDeleted = deletedSnapshots + deletedUpdates;
            log.info("Cleaned up {} expired versions in workspace: {}", totalDeleted, workspaceId);
            
            return totalDeleted;
        });
    }

    public Mono<DocVersionStatsDto> getVersionStats(String workspaceId, String docId, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Read")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to get version stats")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        long totalSnapshots = snapshotRepository.countByWorkspaceId(workspaceId);
                        long totalUpdates = updateRepository.countByWorkspaceIdAndId(workspaceId, docId);
                        
                        // Get first and last versions
                        var snapshots = snapshotRepository.findByWorkspaceId(workspaceId);
                        LocalDateTime firstVersion = snapshots.stream()
                                .map(Snapshot::getCreatedAt)
                                .min(LocalDateTime::compareTo)
                                .orElse(null);
                        
                        LocalDateTime lastVersion = snapshots.stream()
                                .map(Snapshot::getUpdatedAt)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);
                        
                        String lastEditor = snapshots.stream()
                                .sorted((s1, s2) -> s1.getUpdatedAt().compareTo(s2.getUpdatedAt()))
                                .map(Snapshot::getUpdatedBy)
                                .findFirst()
                                .orElse(null);
                        
                        // Calculate total size
                        long totalSize = snapshots.stream()
                                .mapToLong(s -> resolveSnapshotContent(s).length)
                                .sum();
                        
                        return DocVersionStatsDto.builder()
                                .workspaceId(workspaceId)
                                .docId(docId)
                                .totalSnapshots(totalSnapshots)
                                .totalUpdates(totalUpdates)
                                .totalSize(totalSize)
                                .firstVersion(firstVersion)
                                .lastVersion(lastVersion)
                                .lastEditor(lastEditor)
                                .build();
                    })
                );
    }

    /**
     * 将Snapshot实体转换为DocVersionDto
     */
    private DocVersionDto convertToVersionDto(Snapshot snapshot) {
        byte[] content = resolveSnapshotContent(snapshot);
        return DocVersionDto.builder()
                .id(snapshot.getId())
                .docId(snapshot.getId())
                .workspaceId(snapshot.getWorkspaceId())
                .version(String.valueOf(snapshot.getSeq()))
                .createdAt(snapshot.getCreatedAt())
                .createdBy(snapshot.getCreatedBy())
                .updatedAt(snapshot.getUpdatedAt())
                .updatedBy(snapshot.getUpdatedBy())
                .content(content)
                .state(snapshot.getState())
                .build();
    }

    /**
     * 恢复到指定版本
     */
    @Override
    @Transactional
    public Mono<Boolean> restoreVersion(String workspaceId, String docId, String versionId, String userId) {
        return checkDocPermission(workspaceId, docId, userId, "Doc.Restore")
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("No permission to restore version")))
                .flatMap(hasPermission -> 
                    Mono.fromCallable(() -> {
                        // 查找指定版本
                        Snapshot.SnapshotId snapshotId = new Snapshot.SnapshotId();
                        snapshotId.setWorkspaceId(workspaceId);
                        snapshotId.setId(versionId);
                        Optional<Snapshot> snapshotOpt = snapshotRepository.findById(snapshotId);
                        if (snapshotOpt.isEmpty()) {
                            throw new ResourceNotFoundException("DocVersion", versionId);
                        }
                        
                        Snapshot targetSnapshot = snapshotOpt.get();
                        
                        // 创建新的快照（恢复版本）
                        byte[] restoredContent = resolveSnapshotContent(targetSnapshot);
                        snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                                .ifPresent(existing -> binaryStorageService.deletePointer(existing.getBlob()));

                        byte[] pointerBytes = storeSnapshotContent(workspaceId, docId, restoredContent);
                        Snapshot restoredSnapshot = Snapshot.builder()
                                .workspaceId(workspaceId)
                                .id(docId)
                                .blob(pointerBytes)
                                .state(targetSnapshot.getState())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .createdBy(userId)
                                .updatedBy(userId)
                                .seq(getNextSequenceNumber(workspaceId, docId))
                                .build();

                        snapshotRepository.save(restoredSnapshot);
                        
                        log.info("Document version restored: {}/{} to version {}", workspaceId, docId, versionId);
                        return true;
                    })
                );
    }

    // Helper methods
    private int getNextSequenceNumber(String workspaceId, String docId) {
        return snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId)
                .stream()
                .mapToInt(s -> s.getSeq() != null ? s.getSeq() : 0)
                .max()
                .orElse(0) + 1;
    }

    private int getNextUpdateSequence(String workspaceId, String docId) {
        return updateRepository.findByWorkspaceIdAndId(workspaceId, docId)
                .stream()
                .mapToInt(u -> u.getSeq() != null ? u.getSeq() : 0)
                .max()
                .orElse(0) + 1;
    }

    private byte[] calculateMissingUpdates(byte[] currentContent, byte[] stateVector) {
        // Simplified diff calculation
        // In a real implementation, this would use proper CRDT algorithms
        if (Arrays.equals(currentContent, stateVector)) {
            return new byte[0];
        }
        return currentContent;
    }

    private byte[] mergeUpdates(byte[] baseContent, List<Update> updates) {
        // Simplified merge implementation
        // In a real implementation, this would apply CRDT updates sequentially
        byte[] result = baseContent;
        for (Update update : updates) {
            result = applyUpdate(result, resolveUpdateContent(update));
        }
        return result;
    }

    private byte[] applyUpdate(byte[] content, byte[] update) {
        // Simplified update application
        // In a real implementation, this would apply CRDT operations
        return update;
    }

    private SnapshotDto convertToSnapshotDto(Snapshot snapshot) {
        byte[] content = resolveSnapshotContent(snapshot);
        return SnapshotDto.builder()
                .workspaceId(snapshot.getWorkspaceId())
                .id(snapshot.getId())
                .blob(content)
                .state(snapshot.getState())
                .createdAt(snapshot.getCreatedAt())
                .updatedAt(snapshot.getUpdatedAt())
                .createdBy(snapshot.getCreatedBy())
                .updatedBy(snapshot.getUpdatedBy())
                .seq(snapshot.getSeq())
                .build();
    }

    private UpdateDto convertToUpdateDto(Update update) {
        byte[] content = resolveUpdateContent(update);
        return UpdateDto.builder()
                .workspaceId(update.getWorkspaceId())
                .id(update.getId())
                .createdAt(update.getCreatedAt())
                .blob(content)
                .createdBy(update.getCreatedBy())
                .seq(update.getSeq())
                .build();
    }

    private byte[] resolveSnapshotContent(Snapshot snapshot) {
        if (snapshot == null || snapshot.getBlob() == null) {
            return new byte[0];
        }
        return binaryStorageService.resolvePointer(snapshot.getBlob(), snapshot.getWorkspaceId(), snapshot.getId());
    }

    private byte[] resolveUpdateContent(Update update) {
        if (update == null || update.getBlob() == null) {
            return new byte[0];
        }
        return binaryStorageService.resolvePointer(update.getBlob());
    }

    private byte[] storeSnapshotContent(String workspaceId, String docId, byte[] content) {
        String pointer = binaryStorageService.saveSnapshot(workspaceId, docId, content);
        return binaryStorageService.pointerToBytes(pointer);
    }

    private byte[] storeUpdateContent(String workspaceId, String docId, int seq, byte[] content) {
        String pointer = binaryStorageService.saveUpdate(workspaceId, docId, seq, content);
        return binaryStorageService.pointerToBytes(pointer);
    }

    private DocHistoryDto convertToHistoryDto(Snapshot snapshot) {
        byte[] content = resolveSnapshotContent(snapshot);
        // 格式化时间戳为 ISO 字符串
        String timestampStr = snapshot.getUpdatedAt() != null 
            ? snapshot.getUpdatedAt().format(TIMESTAMP_FORMATTER) 
            : null;
        
        String createdAtStr = snapshot.getCreatedAt() != null
            ? snapshot.getCreatedAt().format(TIMESTAMP_FORMATTER)
            : timestampStr;
            
        return DocHistoryDto.builder()
                .id(snapshot.getId())
                .workspaceId(snapshot.getWorkspaceId())
                .docId(snapshot.getId())
                .pageDocId(snapshot.getId())  // 前端期望的字段名
                .timestamp(timestampStr)
                .version(timestampStr)  // 使用时间戳作为版本号
                .title("快照版本 " + (timestampStr != null ? timestampStr : ""))
                .createdAt(createdAtStr)
                .updatedAt(timestampStr)
                .createdBy(snapshot.getUpdatedBy())
                .blobSize((long) content.length)
                .hasState(snapshot.getState() != null)
                .seq(snapshot.getSeq())
                .build();
    }
    
    // 添加权限检查方法
    private Mono<Boolean> checkDocPermission(String workspaceId, String docId, String userId, String permission) {
        // 根据权限类型检查
        switch (permission) {
            case "Doc.Read":
                return Mono.just(permissionService.hasWorkspaceAccess(userId, workspaceId));
            case "Doc.Update":
            case "Doc.Restore":
                return Mono.just(permissionService.hasWorkspaceManagePermission(userId, workspaceId));
            default:
                return Mono.just(false);
        }
    }
}