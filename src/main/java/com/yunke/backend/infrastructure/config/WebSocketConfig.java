package com.yunke.backend.infrastructure.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.yunke.backend.document.collaboration.SpaceSyncGateway;
import com.yunke.backend.document.websocket.DocCollaborationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * WebSocket配置类
 * 配置Socket.IO服务器用于实时协作
 */
@Configuration
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class WebSocketConfig {
    
    private final DocCollaborationHandler collaborationHandler;
    private final SpaceSyncGateway spaceSyncGateway;
    
    @Value("${websocket.port:9092}")
    private int websocketPort;
    
    @Value("${websocket.hostname:localhost}")
    private String websocketHostname;
    
    @Value("${websocket.max-frame-payload-length:65536}")
    private int maxFramePayloadLength;
    
    @Value("${websocket.max-http-content-length:65536}")
    private int maxHttpContentLength;
    
    @Bean
    public SocketIOServer socketIOServer() {
        log.info("🔌 [WebSocketConfig] 初始化统一的Socket.IO服务器 (支持新旧协议)");
        
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(websocketHostname);
        config.setPort(websocketPort);
        
        // 设置传输方式
        config.setTransports(
            com.corundumstudio.socketio.Transport.WEBSOCKET,
            com.corundumstudio.socketio.Transport.POLLING
        );
        
        // 设置缓冲区大小
        config.setMaxFramePayloadLength(maxFramePayloadLength);
        config.setMaxHttpContentLength(maxHttpContentLength);
        
        // 设置心跳检测
        config.setPingInterval(25000); // 25秒
        config.setPingTimeout(60000);  // 60秒
        
        // 启用跨域
        config.setOrigin("*");
        
        // 设置认证（开发环境放宽校验，便于联调）
        // ✅ 注意：Socket.IO Java 库的 AuthorizationListener 只能获取 URL 参数，无法获取 auth 对象
        // 前端现在通过 URL 参数传递 token，以便后端能够获取
        config.setAuthorizationListener(data -> {
            try {
                String token = data.getSingleUrlParam("token");
                if (token != null && !token.isEmpty()) {
                    log.debug("🔐 [WebSocketConfig] 客户端通过URL参数提供了token: {}",
                            token.substring(0, Math.min(10, token.length())) + "...");
                } else {
                    log.debug("🔐 [WebSocketConfig] 未通过URL参数提供token，开发模式放行");
                }
                return new com.corundumstudio.socketio.AuthorizationResult(true);
            } catch (Exception e) {
                log.error("❌ [WebSocketConfig] 客户端认证异常", e);
                return new com.corundumstudio.socketio.AuthorizationResult(true);
            }
        });
        
        SocketIOServer server = new SocketIOServer(config);
        
        // ========== 设置旧协议事件监听器 ==========
        collaborationHandler.setupEventListeners(server);
        
        // ========== 设置新协议事件监听器 (AFFiNE 协议) ==========
        log.info("🔧 [WebSocketConfig] 注册新协议 (space:*) 事件处理器");
        server.addConnectListener(spaceSyncGateway::onConnect);
        server.addDisconnectListener(spaceSyncGateway::onDisconnect);
        server.addEventListener("space:join", java.util.Map.class, spaceSyncGateway::onJoinSpace);
        server.addEventListener("space:leave", java.util.Map.class, spaceSyncGateway::onLeaveSpace);
        server.addEventListener("space:load-doc", java.util.Map.class, spaceSyncGateway::onLoadDoc);
        server.addEventListener("space:push-doc-update", java.util.Map.class, spaceSyncGateway::onPushDocUpdate);
        server.addEventListener("space:delete-doc", java.util.Map.class, (client, data, ackRequest) -> spaceSyncGateway.onDeleteDoc(client, data));
        server.addEventListener("space:load-doc-timestamps", java.util.Map.class, spaceSyncGateway::onLoadDocTimestamps);
        
        // ========== Awareness 事件监听器 (协同感知功能) ==========
        log.info("🔧 [WebSocketConfig] 注册 Awareness 事件处理器");
        
        // Awareness 加入
        server.addEventListener("space:join-awareness", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                String docId = (String) data.get("docId");
                String spaceType = (String) data.get("spaceType");
                
                if (spaceId != null && !spaceId.isEmpty()) {
                    // 加入 awareness 房间（使用 spaceId 作为房间标识）
                    client.joinRoom(spaceId);
                    log.info("✅ [Awareness] 客户端加入 awareness: spaceId={}, docId={}, clientId={}", 
                            spaceId, docId, client.getSessionId());
                    
                    if (ack.isAckRequested()) {
                        ack.sendAckData(java.util.Map.of("clientId", client.getSessionId().toString()));
                    }
                } else {
                    log.warn("⚠️ [Awareness] spaceId 为空，无法加入: clientId={}", client.getSessionId());
                    if (ack.isAckRequested()) {
                        ack.sendAckData(java.util.Map.of(
                            "error", java.util.Map.of(
                                "name", "JoinAwarenessError",
                                "message", "spaceId is required"
                            )
                        ));
                    }
                }
            } catch (Exception e) {
                log.error("❌ [Awareness] 加入失败: {}", e.getMessage(), e);
                if (ack.isAckRequested()) {
                    ack.sendAckData(java.util.Map.of(
                        "error", java.util.Map.of(
                            "name", "JoinAwarenessError",
                            "message", e.getMessage()
                        )
                    ));
                }
            }
        });
        
        // Awareness 触发收集（广播）
        server.addEventListener("space:load-awarenesses", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                String docId = (String) data.get("docId");
                String spaceType = (String) data.get("spaceType");
                
                if (spaceId != null && !spaceId.isEmpty()) {
                    // 广播收集请求到房间内所有客户端
                    server.getRoomOperations(spaceId).sendEvent("space:collect-awareness", data);
                    log.info("📡 [Awareness] 触发收集: spaceId={}, docId={}, 房间内客户端数={}", 
                            spaceId, docId, server.getRoomOperations(spaceId).getClients().size());
                } else {
                    log.warn("⚠️ [Awareness] spaceId 为空，无法触发收集: clientId={}", client.getSessionId());
                }
            } catch (Exception e) {
                log.error("❌ [Awareness] 触发收集失败: {}", e.getMessage(), e);
            }
        });
        
        // Awareness 更新广播
        server.addEventListener("space:update-awareness", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                String docId = (String) data.get("docId");
                String spaceType = (String) data.get("spaceType");
                String awarenessUpdate = (String) data.get("awarenessUpdate");
                
                if (spaceId != null && !spaceId.isEmpty()) {
                    // 广播 awareness 更新到房间内其他客户端（排除发送者）
                    server.getRoomOperations(spaceId).sendEvent("space:broadcast-awareness-update", client, data);
                    
                    log.debug("📡 [Awareness] 广播更新: spaceId={}, docId={}, clientId={}, updateSize={}", 
                            spaceId, docId, client.getSessionId(), 
                            awarenessUpdate != null ? awarenessUpdate.length() : 0);
                } else {
                    log.warn("⚠️ [Awareness] spaceId 为空，无法广播: clientId={}", client.getSessionId());
                }
            } catch (Exception e) {
                log.error("❌ [Awareness] 广播失败: {}", e.getMessage(), e);
            }
        });
        
        // Awareness 离开
        server.addEventListener("space:leave-awareness", java.util.Map.class, (client, data, ack) -> {
            try {
                String spaceId = (String) data.get("spaceId");
                String docId = (String) data.get("docId");
                
                if (spaceId != null && !spaceId.isEmpty()) {
                    // 离开 awareness 房间
                    client.leaveRoom(spaceId);
                    log.info("👋 [Awareness] 客户端离开 awareness: spaceId={}, docId={}, clientId={}", 
                            spaceId, docId, client.getSessionId());
                } else {
                    log.warn("⚠️ [Awareness] spaceId 为空: clientId={}", client.getSessionId());
                }
            } catch (Exception e) {
                log.error("❌ [Awareness] 离开失败: {}", e.getMessage(), e);
            }
        });
        
        log.info("✅ [WebSocketConfig] Socket.IO服务器配置完成 (支持新旧协议 + Awareness): {}:{}", websocketHostname, websocketPort);
        
        return server;
    }
    
    /**
     * 定时清理断开的客户端
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void cleanupDisconnectedClients() {
        try {
            collaborationHandler.cleanupDisconnectedClients();
        } catch (Exception e) {
            log.error("❌ [WebSocketConfig] 清理断开客户端失败", e);
        }
    }
    
    /**
     * 定时打印会话统计
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void logSessionStats() {
        try {
            var stats = collaborationHandler.getSessionStats();
            log.info("📊 [WebSocketConfig] 会话统计: 总会话={}, 文档房间={}, 房间内客户端={}", 
                    stats.get("totalSessions"), stats.get("totalDocRooms"), stats.get("totalClientsInRooms"));
        } catch (Exception e) {
            log.error("❌ [WebSocketConfig] 获取会话统计失败", e);
        }
    }
}
