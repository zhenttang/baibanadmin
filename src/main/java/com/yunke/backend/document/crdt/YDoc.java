package com.yunke.backend.document.crdt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YJS兼容的CRDT文档实现 - Java版本
 * 
 * 完全参考AFFiNE/YJS的核心CRDT算法
 * 实现YATA (Yet Another Transformation Approach) 算法
 */
@Slf4j
@Data
public class YDoc {
    
    private final String guid;
    private final Map<String, YType> share;
    private final StateVector stateVector;
    private final Map<String, AtomicLong> clientClocks;
    private final List<YDocObserver> observers;
    private final Object mutex = new Object();
    
    // 操作历史记录 - 用于OT算法
    private final Map<String, List<Operation>> operationHistory = new ConcurrentHashMap<>();
    private final Map<String, StateVector> stateVectorHistory = new ConcurrentHashMap<>();
    
    // 最大历史记录数量
    private static final int MAX_HISTORY_SIZE = 1000;
    
    /**
     * 创建新的YDoc实例
     */
    public YDoc(String guid) {
        this.guid = guid != null ? guid : UUID.randomUUID().toString();
        this.share = new ConcurrentHashMap<>();
        this.stateVector = new StateVector();
        this.clientClocks = new ConcurrentHashMap<>();
        this.observers = new ArrayList<>();
    }
    
    /**
     * 获取共享类型 - 对应YJS的doc.get()
     */
    @SuppressWarnings("unchecked")
    public <T extends YType> T get(String name, Class<T> type) {
        return (T) share.computeIfAbsent(name, k -> {
            try {
                T instance = type.getDeclaredConstructor(YDoc.class).newInstance(this);
                return instance;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create YType instance", e);
            }
        });
    }
    
    /**
     * 获取YMap - 最常用的类型
     */
    public YMap getMap(String name) {
        return get(name, YMap.class);
    }
    
    /**
     * 获取YArray
     */
    public YArray getArray(String name) {
        return get(name, YArray.class);
    }
    
    /**
     * 获取YText
     */
    public YText getText(String name) {
        return get(name, YText.class);
    }
    
    /**
     * 事务执行 - 保证操作的原子性
     */
    public void transact(String origin, Runnable operations) {
        synchronized (mutex) {
            Transaction transaction = new Transaction(this, origin);
            try {
                operations.run();
                transaction.commit();
                notifyObservers(transaction);
            } catch (Exception e) {
                log.error("Transaction failed: {}", origin, e);
                throw new RuntimeException("Transaction failed", e);
            }
        }
    }
    
    /**
     * 简单事务执行 - 无来源参数
     */
    public void transact(Runnable operations) {
        transact("default", operations);
    }
    /**
     * 应用更新 - 增强版本，记录操作历史
     */
    public void applyUpdate(byte[] update, String origin) {
        synchronized (mutex) {
            try {
                UpdateDecoder decoder = new UpdateDecoder(update);
                Transaction transaction = new Transaction(this, origin);
                
                while (decoder.hasNext()) {
                    Operation op = decoder.readOperation();
                    
                    // 记录操作历史
                    recordOperationHistory(op);
                    
                    applyOperation(op, transaction);
                }
                
                transaction.commit();
                notifyObservers(transaction);
                
            } catch (Exception e) {
                log.error("Failed to apply update", e);
                throw new RuntimeException("Failed to apply update", e);
            }
        }
    }
    
    /**
     * 编码状态为更新 - 对应YJS的Y.encodeStateAsUpdate()
     */
    public byte[] encodeStateAsUpdate(StateVector targetStateVector) {
        synchronized (mutex) {
            UpdateEncoder encoder = new UpdateEncoder();
            
            for (YType type : share.values()) {
                type.encodeUpdate(encoder, targetStateVector);
            }
            
            return encoder.encode();
        }
    }
    
    /**
     * 获取状态向量 - 对应YJS的Y.encodeStateVector()
     */
    public StateVector getStateVector() {
        synchronized (mutex) {
            return stateVector.copy();
        }
    }
    
    /**
     * 应用单个操作
     */
    private void applyOperation(Operation op, Transaction transaction) {
        String typeName = op.getTypeName();
        YType type = share.get(typeName);
        
        if (type == null) {
            log.warn("Type not found: {}", typeName);
            return;
        }
        
        // 检查操作是否已经应用过
        if (isOperationApplied(op)) {
            log.debug("Operation already applied: {}", op.getId());
            return;
        }
        
        // 应用操作到类型
        type.applyOperation(op, transaction);
        
        // 更新状态向量
        stateVector.update(op.getClientId(), op.getClock());
        
        // 更新客户端时钟
        clientClocks.computeIfAbsent(op.getClientId(), k -> new AtomicLong(0))
                   .set(Math.max(clientClocks.get(op.getClientId()).get(), op.getClock()));
    }
    
    /**
     * 检查操作是否已应用
     */
    private boolean isOperationApplied(Operation op) {
        Long clientClock = stateVector.get(op.getClientId());
        return clientClock != null && clientClock >= op.getClock();
    }
    
    /**
     * 添加观察者
     */
    public void observe(YDocObserver observer) {
        observers.add(observer);
    }
    
    /**
     * 移除观察者
     */
    public void unobserve(YDocObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * 通知观察者
     */
    private void notifyObservers(Transaction transaction) {
        for (YDocObserver observer : observers) {
            try {
                observer.onUpdate(this, transaction);
            } catch (Exception e) {
                log.error("Observer notification failed", e);
            }
        }
    }
    
    /**
     * 生成新的客户端ID
     */
    public String generateClientId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 获取下一个时钟值
     */
    public long getNextClock(String clientId) {
        return clientClocks.computeIfAbsent(clientId, k -> new AtomicLong(0))
                          .incrementAndGet();
    }
    
    /**
     * 销毁文档
     */
    public void destroy() {
        synchronized (mutex) {
            observers.clear();
            share.clear();
            clientClocks.clear();
            stateVector.clear();
        }
    }
    
    /**
     * 获取文档大小（用于调试）
     */
    public int size() {
        return share.values().stream()
                   .mapToInt(YType::size)
                   .sum();
    }
    
    /**
     * 获取最近的操作列表
     */
    public List<Operation> getRecentOperations(int limit) {
        List<Operation> allOps = new ArrayList<>();
        
        for (List<Operation> clientOps : operationHistory.values()) {
            allOps.addAll(clientOps);
        }
        
        // 按时间戳排序（最新的在前）
        allOps.sort((op1, op2) -> Long.compare(op2.getClock(), op1.getClock()));
        
        return allOps.stream().limit(limit).toList();
    }
    
    /**
     * 获取操作时的状态向量
     */
    public StateVector getStateVectorAtOperation(Operation operation) {
        String key = operation.getClientId() + ":" + operation.getClock();
        return stateVectorHistory.getOrDefault(key, new StateVector());
    }
    
    /**
     * 记录操作到历史
     */
    private void recordOperationHistory(Operation operation) {
        String clientId = operation.getClientId();
        
        // 记录操作
        operationHistory.computeIfAbsent(clientId, k -> new ArrayList<>()).add(operation);
        
        // 记录状态向量快照
        String key = clientId + ":" + operation.getClock();
        stateVectorHistory.put(key, stateVector.copy());
        
        // 限制历史记录大小
        limitHistorySize(clientId);
    }
    
    /**
     * 限制历史记录大小
     */
    private void limitHistorySize(String clientId) {
        List<Operation> clientOps = operationHistory.get(clientId);
        if (clientOps != null && clientOps.size() > MAX_HISTORY_SIZE) {
            // 移除最旧的操作
            Operation removedOp = clientOps.remove(0);
            String key = clientId + ":" + removedOp.getClock();
            stateVectorHistory.remove(key);
        }
    }
    
    /**
     * 观察者接口
     */
    public interface YDocObserver {
        void onUpdate(YDoc doc, Transaction transaction);
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("guid", guid);
        stats.put("sharedTypes", share.size());
        stats.put("clientClocks", clientClocks.size());
        stats.put("observers", observers.size());
        
        int totalOperations = operationHistory.values().stream()
            .mapToInt(List::size)
            .sum();
        stats.put("totalOperations", totalOperations);
        stats.put("stateVectorSize", stateVectorHistory.size());
        
        return stats;
    }
    
    @Override
    public String toString() {
        return String.format("YDoc{guid='%s', types=%d, size=%d}", 
                           guid, share.size(), size());
    }
}