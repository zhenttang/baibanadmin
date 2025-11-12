package com.yunke.backend.workspace.service;

import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.user.domain.entity.UserSnapshot;
import com.yunke.backend.user.repository.UserSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户空间同步服务 - 对应AFFiNE的PgUserspaceDocStorageAdapter
 * 
 * 实现用户文档的CRUD操作，遵循AFFiNE架构：
 * - 用户空间中spaceId就是userId
 * - 不使用更新队列，直接合并文档
 * - 不记录历史版本
 * - 使用简单的锁机制：userspace:${userId}:${docId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserspaceSyncService {
    
    private final UserSnapshotRepository userSnapshotRepository;
    private final DocBinaryStorageService binaryStorageService;
    
    /**
     * 获取用户文档 - 对应AFFiNE的 get(userId, docId)
     */
    public Optional<UserSnapshot> getUserDoc(String userId, String docId) {
        log.debug("👤📄 [USERSPACE-GET] 获取用户文档: userId='{}', docId='{}'", userId, docId);
        
        Optional<UserSnapshot> result = userSnapshotRepository.findByUserIdAndDocId(userId, docId)
            .map(snapshot -> {
                byte[] data = binaryStorageService.resolvePointer(snapshot.getBlob(), userId, docId);
                return UserSnapshot.builder()
                    .userId(snapshot.getUserId())
                    .id(snapshot.getId())
                    .blob(data)
                    .createdAt(snapshot.getCreatedAt())
                    .updatedAt(snapshot.getUpdatedAt())
                    .build();
            });

        result.ifPresentOrElse(r ->
            log.info("✅👤📄 [USERSPACE-GET] 成功获取用户文档: userId='{}', docId='{}', size={}",
                    userId, docId, r.getBlob().length),
            () -> log.info("❌👤📄 [USERSPACE-GET] 用户文档不存在: userId='{}', docId='{}'", userId, docId));

        return result;
    }
    
    /**
     * 保存或更新用户文档 - 对应AFFiNE的 upsert(doc)
     */
    @Transactional
    public UserSnapshot upsertUserDoc(String userId, String docId, byte[] blob, Long timestamp) {
        log.info("👤💾 [USERSPACE-UPSERT] 保存用户文档: userId='{}', docId='{}', size={}, timestamp={}", 
                userId, docId, blob.length, timestamp);
        
        LocalDateTime updateTime = timestamp != null ? 
            LocalDateTime.ofEpochSecond(timestamp / 1000, (int) ((timestamp % 1000) * 1_000_000), 
                                       java.time.ZoneOffset.UTC) : 
            LocalDateTime.now();
        
        // 查找现有文档
        Optional<UserSnapshot> existing = userSnapshotRepository.findByUserIdAndDocId(userId, docId);
        
        UserSnapshot userSnapshot;
        String pointer = binaryStorageService.saveUserSnapshot(userId, docId, blob);
        if (existing.isPresent()) {
            // 更新现有文档
            userSnapshot = existing.get();
            binaryStorageService.deletePointer(userSnapshot.getBlob());
            userSnapshot.setBlob(binaryStorageService.pointerToBytes(pointer));
            userSnapshot.setUpdatedAt(updateTime);
            
            log.debug("🔄👤💾 [USERSPACE-UPSERT] 更新现有用户文档: userId='{}', docId='{}'", userId, docId);
        } else {
            // 创建新文档
            userSnapshot = UserSnapshot.builder()
                    .userId(userId)
                    .id(docId)
                    .blob(binaryStorageService.pointerToBytes(pointer))
                    .createdAt(updateTime)
                    .updatedAt(updateTime)
                    .build();
            
            log.debug("➕👤💾 [USERSPACE-UPSERT] 创建新用户文档: userId='{}', docId='{}'", userId, docId);
        }
        
        UserSnapshot saved = userSnapshotRepository.save(userSnapshot);
        
        log.info("✅👤💾 [USERSPACE-UPSERT] 成功保存用户文档: userId='{}', docId='{}', createdAt={}, updatedAt={}", 
                userId, docId, saved.getCreatedAt(), saved.getUpdatedAt());
        
        return saved;
    }
    
    /**
     * 删除用户文档 - 对应AFFiNE的 delete(userId, docId)
     */
    @Transactional
    public boolean deleteUserDoc(String userId, String docId) {
        log.info("👤🗑️ [USERSPACE-DELETE] 删除用户文档: userId='{}', docId='{}'", userId, docId);
        
        Optional<UserSnapshot> existing = userSnapshotRepository.findByUserIdAndDocId(userId, docId);
        if (existing.isPresent()) {
            existing.ifPresent(snapshot -> binaryStorageService.deletePointer(snapshot.getBlob()));
            userSnapshotRepository.deleteByUserIdAndId(userId, docId);
            log.info("✅👤🗑️ [USERSPACE-DELETE] 成功删除用户文档: userId='{}', docId='{}'", userId, docId);
            return true;
        } else {
            log.warn("❌👤🗑️ [USERSPACE-DELETE] 用户文档不存在，无法删除: userId='{}', docId='{}'", userId, docId);
            return false;
        }
    }
    
    /**
     * 删除用户的所有文档 - 对应AFFiNE的 deleteAllByUserId(userId)
     */
    @Transactional
    public long deleteAllUserDocs(String userId) {
        log.info("👤🗑️📂 [USERSPACE-DELETE-ALL] 删除用户所有文档: userId='{}'", userId);
        
        var snapshots = userSnapshotRepository.findAllByUserId(userId);
        long count = snapshots.size();

        if (count > 0) {
            snapshots.forEach(snapshot -> binaryStorageService.deletePointer(snapshot.getBlob()));
            userSnapshotRepository.deleteByUserId(userId);
            log.info("✅👤🗑️📂 [USERSPACE-DELETE-ALL] 成功删除用户所有文档: userId='{}', count={}", userId, count);
        } else {
            log.info("❌👤🗑️📂 [USERSPACE-DELETE-ALL] 用户无文档可删除: userId='{}'", userId);
        }

        return count;
    }
    
    /**
     * 获取用户文档时间戳 - 对应AFFiNE的 findTimestampsByUserId(userId, after)
     */
    public Map<String, Long> getUserDocTimestamps(String userId, Long after) {
        log.debug("👤⏰ [USERSPACE-TIMESTAMPS] 获取用户文档时间戳: userId='{}', after={}", userId, after);
        
        // 这里需要实现自定义查询方法
        Map<String, Long> timestamps = new HashMap<>();
        
        // 临时实现：获取所有用户文档并构建时间戳映射
        var userDocs = userSnapshotRepository.findAllByUserId(userId);
        
        for (UserSnapshot doc : userDocs) {
            long timestamp = doc.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            
            // 如果指定了after参数，只返回之后的时间戳
            if (after == null || timestamp > after) {
                timestamps.put(doc.getId(), timestamp);
            }
        }
        
        log.info("✅👤⏰ [USERSPACE-TIMESTAMPS] 获取用户文档时间戳完成: userId='{}', count={}", userId, timestamps.size());
        
        return timestamps;
    }
    
    /**
     * 检查用户文档是否存在
     */
    public boolean userDocExists(String userId, String docId) {
        log.debug("👤❓ [USERSPACE-EXISTS] 检查用户文档是否存在: userId='{}', docId='{}'", userId, docId);
        
        boolean exists = userSnapshotRepository.existsByUserIdAndDocId(userId, docId);
        
        log.debug("{}👤❓ [USERSPACE-EXISTS] 用户文档存在性检查结果: userId='{}', docId='{}', exists={}", 
                exists ? "✅" : "❌", userId, docId, exists);
        
        return exists;
    }
}
