package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YMap CRDT类型 - 支持协同键值对编辑
 * 
 * 完全对应YJS的YMap实现
 * 支持并发的键值对操作
 */
@Slf4j
public class YMap extends YType {
    
    private final Map<String, Object> map;
    private final Map<String, Item> keyItems; // 跟踪每个键对应的Item
    
    public YMap(YDoc doc, String name) {
        super(doc, name);
        this.map = new ConcurrentHashMap<>();
        this.keyItems = new ConcurrentHashMap<>();
    }
    
    /**
     * 设置键值对
     */
    public void set(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        transact("set", () -> {
            Transaction transaction = createTransaction("set");
            
            // 如果键已存在，先删除旧值
            if (map.containsKey(key)) {
                deleteKey(key, transaction);
            }
            
            // 插入新值
            insertKeyValue(key, value, transaction);
        });
    }
    
    /**
     * 获取值
     */
    public Object get(String key) {
        return map.get(key);
    }
    
    /**
     * 获取字符串值
     */
    public String getString(String key) {
        Object value = get(key);
        return value instanceof String ? (String) value : null;
    }
    
    /**
     * 获取数字值
     */
    public Number getNumber(String key) {
        Object value = get(key);
        return value instanceof Number ? (Number) value : null;
    }
    
    /**
     * 获取布尔值
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    /**
     * 获取嵌套的YMap
     */
    public YMap getYMap(String key) {
        Object value = get(key);
        return value instanceof YMap ? (YMap) value : null;
    }
    
    /**
     * 获取嵌套的YArray
     */
    public YArray getYArray(String key) {
        Object value = get(key);
        return value instanceof YArray ? (YArray) value : null;
    }
    
    /**
     * 获取嵌套的YText
     */
    public YText getYText(String key) {
        Object value = get(key);
        return value instanceof YText ? (YText) value : null;
    }
    
    /**
     * 删除键
     */
    public boolean delete(String key) {
        if (!map.containsKey(key)) {
            return false;
        }
        
        transact("delete", () -> {
            Transaction transaction = createTransaction("delete");
            deleteKey(key, transaction);
        });
        
        return true;
    }
    
    /**
     * 检查是否包含键
     */
    public boolean has(String key) {
        return map.containsKey(key);
    }
    
    /**
     * 获取所有键
     */
    public Set<String> keys() {
        return new HashSet<>(map.keySet());
    }
    
    /**
     * 获取所有值
     */
    public Collection<Object> values() {
        return new ArrayList<>(map.values());
    }
    
    /**
     * 获取所有键值对
     */
    public Set<Map.Entry<String, Object>> entries() {
        return new HashSet<>(map.entrySet());
    }
    
    /**
     * 清空Map
     */
    public void clear() {
        transact("clear", () -> {
            Transaction transaction = createTransaction("clear");
            
            for (String key : new ArrayList<>(map.keySet())) {
                deleteKey(key, transaction);
            }
        });
    }
    
    /**
     * 获取Map大小
     */
    public int size() {
        return map.size();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    /**
     * 插入键值对
     */
    private void insertKeyValue(String key, Object value, Transaction transaction) {
        String clientId = doc.generateClientId();
        long clock = doc.getNextClock(clientId);
        
        Operation insertOp = Operation.createInsert(
            clientId, clock, "ymap", doc.getGuid(),
            key, 0, value, transaction.getOrigin()
        );
        
        transaction.addOperation(insertOp);
        
        // 更新本地状态
        map.put(key, value);
        
        // 创建Item
        Item item = new Item(
            insertOp.getId(),
            clientId, clock, this, key, value
        );
        items.add(item);
        keyItems.put(key, item);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, 0, Map.of(key, value), 1)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Inserted key-value: key='{}', value='{}'", key, value);
    }
    
    /**
     * 删除键
     */
    private void deleteKey(String key, Transaction transaction) {
        if (!map.containsKey(key)) {
            return;
        }
        
        Object oldValue = map.get(key);
        
        String clientId = doc.generateClientId();
        long clock = doc.getNextClock(clientId);
        
        Operation deleteOp = Operation.createDelete(
            clientId, clock, "ymap", doc.getGuid(),
            key, 0, 1, transaction.getOrigin()
        );
        
        transaction.addOperation(deleteOp);
        
        // 更新本地状态
        map.remove(key);
        
        // 标记Item为删除
        Item item = keyItems.get(key);
        if (item != null) {
            item.setDeleted(true);
            keyItems.remove(key);
        }
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.DELETE, 0, Map.of(key, oldValue), 1)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Deleted key: key='{}', oldValue='{}'", key, oldValue);
    }
    
    /**
     * 应用操作到Map类型
     */
    @Override
    public void applyOperation(Operation operation, Transaction transaction) {
        switch (operation.getType()) {
            case INSERT:
                applyInsertOperation(operation, transaction);
                break;
            case DELETE:
                applyDeleteOperation(operation, transaction);
                break;
            default:
                log.warn("Unsupported operation type for YMap: {}", operation.getType());
        }
    }
    
    /**
     * 应用插入操作
     */
    private void applyInsertOperation(Operation operation, Transaction transaction) {
        Object key = operation.getKey();
        Object content = operation.getContent();
        
        if (!(key instanceof String)) {
            log.warn("Invalid key type for YMap: {}", key.getClass());
            return;
        }
        
        String mapKey = (String) key;
        
        // 解决并发冲突
        if (map.containsKey(mapKey)) {
            Item existingItem = keyItems.get(mapKey);
            if (existingItem != null && shouldReplaceValue(existingItem, operation)) {
                // 替换现有值
                map.put(mapKey, content);
                existingItem.setDeleted(true);
                
                // 创建新Item
                Item newItem = new Item(
                    operation.getId(),
                    operation.getClientId(),
                    operation.getClock(),
                    this, mapKey, content
                );
                items.add(newItem);
                keyItems.put(mapKey, newItem);
            } else {
                // 忽略这个操作
                return;
            }
        } else {
            // 新键，直接插入
            map.put(mapKey, content);
            
            Item item = new Item(
                operation.getId(),
                operation.getClientId(),
                operation.getClock(),
                this, mapKey, content
            );
            items.add(item);
            keyItems.put(mapKey, item);
        }
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, 0, Map.of(mapKey, content), 1)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied insert operation: key='{}', content='{}'", mapKey, content);
    }
    
    /**
     * 应用删除操作
     */
    private void applyDeleteOperation(Operation operation, Transaction transaction) {
        Object key = operation.getKey();
        
        if (!(key instanceof String)) {
            log.warn("Invalid key type for YMap delete: {}", key.getClass());
            return;
        }
        
        String mapKey = (String) key;
        
        if (!map.containsKey(mapKey)) {
            return; // 键不存在，忽略删除操作
        }
        
        Object oldValue = map.get(mapKey);
        
        // 删除键值对
        map.remove(mapKey);
        
        // 标记Item为删除
        Item item = keyItems.get(mapKey);
        if (item != null) {
            item.setDeleted(true);
            keyItems.remove(mapKey);
        }
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.DELETE, 0, Map.of(mapKey, oldValue), 1)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied delete operation: key='{}', oldValue='{}'", mapKey, oldValue);
    }
    
    /**
     * 判断是否应该替换现有值（CRDT冲突解决）
     */
    private boolean shouldReplaceValue(Item existingItem, Operation newOperation) {
        // 使用时间戳和客户端ID进行冲突解决
        if (newOperation.getClock() > existingItem.getClock()) {
            return true;
        } else if (newOperation.getClock() == existingItem.getClock()) {
            return newOperation.getClientId().compareTo(existingItem.getClientId()) > 0;
        }
        return false;
    }
    
    /**
     * 编码更新数据
     */
    @Override
    public void encodeUpdate(UpdateEncoder encoder, StateVector targetStateVector) {
        for (Item item : items) {
            if (!item.isDeleted() && !targetStateVector.contains(item.getClientId(), item.getClock())) {
                Operation operation = reconstructOperation(item);
                encoder.writeOperation(operation);
            }
        }
    }
    
    /**
     * 从Item重构Operation
     */
    private Operation reconstructOperation(Item item) {
        if (item.isDeleted()) {
            return Operation.createDelete(
                item.getClientId(), item.getClock(), "ymap",
                doc.getGuid(), item.getParentKey(), 0, 1, "sync"
            );
        } else {
            return Operation.createInsert(
                item.getClientId(), item.getClock(), "ymap",
                doc.getGuid(), item.getParentKey(), 0, item.getContent(), "sync"
            );
        }
    }
    
    @Override
    public YType clone() {
        YMap cloned = new YMap(doc, name);
        cloned.map.putAll(map);
        // 深度复制items和keyItems
        for (Item item : items) {
            cloned.items.add(item);
        }
        cloned.keyItems.putAll(keyItems);
        cloned.length = length;
        return cloned;
    }
    
    @Override
    public Object toJSON() {
        Map<String, Object> json = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof YType) {
                json.put(entry.getKey(), ((YType) value).toJSON());
            } else {
                json.put(entry.getKey(), value);
            }
        }
        return json;
    }
    
    /**
     * 获取增量变更
     */
    public List<YTypeDelta> toDelta() {
        if (map.isEmpty()) {
            return new ArrayList<>();
        }
        
        return List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, 0, new HashMap<>(map), map.size())
        );
    }
    
    /**
     * 从另一个YMap合并数据
     */
    public void merge(YMap other) {
        transact("merge", () -> {
            Transaction transaction = createTransaction("merge");
            
            for (Map.Entry<String, Object> entry : other.entries()) {
                insertKeyValue(entry.getKey(), entry.getValue(), transaction);
            }
        });
    }
    
    /**
     * 转换为普通Map
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(map);
    }
    
    @Override
    public String toString() {
        return String.format("YMap{size=%d, keys=%s}", map.size(), map.keySet());
    }
}