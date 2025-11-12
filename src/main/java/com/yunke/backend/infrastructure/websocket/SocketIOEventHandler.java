package com.yunke.backend.infrastructure.websocket;

import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.document.service.DocWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yunke.backend.storage.impl.WorkspaceDocStorageAdapter;
import com.yunke.backend.storage.model.DocDiff;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket.IO事件处理器
 * 处理前端Socket.IO客户端的连接和文档相关事件
 * 
 * ⚠️ 已禁用：功能已迁移到 SpaceSyncGateway (使用 yjs-service)
 */
//@Service  // 已禁用，使用 SpaceSyncGateway 替代
@RequiredArgsConstructor
@Slf4j
public class SocketIOEventHandler {

    private final SocketIOServer socketIOServer;
    private final WorkspaceDocService workspaceDocService;
    private final WorkspaceDocStorageAdapter storageAdapter;
    @Qualifier("databaseDocWriter")
    private final DocWriter docWriter;

    // 存储客户端会话信息
    private final Map<String, ClientSession> clientSessions = new ConcurrentHashMap<>();

    /**
     * 客户端会话信息
     */
    public static class ClientSession {
        public String clientId;
        public String spaceType;
        public String spaceId;
        public String userId;
        
        public ClientSession(String clientId, String spaceType, String spaceId, String userId) {
            this.clientId = clientId;
            this.spaceType = spaceType;
            this.spaceId = spaceId;
            this.userId = userId;
        }
    }

    /**
     * 空间加入请求数据
     */
    public static class SpaceJoinRequest {
        public String spaceType;
        public String spaceId;
        public String clientVersion;
    }

    /**
     * 空间加入响应数据
     */
    public static class SpaceJoinResponse {
        public String clientId;
        
        public SpaceJoinResponse(String clientId) {
            this.clientId = clientId;
        }
    }

    /**
     * 文档更新推送数据
     */
    public static class DocUpdatePushRequest {
        public String spaceType;
        public String spaceId;
        public String docId;
        public String update; // Base64编码的YJS更新数据
    }

    /**
     * 文档更新推送响应
     */
    public static class DocUpdatePushResponse {
        public long timestamp;
        
        public DocUpdatePushResponse(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * 文档加载请求
     */
    public static class SpaceLoadDocRequest {
        public String spaceType;
        public String spaceId;
        public String docId;
        public String stateVector; // Base64，可空
    }

    /**
     * 文档加载响应（符合前端期望的data包裹格式）
     */
    public static class SpaceLoadDocResponseData {
        public String missing;   // Base64
        public String state;     // Base64
        public long timestamp;

        public SpaceLoadDocResponseData(String missing, String state, long timestamp) {
            this.missing = missing;
            this.state = state;
            this.timestamp = timestamp;
        }
    }

    /**
     * 文档时间戳批量请求
     */
    public static class SpaceLoadDocTimestampsRequest {
        public String spaceType;
        public String spaceId;
        public Long timestamp; // 可空，作为after过滤
    }

    @PostConstruct
    public void initializeEventHandlers() {
        // 连接事件
        socketIOServer.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                log.info("🔌 [Socket.IO-连接] 客户端连接成功");
                log.info("  📊 连接详情: sessionId={}, remoteAddress={}, transport={}", 
                    client.getSessionId(), 
                    client.getRemoteAddress(), 
                    client.getTransport());
                log.info("  👥 当前连接数: {}", socketIOServer.getAllClients().size());
                log.info("  🎯 [DEBUG] 新客户端连接 - 等待space:join和space:push-doc-update事件");
            }
        });

        // 断开连接事件
        socketIOServer.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                String sessionId = client.getSessionId().toString();
                ClientSession removedSession = clientSessions.remove(sessionId);
                
                log.info("🔌 [Socket.IO-断开] 客户端断开连接");
                log.info("  📊 断开详情: sessionId={}, remoteAddress={}", sessionId, client.getRemoteAddress());
                if (removedSession != null) {
                    log.info("  📝 会话信息: clientId={}, spaceType={}, spaceId={}", 
                        removedSession.clientId, removedSession.spaceType, removedSession.spaceId);
                }
                log.info("  👥 剩余连接数: {}", socketIOServer.getAllClients().size());
            }
        });

        // 空间加入事件
        socketIOServer.addEventListener("space:join", SpaceJoinRequest.class, new DataListener<SpaceJoinRequest>() {
            @Override
            public void onData(SocketIOClient client, SpaceJoinRequest data, com.corundumstudio.socketio.AckRequest ackSender) {
                try {
                    log.info("🏠 [Socket.IO-空间加入] 收到空间加入请求");
                    log.info("  📊 请求详情: spaceType={}, spaceId={}, clientVersion={}", 
                        data.spaceType, data.spaceId, data.clientVersion);
                    log.info("  🔗 客户端信息: sessionId={}, remoteAddress={}", 
                        client.getSessionId(), client.getRemoteAddress());
                    log.info("  🎯 [DEBUG] space:join事件处理器被调用");
                    
                    String sessionId = client.getSessionId().toString();
                    String clientId = generateClientId();
                    
                    log.info("  🆔 生成客户端ID: {}", clientId);
                    
                    // 存储客户端会话信息
                    ClientSession session = new ClientSession(clientId, data.spaceType, data.spaceId, "anonymous");
                    clientSessions.put(sessionId, session);
                    
                    log.info("  💾 保存会话信息: sessionId={}, clientId={}", sessionId, clientId);
                    
                    // 加入对应的房间
                    client.joinRoom(data.spaceId);
                    log.info("  🏠 加入房间: spaceId={}", data.spaceId);
                    
                    // 响应客户端ID
                    SpaceJoinResponse response = new SpaceJoinResponse(clientId);
                    ackSender.sendAckData(response);
                    
                    log.info("✅ [Socket.IO-空间加入] 空间加入成功: clientId={}, spaceId={}", clientId, data.spaceId);
                    log.info("  📈 当前活跃会话数: {}", clientSessions.size());
                } catch (Exception e) {
                    log.error("❌ [Socket.IO-空间加入] 空间加入失败: spaceId={}", data.spaceId, e);
                    log.error("  🔍 错误详情: message={}, cause={}", e.getMessage(), e.getCause());
                    ackSender.sendAckData(Map.of("error", Map.of("name", "JoinError", "message", e.getMessage())));
                }
            }
        });

        // 文档更新推送事件  
        log.info("🎯 [DEBUG] 注册space:push-doc-update事件监听器");
        socketIOServer.addEventListener("space:push-doc-update", DocUpdatePushRequest.class, new DataListener<DocUpdatePushRequest>() {
            @Override
            public void onData(SocketIOClient client, DocUpdatePushRequest data, com.corundumstudio.socketio.AckRequest ackSender) {
                long startTime = System.currentTimeMillis();
                String sessionId = client.getSessionId().toString();
                ClientSession session = clientSessions.get(sessionId);
                
                try {
                    log.info("🎯🎯🎯 [CRITICAL-DEBUG] space:push-doc-update事件处理器被调用!!!");
                    log.info("📝 [Socket.IO-文档更新] 收到文档更新推送请求");
                    log.info("  📊 请求详情: docId={}, spaceId={}, spaceType={}", 
                        data.docId, data.spaceId, data.spaceType);
                    log.info("  📦 数据详情: updateSize={}字符, base64Length={}", 
                        data.update != null ? data.update.length() : 0,
                        data.update != null ? data.update.length() : 0);
                    log.info("  🔗 客户端详情: sessionId={}, remoteAddress={}", 
                        sessionId, client.getRemoteAddress());
                    
                    if (session != null) {
                        log.info("  👤 会话详情: clientId={}, userId={}", session.clientId, session.userId);
                    } else {
                        log.warn("  ⚠️ 警告: 未找到对应的会话信息");
                    }
                    
                    // 验证必要参数
                    if (data.update == null || data.update.isEmpty()) {
                        throw new IllegalArgumentException("更新数据不能为空");
                    }
                    if (data.docId == null || data.docId.isEmpty()) {
                        throw new IllegalArgumentException("文档ID不能为空");
                    }
                    if (data.spaceId == null || data.spaceId.isEmpty()) {
                        throw new IllegalArgumentException("空间ID不能为空");
                    }
                    
                    log.info("  ✅ 参数验证通过，开始处理文档更新");
                    
                    // 调用现有的文档更新服务
                    log.info("  🔄 调用WorkspaceDocService.applyYjsUpdate()");
                    long timestamp = workspaceDocService.applyYjsUpdate(
                        data.spaceId, 
                        data.docId, 
                        data.update
                    );
                    
                    log.info("  ✅ 文档更新服务调用成功: timestamp={}", timestamp);
                    
                    // 广播给同一空间的其他客户端
                    log.info("  📡 开始广播更新给其他客户端: spaceId={}", data.spaceId);
                    Map<String, Object> broadcastData = Map.of(
                        "spaceType", data.spaceType,
                        "spaceId", data.spaceId,
                        "docId", data.docId,
                        "update", data.update,
                        "timestamp", timestamp,
                        "editor", sessionId
                    );
                    
                    var roomClients = socketIOServer.getRoomOperations(data.spaceId).getClients();
                    log.info("  👥 房间内客户端数量: {}", roomClients.size());
                    
                    socketIOServer.getRoomOperations(data.spaceId)
                        .sendEvent("space:broadcast-doc-update", broadcastData);
                    
                    log.info("  ✅ 广播完成");
                    
                    // 响应时间戳
                    DocUpdatePushResponse response = new DocUpdatePushResponse(timestamp);
                    ackSender.sendAckData(response);
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.info("✅ [Socket.IO-文档更新] 文档更新推送成功");
                    log.info("  📊 处理结果: docId={}, timestamp={}, 耗时={}ms", 
                        data.docId, timestamp, processingTime);
                    
                } catch (Exception e) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.error("❌ [Socket.IO-文档更新] 文档更新推送失败");
                    log.error("  📊 失败详情: docId={}, spaceId={}, 耗时={}ms", 
                        data.docId, data.spaceId, processingTime);
                    log.error("  🔍 错误信息: message={}", e.getMessage());
                    log.error("  📚 完整异常", e);
                    
                    Map<String, Object> errorResponse = Map.of(
                        "error", Map.of(
                            "name", "UpdateError", 
                            "message", e.getMessage(),
                            "docId", data.docId,
                            "spaceId", data.spaceId,
                            "timestamp", System.currentTimeMillis()
                        )
                    );
                    ackSender.sendAckData(errorResponse);
                }
            }
        });

        // 文档加载事件（快照/差异）
        log.info("🎯 [DEBUG] 注册space:load-doc事件监听器");
        socketIOServer.addEventListener("space:load-doc", SpaceLoadDocRequest.class, (client, data, ack) -> {
            try {
                log.info("🧩 [Socket.IO-文档加载] 收到space:load-doc请求: spaceId={}, docId={}, hasStateVector={}",
                        data.spaceId, data.docId, data.stateVector != null && !data.stateVector.isEmpty());

                byte[] stateVectorBytes = null;
                if (data.stateVector != null && !data.stateVector.isEmpty()) {
                    try {
                        stateVectorBytes = java.util.Base64.getDecoder().decode(data.stateVector);
                    } catch (IllegalArgumentException e) {
                        log.warn("stateVector Base64解码失败，忽略stateVector: {}", e.getMessage());
                        stateVectorBytes = null;
                    }
                }

                DocDiff diff = storageAdapter.getDocDiff(data.spaceId, data.docId, stateVectorBytes);
                if (diff == null) {
                    log.warn("📭 [Socket.IO-文档加载] 文档不存在，尝试自动创建初始文档: spaceId={}, docId={}", data.spaceId, data.docId);
                    try {
                        var created = docWriter.createInitialDoc(data.spaceId, data.docId, null).block();
                        if (created != null) {
                            log.info("🆕 [Socket.IO-文档加载] 初始文档已创建: size={}B, ts={}", created.getBlob() != null ? created.getBlob().length : 0, created.getTimestamp());
                        } else {
                            log.warn("⚠️ [Socket.IO-文档加载] 初始文档创建返回null");
                        }
                    } catch (Exception ce) {
                        log.error("❌ [Socket.IO-文档加载] 初始文档创建失败: {}", ce.getMessage(), ce);
                    }
                    // 再次获取diff
                    diff = storageAdapter.getDocDiff(data.spaceId, data.docId, stateVectorBytes);
                    if (diff == null) {
                        log.error("❌ [Socket.IO-文档加载] 自动创建后仍找不到文档: spaceId={}, docId={}", data.spaceId, data.docId);
                        ack.sendAckData(java.util.Map.of(
                            "error", java.util.Map.of(
                                "name", "DOC_NOT_FOUND",
                                "message", "Document not found"
                            )
                        ));
                        return;
                    }
                }

                String missingBase64 = java.util.Base64.getEncoder().encodeToString(diff.getMissing());
                String stateBase64 = java.util.Base64.getEncoder().encodeToString(diff.getState());

                var payload = new SpaceLoadDocResponseData(missingBase64, stateBase64, diff.getTimestamp());
                ack.sendAckData(java.util.Map.of("data", payload));
                log.info("✅ [Socket.IO-文档加载] 返回差异: missingSize={}, stateSize={}, ts={}",
                        diff.getMissing() != null ? diff.getMissing().length : 0,
                        diff.getState() != null ? diff.getState().length : 0,
                        diff.getTimestamp());
            } catch (Exception e) {
                log.error("❌ [Socket.IO-文档加载] 处理失败: {}", e.getMessage(), e);
                ack.sendAckData(java.util.Map.of(
                    "error", java.util.Map.of(
                        "name", "LoadError",
                        "message", e.getMessage()
                    )
                ));
            }
        });

        // 文档时间戳批量加载事件
        log.info("🎯 [DEBUG] 注册space:load-doc-timestamps事件监听器");
        socketIOServer.addEventListener("space:load-doc-timestamps", SpaceLoadDocTimestampsRequest.class, (client, data, ack) -> {
            try {
                Long after = data.timestamp;
                java.util.Map<String, Long> tsMap = storageAdapter.getDocTimestamps(data.spaceId, after);
                ack.sendAckData(java.util.Map.of("data", tsMap));
                log.info("✅ [Socket.IO-时间戳] 返回{}条时间戳", tsMap != null ? tsMap.size() : 0);
            } catch (Exception e) {
                log.error("❌ [Socket.IO-时间戳] 处理失败: {}", e.getMessage(), e);
                ack.sendAckData(java.util.Map.of(
                    "error", java.util.Map.of(
                        "name", "TimestampsError",
                        "message", e.getMessage()
                    )
                ));
            }
        });

        // Awareness 加入
        log.info("🎯 [DEBUG] 注册space:join-awareness事件监听器");
        socketIOServer.addEventListener("space:join-awareness", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                client.joinRoom(spaceId);
                ack.sendAckData(java.util.Map.of("clientId", client.getSessionId().toString()));
                log.info("✅ [Awareness] 加入: spaceId={}, clientId={}", spaceId, client.getSessionId());
            } catch (Exception e) {
                log.error("❌ [Awareness] 加入失败: {}", e.getMessage(), e);
                ack.sendAckData(java.util.Map.of(
                    "error", java.util.Map.of(
                        "name", "JoinAwarenessError",
                        "message", e.getMessage()
                    )
                ));
            }
        });

        // Awareness 触发收集（广播）
        socketIOServer.addEventListener("space:load-awarenesses", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                socketIOServer.getRoomOperations(spaceId).sendEvent("space:collect-awareness", data);
                // 无需ack
                log.info("📡 [Awareness] 触发收集: spaceId={}", spaceId);
            } catch (Exception e) {
                log.error("❌ [Awareness] 触发收集失败: {}", e.getMessage(), e);
            }
        });

        // Awareness 更新广播
        socketIOServer.addEventListener("space:update-awareness", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                socketIOServer.getRoomOperations(spaceId).sendEvent("space:broadcast-awareness-update", data);
                log.info("📡 [Awareness] 广播更新: spaceId={}", spaceId);
            } catch (Exception e) {
                log.error("❌ [Awareness] 广播失败: {}", e.getMessage(), e);
            }
        });

        // Awareness 离开
        socketIOServer.addEventListener("space:leave-awareness", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                client.leaveRoom(spaceId);
                log.info("👋 [Awareness] 离开: spaceId={}", spaceId);
            } catch (Exception e) {
                log.error("❌ [Awareness] 离开失败: {}", e.getMessage(), e);
            }
        });

        log.info("🔧 [Socket.IO] 事件处理器初始化完成");
        log.info("🎯 [DEBUG] 已注册的事件: space:join, space:push-doc-update, space:load-doc, space:load-doc-timestamps, awareness-events");
        log.info("🎯 [DEBUG] 监听端口: {}", socketIOServer.getConfiguration().getPort());
    }

    /**
     * 生成客户端ID
     */
    private String generateClientId() {
        return "client_" + System.currentTimeMillis() + "_" + Math.random();
    }
}
