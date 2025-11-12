package com.yunke.backend.document.websocket;

import com.yunke.backend.document.collaboration.SpaceSyncGateway;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.document.service.AwarenessService;
import com.yunke.backend.infrastructure.websocket.ClientSession;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.AckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket文档协作处理器
 * 对应AFFiNE的协作WebSocket处理逻辑
 * 
 * 核心功能：
 * 1. 处理WebSocket连接和断开
 * 2. 处理文档同步消息
 * 3. 管理客户端会话
 * 4. 实现实时协作广播
 * 
 * 对应开源AFFiNE代码：
 * packages/backend/server/src/core/sync/events/events.gateway.ts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocCollaborationHandler {
    
    private final SpaceSyncGateway syncGateway;
    private final WorkspaceDocService docService;
    private final PermissionService permissionService;
    private final AwarenessService awarenessService;
    
    // 客户端会话管理 - clientId -> ClientSession
    private final Map<String, ClientSession> clientSessions = new ConcurrentHashMap<>();
    
    // 文档房间管理 - workspaceId:docId -> Set<clientId>
    private final Map<String, Set<String>> docRooms = new ConcurrentHashMap<>();
    
    /**
     * 初始化WebSocket事件监听器
     */
    public void setupEventListeners(SocketIOServer server) {
        log.info("🔌 [DocCollaborationHandler] 设置WebSocket事件监听器");
        
        // 连接事件
        server.addConnectListener(onConnect());
        
        // 断开连接事件
        server.addDisconnectListener(onDisconnect());
        
        // 文档同步事件（旧协议）
        server.addEventListener("doc-sync", byte[].class, onDocSync());
        
        // 加入文档房间事件
        server.addEventListener("join-doc", Map.class, onJoinDoc());
        
        // 离开文档房间事件
        server.addEventListener("leave-doc", Map.class, onLeaveDoc());
        
        // 感知状态更新事件（旧协议）
        server.addEventListener("awareness-update", byte[].class, onAwarenessUpdate());

        // 新协议事件由 LegacyDocSyncConfig -> SpaceSyncGateway 注册，避免重复注册
        
        log.info("✅ [DocCollaborationHandler] WebSocket事件监听器设置完成");
    }

    // 新协议 space:* 事件由 SpaceSyncGateway 实现并在 LegacyDocSyncConfig 中注册
    
    /**
     * 连接事件处理器
     */
    private ConnectListener onConnect() {
        return client -> {
            String clientId = client.getSessionId().toString();
            log.info("🔗 [DocCollaborationHandler] 客户端连接: clientId={}", clientId);
            
            // 创建客户端会话
            ClientSession session = new ClientSession(clientId, client);
            clientSessions.put(clientId, session);
            
            // 发送连接确认
            client.sendEvent("connected", Map.of(
                "clientId", clientId,
                "timestamp", System.currentTimeMillis()
            ));
        };
    }
    
    /**
     * 断开连接事件处理器
     */
    private DisconnectListener onDisconnect() {
        return client -> {
            String clientId = client.getSessionId().toString();
            log.info("❌ [DocCollaborationHandler] 客户端断开: clientId={}", clientId);
            
            // 清理客户端会话
            ClientSession session = clientSessions.remove(clientId);
            if (session != null) {
                // 从所有文档房间中移除
                for (String docKey : session.getJoinedDocs()) {
                    leaveDocRoom(docKey, clientId);
                }
            }
        };
    }
    
    /**
     * 文档同步事件处理器
     */
    private DataListener<byte[]> onDocSync() {
        return (client, data, ackRequest) -> {
            String clientId = client.getSessionId().toString();
            
            // 输入验证
            if (data == null || data.length == 0) {
                log.warn("⚠️ [DocCollaborationHandler] 同步数据为空: clientId={}", clientId);
                client.sendEvent("error", Map.of(
                    "type", "sync-error",
                    "message", "同步数据为空"
                ));
                return;
            }
            
            if (data.length > 10 * 1024 * 1024) { // 10MB limit
                log.warn("⚠️ [DocCollaborationHandler] 同步数据过大: clientId={}, size={}MB", 
                        clientId, data.length / (1024 * 1024));
                client.sendEvent("error", Map.of(
                    "type", "sync-error",
                    "message", "同步数据过大"
                ));
                return;
            }
            
            log.debug("🔄 [DocCollaborationHandler] 收到文档同步消息: clientId={}, dataSize={}B", 
                     clientId, data.length);
            
            try {
                // 从客户端会话获取当前文档信息
                ClientSession session = clientSessions.get(clientId);
                if (session == null) {
                    log.warn("⚠️ [DocCollaborationHandler] 客户端会话不存在: clientId={}", clientId);
                    client.sendEvent("error", Map.of(
                        "type", "session-error",
                        "message", "客户端会话不存在"
                    ));
                    return;
                }
                
                String[] docInfo = session.getCurrentDoc();
                if (docInfo == null) {
                    log.warn("⚠️ [DocCollaborationHandler] 客户端未加入任何文档: clientId={}", clientId);
                    client.sendEvent("error", Map.of(
                        "type", "doc-error",
                        "message", "请先加入文档"
                    ));
                    return;
                }
                
                String workspaceId = docInfo[0];
                String docId = docInfo[1];
                
                // 使用同步网关处理消息
                CompletableFuture<List<byte[]>> responseFuture = 
                    syncGateway.handleSyncMessage(workspaceId, docId, data, clientId);
                
                responseFuture.thenAccept(responses -> {
                    try {
                        // 发送响应消息给客户端
                        for (byte[] response : responses) {
                            if (response != null && response.length > 0) {
                                client.sendEvent("doc-sync-response", response);
                            }
                        }
                        
                        // 如果有响应，说明文档有更新，需要广播给其他客户端
                        if (!responses.isEmpty()) {
                            broadcastToDocRoom(workspaceId, docId, "doc-update", data, clientId);
                        }
                    } catch (Exception e) {
                        log.error("❌ [DocCollaborationHandler] 发送同步响应失败: clientId={}", clientId, e);
                    }
                    
                }).exceptionally(throwable -> {
                    log.error("❌ [DocCollaborationHandler] 处理文档同步消息失败: clientId={}", 
                             clientId, throwable);
                    try {
                        client.sendEvent("error", Map.of(
                            "type", "sync-error",
                            "message", "同步失败: " + throwable.getMessage()
                        ));
                    } catch (Exception e) {
                        log.error("❌ [DocCollaborationHandler] 发送错误消息失败: clientId={}", clientId, e);
                    }
                    return null;
                });
                
            } catch (OutOfMemoryError e) {
                log.error("💥 [DocCollaborationHandler] 文档同步内存不足: clientId={}", clientId, e);
                client.sendEvent("error", Map.of(
                    "type", "memory-error",
                    "message", "服务器内存不足"
                ));
            } catch (Exception e) {
                log.error("❌ [DocCollaborationHandler] 文档同步异常: clientId={}", clientId, e);
                client.sendEvent("error", Map.of(
                    "type", "sync-error", 
                    "message", "同步异常: " + e.getMessage()
                ));
            }
        };
    }
    
    /**
     * 加入文档房间事件处理器
     */
    private DataListener<Map> onJoinDoc() {
        return (client, data, ackRequest) -> {
            String clientId = client.getSessionId().toString();
            
            // 输入验证
            if (data == null) {
                log.warn("⚠️ [DocCollaborationHandler] 加入文档参数为空: clientId={}", clientId);
                client.sendEvent("error", Map.of(
                    "type", "param-error",
                    "message", "缺少必要参数"
                ));
                return;
            }
            
            String workspaceId = (String) data.get("workspaceId");
            String docId = (String) data.get("docId");
            String userId = (String) data.get("userId");
            
            // 参数验证
            if (workspaceId == null || workspaceId.trim().isEmpty()) {
                log.warn("⚠️ [DocCollaborationHandler] 工作空间ID无效: clientId={}", clientId);
                client.sendEvent("error", Map.of(
                    "type", "param-error",
                    "message", "工作空间ID不能为空"
                ));
                return;
            }
            
            if (docId == null || docId.trim().isEmpty()) {
                log.warn("⚠️ [DocCollaborationHandler] 文档ID无效: clientId={}", clientId);
                client.sendEvent("error", Map.of(
                    "type", "param-error",
                    "message", "文档ID不能为空"
                ));
                return;
            }
            
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("⚠️ [DocCollaborationHandler] 用户ID无效: clientId={}", clientId);
                client.sendEvent("error", Map.of(
                    "type", "param-error",
                    "message", "用户ID不能为空"
                ));
                return;
            }
            
            log.info("📝 [DocCollaborationHandler] 客户端加入文档: clientId={}, docKey={}:{}", 
                    clientId, workspaceId, docId);
            
            try {
                // 权限检查
                if (!permissionService.hasWorkspaceAccess(userId, workspaceId)) {
                    log.warn("⚠️ [DocCollaborationHandler] 权限不足: userId={}, workspaceId={}", 
                            userId, workspaceId);
                    client.sendEvent("error", Map.of(
                        "type", "permission-denied",
                        "message", "无权限访问该工作空间"
                    ));
                    return;
                }
                
                // 加入文档房间
                joinDocRoom(workspaceId, docId, clientId, userId);
                
                // 发送加入成功响应
                client.sendEvent("doc-joined", Map.of(
                    "workspaceId", workspaceId,
                    "docId", docId,
                    "timestamp", System.currentTimeMillis()
                ));
                
                // 广播用户加入事件给其他客户端
                broadcastToDocRoom(workspaceId, docId, "user-joined", Map.of(
                    "userId", userId,
                    "clientId", clientId,
                    "timestamp", System.currentTimeMillis()
                ), clientId);
                
            } catch (SecurityException e) {
                log.error("🔒 [DocCollaborationHandler] 安全检查失败: clientId={}, userId={}", 
                         clientId, userId, e);
                client.sendEvent("error", Map.of(
                    "type", "security-error",
                    "message", "安全检查失败"
                ));
            } catch (Exception e) {
                log.error("❌ [DocCollaborationHandler] 加入文档失败: clientId={}", clientId, e);
                client.sendEvent("error", Map.of(
                    "type", "join-error",
                    "message", "加入文档失败: " + e.getMessage()
                ));
            }
        };
    }
    
    /**
     * 离开文档房间事件处理器
     */
    private DataListener<Map> onLeaveDoc() {
        return (client, data, ackRequest) -> {
            String clientId = client.getSessionId().toString();
            String workspaceId = (String) data.get("workspaceId");
            String docId = (String) data.get("docId");
            
            log.info("🚪 [DocCollaborationHandler] 客户端离开文档: clientId={}, docKey={}:{}", 
                    clientId, workspaceId, docId);
            
            leaveDocRoom(workspaceId + ":" + docId, clientId);
            
            client.sendEvent("doc-left", Map.of(
                "workspaceId", workspaceId,
                "docId", docId,
                "timestamp", System.currentTimeMillis()
            ));
        };
    }
    
    /**
     * 感知状态更新事件处理器
     */
    private DataListener<byte[]> onAwarenessUpdate() {
        return (client, data, ackRequest) -> {
            String clientId = client.getSessionId().toString();
            log.debug("👁️ [DocCollaborationHandler] 收到感知状态更新: clientId={}, dataSize={}B", 
                     clientId, data.length);
            
            ClientSession session = clientSessions.get(clientId);
            if (session == null || session.getCurrentDoc() == null) {
                log.warn("⚠️ [DocCollaborationHandler] 客户端未加入文档，忽略感知状态更新: clientId={}", clientId);
                return;
            }
            
            try {
                String[] docInfo = session.getCurrentDoc();
                String workspaceId = docInfo[0];
                String docId = docInfo[1];
                
                // 解码感知状态数据
                awarenessService.decodeAwarenessUpdate(workspaceId, data);
                
                // 广播给房间内其他客户端（原始数据）
                broadcastToDocRoom(workspaceId, docId, "awareness-update", data, clientId);
                
            } catch (Exception e) {
                log.error("❌ [DocCollaborationHandler] 处理感知状态更新失败: clientId={}", clientId, e);
            }
        };
    }
    
    /**
     * 加入文档房间
     */
    private void joinDocRoom(String workspaceId, String docId, String clientId, String userId) {
        String docKey = workspaceId + ":" + docId;
        
        // 添加到房间
        docRooms.computeIfAbsent(docKey, k -> ConcurrentHashMap.newKeySet()).add(clientId);
        
        // 更新客户端会话
        ClientSession session = clientSessions.get(clientId);
        if (session != null) {
            session.joinDoc(docKey, workspaceId, docId, userId);
        }
        
        log.info("✅ [DocCollaborationHandler] 客户端成功加入文档房间: docKey={}, clientId={}, 房间人数={}", 
                docKey, clientId, docRooms.get(docKey).size());
    }
    
    /**
     * 离开文档房间
     */
    private void leaveDocRoom(String docKey, String clientId) {
        Set<String> clients = docRooms.get(docKey);
        if (clients != null) {
            clients.remove(clientId);
            if (clients.isEmpty()) {
                docRooms.remove(docKey);
                log.info("🏠 [DocCollaborationHandler] 文档房间已清空: docKey={}", docKey);
            }
        }
        
        // 更新客户端会话
        ClientSession session = clientSessions.get(clientId);
        if (session != null) {
            session.leaveDoc(docKey);
        }
        
        log.info("🚪 [DocCollaborationHandler] 客户端离开文档房间: docKey={}, clientId={}", 
                docKey, clientId);
    }
    
    /**
     * 向文档房间广播消息
     */
    private void broadcastToDocRoom(String workspaceId, String docId, String event, Object data, String excludeClientId) {
        String docKey = workspaceId + ":" + docId;
        Set<String> clients = docRooms.get(docKey);
        
        if (clients == null || clients.isEmpty()) {
            return;
        }
        
        log.debug("📢 [DocCollaborationHandler] 广播消息到文档房间: docKey={}, event={}, 客户端数={}", 
                 docKey, event, clients.size());
        
        for (String clientId : clients) {
            if (clientId.equals(excludeClientId)) {
                continue; // 跳过发送者
            }
            
            ClientSession session = clientSessions.get(clientId);
            if (session != null && session.getClient().isChannelOpen()) {
                try {
                    session.getClient().sendEvent(event, data);
                } catch (Exception e) {
                    log.warn("⚠️ [DocCollaborationHandler] 广播消息失败: clientId={}", clientId, e);
                }
            }
        }
    }
    
    /**
     * 获取文档房间状态
     */
    public Map<String, Object> getDocRoomStatus(String workspaceId, String docId) {
        String docKey = workspaceId + ":" + docId;
        Set<String> clients = docRooms.get(docKey);
        
        return Map.of(
            "docKey", docKey,
            "clientCount", clients != null ? clients.size() : 0,
            "clients", clients != null ? new ArrayList<>(clients) : Collections.emptyList()
        );
    }
    
    /**
     * 获取所有活跃会话统计
     */
    public Map<String, Object> getSessionStats() {
        return Map.of(
            "totalSessions", clientSessions.size(),
            "totalDocRooms", docRooms.size(),
            "totalClientsInRooms", docRooms.values().stream().mapToInt(Set::size).sum()
        );
    }
    
    /**
     * 清理断开的客户端
     */
    public void cleanupDisconnectedClients() {
        List<String> disconnectedClients = new ArrayList<>();
        
        for (Map.Entry<String, ClientSession> entry : clientSessions.entrySet()) {
            String clientId = entry.getKey();
            ClientSession session = entry.getValue();
            
            if (!session.getClient().isChannelOpen()) {
                disconnectedClients.add(clientId);
            }
        }
        
        for (String clientId : disconnectedClients) {
            ClientSession session = clientSessions.remove(clientId);
            if (session != null) {
                for (String docKey : session.getJoinedDocs()) {
                    leaveDocRoom(docKey, clientId);
                }
            }
        }
        
        if (!disconnectedClients.isEmpty()) {
            log.info("🧹 [DocCollaborationHandler] 清理断开连接的客户端: 数量={}", disconnectedClients.size());
        }
    }
}
