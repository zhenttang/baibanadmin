package com.yunke.backend.document.service.impl;

import com.yunke.backend.system.domain.entity.Snapshot;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.document.service.RootDocumentService;
import com.yunke.backend.document.util.YjsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 根文档服务实现
 * 负责工作空间根文档的创建和管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RootDocumentServiceImpl implements RootDocumentService {

    private final SnapshotRepository snapshotRepository;
    private final YjsUtils yjsUtils;

    @Override
    @Transactional
    public Mono<Boolean> createRootDocument(String workspaceId, String creatorUserId) {
        log.info("🏠 [ROOT-DOC-CREATE] 开始为工作空间创建根文档: workspaceId='{}', creatorUserId='{}'", 
                workspaceId, creatorUserId);
        
        return Mono.fromCallable(() -> {
            // 检查根文档是否已存在
            boolean exists = snapshotRepository.existsByWorkspaceIdAndId(workspaceId, workspaceId);
            if (exists) {
                log.info("🏠 [ROOT-DOC-CREATE] 根文档已存在，跳过创建: workspaceId='{}'", workspaceId);
                return true;
            }
            
            // 创建根文档记录
            LocalDateTime now = LocalDateTime.now();
            Snapshot rootDoc = Snapshot.builder()
                    .workspaceId(workspaceId)
                    .id(workspaceId) // 根文档的ID等于工作空间ID
                    .blob(getDefaultRootDocumentContent()) // 设置默认空白文档内容
                    .state(null) // 初始状态向量为空
                    .createdAt(now) // 设置创建时间
                    .updatedAt(now) // 设置更新时间
                    .createdBy(creatorUserId)
                    .updatedBy(creatorUserId)
                    .seq(0) // 初始序列号
                    .build();
            
            // 保存到数据库
            Snapshot saved = snapshotRepository.save(rootDoc);
            
            log.info("🎉 [ROOT-DOC-CREATE] 根文档创建成功!");
            log.info("  📋 workspace_id: '{}'", saved.getWorkspaceId());
            log.info("  📋 guid: '{}'", saved.getId());
            log.info("  📋 blob_size: {} 字节", saved.getBlob() != null ? saved.getBlob().length : 0);
            log.info("  📋 created_by: '{}'", saved.getCreatedBy());
            log.info("  📋 created_at: {}", saved.getCreatedAt());
            
            return true;
        })
        .doOnError(error -> {
            log.error("❌ [ROOT-DOC-CREATE] 根文档创建失败: workspaceId='{}', error={}", 
                    workspaceId, error.getMessage(), error);
        });
    }

    @Override
    public Mono<Boolean> hasRootDocument(String workspaceId) {
        return Mono.fromCallable(() -> {
            boolean exists = snapshotRepository.existsByWorkspaceIdAndId(workspaceId, workspaceId);
            log.debug("🏠 [ROOT-DOC-CHECK] 检查根文档是否存在: workspaceId='{}', exists={}", workspaceId, exists);
            return exists;
        });
    }

    @Override
    public byte[] getDefaultRootDocumentContent() {
        // 使用 YjsUtils 创建符合 AFFiNE 标准的根文档
        // 根文档ID等于工作空间ID，这是AFFiNE的约定
        byte[] rootDocument = yjsUtils.createMinimalValidYjsDoc("root");
        
        log.debug("🏠 [ROOT-DOC-CONTENT] 使用YjsUtils生成根文档内容: size={} 字节", rootDocument.length);
        return rootDocument;
}
}