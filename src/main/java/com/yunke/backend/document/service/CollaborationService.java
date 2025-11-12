package com.yunke.backend.document.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 协作服务接口
 * 处理实时协作功能
 */
public interface CollaborationService {

    /**
     * 用户加入文档协作
     */
    Mono<Void> joinDocument(String docId, String userId, String sessionId);

    /**
     * 用户离开文档协作
     */
    Mono<Void> leaveDocument(String docId, String userId, String sessionId);

    /**
     * 广播操作到文档的所有协作者
     */
    Mono<Void> broadcastOperation(String docId, String userId, Operation operation);

    /**
     * 获取文档的在线协作者
     */
    Mono<List<ActiveCollaborator>> getActiveCollaborators(String docId);

    /**
     * 获取文档操作流
     */
    Flux<Operation> getOperationStream(String docId, String userId);

    /**
     * 处理文档操作
     */
    Mono<Void> handleOperation(String docId, String userId, Operation operation);

    /**
     * 获取文档状态
     */
    Mono<DocumentState> getDocumentState(String docId);

    /**
     * 保存文档快照
     */
    Mono<Void> saveSnapshot(String docId, String content);

    /**
     * 获取文档历史记录
     */
    Mono<List<DocumentVersion>> getDocumentHistory(String docId, int limit);

    /**
     * 文档操作
     */
    record Operation(
            String id,
            String type,
            String docId,
            String userId,
            Map<String, Object> data,
            java.time.Instant timestamp
    ) {}

    /**
     * 活跃协作者
     */
    record ActiveCollaborator(
            String userId,
            String sessionId,
            java.time.Instant joinedAt,
            String cursor
    ) {}

    /**
     * 文档状态
     */
    record DocumentState(
            String docId,
            String content,
            int version,
            java.time.Instant lastModified,
            List<ActiveCollaborator> collaborators
    ) {}

    /**
     * 文档版本
     */
    record DocumentVersion(
            String id,
            String docId,
            String content,
            String userId,
            java.time.Instant createdAt,
            String description
    ) {}
}