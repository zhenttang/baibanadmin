package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.dto.DocHistoryDto;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.user.dto.UserDto;
import com.yunke.backend.system.domain.entity.SnapshotHistory;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.system.repository.SnapshotHistoryRepository;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.document.service.DocumentHistoryService;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 文档历史记录服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentHistoryServiceImpl implements DocumentHistoryService {

    private final SnapshotHistoryRepository snapshotHistoryRepository;
    private final UserRepository userRepository;
    private final DocBinaryStorageService binaryStorageService;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<DocHistoryDto> getDocumentHistories(
            String workspaceId,
            String pageDocId,
            String before,
            int take
    ) {
        log.info("获取文档历史记录列表: workspaceId={}, pageDocId={}, before={}, take={}",
                workspaceId, pageDocId, before, take);

        Pageable pageable = PageRequest.of(0, take);
        Page<SnapshotHistory> page;

        if (before != null && !before.isEmpty()) {
            try {
                // 尝试解析为毫秒时间戳
                Long beforeTime = Long.parseLong(before);
                page = snapshotHistoryRepository.findByWorkspaceIdAndIdAndTimestampBeforeOrderByTimestampDesc(
                        workspaceId, pageDocId, beforeTime, pageable);
            } catch (NumberFormatException e) {
                // 如果不是数字，尝试作为ISO日期时间解析
                try {
                    LocalDateTime beforeDateTime = LocalDateTime.parse(before, TIMESTAMP_FORMATTER);
                    Long beforeTime = beforeDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    page = snapshotHistoryRepository.findByWorkspaceIdAndIdAndTimestampBeforeOrderByTimestampDesc(
                            workspaceId, pageDocId, beforeTime, pageable);
                } catch (Exception ex) {
                    log.error("解析时间戳失败: {}", before, ex);
                    throw new IllegalArgumentException("Invalid timestamp format: " + before);
                }
            }
        } else {
            page = snapshotHistoryRepository.findByWorkspaceIdAndIdOrderByTimestampDesc(
                    workspaceId, pageDocId, pageable);
        }

        List<DocHistoryDto> histories = page.getContent()
                .stream()
                .map(this::convertToDto)
                .toList();

        long total = snapshotHistoryRepository.countByWorkspaceIdAndId(workspaceId, pageDocId);
        boolean hasMore = page.hasNext();

        // 构建完整的分页响应，确保包含前端需要的所有字段
        return PaginatedResponse.<DocHistoryDto>builder()
                .content(histories)
                .data(histories)      // 兼容字段
                .total(total)
                .totalElements(total)
                .hasMore(hasMore)
                .hasNext(hasMore)
                .currentPage(0)
                .size(take)
                .totalPages((int) Math.ceil((double) total / take))
                .first(true)
                .last(!hasMore)
                .hasPrevious(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getDocumentSnapshot(String workspaceId, String pageDocId, String timestamp) {
        log.info("获取文档快照: workspaceId={}, pageDocId={}, timestamp={}",
                workspaceId, pageDocId, timestamp);

        try {
            // 尝试解析为毫秒时间戳
            Long timestampLong;
            try {
                timestampLong = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                // 如果不是数字，尝试作为ISO日期时间解析
                LocalDateTime timestampTime = LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
                timestampLong = timestampTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            
            Optional<SnapshotHistory> snapshotOpt = snapshotHistoryRepository
                    .findByWorkspaceIdAndIdAndTimestamp(workspaceId, pageDocId, timestampLong);

            if (snapshotOpt.isEmpty()) {
                log.warn("找不到指定的快照: workspaceId={}, pageDocId={}, timestamp={}",
                        workspaceId, pageDocId, timestamp);
                throw new IllegalArgumentException("Snapshot not found");
            }

            SnapshotHistory snapshot = snapshotOpt.get();
            if (snapshot.getBlob() == null) {
                log.warn("快照数据为空: workspaceId={}, pageDocId={}, timestamp={}",
                        workspaceId, pageDocId, timestamp);
                throw new IllegalArgumentException("Snapshot data is empty");
            }

            byte[] data = binaryStorageService.resolvePointer(snapshot.getBlob());
            return data;
        } catch (Exception e) {
            log.error("获取快照失败", e);
            throw new RuntimeException("Failed to get snapshot", e);
        }
    }

    @Override
    @Transactional
    public void recoverDocumentVersion(String workspaceId, String docId, String timestamp) {
        log.info("恢复文档版本: workspaceId={}, docId={}, timestamp={}",
                workspaceId, docId, timestamp);

        try {
            // 尝试解析为毫秒时间戳
            Long timestampLong;
            try {
                timestampLong = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                // 如果不是数字，尝试作为ISO日期时间解析
                LocalDateTime timestampTime = LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
                timestampLong = timestampTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            
            Optional<SnapshotHistory> snapshotOpt = snapshotHistoryRepository
                    .findByWorkspaceIdAndIdAndTimestamp(workspaceId, docId, timestampLong);

            if (snapshotOpt.isEmpty()) {
                log.warn("找不到指定的快照进行恢复: workspaceId={}, docId={}, timestamp={}",
                        workspaceId, docId, timestamp);
                throw new IllegalArgumentException("Snapshot not found for recovery");
            }

            SnapshotHistory snapshot = snapshotOpt.get();
            
            // 创建新的快照记录作为恢复点
            byte[] data = binaryStorageService.resolvePointer(snapshot.getBlob());
            long newTimestamp = System.currentTimeMillis();
            String pointer = binaryStorageService.saveSnapshotHistory(workspaceId, docId, newTimestamp, data);
            SnapshotHistory recoveredSnapshot = SnapshotHistory.builder()
                    .workspaceId(workspaceId)
                    .id(docId)
                    .timestamp(newTimestamp) // 使用毫秒时间戳
                    .blob(binaryStorageService.pointerToBytes(pointer))
                    .state(snapshot.getState())
                    .expiredAt(LocalDateTime.now().plusDays(30)) // 30天后过期
                    .createdBy(snapshot.getCreatedBy())
                    .build();

            snapshotHistoryRepository.save(recoveredSnapshot);
            log.info("文档版本恢复成功: workspaceId={}, docId={}, timestamp={}",
                    workspaceId, docId, timestamp);

        } catch (Exception e) {
            log.error("恢复文档版本失败", e);
            throw new RuntimeException("Failed to recover document version", e);
        }
    }

    @Override
    @Transactional
    public SnapshotHistory saveDocumentSnapshot(
            String workspaceId,
            String docId,
            byte[] blob,
            byte[] state,
            String createdBy
    ) {
        log.info("保存文档快照: workspaceId={}, docId={}, createdBy={}",
                workspaceId, docId, createdBy);

        try {
            long timestamp = System.currentTimeMillis();
            String pointer = binaryStorageService.saveSnapshotHistory(workspaceId, docId, timestamp, blob);
            SnapshotHistory snapshot = SnapshotHistory.builder()
                    .workspaceId(workspaceId)
                    .id(docId)
                    .timestamp(timestamp) // 使用毫秒时间戳
                    .blob(binaryStorageService.pointerToBytes(pointer))
                    .state(state)
                    .expiredAt(LocalDateTime.now().plusDays(30)) // 30天后过期
                    .createdBy(createdBy)
                    .build();

            SnapshotHistory savedSnapshot = snapshotHistoryRepository.save(snapshot);
            log.info("文档快照保存成功: workspaceId={}, docId={}, timestamp={}",
                    workspaceId, docId, savedSnapshot.getTimestamp());

            return savedSnapshot;
        } catch (Exception e) {
            log.error("保存文档快照失败", e);
            throw new RuntimeException("Failed to save document snapshot", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SnapshotHistory> getLatestSnapshot(String workspaceId, String docId) {
        log.debug("获取最新快照: workspaceId={}, docId={}", workspaceId, docId);
        return snapshotHistoryRepository.findLatestByWorkspaceIdAndId(workspaceId, docId);
    }

    @Override
    @Transactional
    public int cleanupExpiredHistories() {
        log.info("清理过期的历史记录");
        try {
            LocalDateTime now = LocalDateTime.now();
            var expired = snapshotHistoryRepository.findExpiredHistories(now);
            expired.forEach(history -> binaryStorageService.deletePointer(history.getBlob()));
            snapshotHistoryRepository.deleteAll(expired);
            log.info("过期历史记录清理完成, 删除条数={}", expired.size());
            return expired.size();
        } catch (Exception e) {
            log.error("清理过期历史记录失败", e);
            throw new RuntimeException("Failed to cleanup expired histories", e);
        }
    }

    @Override
    public DocHistoryDto convertToDto(SnapshotHistory snapshotHistory) {
        if (snapshotHistory == null) {
            return null;
        }

        // 获取创建者信息
        UserDto createdByUser = null;
        if (snapshotHistory.getCreatedBy() != null) {
            Optional<User> userOpt = userRepository.findById(snapshotHistory.getCreatedBy());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                createdByUser = UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .build();
            }
        }

        // 格式化时间戳为 ISO 字符串
        String timestampStr = snapshotHistory.getTimestamp() != null 
            ? LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(snapshotHistory.getTimestamp()),
                    java.time.ZoneId.systemDefault()
                ).format(TIMESTAMP_FORMATTER)
            : null;
        
        // SnapshotHistory 没有 createdAt/updatedAt，使用 timestamp 作为替代
        String createdAtStr = timestampStr;
        String updatedAtStr = timestampStr;

        return DocHistoryDto.builder()
                .id(snapshotHistory.getId())
                .workspaceId(snapshotHistory.getWorkspaceId())
                .docId(snapshotHistory.getId())
                .pageDocId(snapshotHistory.getId())  // 前端使用 pageDocId
                .timestamp(timestampStr)
                .version(timestampStr)  // 使用时间戳作为版本号
                .title("历史版本 " + (timestampStr != null ? timestampStr : ""))  // 临时标题
                .createdAt(createdAtStr)
                .updatedAt(updatedAtStr)
                .createdBy(snapshotHistory.getCreatedBy())
                .editor(createdByUser)
                .blobSize(snapshotHistory.getBlob() != null ? (long) binaryStorageService.resolvePointer(snapshotHistory.getBlob()).length : 0L)
                .hasState(snapshotHistory.getState() != null)
                .seq(0) // 可以根据需要实现序号逻辑
                .build();
    }
}
