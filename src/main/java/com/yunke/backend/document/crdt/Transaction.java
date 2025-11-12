package com.yunke.backend.document.crdt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CRDT事务 - 管理操作的批处理和一致性
 * 
 * 完全对应YJS的Transaction实现
 * 确保操作的原子性和一致性
 */
@Slf4j
@Data
public class Transaction {
    
    public enum Status {
        ACTIVE,      // 事务活跃
        COMMITTING,  // 正在提交
        COMMITTED,   // 已提交
        ABORTED      // 已中止
    }
    
    private final String id;
    private final YDoc doc;
    private final String origin;
    private final long timestamp;
    
    private final List<Operation> operations;
    private final Set<String> changedTypes;
    private final Map<String, Object> metadata;
    private final AtomicBoolean readOnly;
    private final AtomicLong operationCounter;
    
    private Status status;
    private String clientId;
    private Exception lastError;
    
    // 回调接口
    public interface TransactionObserver {
        void beforeCommit(Transaction transaction);
        void afterCommit(Transaction transaction);
        void onAbort(Transaction transaction, Exception cause);
    }
    
    private final List<TransactionObserver> observers;
    
    /**
     * 创建新事务
     */
    public Transaction(YDoc doc, String origin) {
        this.id = UUID.randomUUID().toString();
        this.doc = doc;
        this.origin = origin != null ? origin : "unknown";
        this.timestamp = System.currentTimeMillis();
        
        this.operations = new ArrayList<>();
        this.changedTypes = new HashSet<>();
        this.metadata = new HashMap<>();
        this.readOnly = new AtomicBoolean(false);
        this.operationCounter = new AtomicLong(0);
        this.observers = new ArrayList<>();
        
        this.status = Status.ACTIVE;
        this.clientId = doc.generateClientId();
        
        log.debug("Created transaction: id={}, origin={}", id, origin);
    }
    
    /**
     * 添加操作到事务
     */
    public void addOperation(Operation operation) {
        checkActive();
        
        if (readOnly.get()) {
            throw new IllegalStateException("Transaction is read-only");
        }
        
        operations.add(operation);
        changedTypes.add(operation.getTypeName());
        operationCounter.incrementAndGet();
        
        log.debug("Added operation to transaction {}: {}", id, operation);
    }
    
    /**
     * 批量添加操作
     */
    public void addOperations(List<Operation> ops) {
        checkActive();
        
        for (Operation op : ops) {
            addOperation(op);
        }
    }
    
    /**
     * 创建并添加插入操作
     */
    public Operation insert(String typeName, String parentId, Object key, 
                          int index, Object content) {
        checkActive();
        
        long clock = doc.getNextClock(clientId);
        Operation operation = Operation.createInsert(
            clientId, clock, typeName, parentId, key, index, content, origin
        );
        
        addOperation(operation);
        return operation;
    }
    
    /**
     * 创建并添加删除操作
     */
    public Operation delete(String typeName, String parentId, Object key, 
                          int index, int length) {
        checkActive();
        
        long clock = doc.getNextClock(clientId);
        Operation operation = Operation.createDelete(
            clientId, clock, typeName, parentId, key, index, length, origin
        );
        
        addOperation(operation);
        return operation;
    }
    
    /**
     * 创建并添加格式化操作
     */
    public Operation format(String typeName, String parentId, int index, 
                          int length, Object attributes) {
        checkActive();
        
        long clock = doc.getNextClock(clientId);
        Operation operation = Operation.createFormat(
            clientId, clock, typeName, parentId, index, length, attributes, origin
        );
        
        addOperation(operation);
        return operation;
    }
    
    /**
     * 设置事务元数据
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * 获取事务元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 设置为只读模式
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly.set(readOnly);
    }
    
    /**
     * 检查是否为只读
     */
    public boolean isReadOnly() {
        return readOnly.get();
    }
    
    /**
     * 检查是否有变更
     */
    public boolean hasChanges() {
        return !operations.isEmpty();
    }
    
    /**
     * 获取变更的类型列表
     */
    public Set<String> getChangedTypes() {
        return new HashSet<>(changedTypes);
    }
    
    /**
     * 添加观察者
     */
    public void addObserver(TransactionObserver observer) {
        observers.add(observer);
    }
    
    /**
     * 移除观察者
     */
    public void removeObserver(TransactionObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * 提交事务
     */
    public void commit() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Transaction is not active: " + status);
        }
        
        if (operations.isEmpty()) {
            log.debug("Transaction {} has no operations, skipping commit", id);
            status = Status.COMMITTED;
            return;
        }
        
        status = Status.COMMITTING;
        
        try {
            // 通知观察者即将提交
            notifyBeforeCommit();
            
            // 合并相邻的操作以优化
            List<Operation> optimizedOps = optimizeOperations(operations);
            
            // 应用操作到文档
            for (Operation operation : optimizedOps) {
                doc.getStateVector().update(operation.getClientId(), operation.getClock());
            }
            
            // 更新文档状态
            updateDocumentState();
            
            status = Status.COMMITTED;
            
            // 通知观察者提交完成
            notifyAfterCommit();
            
            log.info("Transaction {} committed successfully with {} operations", 
                    id, optimizedOps.size());
            
        } catch (Exception e) {
            lastError = e;
            log.error("Failed to commit transaction {}", id, e);
            abort(e);
            throw new RuntimeException("Transaction commit failed", e);
        }
    }
    
    /**
     * 中止事务
     */
    public void abort() {
        abort(new IllegalStateException("Transaction aborted"));
    }
    
    /**
     * 中止事务（带原因）
     */
    public void abort(Exception cause) {
        if (status == Status.COMMITTED) {
            throw new IllegalStateException("Cannot abort committed transaction");
        }
        
        status = Status.ABORTED;
        lastError = cause;
        
        // 通知观察者
        notifyAbort(cause);
        
        log.warn("Transaction {} aborted: {}", id, cause.getMessage());
    }
    
    /**
     * 检查事务是否活跃
     */
    private void checkActive() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Transaction is not active: " + status);
        }
    }
    
    /**
     * 优化操作序列
     */
    private List<Operation> optimizeOperations(List<Operation> ops) {
        if (ops.size() <= 1) {
            return new ArrayList<>(ops);
        }
        
        List<Operation> optimized = new ArrayList<>();
        Operation current = null;
        
        for (Operation op : ops) {
            if (current == null) {
                current = op;
            } else if (current.canMergeWith(op)) {
                current = current.mergeWith(op);
            } else {
                optimized.add(current);
                current = op;
            }
        }
        
        if (current != null) {
            optimized.add(current);
        }
        
        log.debug("Optimized {} operations to {} for transaction {}", 
                 ops.size(), optimized.size(), id);
        
        return optimized;
    }
    
    /**
     * 更新文档状态
     */
    private void updateDocumentState() {
        // 更新文档的客户端时钟
        for (Operation op : operations) {
            doc.getClientClocks().computeIfAbsent(op.getClientId(), 
                k -> new AtomicLong(0)).set(Math.max(
                    doc.getClientClocks().get(op.getClientId()).get(), 
                    op.getClock()
                ));
        }
    }
    
    /**
     * 通知观察者即将提交
     */
    private void notifyBeforeCommit() {
        for (TransactionObserver observer : observers) {
            try {
                observer.beforeCommit(this);
            } catch (Exception e) {
                log.error("Observer notification failed", e);
            }
        }
    }
    
    /**
     * 通知观察者提交完成
     */
    private void notifyAfterCommit() {
        for (TransactionObserver observer : observers) {
            try {
                observer.afterCommit(this);
            } catch (Exception e) {
                log.error("Observer notification failed", e);
            }
        }
    }
    
    /**
     * 通知观察者事务中止
     */
    private void notifyAbort(Exception cause) {
        for (TransactionObserver observer : observers) {
            try {
                observer.onAbort(this, cause);
            } catch (Exception e) {
                log.error("Observer notification failed", e);
            }
        }
    }
    
    /**
     * 获取事务持续时间
     */
    public long getDuration() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * 检查事务是否已完成
     */
    public boolean isCompleted() {
        return status == Status.COMMITTED || status == Status.ABORTED;
    }
    
    /**
     * 检查事务是否成功
     */
    public boolean isSuccessful() {
        return status == Status.COMMITTED;
    }
    
    /**
     * 获取操作数量
     */
    public int getOperationCount() {
        return operations.size();
    }
    
    /**
     * 获取操作副本
     */
    public List<Operation> getOperations() {
        return new ArrayList<>(operations);
    }
    
    /**
     * 清空操作（仅在事务活跃时）
     */
    public void clearOperations() {
        checkActive();
        operations.clear();
        changedTypes.clear();
        operationCounter.set(0);
    }
    
    /**
     * 创建事务快照
     */
    public TransactionSnapshot createSnapshot() {
        return new TransactionSnapshot(
            id, status, timestamp, new ArrayList<>(operations),
            new HashSet<>(changedTypes), new HashMap<>(metadata)
        );
    }
    
    /**
     * 事务快照类
     */
    @Data
    public static class TransactionSnapshot {
        private final String transactionId;
        private final Status status;
        private final long timestamp;
        private final List<Operation> operations;
        private final Set<String> changedTypes;
        private final Map<String, Object> metadata;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction that = (Transaction) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id='%s', status=%s, operations=%d, origin='%s', duration=%dms}", 
                           id, status, operations.size(), origin, getDuration());
    }
}