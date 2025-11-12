package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.dto.DocRecord;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.system.domain.entity.Update;
import com.yunke.backend.system.domain.entity.Snapshot;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.system.repository.UpdateRepository;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.workspace.service.WorkspaceDocService;

import com.yunke.backend.document.service.DocWriter;
import com.yunke.backend.document.event.DocCreatedEvent;
import com.yunke.backend.monitor.MetricsCollector;
import com.yunke.backend.storage.impl.WorkspaceDocStorageAdapter;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.document.util.YjsUtils;
import com.yunke.backend.document.service.YjsServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 工作空间文档服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceDocServiceImpl implements WorkspaceDocService {

    private final WorkspaceDocRepository docRepository;
    private final UpdateRepository updateRepository;
    private final SnapshotRepository snapshotRepository;
    private final PermissionService permissionService;
    private final MetricsCollector metricsCollector;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DocBinaryStorageService binaryStorageService;
    private final WorkspaceDocStorageAdapter storageAdapter;
    private final YjsServiceClient yjsServiceClient;  // 🔥 YJS微服务客户端

    // 新增的依赖
    @Qualifier("databaseDocWriter")
    private final DocWriter docWriter;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public WorkspaceDoc createDoc(String workspaceId, String userId, String title, String requestedDocId) {
        String docCreationId = UUID.randomUUID().toString().substring(0, 8);
        
        log.info("📝🆕 [DOC-CREATE-001-{}] 开始创建文档流程", docCreationId);
        log.info("📝🆕 [DOC-CREATE-002-{}] 输入参数: workspaceId={}, userId={}, title={}", 
                 docCreationId, workspaceId, userId, title);
        
        try {
            // 步骤1: 权限检查
            log.info("📝🆕 [DOC-CREATE-003-{}] 开始权限检查", docCreationId);
            if (!permissionService.hasWorkspaceAccess(userId, workspaceId)) {
                log.error("❌🆕 [DOC-CREATE-004-{}] 权限检查失败: 用户 {} 无权访问工作空间 {}", 
                         docCreationId, userId, workspaceId);
                throw new IllegalArgumentException("No access to workspace: " + workspaceId);
            }
            log.info("✅🆕 [DOC-CREATE-005-{}] 权限检查通过", docCreationId);
            
            // 步骤2: 生成或使用传入的文档ID
            String resolvedDocId = (requestedDocId != null && !requestedDocId.isBlank())
                ? requestedDocId
                : UUID.randomUUID().toString();
            log.info("📝🆕 [DOC-CREATE-006-{}] 使用文档ID: {}", docCreationId, resolvedDocId);
            
            // 步骤3: 创建元数据记录
            log.info("📝🆕 [DOC-CREATE-007-{}] 开始创建文档元数据记录", docCreationId);
            WorkspaceDoc doc = new WorkspaceDoc();
            // ✅ 修复：使用 setDocId 而不是 setId（WorkspaceDoc 使用复合主键，字段名是 docId）
            doc.setDocId(resolvedDocId);
            doc.setWorkspaceId(workspaceId);
            doc.setTitle(title != null ? title : "Untitled");
            doc.setCreatedAt(Instant.now());
            doc.setUpdatedAt(Instant.now());
            
            log.info("📝🆕 [DOC-CREATE-008-{}] 文档元数据准备完成: docId={}, workspaceId={}, title={}", 
                     docCreationId, resolvedDocId, workspaceId, doc.getTitle());
            
            // 步骤4: 保存到数据库
            log.info("📝🆕 [DOC-CREATE-009-{}] 开始保存文档元数据到数据库", docCreationId);
            WorkspaceDoc savedDoc = docRepository.save(doc);
            log.info("✅🆕 [DOC-CREATE-010-{}] 文档元数据保存成功: workspace_pages表记录已创建", docCreationId);
            
            // 步骤5: 创建初始文档内容
            log.info("📝🆕 [DOC-CREATE-011-{}] 开始创建初始文档内容", docCreationId);
            try {
                DocRecord initialDoc = docWriter.createInitialDoc(workspaceId, resolvedDocId, userId).block();
                
                if (initialDoc == null) {
                    log.warn("⚠️🆕 [DOC-CREATE-012-{}] 初始文档内容创建失败: DocWriter返回null", docCreationId);
                } else {
                    log.info("✅🆕 [DOC-CREATE-013-{}] 初始文档内容创建成功: 大小={}字节, 时间戳={}", 
                            docCreationId, initialDoc.getBlob().length, initialDoc.getTimestamp());
                    
                    // 验证数据确实保存到了数据库
                    log.info("📝🆕 [DOC-CREATE-014-{}] 开始验证文档内容是否已保存到数据库", docCreationId);
                    try {
                        var verifyDoc = storageAdapter.getDoc(workspaceId, resolvedDocId);
                        if (verifyDoc != null && verifyDoc.getBlob() != null) {
                            log.info("✅🆕 [DOC-CREATE-015-{}] 数据库验证成功: 快照数据已存在, 大小={}字节", 
                                    docCreationId, verifyDoc.getBlob().length);
                        } else {
                            log.warn("⚠️🆕 [DOC-CREATE-016-{}] 数据库验证失败: 快照数据不存在或为空", docCreationId);
                        }
                    } catch (Exception verifyError) {
                        log.error("❌🆕 [DOC-CREATE-017-{}] 数据库验证异常: {}", docCreationId, verifyError.getMessage());
                    }
                }
                
            } catch (Exception contentError) {
                log.error("❌🆕 [DOC-CREATE-018-{}] 创建文档内容时发生异常: {}", docCreationId, contentError.getMessage(), contentError);
                // 即使创建内容失败，我们仍然返回已创建的元数据
                log.info("📝🆕 [DOC-CREATE-019-{}] 尽管内容创建失败，文档元数据已创建，可在后续访问时创建内容", docCreationId);
            }
            
            // 步骤6: 发送事件通知
            log.info("📝🆕 [DOC-CREATE-020-{}] 开始发送文档创建事件通知", docCreationId);
            try {
                eventPublisher.publishEvent(new DocCreatedEvent(this, workspaceId, resolvedDocId, userId));
                log.info("✅🆕 [DOC-CREATE-021-{}] 文档创建事件通知发送成功", docCreationId);
            } catch (Exception eventError) {
                log.error("❌🆕 [DOC-CREATE-022-{}] 发送事件通知失败: {}", docCreationId, eventError.getMessage());
            }
            
            // 步骤7: 记录文档访问
            log.info("📝🆕 [DOC-CREATE-023-{}] 开始记录文档访问", docCreationId);
            try {
                recordDocAccess(resolvedDocId, userId);
                log.info("✅🆕 [DOC-CREATE-024-{}] 文档访问记录成功", docCreationId);
            } catch (Exception accessError) {
                log.warn("⚠️🆕 [DOC-CREATE-025-{}] 记录文档访问失败: {}", docCreationId, accessError.getMessage());
            }
            
            // 步骤8: 更新缓存
            log.info("📝🆕 [DOC-CREATE-026-{}] 开始更新缓存", docCreationId);
            try {
                // 清理相关缓存以确保数据一致性
                redisTemplate.delete("doc_access:" + resolvedDocId + ":*");
                redisTemplate.delete("doc_collaborators:" + resolvedDocId);
                log.info("✅🆕 [DOC-CREATE-027-{}] 缓存更新成功", docCreationId);
            } catch (Exception cacheError) {
                log.warn("⚠️🆕 [DOC-CREATE-028-{}] 缓存更新失败: {}", docCreationId, cacheError.getMessage());
            }
            
            // 步骤9: 记录度量指标
            log.info("📝🆕 [DOC-CREATE-029-{}] 开始记录度量指标", docCreationId);
            try {
                metricsCollector.recordDocOperation("create", resolvedDocId);
                log.info("✅🆕 [DOC-CREATE-030-{}] 度量指标记录成功", docCreationId);
            } catch (Exception metricsError) {
                log.warn("⚠️🆕 [DOC-CREATE-031-{}] 度量指标记录失败: {}", docCreationId, metricsError.getMessage());
            }
            
            log.info("🎉🆕 [DOC-CREATE-032-{}] 文档创建流程完成! 文档ID: {}, 标题: {}", 
                     docCreationId, savedDoc.getId(), savedDoc.getTitle());
            
            return savedDoc;
            
        } catch (Exception e) {
            log.error("💥🆕 [DOC-CREATE-033-{}] 文档创建流程失败: {}", docCreationId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "docs", key = "#id", unless="#result == null")
    public Optional<WorkspaceDoc> findById(String id) {
        log.info("🔍 [DOC-FIND] 查找文档: docId='{}'", id);
        // 使用自定义查询直接查找文档
        Optional<WorkspaceDoc> result = docRepository.findByDocId(id);
        if (result.isPresent()) {
            WorkspaceDoc doc = result.get();
            log.info("🔍 [DOC-FIND] ✅ 找到文档: docId='{}', workspaceId='{}', title='{}', isPublic={}", 
                    id, doc.getWorkspaceId(), doc.getTitle(), doc.getPublic());
        } else {
            log.warn("🔍 [DOC-FIND] ❌ 文档不存在: docId='{}'", id);
        }
        return result;
    }

    @Override
    @Cacheable(value = "workspace_doc_mapping", key = "#docId", unless="#result == null")
    public Optional<String> findWorkspaceIdByDocId(String docId) {
        log.info("Finding workspace ID for document: {}", docId);
        
        // 从数据库查找文档
        Optional<WorkspaceDoc> doc = docRepository.findByDocId(docId);
        
        if (doc.isPresent()) {
            String workspaceId = doc.get().getWorkspaceId();
            log.info("Found workspace ID: {} for document: {}", workspaceId, docId);
            return Optional.of(workspaceId);
        } else {
            log.warn("Document not found in database: {}", docId);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "docs", key = "#doc.id")
    public WorkspaceDoc updateDoc(WorkspaceDoc doc) {
        log.info("Updating document: {}", doc.getId());
        
        Optional<WorkspaceDoc> existingDoc = findById(doc.getId());
        if (existingDoc.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + doc.getId());
        }
        
        WorkspaceDoc current = existingDoc.get();
        
        // 更新允许修改的字段
        if (doc.getTitle() != null) {
            current.setTitle(doc.getTitle());
        }
        
        if (doc.getPublic() != null) {
            current.setPublic(doc.getPublic());
        }
        
        current.setUpdatedAt(Instant.now());
        
        WorkspaceDoc updatedDoc = docRepository.save(current);
        
        // 记录指标
        metricsCollector.recordDocOperation("update", updatedDoc.getId());
        
        log.info("Document updated successfully: {}", updatedDoc.getId());
        return updatedDoc;
    }

    @Override
    @Transactional
    @CacheEvict(value = "docs", key = "#id")
    public void deleteDoc(String id) {
        log.info("Deleting document: {}", id);
        
        Optional<WorkspaceDoc> doc = findById(id);
        if (doc.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + id);
        }
        
        WorkspaceDoc workspaceDoc = doc.get();
        String workspaceId = workspaceDoc.getWorkspaceId();
        String docId = workspaceDoc.getDocId();
        
        // 1. 删除元数据
        docRepository.deleteByWorkspaceIdAndDocId(workspaceId, docId);
        
        // 2. 删除快照和更新数据
        try {
            Boolean deleted = docWriter.deleteDoc(workspaceId, docId).block();
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Document content deleted successfully: {}", docId);
            } else {
                log.warn("Failed to delete document content: {}", docId);
            }
        } catch (Exception e) {
            log.error("Error deleting document content: {}", e.getMessage(), e);
        }
        
        // 清除相关缓存
        redisTemplate.delete("doc_access:" + id + ":*");
        redisTemplate.delete("doc_collaborators:" + id);
        
        // 记录指标
        metricsCollector.recordDocOperation("delete", id);
        
        log.info("Document deleted successfully: {}", id);
    }

    @Override
    @Cacheable(value = "docs", key = "'workspace:' + #workspaceId")
    public List<WorkspaceDoc> getWorkspaceDocs(String workspaceId) {
        log.debug("Getting documents for workspace: {}", workspaceId);
        return docRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public Page<WorkspaceDoc> getWorkspaceDocs(String workspaceId, Pageable pageable) {
        log.debug("Getting documents for workspace with pagination: {}", workspaceId);
        return docRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Override
    public List<WorkspaceDoc> searchDocs(String workspaceId, String keyword) {
        log.debug("Searching documents in workspace: {} with keyword: {}", workspaceId, keyword);
        // 添加默认分页
        PageRequest pageable = PageRequest.of(0, 100);
        return docRepository.searchByKeyword(workspaceId, keyword, pageable).getContent();
    }

    @Override
    @Cacheable(value = "docs", key = "'recent:' + #userId + ':' + #limit")
    public List<WorkspaceDoc> getRecentDocs(String userId, int limit) {
        log.debug("Getting recent documents for user: {} (limit: {})", userId, limit);
        
        // 从Redis获取最近访问的文档ID列表
        String key = "user_recent_docs:" + userId;
        List<Object> recentDocIds = redisTemplate.opsForList().range(key, 0, limit - 1);
        
        if (recentDocIds == null || recentDocIds.isEmpty()) {
            return List.of();
        }
        
        // 根据ID查询文档
        List<String> docIds = recentDocIds.stream()
                .map(Object::toString)
                .toList();
        
        // 需要遍历所有工作空间查找文档
        return docRepository.findAll().stream()
                .filter(doc -> docIds.contains(doc.getDocId()))
                .toList();
    }

    @Override
    public void recordDocAccess(String docId, String userId) {
        log.debug("Recording document access: {} by user: {}", docId, userId);
        
        try {
            // 记录到Redis
            String userRecentKey = "user_recent_docs:" + userId;
            String docAccessKey = "doc_access:" + docId + ":" + userId;
            
            // 更新用户最近访问的文档列表
            redisTemplate.opsForList().leftPush(userRecentKey, docId);
            redisTemplate.opsForList().trim(userRecentKey, 0, 99); // 保留最近100个
            redisTemplate.expire(userRecentKey, 30, TimeUnit.DAYS);
            
            // 记录访问时间
            redisTemplate.opsForValue().set(docAccessKey, Instant.now().toString(), 30, TimeUnit.DAYS);
            
            // 更新文档协作者列表
            String collaboratorsKey = "doc_collaborators:" + docId;
            redisTemplate.opsForSet().add(collaboratorsKey, userId);
            redisTemplate.expire(collaboratorsKey, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Error recording document access: {}", e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "docs", key = "'collaborators:' + #docId")
    public List<String> getDocCollaborators(String docId) {
        log.debug("Getting collaborators for document: {}", docId);
        
        String collaboratorsKey = "doc_collaborators:" + docId;
        return redisTemplate.opsForSet().members(collaboratorsKey)
                .stream()
                .map(Object::toString)
                .toList();
    }

    @Override
    public boolean hasDocAccess(String docId, String userId) {
        log.info("🔐 [PERMISSION-CHECK] 开始检查文档访问权限: docId='{}', userId='{}'", docId, userId);

        // 查找文档（通过 docId）
        Optional<WorkspaceDoc> doc = findById(docId);
        if (doc.isEmpty()) {
            log.warn("🔐 [PERMISSION-CHECK] ⚠️ 通过docId未找到文档: docId='{}'", docId);
            log.warn("🔐 [PERMISSION-CHECK] 文档可能不存在，或者需要使用workspaceId+docId组合查找");
            // 文档不存在，无法判断权限
            return false;
        }

        WorkspaceDoc document = doc.get();
        log.info("🔐 [PERMISSION-CHECK] ✅ 文档存在: docId='{}', workspaceId='{}', title='{}', isPublic={}", 
                docId, document.getWorkspaceId(), document.getTitle(), document.getPublic());

        // 如果文档是公开的，任何人都可以访问
        if (document.getPublic() != null && document.getPublic()) {
            log.info("🔐 [PERMISSION-CHECK] ✅ 文档是公开的，允许访问: docId='{}'", docId);
            return true;
        }

        // 检查工作空间权限
        log.info("🔐 [PERMISSION-CHECK] 检查工作空间访问权限: userId='{}', workspaceId='{}'", 
                userId, document.getWorkspaceId());
        boolean hasAccess = permissionService.hasWorkspaceAccess(userId, document.getWorkspaceId());
        if (hasAccess) {
            log.info("🔐 [PERMISSION-CHECK] ✅ 工作空间权限检查通过: userId='{}', workspaceId='{}'", 
                    userId, document.getWorkspaceId());
        } else {
            log.warn("🔐 [PERMISSION-CHECK] ❌ 工作空间权限检查失败: userId='{}', workspaceId='{}'", 
                    userId, document.getWorkspaceId());
        }
        return hasAccess;
    }
    
    /**
     * 检查文档访问权限（带工作空间ID，用于更精确的查找）
     * 如果文档不存在但用户有工作空间权限，会自动创建文档元数据
     */
    public boolean hasDocAccess(String workspaceId, String docId, String userId) {
        log.info("🔐 [PERMISSION-CHECK] 开始检查文档访问权限（带workspaceId）: workspaceId='{}', docId='{}', userId='{}'", 
                workspaceId, docId, userId);

        // 先通过 workspaceId + docId 查找（更精确）
        Optional<WorkspaceDoc> doc = docRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
        if (doc.isEmpty()) {
            log.warn("🔐 [PERMISSION-CHECK] ⚠️ 通过workspaceId+docId未找到文档: workspaceId='{}', docId='{}'", 
                    workspaceId, docId);
            // 尝试仅通过 docId 查找（可能文档存在但workspaceId不匹配）
            doc = findById(docId);
            if (doc.isEmpty()) {
                log.info("🔐 [PERMISSION-CHECK] 📝 文档不存在: docId='{}'，这是创建新文档的正常情况", docId);
                // ✅ 文档不存在，但如果用户有工作空间权限，自动创建文档元数据（用于创建新文档）
                log.info("🔐 [PERMISSION-CHECK] 检查用户是否有工作空间权限（用于自动创建文档元数据）");
                boolean hasWorkspaceAccess = permissionService.hasWorkspaceAccess(userId, workspaceId);
                if (hasWorkspaceAccess) {
                    log.info("🔐 [PERMISSION-CHECK] ✅ 用户有工作空间权限，自动创建文档元数据: userId='{}', workspaceId='{}', docId='{}'", 
                            userId, workspaceId, docId);
                    // ✅ 自动创建文档元数据（不创建快照，快照由后续的更新请求创建）
                    try {
                        ensureMetadataExists(workspaceId, docId);
                        log.info("🔐 [PERMISSION-CHECK] ✅ 文档元数据已自动创建");
                        return true;
                    } catch (Exception e) {
                        log.error("🔐 [PERMISSION-CHECK] ❌ 自动创建文档元数据失败: {}", e.getMessage(), e);
                        // 即使创建失败，也允许访问（因为用户有工作空间权限）
                        return true;
                    }
                } else {
                    log.warn("🔐 [PERMISSION-CHECK] ❌ 用户无工作空间权限: userId='{}', workspaceId='{}'", 
                            userId, workspaceId);
                    return false;
                }
            } else {
                log.warn("🔐 [PERMISSION-CHECK] ⚠️ 文档存在但workspaceId不匹配: 请求workspaceId='{}', 文档workspaceId='{}'", 
                        workspaceId, doc.get().getWorkspaceId());
            }
        }

        WorkspaceDoc document = doc.get();
        log.info("🔐 [PERMISSION-CHECK] ✅ 文档存在: docId='{}', workspaceId='{}', title='{}', isPublic={}", 
                docId, document.getWorkspaceId(), document.getTitle(), document.getPublic());

        // 如果文档是公开的，任何人都可以访问
        if (document.getPublic() != null && document.getPublic()) {
            log.info("🔐 [PERMISSION-CHECK] ✅ 文档是公开的，允许访问: docId='{}'", docId);
            return true;
        }

        // 检查工作空间权限
        log.info("🔐 [PERMISSION-CHECK] 检查工作空间访问权限: userId='{}', workspaceId='{}'", 
                userId, document.getWorkspaceId());
        boolean hasAccess = permissionService.hasWorkspaceAccess(userId, document.getWorkspaceId());
        if (hasAccess) {
            log.info("🔐 [PERMISSION-CHECK] ✅ 工作空间权限检查通过: userId='{}', workspaceId='{}'", 
                    userId, document.getWorkspaceId());
        } else {
            log.warn("🔐 [PERMISSION-CHECK] ❌ 工作空间权限检查失败: userId='{}', workspaceId='{}'", 
                    userId, document.getWorkspaceId());
        }
        return hasAccess;
    }

    @Override
    public boolean hasDocEditPermission(String docId, String userId) {
        log.debug("Checking document edit permission: {} for user: {}", docId, userId);
        
        // 查找文档
        Optional<WorkspaceDoc> doc = findById(docId);
        if (doc.isEmpty()) {
            return false;
        }
        
        // 检查工作空间管理权限
        return permissionService.hasWorkspaceManagePermission(userId, doc.get().getWorkspaceId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "docs", key = "#docId")
    public void setDocTitle(String docId, String title) {
        log.info("Setting document title: {} -> {}", docId, title);
        
        Optional<WorkspaceDoc> doc = findById(docId);
        if (doc.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        
        WorkspaceDoc current = doc.get();
        current.setTitle(title);
        current.setUpdatedAt(Instant.now());
        
        docRepository.save(current);
        
        log.info("Document title updated successfully: {}", docId);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "docs", key = "#docId")
    public void setDocPublic(String docId, boolean isPublic, String publicPermission, String publicMode) {
        log.info("Setting document public status: {} -> {}, permission: {}, mode: {}", 
                docId, isPublic, publicPermission, publicMode);
        
        Optional<WorkspaceDoc> doc = findById(docId);
        if (doc.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        
        WorkspaceDoc current = doc.get();
        current.setPublic(isPublic);
        
        // 设置公开权限和模式
        if (publicPermission != null && !publicPermission.isEmpty()) {
            current.setPublicPermission(publicPermission);
        }
        if (publicMode != null && !publicMode.isEmpty()) {
            current.setPublicMode(publicMode);
        }
        
        current.setUpdatedAt(Instant.now());
        
        docRepository.save(current);
        
        log.info("Document public status updated successfully: {}", docId);
    }
    
    @Override
    public WorkspaceDocService.DocStats getDocStats(String docId) {
        log.debug("Getting document stats: {}", docId);
        
        Optional<WorkspaceDoc> doc = findById(docId);
        if (doc.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        
        WorkspaceDoc workspaceDoc = doc.get();
        
        // 从Redis获取协作者数量
        String collaboratorsKey = "doc_collaborators:" + docId;
        Long collaboratorsCount = redisTemplate.opsForSet().size(collaboratorsKey);
        
        // 获取访问和编辑计数（这里简化实现）
        int viewCount = 0;
        int editCount = 0;
        try {
            String viewKey = "doc_views:" + docId;
            Object viewObj = redisTemplate.opsForValue().get(viewKey);
            if (viewObj != null) {
                viewCount = Integer.parseInt(viewObj.toString());
            }
            
            String editKey = "doc_edits:" + docId;
            Object editObj = redisTemplate.opsForValue().get(editKey);
            if (editObj != null) {
                editCount = Integer.parseInt(editObj.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to get document view/edit counts", e);
        }
        
        // 获取更新时间并转换为Instant
        java.time.Instant lastModified;
        java.time.LocalDateTime updatedAt = workspaceDoc.getUpdatedAt();
        if (updatedAt != null) {
            lastModified = updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant();
        } else {
            lastModified = java.time.Instant.now();
        }
        
        // 返回符合接口定义的DocStats记录
        return new WorkspaceDocService.DocStats(
                viewCount,
                editCount,
                collaboratorsCount != null ? collaboratorsCount.intValue() : 0,
                lastModified
        );
    }

    @Override
    @Transactional
    public boolean applyYjsUpdate(String workspaceId, String docId, byte[] updateData, String userId, Long timestamp) {
        long startTime = System.currentTimeMillis();
        log.info("💾 [WorkspaceDocService-数据库] 开始处理YJS更新数据库操作");
        log.info("  📊 请求参数: workspaceId={}, docId={}, userId={}", workspaceId, docId, userId);
        log.info("  📦 数据详情: dataSize={}字节, timestamp={}", updateData.length, timestamp);
        
        // 记录详细的调试信息
        log.info("  🔍 数据分析: 数据类型={}, 前20字节={}", 
                updateData.getClass().getSimpleName(), 
                java.util.Arrays.toString(java.util.Arrays.copyOf(updateData, Math.min(20, updateData.length))));
        
        try {
            // 检查数据库连接和表是否存在
            log.info("  🔄 检查数据库状态...");
            try {
                long existingUpdatesCount = updateRepository.countByWorkspaceIdAndId(workspaceId, docId);
                log.info("  📊 数据库状态检查: updates表存在, 现有记录数={}", existingUpdatesCount);
            } catch (Exception dbCheckException) {
                log.error("  ❌ 数据库状态检查失败: {}", dbCheckException.getMessage());
                log.error("  🔍 这可能表示updates表不存在或有连接问题", dbCheckException);
                throw new RuntimeException("数据库状态检查失败: " + dbCheckException.getMessage(), dbCheckException);
            }
            
            // 1. 保存更新记录到 updates 表
            log.info("  💾 开始保存到updates表...");
            try {
                int nextSeq = getNextUpdateSequence(workspaceId, docId);
                log.info("  🔢 获取下一个序号: {}", nextSeq);
                
                String pointer = binaryStorageService.saveUpdate(workspaceId, docId, nextSeq, updateData);
                Update update = Update.builder()
                        .workspaceId(workspaceId)
                        .id(docId)
                        .createdAt(LocalDateTime.now())
                        .blob(binaryStorageService.pointerToBytes(pointer))
                        .createdBy(userId)
                        .seq(nextSeq)
                        .build();

                log.info("  🔄 准备保存Update实体: workspaceId={}, id={}, seq={}, originalSize={}, createdBy={}", 
                        update.getWorkspaceId(), update.getId(), update.getSeq(), 
                        updateData.length, update.getCreatedBy());

                Update savedUpdate = updateRepository.save(update);
                
                log.info("  ✅ 更新记录已保存到updates表");
                log.info("    📊 保存结果: id={}, workspaceId={}, seq={}, createdAt={}", 
                        savedUpdate.getId(), savedUpdate.getWorkspaceId(), 
                        savedUpdate.getSeq(), savedUpdate.getCreatedAt());
                
                // 验证数据确实保存了
                long updatesCount = updateRepository.countByWorkspaceIdAndId(workspaceId, docId);
                log.info("  📈 验证保存结果: 当前文档在updates表中的记录数={}", updatesCount);
                
            } catch (Exception updateSaveException) {
                log.error("  ❌ 保存到updates表失败: {}", updateSaveException.getMessage());
                log.error("  📚 完整异常", updateSaveException);
                throw new RuntimeException("保存到updates表失败: " + updateSaveException.getMessage(), updateSaveException);
            }
            
            // 2. 更新或创建文档快照到 workspace_pages 表
            log.info("  💾 开始更新workspace_pages表...");
            boolean metadataCreated = false;
            try {
                updateDocumentSnapshot(workspaceId, docId, updateData, userId, timestamp);
                log.info("  ✅ workspace_pages表更新完成");
                metadataCreated = true;
            } catch (Exception snapshotException) {
                log.error("  ❌ 更新workspace_pages表失败: {}", snapshotException.getMessage());
                log.error("  📚 完整异常", snapshotException);
                
                // ✅ 尝试单独创建元数据记录（不依赖存储适配器）
                log.warn("  ⚠️ workspace_pages表更新失败，尝试单独创建元数据...");
                try {
                    ensureMetadataExists(workspaceId, docId);
                    log.info("  ✅ 元数据记录已创建");
                    metadataCreated = true;
                } catch (Exception metadataException) {
                    log.error("  ❌ 创建元数据记录也失败: {}", metadataException.getMessage());
                    log.error("  📚 完整异常", metadataException);
                    // 不抛出异常，因为快照可能已经保存成功
                    // 但记录错误以便后续修复
                }
            }
            
            // 3. 创建或更新快照到 snapshots 表
            log.info("  💾 开始更新snapshots表...");
            // ✅ 在创建快照前确保元数据存在（在 try-catch 之外，如果失败则抛出异常）
            if (!metadataCreated) {
                log.warn("  ⚠️ 元数据未创建，在快照更新前确保元数据存在...");
                ensureMetadataExists(workspaceId, docId);
                log.info("  ✅ 元数据记录已创建");
            }
            
            // ✅ 调用 updateSnapshotRecord，如果失败会抛出异常，确保数据一致性
            updateSnapshotRecord(workspaceId, docId, updateData, userId);
            log.info("  ✅ snapshots表更新完成");
            
            // 4. 记录指标
            try {
                log.info("  📊 记录指标...");
                metricsCollector.recordDocOperation("yjs_update", docId);
                log.info("  ✅ 指标记录完成");
            } catch (Exception metricsException) {
                log.warn("  ⚠️ 指标记录失败: {}", metricsException.getMessage());
            }
            
            // 5. 增加编辑计数
            try {
                log.info("  📈 增加编辑计数...");
                incrementDocEditCount(docId);
                log.info("  ✅ 编辑计数更新完成");
            } catch (Exception countException) {
                log.warn("  ⚠️ 编辑计数更新失败: {}", countException.getMessage());
            }
            
            // 最终验证 - 检查所有表中的数据
            log.info("  🔍 开始最终验证...");
            try {
                long finalUpdatesCount = updateRepository.countByWorkspaceIdAndId(workspaceId, docId);
                Optional<WorkspaceDoc> finalDoc = docRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
                long finalSnapshotsCount = snapshotRepository.countByWorkspaceId(workspaceId);
                
                log.info("  📊 最终验证结果: updates表记录数={}, workspace_pages表文档存在={}, snapshots表记录数={}", 
                        finalUpdatesCount, finalDoc.isPresent(), finalSnapshotsCount);
                
                if (finalDoc.isPresent()) {
                    WorkspaceDoc doc = finalDoc.get();
                    
                    // 使用新的存储适配器获取文档内容
                    try {
                        var docRecord = storageAdapter.getDoc(workspaceId, docId);
                        if (docRecord != null && docRecord.getBlob() != null) {
                            log.info("  🎉 成功验证: snapshots中的文档数据大小={}B", docRecord.getBlob().length);
                        } else {
                            log.warn("  ⚠️ 验证失败: snapshots中没有找到文档数据");
                        }
                    } catch (Exception e) {
                        log.warn("  ⚠️ 无法从存储适配器获取文档内容: {}", e.getMessage());
                    }
                } else {
                    log.warn("  ⚠️ 验证失败: workspace_pages中没有找到文档记录");
                }
                
                // 详细查询验证 - 最新的3条updates记录
                log.info("  🔍 查询最新的updates记录...");
                List<Update> recentUpdates = updateRepository.findTopByWorkspaceIdAndIdOrderByCreatedAtDesc(workspaceId, docId);
                for (int i = 0; i < Math.min(3, recentUpdates.size()); i++) {
                    Update update = recentUpdates.get(i);
                    int resolvedSize = update.getBlob() != null
                            ? binaryStorageService.resolvePointer(update.getBlob()).length
                            : 0;
                    log.info("    📝 Update[{}]: seq={}, size={}B, createdAt={}, createdBy={}", 
                            i, update.getSeq(), 
                            resolvedSize,
                            update.getCreatedAt(), update.getCreatedBy());
                }
                
                // 查询workspace_pages详细信息
                if (finalDoc.isPresent()) {
                    WorkspaceDoc doc = finalDoc.get();
                    log.info("  📄 workspace_pages详情: docId={}, title='{}', createdAt={}, updatedAt={}",
                            doc.getDocId(), doc.getTitle(), doc.getCreatedAt(), doc.getUpdatedAt());
                    
                    // 尝试从snapshots获取并解析YJS文档内容
                    try {
                        var docRecord = storageAdapter.getDoc(workspaceId, docId);
                        if (docRecord != null && docRecord.getBlob() != null && docRecord.getBlob().length > 0) {
                            log.info("  📊 snapshots数据大小: {}B", docRecord.getBlob().length);
                            try {
                                String plainText = YjsUtils.extractPlainText(docRecord.getBlob());
                                if (plainText != null && plainText.length() > 200) {
                                    log.info("  📝 解析出的明文内容: '{}...' (前200字符)", plainText.substring(0, 200));
                                } else {
                                    log.info("  📝 解析出的明文内容: '{}'", plainText != null ? plainText : "无法解析");
                                }
                                if (plainText != null && plainText.length() > 200) {
                                    log.info("  📄 完整内容长度: {}字符", plainText.length());
                                }
                            } catch (Exception e) {
                                log.warn("  ⚠️ 解析YJS文档内容失败: {}", e.getMessage());
                            }
                        } else {
                            log.warn("  ⚠️ snapshots中没有找到文档内容");
                        }
                    } catch (Exception e) {
                        log.warn("  ⚠️ 无法从存储适配器获取文档内容: {}", e.getMessage());
                    }
                }
                
                // 查询snapshots详细信息
                log.info("  🔍 查询snapshots记录...");
                Optional<Snapshot> finalSnapshot = snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId);
                if (finalSnapshot.isPresent()) {
                    Snapshot snapshot = finalSnapshot.get();
                    int snapshotSize = snapshot.getBlob() != null
                            ? binaryStorageService.resolvePointer(snapshot.getBlob(), snapshot.getWorkspaceId(), snapshot.getId()).length
                            : 0;
                    log.info("  📸 snapshots详情: id={}, seq={}, size={}B, createdAt={}, updatedAt={}, createdBy={}, updatedBy={}",
                            snapshot.getId(), snapshot.getSeq(),
                            snapshotSize,
                            snapshot.getCreatedAt(), snapshot.getUpdatedAt(),
                            snapshot.getCreatedBy(), snapshot.getUpdatedBy());
                } else {
                    log.warn("  ⚠️ 在snapshots表中未找到记录: workspaceId={}, docId={}", workspaceId, docId);
                }
                
            } catch (Exception e) {
                log.error("  ❌ 最终验证失败", e);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ [WorkspaceDocService-数据库] YJS更新数据库操作成功");
            log.info("  📊 处理结果: docId={}, 总耗时={}ms", docId, processingTime);
            
            return true;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ [WorkspaceDocService-数据库] YJS更新数据库操作失败");
            log.error("  📊 失败详情: workspaceId={}, docId={}, userId={}, 耗时={}ms", 
                    workspaceId, docId, userId, processingTime);
            log.error("  🔍 错误信息: {}", e.getMessage());
            log.error("  📚 完整异常堆栈", e);
            return false;
        }
    }
    
    @Override
    public long getDocTimestamp(String workspaceId, String docId) {
        log.debug("【文档时间戳】获取: workspaceId={}, docId={}", workspaceId, docId);
        
        try {
            // 先查看workspace_pages表的最后更新时间
            Optional<WorkspaceDoc> doc = docRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
            if (doc.isPresent() && doc.get().getUpdatedAt() != null) {
                long timestamp = doc.get().getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                log.debug("【文档时间戳】从workspace_pages获取: {}", timestamp);
                return timestamp;
            }
            
            // 查看updates表的最新记录
            List<Update> latestUpdates = updateRepository.findTopByWorkspaceIdAndIdOrderByCreatedAtDesc(workspaceId, docId);
            if (!latestUpdates.isEmpty()) {
                long timestamp = latestUpdates.get(0).getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                log.debug("【文档时间戳】从updates获取: {}", timestamp);
                return timestamp;
            }
            
            // 查看snapshots表的最新记录
            Optional<Snapshot> snapshot = snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId);
            if (snapshot.isPresent()) {
                long timestamp = snapshot.get().getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                log.debug("【文档时间戳】从snapshots获取: {}", timestamp);
                return timestamp;
            }
            
            // 都没有则返回当前时间
            long currentTime = System.currentTimeMillis();
            log.debug("【文档时间戳】未找到记录，返回当前时间: {}", currentTime);
            return currentTime;
            
        } catch (Exception e) {
            log.error("【文档时间戳】获取失败: docId={}", docId, e);
            return System.currentTimeMillis();
        }
    }
    
    /**
     * 获取下一个更新序号
     */
    private Integer getNextUpdateSequence(String workspaceId, String docId) {
        try {
            List<Update> latestUpdates = updateRepository.findTopByWorkspaceIdAndIdOrderBySeqDesc(workspaceId, docId);
            if (latestUpdates.isEmpty()) {
                return 1;
            }
            Integer lastSeq = latestUpdates.get(0).getSeq();
            return (lastSeq != null ? lastSeq : 0) + 1;
        } catch (Exception e) {
            log.warn("获取更新序号失败，使用默认值: {}", e.getMessage());
            return 1;
        }
    }
    
    /**
     * 更新文档快照（重构为使用新的存储架构）
     */
    private void updateDocumentSnapshot(String workspaceId, String docId, byte[] updateData, String userId, Long timestamp) {
        log.info("【文档快照更新】开始更新: workspaceId={}, docId={}, userId={}, dataSize={}", 
                workspaceId, docId, userId, updateData.length);
        
        try {
            // 1. 确保workspace_pages表中有文档元数据记录
            Optional<WorkspaceDoc> existingDoc = docRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
            
            WorkspaceDoc doc;
            if (existingDoc.isPresent()) {
                doc = existingDoc.get();
                log.info("【文档快照更新】✅ 找到现有文档元数据: {}", docId);
            } else {
                // 创建新文档元数据记录（注意：不包含二进制数据）
                doc = WorkspaceDoc.builder()
                        .workspaceId(workspaceId)
                        .docId(docId)
                        .title("Untitled Document")
                        .createdAt(Instant.now())
                        .build();
                log.info("【文档快照更新】🆕 创建新文档元数据记录: {}", docId);
            }
            
            // 2. 使用新的存储适配器处理文档更新
            try {
                // 推送更新到新的存储架构
                List<byte[]> updates = List.of(updateData);
                long updateTimestamp = storageAdapter.pushDocUpdates(workspaceId, docId, updates, userId);
                
                log.info("【文档快照更新】✅ 通过存储适配器保存更新: timestamp={}", updateTimestamp);
                
            } catch (Exception storageEx) {
                log.error("【文档快照更新】❌ 存储适配器更新失败", storageEx);
                throw storageEx;
            }
            
            // 3. 更新文档元数据的时间戳
            doc.setUpdatedAt(Instant.now());
            
            WorkspaceDoc savedDoc = docRepository.save(doc);
            log.info("【文档快照更新】✅ 元数据保存成功到workspace_pages表: docId={}", docId);
            
            // 4. 验证存储结果
            try {
                var docRecord = storageAdapter.getDoc(workspaceId, docId);
                if (docRecord != null && docRecord.getBlob() != null) {
                    log.info("【文档快照更新】🎉 验证成功: snapshots中数据大小={}字节", docRecord.getBlob().length);
                } else {
                    log.warn("【文档快照更新】⚠️ 验证失败: 无法从snapshots获取数据");
                }
            } catch (Exception verifyEx) {
                log.warn("【文档快照更新】⚠️ 验证失败: {}", verifyEx.getMessage());
            }
            
            // 验证保存结果
            long pagesCount = docRepository.countByWorkspaceId(workspaceId);
            log.info("【文档快照更新】📊 当前工作空间在workspace_pages表中的文档数: {}", pagesCount);
            
        } catch (Exception e) {
            log.error("【文档快照更新】❌ 更新失败: docId={}", docId, e);
            throw new RuntimeException("Failed to update document snapshot", e);
        }
    }
    
    /**
     * 确保文档元数据存在（如果不存在则创建）
     * 注意：此方法不进行权限检查，用于内部自动创建场景（如 Socket.IO 事件处理）
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @return 文档元数据（已存在或新创建的）
     */
    public WorkspaceDoc ensureMetadataExists(String workspaceId, String docId) {
        Optional<WorkspaceDoc> existingDoc = docRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
        
        if (existingDoc.isPresent()) {
            log.debug("📝 [METADATA-CHECK] 文档元数据已存在: docId={}", docId);
            return existingDoc.get();
        }
        
        log.info("📝 [METADATA-AUTO-CREATE] 文档元数据不存在，自动创建: workspaceId={}, docId={}", 
                workspaceId, docId);
        
        try {
            WorkspaceDoc doc = WorkspaceDoc.builder()
                    .workspaceId(workspaceId)
                    .docId(docId)
                    .title("Untitled Document")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .public_(false)
                    .blocked(false)
                    .defaultRole(30) // Manager
                    .mode(0) // Page mode
                    .build();
            
            WorkspaceDoc savedDoc = docRepository.save(doc);
            log.info("✅ [METADATA-AUTO-CREATE] 文档元数据创建成功: docId={}", docId);
            return savedDoc;
        } catch (Exception e) {
            log.error("❌ [METADATA-AUTO-CREATE] 文档元数据创建失败: docId={}", docId, e);
            throw new RuntimeException("Failed to create document metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新或创建快照记录到snapshots表
     */
    private void updateSnapshotRecord(String workspaceId, String docId, byte[] updateData, String userId) {
        log.info("【快照更新】开始更新: workspaceId={}, docId={}, userId={}, dataSize={}", 
                workspaceId, docId, userId, updateData.length);
        
        // ✅ 确保文档元数据存在（在 try-catch 之外，如果失败则抛出异常）
        // 这样可以确保如果元数据创建失败，快照也不会被创建
        ensureMetadataExists(workspaceId, docId);
        
        try {
            Optional<Snapshot> existingSnapshot = snapshotRepository.findByWorkspaceIdAndId(workspaceId, docId);

            Snapshot snapshot;
            byte[] currentBlob = null;
            if (existingSnapshot.isPresent()) {
                snapshot = existingSnapshot.get();
                currentBlob = binaryStorageService.resolvePointer(snapshot.getBlob(), workspaceId, docId);
                log.info("【快照更新】✅ 找到现有快照: {}", docId);
            } else {
                // 创建新快照
                snapshot = Snapshot.builder()
                        .workspaceId(workspaceId)
                        .id(docId)
                        .createdAt(LocalDateTime.now())
                        .createdBy(userId)
                        .seq(0)
                        .build();
                log.info("【快照更新】🆕 创建新快照: {}", docId);
            }

            // 合并数据 - 🔥 使用YJS微服务进行正确的CRDT合并
            byte[] mergedBlob;

            if (currentBlob != null && currentBlob.length > 0) {
                // ✅ 使用YJS微服务进行正确合并
                log.info("【快照更新】🔄 调用YJS微服务合并: 原始={}字节, 新增={}字节",
                        currentBlob.length, updateData.length);

                try {
                    java.util.List<byte[]> updates = java.util.List.of(currentBlob, updateData);
                    mergedBlob = yjsServiceClient.mergeUpdates(updates);
                    log.info("【快照更新】✅ YJS微服务合并成功: 合并后={}字节", mergedBlob.length);
                } catch (Exception mergeException) {
                    log.error("【快照更新】❌ YJS微服务合并失败，使用新数据覆盖: {}", mergeException.getMessage());
                    mergedBlob = updateData;
                }
            } else {
                mergedBlob = updateData;
                log.info("【快照更新】📝 首次快照，使用新数据: {}字节", mergedBlob.length);
            }

            // 更新快照数据
            binaryStorageService.deletePointer(snapshot.getBlob());
            String pointer = binaryStorageService.saveSnapshot(workspaceId, docId, mergedBlob);
            snapshot.setBlob(binaryStorageService.pointerToBytes(pointer));
            // 🔥 暂时不设置state，因为YjsUtils.computeState()是假的实现
            // TODO: 实现真正的YJS状态向量计算
            // snapshot.setState(YjsUtils.computeState(mergedBlob));
            snapshot.setUpdatedAt(LocalDateTime.now());
            snapshot.setUpdatedBy(userId);
            snapshot.setSeq(snapshot.getSeq() + 1);

            Snapshot savedSnapshot = snapshotRepository.save(snapshot);
            log.info("【快照更新】✅ 保存成功到snapshots表: docId={}, seq={}, 数据大小={}字节", 
                    docId, savedSnapshot.getSeq(), mergedBlob.length);
            
            // 验证保存结果
            long snapshotsCount = snapshotRepository.countByWorkspaceId(workspaceId);
            log.info("【快照更新】📊 当前工作空间在snapshots表中的快照数: {}", snapshotsCount);
            
        } catch (Exception e) {
            log.error("【快照更新】❌ 更新失败: docId={}", docId, e);
            // ✅ 重新抛出异常，确保数据一致性：如果快照创建失败，应该抛出异常
            throw new RuntimeException("Failed to update snapshot record: " + e.getMessage(), e);
        }
    }
    
    /**
     * 增加文档编辑计数
     */
    private void incrementDocEditCount(String docId) {
        try {
            String editKey = "doc_edits:" + docId;
            redisTemplate.opsForValue().increment(editKey);
            redisTemplate.expire(editKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("增加文档编辑计数失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public long applyYjsUpdate(String workspaceId, String docId, String base64UpdateData) {
        long startTime = System.currentTimeMillis();
        log.info("🔌 [WorkspaceDocService-Socket.IO] 开始处理YJS更新");
        log.info("  📊 请求参数: workspaceId={}, docId={}", workspaceId, docId);
        log.info("  📦 数据详情: base64DataSize={}字符", base64UpdateData != null ? base64UpdateData.length() : 0);
        
        try {
            // 参数验证
            if (workspaceId == null || workspaceId.trim().isEmpty()) {
                throw new IllegalArgumentException("workspaceId不能为空");
            }
            if (docId == null || docId.trim().isEmpty()) {
                throw new IllegalArgumentException("docId不能为空");
            }
            if (base64UpdateData == null || base64UpdateData.trim().isEmpty()) {
                throw new IllegalArgumentException("base64UpdateData不能为空");
            }
            
            log.info("  ✅ 参数验证通过");
            
            // 1. Base64解码
            log.info("  🔄 开始Base64解码...");
            byte[] updateData;
            try {
                updateData = java.util.Base64.getDecoder().decode(base64UpdateData);
                log.info("  ✅ Base64解码成功: 原始大小={}字符, 解码后大小={}字节", 
                        base64UpdateData.length(), updateData.length);
                
                // 显示解码后数据的前几个字节用于调试
                if (updateData.length > 0) {
                    int showBytes = Math.min(20, updateData.length);
                    byte[] preview = java.util.Arrays.copyOf(updateData, showBytes);
                    log.info("  🔍 解码后数据预览(前{}字节): {}", showBytes, 
                            java.util.Arrays.toString(preview));
                }
            } catch (IllegalArgumentException e) {
                log.error("  ❌ Base64解码失败: {}", e.getMessage());
                throw new RuntimeException("Base64解码失败: " + e.getMessage(), e);
            }
            
            // 2. 调用现有的YJS更新方法 
            log.info("  🔄 调用applyYjsUpdate(byte[])方法...");
            try {
                boolean success = applyYjsUpdate(workspaceId, docId, updateData, null, null);
                log.info("  📊 applyYjsUpdate调用结果: success={}", success);
                
                if (success) {
                    // 3. 获取并返回时间戳
                    log.info("  🔄 获取文档时间戳...");
                    long timestamp = getDocTimestamp(workspaceId, docId);
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.info("✅ [WorkspaceDocService-Socket.IO] YJS更新处理成功");
                    log.info("  📊 处理结果: docId={}, timestamp={}, 总耗时={}ms", 
                            docId, timestamp, processingTime);
                    return timestamp;
                } else {
                    log.error("  ❌ applyYjsUpdate返回false");
                    throw new RuntimeException("YJS update returned false");
                }
            } catch (Exception updateException) {
                log.error("  ❌ applyYjsUpdate内部异常: {}", updateException.getMessage());
                log.error("  📚 完整异常堆栈", updateException);
                throw new RuntimeException("YJS update internal error: " + updateException.getMessage(), updateException);
            }
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ [WorkspaceDocService-Socket.IO] YJS更新处理失败");
            log.error("  📊 失败详情: docId={}, workspaceId={}, 耗时={}ms", 
                    docId, workspaceId, processingTime);
            log.error("  🔍 错误信息: {}", e.getMessage());
            log.error("  📚 完整异常堆栈", e);
            throw new RuntimeException("Failed to process Socket.IO YJS update: " + e.getMessage(), e);
        }
    }
}
