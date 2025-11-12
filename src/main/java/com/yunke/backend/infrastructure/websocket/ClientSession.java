package com.yunke.backend.infrastructure.websocket;

import com.corundumstudio.socketio.SocketIOClient;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 客户端会话模型
 * 维护WebSocket客户端的会话信息
 */
@Data
public class ClientSession {
    
    /**
     * 客户端ID
     */
    private final String clientId;
    
    /**
     * Socket.IO客户端连接
     */
    private final SocketIOClient client;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 连接时间戳
     */
    private final long connectTime;
    
    /**
     * 最后活跃时间
     */
    private long lastActiveTime;
    
    /**
     * 已加入的文档集合
     */
    private final Set<String> joinedDocs = new HashSet<>();
    
    /**
     * 当前正在编辑的文档 [workspaceId, docId]
     */
    private String[] currentDoc;
    
    public ClientSession(String clientId, SocketIOClient client) {
        this.clientId = clientId;
        this.client = client;
        this.connectTime = System.currentTimeMillis();
        this.lastActiveTime = this.connectTime;
    }
    
    /**
     * 加入文档
     */
    public void joinDoc(String docKey, String workspaceId, String docId, String userId) {
        this.joinedDocs.add(docKey);
        this.currentDoc = new String[]{workspaceId, docId};
        this.userId = userId;
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 离开文档
     */
    public void leaveDoc(String docKey) {
        this.joinedDocs.remove(docKey);
        if (this.currentDoc != null && docKey.equals(this.currentDoc[0] + ":" + this.currentDoc[1])) {
            this.currentDoc = null;
        }
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 更新活跃时间
     */
    public void updateActivity() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 获取连接持续时间
     */
    public long getConnectionDuration() {
        return System.currentTimeMillis() - connectTime;
    }
    
    /**
     * 获取非活跃时间
     */
    public long getInactiveTime() {
        return System.currentTimeMillis() - lastActiveTime;
    }
    
    /**
     * 检查是否连接正常
     */
    public boolean isConnected() {
        return client.isChannelOpen();
    }
}

