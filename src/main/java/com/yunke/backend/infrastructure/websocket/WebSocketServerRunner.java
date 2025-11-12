package com.yunke.backend.infrastructure.websocket;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * WebSocket服务器启动器
 * 负责启动和关闭Socket.IO服务器
 * 
 * 统一启动 Socket.IO 服务器 (9092端口)，支持新旧协议
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketServerRunner implements CommandLineRunner {
    
    private final SocketIOServer socketIOServer;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 [WebSocketServerRunner] 启动Socket.IO服务器");
        
        try {
            socketIOServer.start();
            log.info("✅ [WebSocketServerRunner] Socket.IO服务器启动成功");
        } catch (Exception e) {
            log.error("❌ [WebSocketServerRunner] Socket.IO服务器启动失败", e);
            throw e;
        }
    }
    
    @PreDestroy
    public void destroy() {
        log.info("🛑 [WebSocketServerRunner] 停止Socket.IO服务器");
        
        try {
            if (socketIOServer != null) {
                socketIOServer.stop();
                log.info("✅ [WebSocketServerRunner] Socket.IO服务器停止成功");
            }
        } catch (Exception e) {
            log.error("❌ [WebSocketServerRunner] Socket.IO服务器停止失败", e);
        }
    }
}

