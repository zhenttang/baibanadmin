package com.yunke.backend.infrastructure.websocket;

import com.corundumstudio.socketio.SocketIOServer;
import com.yunke.backend.system.domain.entity.GanttTaskDependency;
import com.yunke.backend.system.domain.entity.GanttOperationLog;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunke.backend.system.service.GanttManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 甘特图实时协作处理器
 * 处理甘特图相关的Socket.IO事件，支持多用户实时协作
 * 
 * @author AFFiNE Development Team
 */
// @Component  // 临时禁用：与DocumentSyncGateway冲突
@Slf4j
public class GanttCollaborationHandler {
    
    private final SocketIOServer socketIOServer;
    private final GanttManagementService ganttManagementService;
    private final ObjectMapper objectMapper;
    
    public GanttCollaborationHandler(SocketIOServer socketIOServer, 
                                   GanttManagementService ganttManagementService,
                                   ObjectMapper objectMapper) {
        this.socketIOServer = socketIOServer;
        this.ganttManagementService = ganttManagementService;
        this.objectMapper = objectMapper;
    }
    
    // 存储用户会话信息：sessionId -> 用户信息
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();
    
    // 存储文档的活跃用户：docId -> Set<sessionId>
    private final Map<String, ConcurrentHashMap<String, UserSession>> documentSessions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 连接事件监听
        socketIOServer.addConnectListener(onConnect);
        socketIOServer.addDisconnectListener(onDisconnect);
        
        // 甘特图相关事件监听
        socketIOServer.addEventListener("gantt:join-document", GanttJoinDocumentData.class, onJoinDocument);
        socketIOServer.addEventListener("gantt:leave-document", GanttLeaveDocumentData.class, onLeaveDocument);
        socketIOServer.addEventListener("gantt:config-update", GanttConfigUpdateData.class, onConfigUpdate);
        socketIOServer.addEventListener("gantt:dependency-add", GanttDependencyAddData.class, onDependencyAdd);
        socketIOServer.addEventListener("gantt:dependency-remove", GanttDependencyRemoveData.class, onDependencyRemove);
        socketIOServer.addEventListener("gantt:task-update", GanttTaskUpdateData.class, onTaskUpdate);
        socketIOServer.addEventListener("gantt:cursor-update", GanttCursorUpdateData.class, onCursorUpdate);
        
        log.info("Gantt collaboration handler initialized");
    }
    
    @PreDestroy
    public void destroy() {
        userSessions.clear();
        documentSessions.clear();
        log.info("Gantt collaboration handler destroyed");
    }
    
    // ============ 连接管理 ============
    
    private final ConnectListener onConnect = client -> {
        String sessionId = client.getSessionId().toString();
        String userId = client.getHandshakeData().getSingleUrlParam("userId");
        String username = client.getHandshakeData().getSingleUrlParam("username");
        
        if (userId == null) {
            log.warn("Client connected without userId, disconnecting: {}", sessionId);
            client.disconnect();
            return;
        }
        
        UserSession userSession = new UserSession(sessionId, userId, username, System.currentTimeMillis());
        userSessions.put(sessionId, userSession);
        
        log.info("User connected to gantt collaboration: userId={}, sessionId={}", userId, sessionId);
        
        // 发送连接确认
        client.sendEvent("gantt:connected", Map.of(
            "sessionId", sessionId,
            "userId", userId,
            "timestamp", System.currentTimeMillis()
        ));
    };
    
    private final DisconnectListener onDisconnect = client -> {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.remove(sessionId);
        
        if (userSession != null) {
            // 从所有文档中移除用户
            documentSessions.values().forEach(docSessions -> {
                docSessions.remove(sessionId);
            });
            
            // 通知其他用户该用户已离线
            broadcastUserOffline(userSession);
            
            log.info("User disconnected from gantt collaboration: userId={}, sessionId={}", 
                    userSession.userId(), sessionId);
        }
    };
    
    // ============ 文档协作管理 ============
    
    private final DataListener<GanttJoinDocumentData> onJoinDocument = (client, data, ackSender) -> {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            log.warn("Unknown session trying to join document: {}", sessionId);
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        // 添加用户到文档会话
        documentSessions.computeIfAbsent(docKey, k -> new ConcurrentHashMap<>())
            .put(sessionId, userSession);
        
        // 将客户端加入文档房间
        client.joinRoom(docKey);
        
        // 获取文档当前的活跃用户列表
        var activeUsers = documentSessions.get(docKey).values().stream()
            .map(session -> Map.of(
                "userId", session.userId(),
                "username", session.username(),
                "joinedAt", session.connectedAt()
            ))
            .toList();
        
        log.info("User joined gantt document: userId={}, workspace={}, doc={}, activeUsers={}", 
                userSession.userId(), data.workspaceId(), data.docId(), activeUsers.size());
        
        // 发送加入确认和当前活跃用户列表
        client.sendEvent("gantt:document-joined", Map.of(
            "workspaceId", data.workspaceId(),
            "docId", data.docId(),
            "activeUsers", activeUsers
        ));
        
        // 通知其他用户有新用户加入
        client.getNamespace().getRoomOperations(docKey).sendEvent("gantt:user-joined", Map.of(
            "userId", userSession.userId(),
            "username", userSession.username(),
            "joinedAt", System.currentTimeMillis()
        ), client.getSessionId()); // 排除自己
    };
    
    private final DataListener<GanttLeaveDocumentData> onLeaveDocument = (client, data, ackSender) -> {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        // 从文档会话中移除用户
        var docSessions = documentSessions.get(docKey);
        if (docSessions != null) {
            docSessions.remove(sessionId);
            
            // 如果文档没有活跃用户了，清理文档会话
            if (docSessions.isEmpty()) {
                documentSessions.remove(docKey);
            }
        }
        
        // 离开文档房间
        client.leaveRoom(docKey);
        
        log.info("User left gantt document: userId={}, workspace={}, doc={}", 
                userSession.userId(), data.workspaceId(), data.docId());
        
        // 通知其他用户该用户已离开
        client.getNamespace().getRoomOperations(docKey).sendEvent("gantt:user-left", Map.of(
            "userId", userSession.userId(),
            "leftAt", System.currentTimeMillis()
        ));
    };
    
    // ============ 甘特图协作事件 ============
    
    private final DataListener<GanttConfigUpdateData> onConfigUpdate = this::handleConfigUpdate;
    
    private final DataListener<GanttDependencyAddData> onDependencyAdd = this::handleDependencyAdd;
    
    private final DataListener<GanttDependencyRemoveData> onDependencyRemove = this::handleDependencyRemove;
    
    private final DataListener<GanttTaskUpdateData> onTaskUpdate = this::handleTaskUpdate;
    
    private final DataListener<GanttCursorUpdateData> onCursorUpdate = (client, data, ackSender) -> {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        // 广播光标位置更新（不记录到数据库）
        Map<String, Object> broadcastData = Map.of(
            "userId", userSession.userId(),
            "username", userSession.username(),
            "position", data.position(),
            "timestamp", System.currentTimeMillis()
        );
        
        client.getNamespace().getRoomOperations(docKey)
            .sendEvent("gantt:cursor-updated", broadcastData, client.getSessionId());
    };
    
    // ============ 事件处理方法 ============
    
    private void handleConfigUpdate(com.corundumstudio.socketio.SocketIOClient client, 
                                  GanttConfigUpdateData data, 
                                  com.corundumstudio.socketio.AckRequest ackSender) {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        log.info("Gantt config update from user: userId={}, workspace={}, doc={}, type={}", 
                userSession.userId(), data.workspaceId(), data.docId(), data.updateType());
        
        // 异步更新数据库
        try {
            if ("timeline".equals(data.updateType())) {
                ganttManagementService.updateGanttConfig(
                    data.workspaceId(), data.docId(),
                    objectMapper.writeValueAsString(data.configData()),
                    null, null, userSession.userId()
                )
                .doOnError(e -> log.warn("Failed to update gantt config", e))
                .subscribe();
            } else if ("display".equals(data.updateType())) {
                ganttManagementService.updateGanttConfig(
                    data.workspaceId(), data.docId(), 
                    null, objectMapper.writeValueAsString(data.configData()),
                    null, userSession.userId()
                )
                .doOnError(e -> log.warn("Failed to update gantt config", e))
                .subscribe();
            }
        } catch (Exception e) {
            log.error("Failed to update gantt config: {}", e.getMessage());
        }
        
        // 广播配置更新给其他用户
        Map<String, Object> broadcastData = Map.of(
            "workspaceId", data.workspaceId(),
            "docId", data.docId(),
            "updateType", data.updateType(),
            "configData", data.configData(),
            "updatedBy", userSession.userId(),
            "timestamp", System.currentTimeMillis()
        );
        
        client.getNamespace().getRoomOperations(docKey)
            .sendEvent("gantt:config-updated", broadcastData, client.getSessionId());
    }
    
    private void handleDependencyAdd(com.corundumstudio.socketio.SocketIOClient client,
                                    GanttDependencyAddData data,
                                    com.corundumstudio.socketio.AckRequest ackSender) {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        log.info("Gantt dependency add from user: userId={}, workspace={}, doc={}, {} -> {}", 
                userSession.userId(), data.workspaceId(), data.docId(), 
                data.fromTaskId(), data.toTaskId());
        
        // 异步创建依赖关系
        ganttManagementService.createTaskDependency(
            data.workspaceId(), data.docId(),
            data.fromTaskId(), data.toTaskId(),
            GanttTaskDependency.DependencyType.fromValue(data.dependencyType()),
            data.lagDays(), userSession.userId()
        ).subscribe(
            dependency -> {
                // 广播依赖关系创建成功
                Map<String, Object> broadcastData = Map.of(
                    "workspaceId", data.workspaceId(),
                    "docId", data.docId(),
                    "dependencyId", dependency.getId(),
                    "fromTaskId", data.fromTaskId(),
                    "toTaskId", data.toTaskId(),
                    "dependencyType", data.dependencyType(),
                    "lagDays", data.lagDays() != null ? data.lagDays() : 0,
                    "createdBy", userSession.userId(),
                    "timestamp", System.currentTimeMillis()
                );
                
                client.getNamespace().getRoomOperations(docKey)
                    .sendEvent("gantt:dependency-added", broadcastData);
            },
            error -> {
                // 发送错误信息给创建者
                client.sendEvent("gantt:dependency-add-failed", Map.of(
                    "error", error.getMessage(),
                    "fromTaskId", data.fromTaskId(),
                    "toTaskId", data.toTaskId()
                ));
            }
        );
    }
    
    private void handleDependencyRemove(com.corundumstudio.socketio.SocketIOClient client,
                                       GanttDependencyRemoveData data,
                                       com.corundumstudio.socketio.AckRequest ackSender) {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        log.info("Gantt dependency remove from user: userId={}, workspace={}, doc={}, dependencyId={}", 
                userSession.userId(), data.workspaceId(), data.docId(), data.dependencyId());
        
        // 异步删除依赖关系
        ganttManagementService.deleteTaskDependency(data.dependencyId(), userSession.userId())
            .subscribe(
                unused -> {
                    // 广播依赖关系删除成功
                    Map<String, Object> broadcastData = Map.of(
                        "workspaceId", data.workspaceId(),
                        "docId", data.docId(),
                        "dependencyId", data.dependencyId(),
                        "removedBy", userSession.userId(),
                        "timestamp", System.currentTimeMillis()
                    );
                    
                    client.getNamespace().getRoomOperations(docKey)
                        .sendEvent("gantt:dependency-removed", broadcastData);
                },
                error -> {
                    // 发送错误信息给删除者
                    client.sendEvent("gantt:dependency-remove-failed", Map.of(
                        "error", error.getMessage(),
                        "dependencyId", data.dependencyId()
                    ));
                }
            );
    }
    
    private void handleTaskUpdate(com.corundumstudio.socketio.SocketIOClient client,
                                 GanttTaskUpdateData data,
                                 com.corundumstudio.socketio.AckRequest ackSender) {
        String sessionId = client.getSessionId().toString();
        UserSession userSession = userSessions.get(sessionId);
        
        if (userSession == null) {
            return;
        }
        
        String docKey = getDocumentKey(data.workspaceId(), data.docId());
        
        log.debug("Gantt task update from user: userId={}, workspace={}, doc={}, taskId={}", 
                userSession.userId(), data.workspaceId(), data.docId(), data.taskId());
        
        // 记录任务更新操作 (fire-and-forget)
        ganttManagementService.logOperation(
            data.workspaceId(), data.docId(), userSession.userId(),
            GanttOperationLog.OperationType.TASK_UPDATE,
            data
        )
        .doOnError(e -> log.warn("Failed to log gantt operation", e))
        .subscribe();
        
        // 广播任务更新给其他用户
        Map<String, Object> broadcastData = Map.of(
            "workspaceId", data.workspaceId(),
            "docId", data.docId(),
            "taskId", data.taskId(),
            "updateType", data.updateType(),
            "updateData", data.updateData(),
            "updatedBy", userSession.userId(),
            "timestamp", System.currentTimeMillis()
        );
        
        client.getNamespace().getRoomOperations(docKey)
            .sendEvent("gantt:task-updated", broadcastData, client.getSessionId());
    }
    
    // ============ 私有辅助方法 ============
    
    private String getDocumentKey(String workspaceId, String docId) {
        return "gantt:" + workspaceId + ":" + docId;
    }
    
    private void broadcastUserOffline(UserSession userSession) {
        // 通知所有文档房间该用户已离线
        documentSessions.forEach((docKey, docSessions) -> {
            if (docSessions.containsKey(userSession.sessionId())) {
                socketIOServer.getRoomOperations(docKey).sendEvent("gantt:user-offline", Map.of(
                    "userId", userSession.userId(),
                    "offlineAt", System.currentTimeMillis()
                ));
            }
        });
    }
    
    // ============ 数据类定义 ============
    
    record UserSession(String sessionId, String userId, String username, long connectedAt) {}
    
    record GanttJoinDocumentData(String workspaceId, String docId) {}
    
    record GanttLeaveDocumentData(String workspaceId, String docId) {}
    
    record GanttConfigUpdateData(
        String workspaceId, 
        String docId, 
        String updateType, // "timeline" | "display" | "workingCalendar"
        Map<String, Object> configData
    ) {}
    
    record GanttDependencyAddData(
        String workspaceId,
        String docId,
        String fromTaskId,
        String toTaskId,
        String dependencyType,
        Integer lagDays
    ) {}
    
    record GanttDependencyRemoveData(
        String workspaceId,
        String docId,
        Long dependencyId
    ) {}
    
    record GanttTaskUpdateData(
        String workspaceId,
        String docId,
        String taskId,
        String updateType, // "schedule" | "progress" | "status" | "workingDays"
        Map<String, Object> updateData
    ) {}
    
    record GanttCursorUpdateData(
        String workspaceId,
        String docId,
        Map<String, Object> position // x, y, element等位置信息
    ) {}
}