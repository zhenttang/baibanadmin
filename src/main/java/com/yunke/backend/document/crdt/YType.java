package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CRDT类型基类 - 所有YJS类型的抽象基类
 * 
 * 完全对应YJS的AbstractType实现
 * 提供CRDT的核心功能和观察者模式
 */
@Slf4j
public abstract class YType {
    
    protected final YDoc doc;
    protected final String name;
    protected final List<YTypeObserver> observers;
    protected YType parent;
    protected Object parentKey;
    
    // CRDT相关属性
    protected final List<Item> items;
    protected boolean deleted = false;
    protected long length = 0;
    
    /**
     * 观察者接口
     */
    public interface YTypeObserver {
        void onChange(YTypeEvent event);
    }
    
    /**
     * 类型事件
     */
    public static class YTypeEvent {
        private final YType target;
        private final List<YTypeDelta> changes;
        private final Transaction transaction;
        
        public YTypeEvent(YType target, List<YTypeDelta> changes, Transaction transaction) {
            this.target = target;
            this.changes = changes;
            this.transaction = transaction;
        }
        
        // Getters
        public YType getTarget() { return target; }
        public List<YTypeDelta> getChanges() { return changes; }
        public Transaction getTransaction() { return transaction; }
    }
    
    /**
     * 变更增量
     */
    public static class YTypeDelta {
        public enum Type { INSERT, DELETE, RETAIN }
        
        private final Type type;
        private final int index;
        private final Object content;
        private final int length;
        
        public YTypeDelta(Type type, int index, Object content, int length) {
            this.type = type;
            this.index = index;
            this.content = content;
            this.length = length;
        }
        
        // Getters
        public Type getType() { return type; }
        public int getIndex() { return index; }
        public Object getContent() { return content; }
        public int getLength() { return length; }
    }
    
    /**
     * CRDT项目
     */
    protected static class Item {
        private final String id;
        private final String clientId;
        private final long clock;
        private final YType parent;
        private final Object parentKey;
        private final Object content;
        private boolean deleted = false;
        
        public Item(String id, String clientId, long clock, YType parent, 
                   Object parentKey, Object content) {
            this.id = id;
            this.clientId = clientId;
            this.clock = clock;
            this.parent = parent;
            this.parentKey = parentKey;
            this.content = content;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getClientId() { return clientId; }
        public long getClock() { return clock; }
        public YType getParent() { return parent; }
        public Object getParentKey() { return parentKey; }
        public Object getContent() { return content; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
        
        public int getLength() {
            if (content instanceof String) {
                return ((String) content).length();
            }
            return 1;
        }
    }
    
    protected YType(YDoc doc, String name) {
        this.doc = doc;
        this.name = name;
        this.observers = new CopyOnWriteArrayList<>();
        this.items = new ArrayList<>();
    }
    
    /**
     * 添加观察者
     */
    public void observe(YTypeObserver observer) {
        observers.add(observer);
    }
    
    /**
     * 移除观察者
     */
    public void unobserve(YTypeObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * 通知观察者
     */
    protected void notifyObservers(List<YTypeDelta> changes, Transaction transaction) {
        if (observers.isEmpty()) {
            return;
        }
        
        YTypeEvent event = new YTypeEvent(this, changes, transaction);
        for (YTypeObserver observer : observers) {
            try {
                observer.onChange(event);
            } catch (Exception e) {
                log.error("Observer notification failed", e);
            }
        }
    }
    
    /**
     * 应用操作到类型
     */
    public abstract void applyOperation(Operation operation, Transaction transaction);
    
    /**
     * 编码更新数据
     */
    public abstract void encodeUpdate(UpdateEncoder encoder, StateVector targetStateVector);
    
    /**
     * 获取类型大小
     */
    public abstract int size();
    
    /**
     * 克隆类型
     */
    public abstract YType clone();
    
    /**
     * 转换为JSON
     */
    public abstract Object toJSON();
    
    /**
     * 获取内容长度
     */
    public long getLength() {
        return length;
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return length == 0;
    }
    
    /**
     * 检查是否已删除
     */
    public boolean isDeleted() {
        return deleted;
    }
    
    /**
     * 设置删除状态
     */
    protected void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    /**
     * 获取父类型
     */
    public YType getParent() {
        return parent;
    }
    
    /**
     * 设置父类型
     */
    protected void setParent(YType parent, Object key) {
        this.parent = parent;
        this.parentKey = key;
    }
    
    /**
     * 获取在父对象中的键
     */
    public Object getParentKey() {
        return parentKey;
    }
    
    /**
     * 获取类型名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取文档引用
     */
    public YDoc getDoc() {
        return doc;
    }
    
    /**
     * 创建事务
     */
    protected Transaction createTransaction(String origin) {
        return new Transaction(doc, origin);
    }
    
    /**
     * 在事务中执行操作
     */
    protected void transact(String origin, Runnable action) {
        Transaction transaction = createTransaction(origin);
        try {
            action.run();
            transaction.commit();
        } catch (Exception e) {
            transaction.abort(e);
            throw e;
        }
    }
    
    /**
     * 插入内容到指定位置
     */
    protected void insertAt(int index, Object content, Transaction transaction) {
        String clientId = doc.generateClientId();
        long clock = doc.getNextClock(clientId);
        
        Operation insertOp = Operation.createInsert(
            clientId, clock, this.getClass().getSimpleName().toLowerCase(),
            doc.getGuid(), index, index, content, transaction.getOrigin()
        );
        
        transaction.addOperation(insertOp);
        
        // 创建Item并添加到列表
        Item item = new Item(insertOp.getId(), clientId, clock, this, index, content);
        items.add(Math.min(index, items.size()), item);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, index, content, getContentLength(content))
        );
        notifyObservers(changes, transaction);
    }
    
    /**
     * 删除指定范围的内容
     */
    protected void deleteRange(int index, int length, Transaction transaction) {
        if (length <= 0 || index < 0 || index >= this.length) {
            return;
        }
        
        String clientId = doc.generateClientId();
        long clock = doc.getNextClock(clientId);
        
        Operation deleteOp = Operation.createDelete(
            clientId, clock, this.getClass().getSimpleName().toLowerCase(),
            doc.getGuid(), null, index, length, transaction.getOrigin()
        );
        
        transaction.addOperation(deleteOp);
        
        // 标记对应的Items为删除
        markItemsDeleted(index, length);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.DELETE, index, null, length)
        );
        notifyObservers(changes, transaction);
    }
    
    /**
     * 标记Items为删除状态
     */
    protected void markItemsDeleted(int startIndex, int length) {
        int currentIndex = 0;
        int remaining = length;
        
        for (Item item : items) {
            if (remaining <= 0) break;
            
            if (currentIndex >= startIndex && !item.isDeleted()) {
                item.setDeleted(true);
                remaining--;
            }
            currentIndex++;
        }
    }
    
    /**
     * 更新长度计算
     */
    protected void updateLength() {
        this.length = items.stream()
            .filter(item -> !item.isDeleted())
            .mapToInt(Item::getLength)
            .sum();
    }
    
    /**
     * 获取内容长度
     */
    private int getContentLength(Object content) {
        if (content instanceof String) {
            return ((String) content).length();
        }
        return 1;
    }
    
    /**
     * 查找指定位置的Item
     */
    protected Item findItemAt(int index) {
        int currentIndex = 0;
        for (Item item : items) {
            if (!item.isDeleted()) {
                if (currentIndex == index) {
                    return item;
                }
                currentIndex++;
            }
        }
        return null;
    }
    
    /**
     * 获取有效Items（未删除）
     */
    protected List<Item> getValidItems() {
        return items.stream()
            .filter(item -> !item.isDeleted())
            .toList();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        YType that = (YType) obj;
        return Objects.equals(name, that.name) && Objects.equals(doc, that.doc);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, doc);
    }
    
    @Override
    public String toString() {
        return String.format("%s{name='%s', length=%d, items=%d}", 
                           getClass().getSimpleName(), name, length, items.size());
    }
}