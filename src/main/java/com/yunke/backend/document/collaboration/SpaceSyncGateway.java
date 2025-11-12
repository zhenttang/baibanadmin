package com.yunke.backend.document.collaboration;

import com.yunke.backend.document.service.YjsServiceClient;
import com.yunke.backend.storage.impl.WorkspaceDocStorageAdapter;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.document.collaboration.model.DocState;
import com.yunke.backend.document.collaboration.model.SyncMessage;
import com.yunke.backend.common.concurrency.ConcurrencyControlService;
import com.yunke.backend.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;

/**
 * 空间同步网关 - 对应AFFiNE的SpaceSyncGateway
 * 
 * 核心功能：
 * 1. 处理客户端的YJS同步消息
 * 2. 管理文档状态和版本控制
 * 3. 实现实时协作的冲突解决
 * 4. 提供WebSocket事件处理接口
 * 
 * 架构说明：
 * - 使用 YjsServiceClient 调用 Node.js yjs-service 进行 CRDT 合并
 * - Java 后端只负责保存原始更新和业务逻辑
 * 
 * 对应开源AFFiNE代码：
 * packages/backend/server/src/core/sync/gateway.ts
 */
@Component
@Slf4j
public class SpaceSyncGateway {
    
    private final YjsServiceClient yjsServiceClient;  // 🔥 使用 yjs-service 微服务
    private final WorkspaceDocStorageAdapter storageAdapter;
    private final WorkspaceDocService docService;
    private final ConcurrencyControlService concurrencyControl;
    private final com.corundumstudio.socketio.SocketIOServer socketIOServer;  // Socket.IO 服务器实例
    private final JwtUtil jwtUtil;  // JWT工具类，用于解析token获取用户ID
    private final ScheduledExecutorService broadcastScheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "doc-broadcast-flusher");
        thread.setDaemon(true);
        return thread;
    });
    
    // 显式构造函数，使用 @Lazy 解决循环依赖
    public SpaceSyncGateway(
            YjsServiceClient yjsServiceClient,
            WorkspaceDocStorageAdapter storageAdapter,
            WorkspaceDocService docService,
            ConcurrencyControlService concurrencyControl,
            @org.springframework.context.annotation.Lazy com.corundumstudio.socketio.SocketIOServer socketIOServer,
            JwtUtil jwtUtil) {
        this.yjsServiceClient = yjsServiceClient;
        this.storageAdapter = storageAdapter;
        this.docService = docService;
        this.concurrencyControl = concurrencyControl;
        this.socketIOServer = socketIOServer;
        this.jwtUtil = jwtUtil;
    }
    
    // 文档状态缓存 - workspaceId:docId -> DocState
    private final Map<String, DocState> docStates = new ConcurrentHashMap<>();

    // 读写锁管理器 - workspaceId:docId -> ReadWriteLock
    private final Map<String, ReentrantReadWriteLock> docLocks = new ConcurrentHashMap<>();

    private static final long BROADCAST_DEBOUNCE_MS = 40L;
    private final ConcurrentHashMap<String, BroadcastBuffer> broadcastBuffers = new ConcurrentHashMap<>();
    
    // YJS消息类型常量
    private static final int YJS_MSG_SYNC = 0;
    private static final int YJS_MSG_AWARENESS = 1;
    private static final int YJS_MSG_AUTH = 2;
    private static final int YJS_MSG_QUERY_AWARENESS = 3;
    
    // YJS同步步骤
    private static final int YJS_SYNC_STEP1 = 0; // 请求状态向量
    private static final int YJS_SYNC_STEP2 = 1; // 发送更新
    private static final int YJS_SYNC_UPDATE = 2; // 增量更新
    
    /**
     * 处理客户端同步消息 - 主入口方法
     * 对应AFFiNE的handleSyncMessage方法
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param message 同步消息二进制数据
     * @param clientId 客户端ID
     * @return 响应消息列表
     */
    public CompletableFuture<List<byte[]>> handleSyncMessage(
            String workspaceId, 
            String docId, 
            byte[] message, 
            String clientId) {
        
        String docKey = workspaceId + ":" + docId;
        log.info("🔄 [SpaceSyncGateway] 处理同步消息: docKey={}, clientId={}, messageSize={}B", 
                docKey, clientId, message.length);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 解析消息
                SyncMessage syncMessage = parseSyncMessage(message);
                log.debug("📨 [SpaceSyncGateway] 解析消息: type={}, syncType={}", 
                         syncMessage.getMessageType(), syncMessage.getSyncType());
                
                // 获取文档锁
                ReentrantReadWriteLock lock = getDocLock(docKey);
                
                switch (syncMessage.getMessageType()) {
                    case YJS_MSG_SYNC:
                        return handleYjsSyncMessage(workspaceId, docId, syncMessage, clientId, lock);
                        
                    case YJS_MSG_AWARENESS:
                        return handleAwarenessMessage(workspaceId, docId, syncMessage, clientId);
                        
                    case YJS_MSG_AUTH:
                        return handleAuthMessage(workspaceId, docId, syncMessage, clientId);
                        
                    default:
                        log.warn("⚠️ [SpaceSyncGateway] 未知消息类型: {}", syncMessage.getMessageType());
                        return Collections.emptyList();
                }
                
            } catch (Exception e) {
                log.error("❌ [SpaceSyncGateway] 处理同步消息失败: docKey={}", docKey, e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * 处理YJS同步消息
     */
    private List<byte[]> handleYjsSyncMessage(
            String workspaceId, 
            String docId, 
            SyncMessage syncMessage, 
            String clientId,
            ReentrantReadWriteLock lock) throws IOException {
        
        List<byte[]> responses = new ArrayList<>();
        String docKey = workspaceId + ":" + docId;
        
        switch (syncMessage.getSyncType()) {
            case YJS_SYNC_STEP1:
                // 客户端请求状态向量
                log.info("📥 [SpaceSyncGateway] Step1 - 客户端请求状态向量: docKey={}", docKey);
                responses.addAll(handleSyncStep1(workspaceId, docId, syncMessage, clientId, lock));
                break;
                
            case YJS_SYNC_STEP2:
                // 客户端发送更新数据
                log.info("📤 [SpaceSyncGateway] Step2 - 客户端发送更新: docKey={}", docKey);
                responses.addAll(handleSyncStep2(workspaceId, docId, syncMessage, clientId, lock));
                break;
                
            case YJS_SYNC_UPDATE:
                // 增量更新
                log.info("🔄 [SpaceSyncGateway] Update - 增量更新: docKey={}", docKey);
                responses.addAll(handleSyncUpdate(workspaceId, docId, syncMessage, clientId, lock));
                break;
                
            default:
                log.warn("⚠️ [SpaceSyncGateway] 未知同步类型: {}", syncMessage.getSyncType());
        }
        
        return responses;
    }
    
    /**
     * 处理同步步骤1：客户端请求状态向量
     * 服务器返回当前文档状态和差异更新
     */
    private List<byte[]> handleSyncStep1(
            String workspaceId, 
            String docId, 
            SyncMessage syncMessage, 
            String clientId,
            ReentrantReadWriteLock lock) throws IOException {
        
        List<byte[]> responses = new ArrayList<>();
        String docKey = workspaceId + ":" + docId;
        
        lock.readLock().lock();
        try {
            // 获取客户端状态向量
            byte[] clientStateVector = syncMessage.getPayload();
            log.debug("📊 [SpaceSyncGateway] 客户端状态向量: size={}B", clientStateVector.length);
            
            // 获取服务器文档状态
            DocState docState = getOrCreateDocState(workspaceId, docId);
            byte[] serverDoc = docState.getCurrentDoc();
            
            if (serverDoc == null || serverDoc.length == 0) {
                log.info("📄 [SpaceSyncGateway] 服务器文档为空，创建新文档: docKey={}", docKey);
                // 创建一个空的 Y.js 更新
                serverDoc = new byte[0];
                docState.setCurrentDoc(serverDoc);
            }
            
            // 🔥 使用 yjs-service 计算差异更新
            byte[] diffUpdate = null;
            if (serverDoc.length > 0) {
                diffUpdate = yjsServiceClient.diffUpdate(serverDoc, clientStateVector);
            } else {
                diffUpdate = serverDoc;
            }
            
            if (diffUpdate != null && diffUpdate.length > 0) {
                log.info("📤 [SpaceSyncGateway] 发送差异更新给客户端: docKey={}, diffSize={}B", 
                        docKey, diffUpdate.length);
                
                // 构造Step2响应消息
                byte[] step2Response = createSyncStep2Message(diffUpdate);
                responses.add(step2Response);
            } else {
                log.debug("✅ [SpaceSyncGateway] 客户端已是最新状态: docKey={}", docKey);
            }
            
            // 🔥 使用 yjs-service 编码状态向量
            byte[] serverStateVector = null;
            if (serverDoc.length > 0) {
                serverStateVector = yjsServiceClient.encodeStateVector(serverDoc);
            } else {
                serverStateVector = new byte[0];
            }
            byte[] step1Response = createSyncStep1Response(serverStateVector);
            responses.add(step1Response);
            
        } finally {
            lock.readLock().unlock();
        }
        
        return responses;
    }
    
    /**
     * 处理同步步骤2：客户端发送更新数据
     * 服务器应用更新并可能返回冲突解决后的数据
     */
    private List<byte[]> handleSyncStep2(
            String workspaceId, 
            String docId, 
            SyncMessage syncMessage, 
            String clientId,
            ReentrantReadWriteLock lock) throws IOException {
        
        List<byte[]> responses = new ArrayList<>();
        String docKey = workspaceId + ":" + docId;
        
        lock.writeLock().lock();
        try {
            byte[] clientUpdate = syncMessage.getPayload();
            log.info("📥 [SpaceSyncGateway] 接收客户端更新: docKey={}, updateSize={}B", 
                    docKey, clientUpdate.length);
            
            // 获取文档状态
            DocState docState = getOrCreateDocState(workspaceId, docId);
            byte[] currentDoc = docState.getCurrentDoc();
            
            // 应用客户端更新
            List<byte[]> updates = new ArrayList<>();
            if (currentDoc != null && currentDoc.length > 0) {
                updates.add(currentDoc);
            }
            updates.add(clientUpdate);
            
            // 🔥 使用 yjs-service 微服务合并更新
            log.info("📞 [SpaceSyncGateway] 调用 yjs-service 合并 {} 个更新", updates.size());
            byte[] mergedDoc = yjsServiceClient.mergeUpdates(updates);
            docState.setCurrentDoc(mergedDoc);
            docState.setLastModified(System.currentTimeMillis());
            
            log.info("✅ [SpaceSyncGateway] 文档更新完成: docKey={}, newSize={}B", 
                    docKey, mergedDoc.length);

            // 返回一个同步更新包给发送者，确保客户端完成Yjs协议流程
            if (clientUpdate != null && clientUpdate.length > 0) {
                try {
                    responses.add(createSyncUpdateMessage(clientUpdate));
                } catch (IOException ioException) {
                    log.warn("⚠️ [SpaceSyncGateway] 构造同步更新消息失败: docKey={}", docKey, ioException);
                }
            }
            
            // 异步保存原始更新到数据库（不保存合并后的，只保存原始更新）
            CompletableFuture.runAsync(() -> {
                try {
                    List<byte[]> updateList = Collections.singletonList(clientUpdate);
                    storageAdapter.pushDocUpdates(workspaceId, docId, updateList, clientId);
                    log.debug("💾 [SpaceSyncGateway] 异步保存原始更新成功: docKey={}", docKey);
                } catch (Exception e) {
                    log.error("❌ [SpaceSyncGateway] 异步保存失败: docKey={}", docKey, e);
                }
            });
            
            // 通常Step2不需要响应，除非有冲突需要解决
            
        } finally {
            lock.writeLock().unlock();
        }
        
        return responses;
    }
    
    /**
     * 处理增量更新：实时协作中的增量数据
     */
    private List<byte[]> handleSyncUpdate(
            String workspaceId, 
            String docId, 
            SyncMessage syncMessage, 
            String clientId,
            ReentrantReadWriteLock lock) throws IOException {
        
        // 增量更新的处理逻辑与Step2一致
        return handleSyncStep2(workspaceId, docId, syncMessage, clientId, lock);
    }
    
    /**
     * 处理感知（Awareness）消息 - 用于显示其他用户的光标和选择
     */
    private List<byte[]> handleAwarenessMessage(
            String workspaceId, 
            String docId, 
            SyncMessage syncMessage, 
            String clientId) {
        
        String docKey = workspaceId + ":" + docId;
        log.debug("👁️ [SpaceSyncGateway] 处理感知消息: docKey={}, clientId={}", docKey, clientId);
        
        try {
            // 获取感知状态数据
            byte[] awarenessData = syncMessage.getPayload();
            if (awarenessData == null || awarenessData.length == 0) {
                log.warn("⚠️ [SpaceSyncGateway] 感知消息数据为空: clientId={}", clientId);
                return Collections.emptyList();
            }
            
            // 验证感知数据格式
            if (awarenessData.length > 8192) { // 8KB limit for awareness data
                log.warn("⚠️ [SpaceSyncGateway] 感知消息过大: clientId={}, size={}B", clientId, awarenessData.length);
                return Collections.emptyList();
            }
            
            log.debug("👁️ [SpaceSyncGateway] 感知状态处理完成: clientId={}, dataSize={}B", 
                     clientId, awarenessData.length);
            
            // 通常感知消息不需要响应，而是由WebSocket层负责广播
            return Collections.emptyList();
            
        } catch (OutOfMemoryError e) {
            log.error("💥 [SpaceSyncGateway] 处理感知消息内存不足: docKey={}, clientId={}", 
                     docKey, clientId, e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ [SpaceSyncGateway] 处理感知消息失败: docKey={}, clientId={}", 
                     docKey, clientId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 处理认证消息
     */
    private List<byte[]> handleAuthMessage(
            String workspaceId, 
            String docId, 
            SyncMessage syncMessage, 
            String clientId) {
        
        String docKey = workspaceId + ":" + docId;
        log.debug("🔐 [SpaceSyncGateway] 处理认证消息: docKey={}, clientId={}", docKey, clientId);
        
        try {
            // 获取认证数据
            byte[] authData = syncMessage.getPayload();
            if (authData == null || authData.length == 0) {
                log.warn("⚠️ [SpaceSyncGateway] 认证消息数据为空: clientId={}", clientId);
                return Collections.emptyList();
            }
            
            // 验证认证数据大小
            if (authData.length > 1024) { // 1KB limit for auth data
                log.warn("⚠️ [SpaceSyncGateway] 认证消息过大: clientId={}, size={}B", clientId, authData.length);
                return Collections.emptyList();
            }
            
            // TODO: 实现具体的认证逻辑
            // 1. 解析认证令牌
            // 2. 验证用户权限
            // 3. 返回认证结果
            
            // 基础验证：检查是否为有效的JSON或token格式
            String authString = new String(authData, "UTF-8");
            if (authString.trim().isEmpty() || authString.length() < 5) {
                log.warn("⚠️ [SpaceSyncGateway] 认证数据格式无效: clientId={}", clientId);
                return Collections.emptyList();
            }
            
            log.debug("🔐 [SpaceSyncGateway] 认证消息处理完成: clientId={}", clientId);
            
            // 认证成功，返回空响应（表示通过）
            return Collections.emptyList();
            
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("💥 [SpaceSyncGateway] 认证数据编码错误: docKey={}, clientId={}", 
                     docKey, clientId, e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ [SpaceSyncGateway] 处理认证消息失败: docKey={}, clientId={}", 
                     docKey, clientId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取或创建文档状态
     */
    private DocState getOrCreateDocState(String workspaceId, String docId) {
        String docKey = workspaceId + ":" + docId;
        
        return docStates.computeIfAbsent(docKey, key -> {
            log.info("📄 [SpaceSyncGateway] 创建新文档状态: docKey={}", key);
            
            // 从数据库加载现有文档
            try {
                var docRecord = storageAdapter.getDoc(workspaceId, docId);
                if (docRecord != null && docRecord.getBlob() != null) {
                    byte[] docBlob = docRecord.getBlob();
                    
                    // 验证文档数据
                    if (docBlob.length > 50 * 1024 * 1024) { // 50MB limit
                        log.warn("⚠️ [SpaceSyncGateway] 文档过大，跳过加载: docKey={}, size={}MB", 
                                key, docBlob.length / (1024 * 1024));
                        return new DocState();
                    }
                    
                    log.info("💾 [SpaceSyncGateway] 从数据库加载文档: docKey={}, size={}B", 
                            key, docBlob.length);
                    return new DocState(docBlob, docRecord.getTimestamp());
                }
            } catch (OutOfMemoryError e) {
                log.error("💥 [SpaceSyncGateway] 加载文档内存不足: docKey={}", key, e);
                return new DocState();
            } catch (Exception e) {
                log.warn("⚠️ [SpaceSyncGateway] 从数据库加载文档失败: docKey={}", key, e);
            }
            
            // 创建空文档状态
            return new DocState();
        });
    }
    
    /**
     * 获取文档锁
     */
    private ReentrantReadWriteLock getDocLock(String docKey) {
        return docLocks.computeIfAbsent(docKey, key -> new ReentrantReadWriteLock());
    }
    
    /**
     * 解析同步消息
     */
    private SyncMessage parseSyncMessage(byte[] message) throws IOException {
        if (message == null || message.length == 0) {
            throw new IOException("消息数据为空");
        }
        
        if (message.length > 1024 * 1024) { // 1MB limit
            throw new IOException("消息过大: " + message.length + " bytes");
        }
        
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(message))) {
            int messageType = readVarUint(dis);
            
            // 验证消息类型
            if (messageType < 0 || messageType > 10) {
                throw new IOException("无效的消息类型: " + messageType);
            }
            
            if (messageType == YJS_MSG_SYNC) {
                int syncType = readVarUint(dis);
                
                // 验证同步类型
                if (syncType < 0 || syncType > 5) {
                    throw new IOException("无效的同步类型: " + syncType);
                }
                
                int remainingBytes = dis.available();
                if (remainingBytes > 10 * 1024 * 1024) { // 10MB limit for payload
                    throw new IOException("载荷过大: " + remainingBytes + " bytes");
                }
                
                byte[] payload = new byte[remainingBytes];
                dis.readFully(payload);
                
                return new SyncMessage(messageType, syncType, payload);
            } else {
                // 其他类型的消息
                int remainingBytes = dis.available();
                if (remainingBytes > 1024 * 1024) { // 1MB limit for other message types
                    throw new IOException("非同步消息载荷过大: " + remainingBytes + " bytes");
                }
                
                byte[] payload = new byte[remainingBytes];
                dis.readFully(payload);
                
                return new SyncMessage(messageType, -1, payload);
            }
        } catch (IOException e) {
            throw e; // Re-throw IOException
        } catch (Exception e) {
            throw new IOException("解析消息时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建Step1响应消息
     */
    private byte[] createSyncStep1Response(byte[] stateVector) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            
            writeVarUint(dos, YJS_MSG_SYNC);
            writeVarUint(dos, YJS_SYNC_STEP1);
            dos.write(stateVector);
            
            return baos.toByteArray();
        }
    }
    
    /**
     * 创建Step2消息
     */
    private byte[] createSyncStep2Message(byte[] update) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            
            writeVarUint(dos, YJS_MSG_SYNC);
            writeVarUint(dos, YJS_SYNC_STEP2);
            dos.write(update);
            
            return baos.toByteArray();
        }
    }

    private byte[] createSyncUpdateMessage(byte[] update) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            writeVarUint(dos, YJS_MSG_SYNC);
            writeVarUint(dos, YJS_SYNC_UPDATE);
            dos.write(update);

            return baos.toByteArray();
        }
    }
    
    // ================== 二进制编码工具方法 ==================
    
    /**
     * 读取变长无符号整数
     */
    private int readVarUint(DataInputStream dis) throws IOException {
        int result = 0;
        int shift = 0;
        int bytesRead = 0;
        
        while (true) {
            if (bytesRead >= 5) { // VarUint最多5字节
                throw new IOException("VarUint过长，可能数据损坏");
            }
            
            byte b = dis.readByte();
            result |= (b & 0x7F) << shift;
            bytesRead++;
            
            if ((b & 0x80) == 0) {
                break;
            }
            
            shift += 7;
            if (shift >= 32) {
                throw new IOException("VarUint超出32位范围");
            }
        }
        
        if (result < 0) {
            throw new IOException("VarUint结果为负数，数据可能损坏");
        }
        
        return result;
    }
    
    /**
     * 写入变长无符号整数
     */
    private void writeVarUint(DataOutputStream dos, int value) throws IOException {
        if (value < 0) {
            throw new IOException("VarUint值不能为负数: " + value);
        }
        
        int bytesWritten = 0;
        while (value >= 0x80) {
            if (bytesWritten >= 5) { // 防止无限循环
                throw new IOException("VarUint编码过长");
            }
            dos.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
            bytesWritten++;
        }
        dos.writeByte(value & 0x7F);
    }
    
    /**
     * 清理文档状态缓存
     * 应该定期调用以释放内存
     */
    public void cleanupDocStates() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 3600_000; // 1小时
        
        docStates.entrySet().removeIf(entry -> {
            DocState state = entry.getValue();
            return (currentTime - state.getLastModified()) > maxAge;
        });
        
        log.info("🧹 [SpaceSyncGateway] 清理文档状态缓存完成，当前缓存数量: {}", docStates.size());
    }
    
    /**
     * 获取当前缓存的文档数量
     */
    public int getCachedDocCount() {
        return docStates.size();
    }
    
    /**
     * 强制刷新文档状态到数据库
     */
    public CompletableFuture<Void> flushDocState(String workspaceId, String docId) {
        String docKey = workspaceId + ":" + docId;
        DocState docState = docStates.get(docKey);
        
        if (docState == null || docState.getCurrentDoc() == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                ReentrantReadWriteLock lock = getDocLock(docKey);
                lock.readLock().lock();
                try {
                    byte[] currentDoc = docState.getCurrentDoc();
                    if (currentDoc != null && currentDoc.length > 0) {
                        List<byte[]> updates = Collections.singletonList(currentDoc);
                        storageAdapter.pushDocUpdates(workspaceId, docId, updates, "system");
                        log.info("💾 [SpaceSyncGateway] 强制刷新文档状态: docKey={}", docKey);
                    }
                } finally {
                    lock.readLock().unlock();
                }
            } catch (Exception e) {
                log.error("❌ [SpaceSyncGateway] 强制刷新文档状态失败: docKey={}", docKey, e);
            }
        });
    }
    
    /**
     * 获取文档房间状态
     */
    public Map<String, Object> getDocRoomStatus(String workspaceId, String docId) {
        String docKey = workspaceId + ":" + docId;
        DocState docState = docStates.get(docKey);
        
        return Map.of(
            "docKey", docKey,
            "hasState", docState != null,
            "docSize", docState != null && docState.getCurrentDoc() != null ? docState.getCurrentDoc().length : 0,
            "lastModified", docState != null ? docState.getLastModified() : 0
        );
    }
    
    // ================== 兼容性方法 ==================
    
    /**
     * 客户端连接处理（兼容性）
     */
    public void onConnect(com.corundumstudio.socketio.SocketIOClient client) {
        String clientId = client.getSessionId().toString();
        
        // ✅ 从 handshake 数据中获取 token，解析用户ID并存储到客户端会话
        try {
            String token = null;
            
            // 方法1: 从 URL 参数获取 token
            try {
                token = client.getHandshakeData().getSingleUrlParam("token");
            } catch (Exception e) {
                // 忽略
            }
            
            // 方法2: 从 HTTP header 获取 token (Authorization: Bearer <token>)
            if ((token == null || token.isEmpty()) && client.getHandshakeData().getHttpHeaders() != null) {
                try {
                    String authHeader = client.getHandshakeData().getHttpHeaders().get("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
            
            if (token != null && !token.isEmpty()) {
                String userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null && !userId.isEmpty()) {
                    // 存储用户ID到客户端会话
                    client.set("userId", userId);
                    log.info("🔗 [SpaceSyncGateway] 客户端连接: clientId={}, userId={}", clientId, userId);
                } else {
                    log.warn("⚠️ [SpaceSyncGateway] 无法从token解析用户ID: clientId={}", clientId);
                }
            } else {
                log.warn("⚠️ [SpaceSyncGateway] 客户端未提供token: clientId={}", clientId);
            }
        } catch (Exception e) {
            log.warn("⚠️ [SpaceSyncGateway] 解析token失败: clientId={}, error={}", clientId, e.getMessage());
        }
        
        log.info("🔗 [SpaceSyncGateway] 客户端连接: clientId={}", clientId);
    }
    
    /**
     * 客户端断开处理（兼容性）
     */
    public void onDisconnect(com.corundumstudio.socketio.SocketIOClient client) {
        String clientId = client.getSessionId().toString();
        log.info("❌ [SpaceSyncGateway] 客户端断开: clientId={}", clientId);
    }
    
    /**
     * 加入空间处理（兼容性）
     */
    public void onJoinSpace(com.corundumstudio.socketio.SocketIOClient client, java.util.Map data, com.corundumstudio.socketio.AckRequest ackRequest) {
        String clientId = client.getSessionId().toString();
        String spaceId = sanitizeIdentifier(data != null ? data.get("spaceId") : null);
        String spaceType = sanitizeIdentifier(data != null ? data.get("spaceType") : null);
        log.info("🏠 [SpaceSyncGateway] 客户端加入空间: clientId={}, spaceId={}, spaceType={}", clientId, spaceId, spaceType);
        
        // 将客户端加入 Socket.IO 房间，用于广播
        if (spaceId != null && !spaceId.isEmpty() && !"null".equals(spaceId)) {
            client.joinRoom(spaceId);
            log.info("✅ [SpaceSyncGateway] 客户端已加入房间: clientId={}, spaceId={}", clientId, spaceId);
        } else {
            log.warn("⚠️ [SpaceSyncGateway] spaceId 为空，无法加入房间: clientId={}", clientId);
        }
        
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData(java.util.Map.of("data", java.util.Map.of("clientId", clientId)));
        }
    }
    
    /**
     * 离开空间处理（兼容性）
     */
    public void onLeaveSpace(com.corundumstudio.socketio.SocketIOClient client, java.util.Map data, com.corundumstudio.socketio.AckRequest ackRequest) {
        String clientId = client.getSessionId().toString();
        String spaceId = sanitizeIdentifier(data != null ? data.get("spaceId") : null);
        log.info("🚪 [SpaceSyncGateway] 客户端离开空间: clientId={}, spaceId={}", clientId, spaceId);
        
        // 将客户端从 Socket.IO 房间移除
        if (spaceId != null && !spaceId.isEmpty() && !"null".equals(spaceId)) {
            client.leaveRoom(spaceId);
            log.info("✅ [SpaceSyncGateway] 客户端已离开房间: clientId={}, spaceId={}", clientId, spaceId);
        }
        
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData(java.util.Map.of("data", java.util.Map.of("ok", true)));
        }
    }
    
    /**
     * 加载文档处理（兼容性）
     */
    public void onLoadDoc(com.corundumstudio.socketio.SocketIOClient client, java.util.Map data, com.corundumstudio.socketio.AckRequest ackRequest) {
        String clientId = client.getSessionId().toString();
        String spaceId = sanitizeIdentifier(data != null ? data.get("spaceId") : null);
        String docId = sanitizeIdentifier(data != null ? data.get("docId") : null);
        String stateVectorB64 = sanitizeIdentifier(data != null ? data.get("stateVector") : null);

        long startTime = System.currentTimeMillis();  // 开始计时
        log.info("📄 [SpaceSyncGateway] 加载文档: clientId={}, spaceId={}, docId={}, hasStateVector={}",
                clientId, spaceId, docId, stateVectorB64 != null && !stateVectorB64.isEmpty());
        try {
            long dbStart = System.currentTimeMillis();
            var docRecord = storageAdapter.getDoc(spaceId, docId);
            long dbTime = System.currentTimeMillis() - dbStart;

            if (docRecord == null || docRecord.getBlob() == null) {
                // ✅ 文档不存在，这是创建新文档的正常情况
                // 自动创建文档元数据，并返回空文档让前端初始化
                log.info("📝 [SpaceSyncGateway] 文档不存在: docId={}，这是创建新文档的正常情况，自动创建元数据", docId);
                
                try {
                    // ✅ 从客户端会话获取用户ID
                    String userId = (String) client.get("userId");
                    if (userId == null || userId.isEmpty()) {
                        log.warn("⚠️ [SpaceSyncGateway] 无法从客户端会话获取用户ID: clientId={}", clientId);
                        // 尝试从 handshake 数据中获取 token 并解析
                        try {
                            String token = null;
                            
                            // 方法1: 从 URL 参数获取 token
                            try {
                                token = client.getHandshakeData().getSingleUrlParam("token");
                            } catch (Exception e) {
                                // 忽略
                            }
                            
                            // 方法2: 从 HTTP header 获取 token (Authorization: Bearer <token>)
                            if ((token == null || token.isEmpty()) && client.getHandshakeData().getHttpHeaders() != null) {
                                try {
                                    String authHeader = client.getHandshakeData().getHttpHeaders().get("Authorization");
                                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                        token = authHeader.substring(7);
                                    }
                                } catch (Exception e) {
                                    // 忽略
                                }
                            }
                            
                            if (token != null && !token.isEmpty()) {
                                userId = jwtUtil.getUserIdFromToken(token);
                                if (userId != null && !userId.isEmpty()) {
                                    client.set("userId", userId);  // 存储到会话中
                                    log.info("📝 [SpaceSyncGateway] 从token解析用户ID成功: userId={}", userId);
                                }
                            }
                        } catch (Exception tokenEx) {
                            log.debug("📝 [SpaceSyncGateway] 从token解析用户ID失败: {}", tokenEx.getMessage());
                        }
                    }
                    
                    // ✅ 直接创建文档元数据（不创建快照，快照由后续的更新请求创建）
                    // 注意：不需要先查询，因为我们已经知道文档不存在（docRecord == null）
                    // 使用 ensureMetadataExists 方法，它不进行权限检查（因为用户已经通过 Socket.IO 认证）
                    if (docService instanceof com.yunke.backend.document.service.impl.WorkspaceDocServiceImpl) {
                        try {
                            ((com.yunke.backend.document.service.impl.WorkspaceDocServiceImpl) docService)
                                    .ensureMetadataExists(spaceId, docId);
                            log.info("📝 [SpaceSyncGateway] ✅ 文档元数据已自动创建: docId={}, userId={}", docId, userId);
                        } catch (Exception createEx) {
                            // 如果创建失败（可能是因为已存在），记录日志但继续
                            log.debug("📝 [SpaceSyncGateway] 文档元数据可能已存在或创建失败: docId={}, error={}", 
                                    docId, createEx.getMessage());
                            // 不抛出异常，继续返回空文档
                        }
                    } else {
                        // 降级处理：直接创建文档元数据（会进行权限检查，可能失败）
                        try {
                            docService.createDoc(spaceId, userId, "Untitled", docId);
                            log.info("📝 [SpaceSyncGateway] ✅ 文档元数据已创建（降级方式）: docId={}, userId={}", docId, userId);
                        } catch (Exception createEx) {
                            log.debug("📝 [SpaceSyncGateway] 文档元数据创建失败（降级方式）: docId={}, userId={}, error={}", 
                                    docId, userId, createEx.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [SpaceSyncGateway] 自动创建文档元数据失败: {}", e.getMessage(), e);
                    // 继续返回空文档，让前端可以初始化
                }
                
                // 返回空文档（空的 YJS 文档）
                byte[] emptyDoc = yjsServiceClient.createEmptyDoc(docId);
                byte[] emptyStateVector = yjsServiceClient.encodeStateVector(emptyDoc);
                
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData(java.util.Map.of(
                        "data", java.util.Map.of(
                            "missing", java.util.Base64.getEncoder().encodeToString(emptyDoc),
                            "state", java.util.Base64.getEncoder().encodeToString(emptyStateVector),
                            "timestamp", System.currentTimeMillis()
                        )
                    ));
                }
                log.info("📝 [SpaceSyncGateway] ✅ 返回空文档让前端初始化: docId={}, 大小={}B", docId, emptyDoc.length);
                return;
            }

            byte[] serverUpdate = docRecord.getBlob();
            byte[] serverStateVector;
            long yjsStart = System.currentTimeMillis();
            try {
                // 🔥 使用 yjs-service 编码状态向量
                serverStateVector = yjsServiceClient.encodeStateVector(serverUpdate);
            } catch (Exception e) {
                log.warn("⚠️ [SpaceSyncGateway] 编码状态向量失败，置空: {}", e.getMessage());
                serverStateVector = new byte[0];
            }
            long yjsTime = System.currentTimeMillis() - yjsStart;

            byte[] missing;
            try {
                if (stateVectorB64 != null && !stateVectorB64.isEmpty()) {
                    byte[] clientStateVector = java.util.Base64.getDecoder().decode(stateVectorB64);
                    // 🔥 使用 yjs-service 计算差异
                    missing = yjsServiceClient.diffUpdate(serverUpdate, clientStateVector);
                } else {
                    missing = serverUpdate;
                }
            } catch (Exception e) {
                log.warn("⚠️ [SpaceSyncGateway] 计算diff失败，返回完整文档: {}", e.getMessage());
                missing = serverUpdate;
            }

            long ts = docRecord.getTimestamp();
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(java.util.Map.of(
                    "data", java.util.Map.of(
                        "missing", java.util.Base64.getEncoder().encodeToString(missing),
                        "state", java.util.Base64.getEncoder().encodeToString(serverStateVector),
                        "timestamp", ts
                    )
                ));
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("⚡ [Performance] 文档加载完成: docId={}, 总耗时={}ms (数据库={}ms, YJS={}ms, 文档大小={}B)",
                    docId, totalTime, dbTime, yjsTime, docRecord.getBlob().length);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("❌ [SpaceSyncGateway] 加载文档失败: spaceId={}, docId={}, 耗时={}ms", spaceId, docId, totalTime, e);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(java.util.Map.of(
                    "error", java.util.Map.of("name", "INTERNAL_ERROR", "message", e.getMessage())
                ));
            }
        }
    }
    
    /**
     * 推送文档更新处理（兼容性）
     */
    public void onPushDocUpdate(com.corundumstudio.socketio.SocketIOClient client, java.util.Map data, com.corundumstudio.socketio.AckRequest ackRequest) {
        String clientId = client.getSessionId().toString();
        String providedClientId = sanitizeIdentifier(data != null ? data.get("clientId") : null);
        String sessionId = sanitizeIdentifier(data != null ? data.get("sessionId") : null);
        String spaceId = sanitizeIdentifier(data != null ? data.get("spaceId") : null);
        String docId = sanitizeIdentifier(data != null ? data.get("docId") : null);
        String spaceType = sanitizeIdentifier(data != null ? data.get("spaceType") : null);
        String updateB64 = sanitizeIdentifier(data != null ? data.get("update") : null);
        log.info("📤 [SpaceSyncGateway] 推送更新: clientId={}, spaceId={}, docId={}, hasUpdate={}",
                clientId, spaceId, docId, updateB64 != null && !updateB64.isEmpty());
        try {
            if (spaceId == null || docId == null) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData(java.util.Map.of(
                        "error", java.util.Map.of("name", "INVALID_PARAMS", "message", "spaceId and docId are required")
                    ));
                }
                return;
            }
            if (updateB64 == null || updateB64.isEmpty()) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData(java.util.Map.of(
                        "error", java.util.Map.of("name", "INVALID_PARAMS", "message", "update is required")
                    ));
                }
                return;
            }
            byte[] update = java.util.Base64.getDecoder().decode(updateB64);
            String editorIdentifier = firstNonBlank(sessionId, providedClientId, clientId);
            long ts = storageAdapter.pushDocUpdates(spaceId, docId, java.util.List.of(update), editorIdentifier);

            enqueueDocBroadcast(spaceType, spaceId, docId, updateB64, ts, clientId, editorIdentifier, providedClientId, client);
            if (ackRequest.isAckRequested()) {
                // CloudDocStorage 期望顶层 timestamp 字段
                ackRequest.sendAckData(java.util.Map.of("timestamp", ts));
            }
        } catch (Exception e) {
            log.error("❌ [SpaceSyncGateway] 推送更新失败: spaceId={}, docId={}", spaceId, docId, e);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(java.util.Map.of(
                    "error", java.util.Map.of("name", "INTERNAL_ERROR", "message", e.getMessage())
                ));
            }
        }
    }

    private void enqueueDocBroadcast(
        String spaceType,
        String spaceId,
        String docId,
        String update,
        long timestamp,
        String editorClientId,
        String sessionIdentifier,
        String providedClientId,
        com.corundumstudio.socketio.SocketIOClient originClient
    ) {
        if (spaceId == null || spaceId.isEmpty()) {
            return;
        }
        BroadcastBuffer buffer = broadcastBuffers.compute(spaceId + ':' + docId, (key, existing) -> {
            if (existing == null) {
                existing = new BroadcastBuffer(spaceType, spaceId, docId);
            }
            existing.addPayload(new PendingPayload(buildBroadcastPayload(spaceType, spaceId, docId, update, timestamp, editorClientId, sessionIdentifier, providedClientId), originClient));
            return existing;
        });

        if (buffer != null && buffer.markScheduled()) {
            broadcastScheduler.schedule(() -> flushBroadcastBuffer(spaceId, docId, buffer), BROADCAST_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private Map<String, Object> buildBroadcastPayload(
        String spaceType,
        String spaceId,
        String docId,
        String update,
        long timestamp,
        String editorClientId,
        String sessionIdentifier,
        String providedClientId
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("spaceType", spaceType);
        payload.put("spaceId", spaceId);
        payload.put("docId", docId);
        payload.put("update", update);
        payload.put("timestamp", timestamp);
        payload.put("editor", editorClientId);
        if (sessionIdentifier != null) {
            payload.put("sessionId", sessionIdentifier);
        }
        if (providedClientId != null) {
            payload.put("clientId", providedClientId);
        }
        return payload;
    }

    private void flushBroadcastBuffer(String spaceId, String docId, BroadcastBuffer buffer) {
        String key = spaceId + ':' + docId;
        broadcastBuffers.remove(key, buffer);
        List<PendingPayload> pending = buffer.drainPayloads();
        buffer.clearScheduled();

        if (pending.isEmpty()) {
            return;
        }

        if (pending.size() == 1) {
            PendingPayload single = pending.get(0);
            try {
                socketIOServer.getRoomOperations(spaceId).sendEvent("space:broadcast-doc-update", single.originClient, single.payload);
            } catch (Exception ex) {
                log.warn("⚠️ [SpaceSyncGateway] 单条广播失败: {}", ex.getMessage());
            }
            return;
        }

        List<Map<String, Object>> updates = pending.stream()
            .map(payload -> payload.payload)
            .collect(Collectors.toList());

        Map<String, Object> batchMessage = new HashMap<>();
        batchMessage.put("spaceType", buffer.spaceType);
        batchMessage.put("spaceId", spaceId);
        batchMessage.put("docId", docId);
        batchMessage.put("updates", updates);

        try {
            socketIOServer.getRoomOperations(spaceId).getClients().forEach(roomClient -> roomClient.sendEvent("space:broadcast-doc-updates", batchMessage));
        } catch (Exception ex) {
            log.warn("⚠️ [SpaceSyncGateway] 批量广播失败: {}", ex.getMessage());
        }
    }

    private static final class BroadcastBuffer {
        private final String spaceType;
        private final String spaceId;
        private final String docId;
        private final List<PendingPayload> payloads = new ArrayList<>();
        private final AtomicBoolean scheduled = new AtomicBoolean(false);

        BroadcastBuffer(String spaceType, String spaceId, String docId) {
            this.spaceType = spaceType;
            this.spaceId = spaceId;
            this.docId = docId;
        }

        void addPayload(PendingPayload payload) {
            synchronized (payloads) {
                payloads.add(payload);
            }
        }

        boolean markScheduled() {
            return scheduled.compareAndSet(false, true);
        }

        void clearScheduled() {
            scheduled.set(false);
        }

        List<PendingPayload> drainPayloads() {
            synchronized (payloads) {
                List<PendingPayload> drained = new ArrayList<>(payloads);
                payloads.clear();
                return drained;
            }
        }
    }

    private record PendingPayload(Map<String, Object> payload, com.corundumstudio.socketio.SocketIOClient originClient) {}

    @PreDestroy
    public void shutdownBroadcastScheduler() {
        broadcastScheduler.shutdown();
    }

    /**
     * 删除文档处理（兼容性）
     */
    public void onDeleteDoc(com.corundumstudio.socketio.SocketIOClient client, java.util.Map data) {
        String clientId = client.getSessionId().toString();
        log.info("🗑️ [SpaceSyncGateway] 删除文档事件: clientId={}", clientId);
    }
    
    /**
     * 加载文档时间戳处理（兼容性）
     */
    public void onLoadDocTimestamps(com.corundumstudio.socketio.SocketIOClient client, java.util.Map data, com.corundumstudio.socketio.AckRequest ackRequest) {
        String clientId = client.getSessionId().toString();
        String spaceId = sanitizeIdentifier(data != null ? data.get("spaceId") : null);
        Long after = null;
        try {
            Object ts = data != null ? data.get("timestamp") : null;
            if (ts instanceof Number) {
                after = ((Number) ts).longValue();
            } else if (ts instanceof String && !((String) ts).isBlank()) {
                after = Long.parseLong((String) ts);
            }
        } catch (Exception ignore) {}
        log.info("🕒 [SpaceSyncGateway] 加载文档时间戳: clientId={}, spaceId={}, after={}", clientId, spaceId, after);
        try {
            java.util.Map<String, Long> timestamps = storageAdapter.getDocTimestamps(spaceId, after);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(java.util.Map.of("data", timestamps));
            }
        } catch (Exception e) {
            log.error("❌ [SpaceSyncGateway] 加载文档时间戳失败: spaceId={}", spaceId, e);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(java.util.Map.of(
                    "error", java.util.Map.of("name", "INTERNAL_ERROR", "message", e.getMessage())
                ));
            }
        }
    }

    private String sanitizeIdentifier(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("null".equalsIgnoreCase(value) || "undefined".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
