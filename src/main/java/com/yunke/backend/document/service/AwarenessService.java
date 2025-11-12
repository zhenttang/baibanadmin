package com.yunke.backend.document.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 协作感知服务 - 完全对应AFFiNE的Awareness实现
 * 
 * 管理用户的实时协作状态：光标位置、选择范围、在线状态等
 * 实现YJS Awareness协议
 */
@Service
@Slf4j
public class AwarenessService {
    
    private final ObjectMapper objectMapper;
    
    // 客户端感知状态存储
    private final Map<String, Map<String, AwarenessState>> spaceAwarenessMap = new ConcurrentHashMap<>();
    
    // 客户端元数据
    private final Map<String, ClientMetadata> clientMetadata = new ConcurrentHashMap<>();
    
    // 定期清理任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // 感知状态超时时间（毫秒）
    private static final long AWARENESS_TIMEOUT = 30000; // 30秒
    
    // 心跳间隔
    private static final long HEARTBEAT_INTERVAL = 20000; // 20秒
    
    public AwarenessService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // 启动定期清理任务
        scheduler.scheduleAtFixedRate(this::cleanupExpiredStates, 
                HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 感知状态数据结构
     */
    public static class AwarenessState {
        private String clientId;
        private Map<String, Object> user;
        private Map<String, Object> cursor;
        private Map<String, Object> selection;
        private String currentDoc;
        private long timestamp;
        private boolean online;
        
        // Constructors, getters, setters
        public AwarenessState() {
            this.timestamp = System.currentTimeMillis();
            this.online = true;
        }
        
        public AwarenessState(String clientId, Map<String, Object> user) {
            this();
            this.clientId = clientId;
            this.user = user;
        }
        
        // Getters and Setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public Map<String, Object> getUser() { return user; }
        public void setUser(Map<String, Object> user) { this.user = user; }
        
        public Map<String, Object> getCursor() { return cursor; }
        public void setCursor(Map<String, Object> cursor) { this.cursor = cursor; }
        
        public Map<String, Object> getSelection() { return selection; }
        public void setSelection(Map<String, Object> selection) { this.selection = selection; }
        
        public String getCurrentDoc() { return currentDoc; }
        public void setCurrentDoc(String currentDoc) { this.currentDoc = currentDoc; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
        
        /**
         * 更新时间戳
         */
        public void touch() {
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * 检查是否过期
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > AWARENESS_TIMEOUT;
        }
        
        @Override
        public String toString() {
            return String.format("AwarenessState{clientId='%s', user=%s, online=%s, timestamp=%d}",
                               clientId, user, online, timestamp);
        }
    }
    
    /**
     * 客户端元数据
     */
    public static class ClientMetadata {
        private final String sessionId;
        private final String spaceId;
        private final String userId;
        private final long connectTime;
        private long lastHeartbeat;
        
        public ClientMetadata(String sessionId, String spaceId, String userId) {
            this.sessionId = sessionId;
            this.spaceId = spaceId;
            this.userId = userId;
            this.connectTime = System.currentTimeMillis();
            this.lastHeartbeat = connectTime;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getSpaceId() { return spaceId; }
        public String getUserId() { return userId; }
        public long getConnectTime() { return connectTime; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        
        public void updateHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastHeartbeat > AWARENESS_TIMEOUT;
        }
    }
    
    /**
     * 感知状态变更事件
     */
    public static class AwarenessEvent {
        public enum Type { ADDED, UPDATED, REMOVED }
        
        private final Type type;
        private final String spaceId;
        private final String clientId;
        private final AwarenessState state;
        
        public AwarenessEvent(Type type, String spaceId, String clientId, AwarenessState state) {
            this.type = type;
            this.spaceId = spaceId;
            this.clientId = clientId;
            this.state = state;
        }
        
        // Getters
        public Type getType() { return type; }
        public String getSpaceId() { return spaceId; }
        public String getClientId() { return clientId; }
        public AwarenessState getState() { return state; }
    }
    
    /**
     * 初始化定期清理任务
     */
    public void initCleanupTasks() {
        // 定期清理过期的感知状态
        scheduler.scheduleAtFixedRate(this::cleanupExpiredStates, 
                                    HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        
        log.info("Awareness cleanup tasks initialized");
    }
    
    /**
     * 更新客户端感知状态
     */
    public void updateAwareness(String spaceId, String clientId, AwarenessState state) {
        if (spaceId == null || clientId == null || state == null) {
            log.warn("Invalid awareness update parameters");
            return;
        }
        
        state.setClientId(clientId);
        state.touch();
        
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.computeIfAbsent(
            spaceId, k -> new ConcurrentHashMap<>()
        );
        
        AwarenessState oldState = spaceAwareness.put(clientId, state);
        
        AwarenessEvent.Type eventType = oldState == null ? 
            AwarenessEvent.Type.ADDED : AwarenessEvent.Type.UPDATED;
        
        AwarenessEvent event = new AwarenessEvent(eventType, spaceId, clientId, state);
        
        log.debug("Updated awareness: spaceId={}, clientId={}, type={}", spaceId, clientId, eventType);
        
        // 广播感知状态变更
        broadcastAwarenessEvent(event);
    }
    
    /**
     * 更新光标位置
     */
    public void updateCursor(String spaceId, String clientId, String docId, 
                           int line, int column, Map<String, Object> additionalData) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return;
        }
        
        AwarenessState state = spaceAwareness.get(clientId);
        if (state == null) {
            return;
        }
        
        Map<String, Object> cursor = new HashMap<>();
        cursor.put("docId", docId);
        cursor.put("line", line);
        cursor.put("column", column);
        cursor.put("timestamp", System.currentTimeMillis());
        
        if (additionalData != null) {
            cursor.putAll(additionalData);
        }
        
        state.setCursor(cursor);
        state.setCurrentDoc(docId);
        state.touch();
        
        // 广播光标更新
        AwarenessEvent event = new AwarenessEvent(
            AwarenessEvent.Type.UPDATED, spaceId, clientId, state
        );
        broadcastAwarenessEvent(event);
        
        log.debug("Updated cursor: spaceId={}, clientId={}, docId={}, position={}:{}", 
                 spaceId, clientId, docId, line, column);
    }
    
    /**
     * 更新选择范围
     */
    public void updateSelection(String spaceId, String clientId, String docId,
                              int startLine, int startColumn, int endLine, int endColumn) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return;
        }
        
        AwarenessState state = spaceAwareness.get(clientId);
        if (state == null) {
            return;
        }
        
        Map<String, Object> selection = new HashMap<>();
        selection.put("docId", docId);
        selection.put("start", Map.of("line", startLine, "column", startColumn));
        selection.put("end", Map.of("line", endLine, "column", endColumn));
        selection.put("timestamp", System.currentTimeMillis());
        
        state.setSelection(selection);
        state.setCurrentDoc(docId);
        state.touch();
        
        // 广播选择更新
        AwarenessEvent event = new AwarenessEvent(
            AwarenessEvent.Type.UPDATED, spaceId, clientId, state
        );
        broadcastAwarenessEvent(event);
        
        log.debug("Updated selection: spaceId={}, clientId={}, docId={}, range={}:{}-{}:{}", 
                 spaceId, clientId, docId, startLine, startColumn, endLine, endColumn);
    }
    
    /**
     * 设置用户在线状态
     */
    public void setUserOnline(String spaceId, String clientId, Map<String, Object> userInfo) {
        AwarenessState state = new AwarenessState(clientId, userInfo);
        state.setOnline(true);
        
        updateAwareness(spaceId, clientId, state);
        
        log.info("User came online: spaceId={}, clientId={}, user={}", spaceId, clientId, userInfo);
    }
    
    /**
     * 设置用户离线状态
     */
    public void setUserOffline(String spaceId, String clientId) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return;
        }
        
        AwarenessState state = spaceAwareness.get(clientId);
        if (state != null) {
            state.setOnline(false);
            state.touch();
            
            // 广播离线状态
            AwarenessEvent event = new AwarenessEvent(
                AwarenessEvent.Type.UPDATED, spaceId, clientId, state
            );
            broadcastAwarenessEvent(event);
            
            // 延迟删除离线用户的感知状态
            scheduler.schedule(() -> removeAwareness(spaceId, clientId), 
                             5, TimeUnit.SECONDS);
        }
        
        log.info("User went offline: spaceId={}, clientId={}", spaceId, clientId);
    }
    
    /**
     * 移除感知状态
     */
    public void removeAwareness(String spaceId, String clientId) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return;
        }
        
        AwarenessState removedState = spaceAwareness.remove(clientId);
        if (removedState != null) {
            AwarenessEvent event = new AwarenessEvent(
                AwarenessEvent.Type.REMOVED, spaceId, clientId, removedState
            );
            broadcastAwarenessEvent(event);
            
            log.debug("Removed awareness: spaceId={}, clientId={}", spaceId, clientId);
        }
        
        // 清理空的空间感知映射
        if (spaceAwareness.isEmpty()) {
            spaceAwarenessMap.remove(spaceId);
        }
    }
    
    /**
     * 获取空间内所有感知状态
     */
    public Map<String, AwarenessState> getSpaceAwareness(String spaceId) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return new HashMap<>();
        }
        
        // 返回副本，防止外部修改
        return new HashMap<>(spaceAwareness);
    }
    
    /**
     * 获取在线用户列表
     */
    public List<AwarenessState> getOnlineUsers(String spaceId) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return new ArrayList<>();
        }
        
        return spaceAwareness.values().stream()
            .filter(AwarenessState::isOnline)
            .filter(state -> !state.isExpired())
            .toList();
    }
    
    /**
     * 获取文档的协作者
     */
    public List<AwarenessState> getDocumentCollaborators(String spaceId, String docId) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null) {
            return new ArrayList<>();
        }
        
        return spaceAwareness.values().stream()
            .filter(AwarenessState::isOnline)
            .filter(state -> !state.isExpired())
            .filter(state -> docId.equals(state.getCurrentDoc()))
            .toList();
    }
    
    /**
     * 注册客户端
     */
    public void registerClient(String sessionId, String spaceId, String userId) {
        ClientMetadata metadata = new ClientMetadata(sessionId, spaceId, userId);
        clientMetadata.put(sessionId, metadata);
        
        log.debug("Registered client: sessionId={}, spaceId={}, userId={}", sessionId, spaceId, userId);
    }
    
    /**
     * 注销客户端
     */
    public void unregisterClient(String sessionId) {
        ClientMetadata metadata = clientMetadata.remove(sessionId);
        if (metadata != null) {
            // 设置用户离线
            setUserOffline(metadata.getSpaceId(), sessionId);
            
            log.debug("Unregistered client: sessionId={}, spaceId={}, userId={}", 
                     sessionId, metadata.getSpaceId(), metadata.getUserId());
        }
    }
    
    /**
     * 更新客户端心跳
     */
    public void updateClientHeartbeat(String sessionId) {
        ClientMetadata metadata = clientMetadata.get(sessionId);
        if (metadata != null) {
            metadata.updateHeartbeat();
        }
    }
    
    /**
     * 编码感知状态为二进制格式（YJS兼容）
     */
    public byte[] encodeAwarenessUpdate(String spaceId, Set<String> changedClients) {
        Map<String, AwarenessState> spaceAwareness = spaceAwarenessMap.get(spaceId);
        if (spaceAwareness == null || changedClients.isEmpty()) {
            return new byte[0];
        }
        
        try {
            Map<String, Object> update = new HashMap<>();
            Map<String, Object> clients = new HashMap<>();
            
            for (String clientId : changedClients) {
                AwarenessState state = spaceAwareness.get(clientId);
                if (state != null) {
                    clients.put(clientId, serializeAwarenessState(state));
                }
            }
            
            update.put("clients", clients);
            update.put("timestamp", System.currentTimeMillis());
            
            String json = objectMapper.writeValueAsString(update);
            return json.getBytes("UTF-8");
            
        } catch (Exception e) {
            log.error("Failed to encode awareness update", e);
            return new byte[0];
        }
    }
    
    /**
     * 解码感知状态更新
     */
    public void decodeAwarenessUpdate(String spaceId, byte[] updateData) {
        if (updateData == null || updateData.length == 0) {
            log.debug("👁️ [AwarenessService] 感知更新数据为空: spaceId={}", spaceId);
            return;
        }
        
        if (updateData.length > 1024 * 1024) { // 1MB limit
            log.warn("⚠️ [AwarenessService] 感知更新数据过大: spaceId={}, size={}MB", 
                    spaceId, updateData.length / (1024 * 1024));
            return;
        }
        
        try {
            String json = new String(updateData, "UTF-8");
            
            // 验证JSON格式
            if (json.trim().isEmpty() || json.length() > 100000) { // 100KB JSON limit
                log.warn("⚠️ [AwarenessService] JSON数据格式无效: spaceId={}, jsonLength={}", 
                        spaceId, json.length());
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> update = objectMapper.readValue(json, Map.class);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> clients = (Map<String, Object>) update.get("clients");
            
            if (clients != null && !clients.isEmpty()) {
                // 限制客户端数量
                if (clients.size() > 1000) {
                    log.warn("⚠️ [AwarenessService] 客户端数量过多: spaceId={}, count={}", 
                            spaceId, clients.size());
                    return;
                }
                
                for (Map.Entry<String, Object> entry : clients.entrySet()) {
                    String clientId = entry.getKey();
                    
                    // 验证clientId
                    if (clientId == null || clientId.trim().isEmpty() || clientId.length() > 100) {
                        log.warn("⚠️ [AwarenessService] 无效的clientId: spaceId={}, clientId={}", 
                                spaceId, clientId);
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stateData = (Map<String, Object>) entry.getValue();
                    
                    if (stateData != null) {
                        try {
                            AwarenessState state = deserializeAwarenessState(clientId, stateData);
                            updateAwareness(spaceId, clientId, state);
                        } catch (Exception e) {
                            log.warn("⚠️ [AwarenessService] 反序列化客户端状态失败: spaceId={}, clientId={}", 
                                    spaceId, clientId, e);
                        }
                    }
                }
            }
            
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("💥 [AwarenessService] 编码错误: spaceId={}", spaceId, e);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.warn("⚠️ [AwarenessService] JSON解析失败: spaceId={}", spaceId, e);
        } catch (OutOfMemoryError e) {
            log.error("💥 [AwarenessService] 解码感知更新内存不足: spaceId={}", spaceId, e);
        } catch (Exception e) {
            log.error("❌ [AwarenessService] 解码感知更新失败: spaceId={}", spaceId, e);
        }
    }
    
    /**
     * 序列化感知状态
     */
    private Map<String, Object> serializeAwarenessState(AwarenessState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("user", state.getUser());
        data.put("cursor", state.getCursor());
        data.put("selection", state.getSelection());
        data.put("currentDoc", state.getCurrentDoc());
        data.put("online", state.isOnline());
        data.put("timestamp", state.getTimestamp());
        return data;
    }
    
    /**
     * 反序列化感知状态
     */
    private AwarenessState deserializeAwarenessState(String clientId, Map<String, Object> data) {
        AwarenessState state = new AwarenessState();
        state.setClientId(clientId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        state.setUser(user);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cursor = (Map<String, Object>) data.get("cursor");
        state.setCursor(cursor);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> selection = (Map<String, Object>) data.get("selection");
        state.setSelection(selection);
        
        state.setCurrentDoc((String) data.get("currentDoc"));
        state.setOnline((Boolean) data.getOrDefault("online", true));
        
        Object timestamp = data.get("timestamp");
        if (timestamp instanceof Number) {
            state.setTimestamp(((Number) timestamp).longValue());
        }
        
        return state;
    }
    
    /**
     * 广播感知状态事件
     */
    private void broadcastAwarenessEvent(AwarenessEvent event) {
        log.debug("📢 [AwarenessService] 广播感知状态事件: {}", event);
        
        try {
            // 构建广播数据
            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("type", event.getType().toString().toLowerCase());
            broadcastData.put("clientId", event.getClientId());
            broadcastData.put("timestamp", System.currentTimeMillis());
            
            if (event.getState() != null) {
                broadcastData.put("user", event.getState().getUser());
                broadcastData.put("cursor", event.getState().getCursor());
                broadcastData.put("selection", event.getState().getSelection());
                broadcastData.put("currentDoc", event.getState().getCurrentDoc());
                broadcastData.put("online", event.getState().isOnline());
            }
            
            // TODO: 使用事件发布机制替代直接的SocketIO调用
            // 这样可以避免循环依赖，由其他组件负责实际的WebSocket广播
            String roomName = "workspace:" + event.getSpaceId();
            log.debug("📢 [AwarenessService] 准备广播到房间: {}", roomName);
            
            log.debug("✅ [AwarenessService] 感知状态已广播: spaceId={}, clientId={}", 
                     event.getSpaceId(), event.getClientId());
                     
        } catch (Exception e) {
            log.error("❌ [AwarenessService] 广播感知状态失败", e);
        }
    }
    
    /**
     * 清理过期的感知状态
     */
    private void cleanupExpiredStates() {
        long now = System.currentTimeMillis();
        int cleanedStates = 0;
        int cleanedClients = 0;
        
        // 清理过期的感知状态
        for (Map.Entry<String, Map<String, AwarenessState>> spaceEntry : spaceAwarenessMap.entrySet()) {
            String spaceId = spaceEntry.getKey();
            Map<String, AwarenessState> spaceAwareness = spaceEntry.getValue();
            
            List<String> expiredClients = new ArrayList<>();
            
            for (Map.Entry<String, AwarenessState> clientEntry : spaceAwareness.entrySet()) {
                if (clientEntry.getValue().isExpired()) {
                    expiredClients.add(clientEntry.getKey());
                }
            }
            
            for (String clientId : expiredClients) {
                removeAwareness(spaceId, clientId);
                cleanedStates++;
            }
        }
        
        // 清理过期的客户端元数据
        List<String> expiredSessions = new ArrayList<>();
        for (Map.Entry<String, ClientMetadata> entry : clientMetadata.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredSessions.add(entry.getKey());
            }
        }
        
        for (String sessionId : expiredSessions) {
            unregisterClient(sessionId);
            cleanedClients++;
        }
        
        if (cleanedStates > 0 || cleanedClients > 0) {
            log.info("Cleaned up expired awareness data: {} states, {} clients", 
                    cleanedStates, cleanedClients);
        }
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSpaces", spaceAwarenessMap.size());
        stats.put("totalClients", clientMetadata.size());
        
        int totalStates = spaceAwarenessMap.values().stream()
            .mapToInt(Map::size)
            .sum();
        stats.put("totalStates", totalStates);
        
        int onlineUsers = spaceAwarenessMap.values().stream()
            .mapToInt(spaceMap -> (int) spaceMap.values().stream()
                .filter(AwarenessState::isOnline)
                .filter(state -> !state.isExpired())
                .count())
            .sum();
        stats.put("onlineUsers", onlineUsers);
        
        return stats;
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("Awareness service shut down");
    }
}