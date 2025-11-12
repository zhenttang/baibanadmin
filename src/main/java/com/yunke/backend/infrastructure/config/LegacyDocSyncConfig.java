package com.yunke.backend.infrastructure.config;

import com.yunke.backend.document.collaboration.SpaceSyncGateway;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 遗留文档同步配置（参考上游 WebSocket 配置）
 *
 * ⚠️ 已弃用：现在统一使用 WebSocketConfig (9092端口)
 * 该配置创建的 9093 端口服务器已被整合到 WebSocketConfig 中
 *
 * 对应上游实现中的 WebSocket 模块配置，保留作参考
 */
//@Component  // 已禁用，使用 WebSocketConfig 统一处理
@Slf4j
public class LegacyDocSyncConfig {
    
    @Value("${socketio.server.host:localhost}")
    private String host;
    
    @Value("${socketio.server.port:9093}")
    private Integer port;
    
    @Value("${socketio.server.bossCount:1}")
    private int bossCount;
    
    @Value("${socketio.server.workCount:100}")
    private int workCount;
    
    @Value("${socketio.server.allowCustomRequests:true}")
    private boolean allowCustomRequests;
    
    @Value("${socketio.server.upgradeTimeout:10000}")
    private int upgradeTimeout;
    
    @Value("${socketio.server.pingTimeout:60000}")
    private int pingTimeout;
    
    @Value("${socketio.server.pingInterval:25000}")
    private int pingInterval;
    
    /**
     * 创建SocketIO服务器 - 但不立即启动
     * 对应上游的 WebSocket 服务器配置
     */
    @Bean("legacyDocSyncSocketIOServer")
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(bossCount);
        config.setWorkerThreads(workCount);
        config.setAllowCustomRequests(allowCustomRequests);
        config.setUpgradeTimeout(upgradeTimeout);
        config.setPingTimeout(pingTimeout);
        config.setPingInterval(pingInterval);
        
        // CORS配置
        config.setOrigin("*");
        
        // WebSocket传输设置
        config.setTransports(
            com.corundumstudio.socketio.Transport.WEBSOCKET,
            com.corundumstudio.socketio.Transport.POLLING
        );
        
        return new SocketIOServer(config);
    }
    
    /**
     * SocketIO服务器启动器 - 在应用完全启动后才启动服务器
     */
    @Bean
    public CommandLineRunner socketIOServerRunner(@Qualifier("legacyDocSyncSocketIOServer") SocketIOServer server, SpaceSyncGateway gateway) {
        return args -> {
            try {
                log.info("🔧 正在配置 SocketIO 事件处理器...");
                
                // 注册事件处理器
                server.addConnectListener(gateway::onConnect);
                server.addDisconnectListener(gateway::onDisconnect);
                
                // 注册事件监听器
                server.addEventListener("space:join", java.util.Map.class, gateway::onJoinSpace);
                server.addEventListener("space:leave", java.util.Map.class, gateway::onLeaveSpace);
                server.addEventListener("space:load-doc", java.util.Map.class, gateway::onLoadDoc);
                server.addEventListener("space:push-doc-update", java.util.Map.class, gateway::onPushDocUpdate);
                server.addEventListener("space:delete-doc", java.util.Map.class, (client, data, ackRequest) -> gateway.onDeleteDoc(client, data));
                server.addEventListener("space:load-doc-timestamps", java.util.Map.class, gateway::onLoadDocTimestamps);
                
                // 启动服务器
                server.start();
                
                log.info("🚀 SocketIO 服务器启动成功 - Host: {}, Port: {}", host, port);
                
            } catch (Exception e) {
                log.error("❌ SocketIO 服务器启动失败", e);
                throw e;
            }
        };
    }
}
