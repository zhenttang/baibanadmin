package com.yunke.backend.document.service.impl;


import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.monitor.MetricsCollector;
import com.yunke.backend.document.service.CollaborationService;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 协作服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationServiceImpl implements CollaborationService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WorkspaceDocService docService;
    private final MetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;

    // 内存中的连接管理
    private final Map<String, Map<String, ActiveCollaborator>> documentCollaborators = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> joinDocument(String docId, String userId, String sessionId) {
        log.info("User joining document collaboration: {} -> {}", userId, docId);
        
        return Mono.fromRunnable(() -> {
            // 检查文档访问权限
            if (!docService.hasDocAccess(docId, userId)) {
                throw new IllegalArgumentException("No access to document: " + docId);
            }
            
            // 创建协作者记录
            ActiveCollaborator collaborator = new ActiveCollaborator(
                    userId, sessionId, Instant.now(), null
            );
            
            // 更新内存中的协作者列表
            documentCollaborators.computeIfAbsent(docId, k -> new ConcurrentHashMap<>())
                    .put(userId, collaborator);
            
            // 更新Redis中的协作者列表
            String key = "doc_active_collaborators:" + docId;
            try {
                String collaboratorJson = objectMapper.writeValueAsString(collaborator);
                redisTemplate.opsForHash().put(key, userId, collaboratorJson);
                redisTemplate.expire(key, Duration.ofHours(1));
            } catch (Exception e) {
                log.warn("Failed to update collaborator in Redis", e);
            }
            
            // 记录文档访问
            docService.recordDocAccess(docId, userId);
            
            // 更新指标
            metricsCollector.incrementActiveWebsockets();

            log.info("User joined document collaboration successfully: {} -> {}", userId, docId);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> leaveDocument(String docId, String userId, String sessionId) {
        log.info("User leaving document collaboration: {} -> {}", userId, docId);
        
        return Mono.fromRunnable(() -> {
            // 从内存中移除协作者
            Map<String, ActiveCollaborator> collaborators = documentCollaborators.get(docId);
            if (collaborators != null) {
                collaborators.remove(userId);
                if (collaborators.isEmpty()) {
                    documentCollaborators.remove(docId);
                }
            }
            
            // 从Redis中移除协作者
            String key = "doc_active_collaborators:" + docId;
            redisTemplate.opsForHash().delete(key, userId);
            
            // 更新指标
            metricsCollector.decrementActiveWebsockets();

            log.info("User left document collaboration successfully: {} -> {}", userId, docId);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> broadcastOperation(String docId, String userId, Operation operation) {
        log.debug("Broadcasting operation: {} in document: {}", operation.type(), docId);
        
        return Mono.fromRunnable(() -> {
            try {
                // 将操作序列化
                String operationJson = objectMapper.writeValueAsString(operation);
                
                // 发布到Redis频道
                String channel = "doc_operations:" + docId;
                redisTemplate.convertAndSend(channel, operationJson);
                
                // 保存操作历史
                String historyKey = "doc_history:" + docId;
                redisTemplate.opsForList().leftPush(historyKey, operationJson);
                redisTemplate.opsForList().trim(historyKey, 0, 999); // 保留最近1000个操作
                redisTemplate.expire(historyKey, Duration.ofDays(7));
                
                log.debug("Operation broadcasted successfully: {} in document: {}", operation.type(), docId);
            } catch (Exception e) {
                log.error("Failed to broadcast operation", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<List<ActiveCollaborator>> getActiveCollaborators(String docId) {
        log.debug("Getting active collaborators for document: {}", docId);
        
        return Mono.fromCallable(() -> {
            // 优先从内存获取
            Map<String, ActiveCollaborator> collaborators = documentCollaborators.get(docId);
            if (collaborators != null && !collaborators.isEmpty()) {
                return List.copyOf(collaborators.values());
            }
            
            // 从Redis获取
            String key = "doc_active_collaborators:" + docId;
            Map<Object, Object> redisCollaborators = redisTemplate.opsForHash().entries(key);
            
            return redisCollaborators.values().stream()
                    .map(obj -> {
                        try {
                            return objectMapper.readValue(obj.toString(), ActiveCollaborator.class);
                        } catch (Exception e) {
                            log.warn("Failed to deserialize collaborator", e);
                            return null;
                        }
                    })
                    .filter(collaborator -> collaborator != null)
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Operation> getOperationStream(String docId, String userId) {
        log.debug("Creating operation stream for user: {} in document: {}", userId, docId);
        
        // 检查访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return Flux.error(new IllegalArgumentException("No access to document: " + docId));
        }
        
        String channel = "doc_operations:" + docId;
        
        return reactiveRedisTemplate.listenToChannel(channel)
                .map(message -> {
                    try {
                        return objectMapper.readValue(message.getMessage().toString(), Operation.class);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize operation", e);
                        return null;
                    }
                })
                .filter(operation -> operation != null)
                .filter(operation -> !operation.userId().equals(userId)) // 不发送自己的操作
                .doOnSubscribe(subscription -> log.debug("User subscribed to operation stream: {} -> {}", userId, docId))
                .doOnTerminate(() -> log.debug("User unsubscribed from operation stream: {} -> {}", userId, docId));
    }

    @Override
    public Mono<Void> handleOperation(String docId, String userId, Operation operation) {
        log.debug("Handling operation: {} from user: {} in document: {}", operation.type(), userId, docId);
        
        return Mono.fromRunnable(() -> {
            // 验证操作
            if (!operation.docId().equals(docId) || !operation.userId().equals(userId)) {
                throw new IllegalArgumentException("Invalid operation");
            }
            
            // 检查编辑权限（如果是修改操作）
            if (isModifyOperation(operation.type()) && !docService.hasDocEditPermission(docId, userId)) {
                throw new IllegalArgumentException("No edit permission for document: " + docId);
            }
            
            // 广播操作 (fire-and-forget)
            broadcastOperation(docId, userId, operation)
                .doOnError(e -> log.warn("Failed to broadcast operation", e))
                .subscribe();
            
            log.debug("Operation handled successfully: {} from user: {} in document: {}", 
                    operation.type(), userId, docId);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<DocumentState> getDocumentState(String docId) {
        log.debug("Getting document state: {}", docId);

        Mono<List<ActiveCollaborator>> collaboratorsMono = getActiveCollaborators(docId);
        Mono<Integer> versionMono = Mono.fromCallable(() -> getDocumentVersion(docId))
                .subscribeOn(Schedulers.boundedElastic());

        return Mono.zip(collaboratorsMono, versionMono)
                .map(tuple -> {
                    List<ActiveCollaborator> collaborators = tuple.getT1();
                    int version = tuple.getT2();
                    Instant lastModified = Instant.now();
                    return new DocumentState(
                            docId,
                            "",
                            version,
                            lastModified,
                            collaborators
                    );
                });
    }

    @Override
    public Mono<Void> saveSnapshot(String docId, String content) {
        log.info("Saving document snapshot: {}", docId);
        
        return Mono.fromRunnable(() -> {
            try {
                // 保存快照到Redis
                String snapshotKey = "doc_snapshot:" + docId;
                Map<String, Object> snapshot = Map.of(
                        "content", content,
                        "version", getDocumentVersion(docId),
                        "timestamp", Instant.now().toString()
                );
                
                String snapshotJson = objectMapper.writeValueAsString(snapshot);
                redisTemplate.opsForValue().set(snapshotKey, snapshotJson, Duration.ofDays(30));
                
                log.info("Document snapshot saved successfully: {}", docId);
            } catch (Exception e) {
                log.error("Failed to save document snapshot", e);
            }
        });
    }

    @Override
    public Mono<List<CollaborationService.DocumentVersion>> getDocumentHistory(String docId, int limit) {
        log.debug("Getting document history: {} (limit: {})", docId, limit);

        return Mono.fromCallable(() -> {
            String historyKey = "doc_history:" + docId;
            List<Object> operations = redisTemplate.opsForList().range(historyKey, 0, limit - 1);
            
            if (operations == null) {
                return List.<CollaborationService.DocumentVersion>of();
            }
            
            return operations.stream()
                    .map(obj -> {
                        try {
                            Operation operation = objectMapper.readValue(obj.toString(), Operation.class);
                            return new CollaborationService.DocumentVersion(
                                    operation.id(),
                                    docId,
                                    "", // 简化实现，实际需要重建内容
                                    operation.userId(),
                                    operation.timestamp(),
                                    operation.type()
                            );
                        } catch (Exception e) {
                            log.warn("Failed to deserialize operation history", e);
                            return null;
                        }
                    })
                    .filter(version -> version != null)
                    .collect(Collectors.<CollaborationService.DocumentVersion>toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 检查是否是修改操作
     */
    private boolean isModifyOperation(String operationType) {
        return operationType.equals("insert") || 
               operationType.equals("delete") || 
               operationType.equals("format") ||
               operationType.equals("replace");
    }

    /**
     * 获取文档版本号
     */
    private int getDocumentVersion(String docId) {
        String versionKey = "doc_version:" + docId;
        Object version = redisTemplate.opsForValue().get(versionKey);
        
        if (version == null) {
            redisTemplate.opsForValue().set(versionKey, 1);
            return 1;
        }
        
        return Integer.parseInt(version.toString());
    }
}
