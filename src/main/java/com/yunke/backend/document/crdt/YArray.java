package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * YArray CRDT类型 - 支持协同数组编辑
 * 
 * 完全对应YJS的YArray实现
 * 支持并发的数组操作
 */
@Slf4j
public class YArray extends YType {
    
    private final List<Object> array;
    
    public YArray(YDoc doc, String name) {
        super(doc, name);
        this.array = new CopyOnWriteArrayList<>();
    }
    
    /**
     * 在指定位置插入元素
     */
    public void insert(int index, Object... elements) {
        if (elements.length == 0) {
            return;
        }
        
        transact("insert", () -> {
            Transaction transaction = createTransaction("insert");
            
            for (int i = 0; i < elements.length; i++) {
                insertAt(index + i, elements[i], transaction);
                
                // 更新本地数组
                if (index + i <= array.size()) {
                    array.add(index + i, elements[i]);
                }
            }
        });
    }
    
    /**
     * 在数组末尾添加元素
     */
    public void push(Object... elements) {
        insert(array.size(), elements);
    }
    
    /**
     * 在数组开头添加元素
     */
    public void unshift(Object... elements) {
        insert(0, elements);
    }
    
    /**
     * 删除指定位置的元素
     */
    public void delete(int index, int length) {
        if (length <= 0 || index < 0 || index >= array.size()) {
            return;
        }
        
        transact("delete", () -> {
            Transaction transaction = createTransaction("delete");
            deleteRange(index, length, transaction);
            
            // 更新本地数组
            int endIndex = Math.min(index + length, array.size());
            for (int i = endIndex - 1; i >= index; i--) {
                if (i < array.size()) {
                    array.remove(i);
                }
            }
        });
    }
    
    /**
     * 删除指定位置的单个元素
     */
    public Object delete(int index) {
        if (index < 0 || index >= array.size()) {
            return null;
        }
        
        Object removed = array.get(index);
        delete(index, 1);
        return removed;
    }
    
    /**
     * 获取指定位置的元素
     */
    public Object get(int index) {
        if (index < 0 || index >= array.size()) {
            return null;
        }
        return array.get(index);
    }
    
    /**
     * 获取字符串元素
     */
    public String getString(int index) {
        Object value = get(index);
        return value instanceof String ? (String) value : null;
    }
    
    /**
     * 获取数字元素
     */
    public Number getNumber(int index) {
        Object value = get(index);
        return value instanceof Number ? (Number) value : null;
    }
    
    /**
     * 获取布尔元素
     */
    public Boolean getBoolean(int index) {
        Object value = get(index);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    /**
     * 获取嵌套的YMap
     */
    public YMap getYMap(int index) {
        Object value = get(index);
        return value instanceof YMap ? (YMap) value : null;
    }
    
    /**
     * 获取嵌套的YArray
     */
    public YArray getYArray(int index) {
        Object value = get(index);
        return value instanceof YArray ? (YArray) value : null;
    }
    
    /**
     * 获取嵌套的YText
     */
    public YText getYText(int index) {
        Object value = get(index);
        return value instanceof YText ? (YText) value : null;
    }
    
    /**
     * 设置指定位置的元素
     */
    public void set(int index, Object value) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative");
        }
        
        // 如果索引超出范围，需要先扩展数组
        if (index >= array.size()) {
            // 填充null直到目标索引
            for (int i = array.size(); i <= index; i++) {
                push((Object) null);
            }
        }
        
        transact("set", () -> {
            Transaction transaction = createTransaction("set");
            
            // 先删除现有元素
            if (index < array.size()) {
                deleteRange(index, 1, transaction);
                array.remove(index);
            }
            
            // 插入新元素
            insertAt(index, value, transaction);
            array.add(index, value);
        });
    }
    
    /**
     * 获取数组长度
     */
    public int length() {
        return array.size();
    }
    
    /**
     * 检查数组是否为空
     */
    public boolean isEmpty() {
        return array.isEmpty();
    }
    
    /**
     * 清空数组
     */
    public void clear() {
        if (array.isEmpty()) {
            return;
        }
        
        delete(0, array.size());
    }
    
    /**
     * 获取数组的切片
     */
    public List<Object> slice(int start, int end) {
        if (start < 0) start = 0;
        if (end > array.size()) end = array.size();
        if (start >= end) return new ArrayList<>();
        
        return new ArrayList<>(array.subList(start, end));
    }
    
    /**
     * 查找元素的索引
     */
    public int indexOf(Object element) {
        return array.indexOf(element);
    }
    
    /**
     * 检查是否包含指定元素
     */
    public boolean contains(Object element) {
        return array.contains(element);
    }
    
    /**
     * 转换为Java List
     */
    public List<Object> toList() {
        return new ArrayList<>(array);
    }
    
    /**
     * 转换为数组
     */
    public Object[] toArray() {
        return array.toArray();
    }
    
    /**
     * 遍历数组元素
     */
    public void forEach(java.util.function.Consumer<Object> action) {
        array.forEach(action);
    }
    
    /**
     * 应用操作到数组类型
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
                log.warn("Unsupported operation type for YArray: {}", operation.getType());
        }
    }
    
    /**
     * 应用插入操作
     */
    private void applyInsertOperation(Operation operation, Transaction transaction) {
        int index = operation.getIndex();
        Object content = operation.getContent();
        
        // 使用YATA算法解决位置冲突
        int adjustedIndex = resolveInsertPosition(index, operation);
        
        // 插入到数组中
        if (adjustedIndex <= array.size()) {
            array.add(adjustedIndex, content);
        }
        
        // 创建Item
        Item item = new Item(
            operation.getId(),
            operation.getClientId(),
            operation.getClock(),
            this, adjustedIndex, content
        );
        items.add(item);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, adjustedIndex, content, 1)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied insert operation: index={}, content='{}', adjustedIndex={}", 
                 index, content, adjustedIndex);
    }
    
    /**
     * 应用删除操作
     */
    private void applyDeleteOperation(Operation operation, Transaction transaction) {
        int index = operation.getIndex();
        int length = operation.getContentLength();
        
        // 使用YATA算法解决范围冲突
        int[] adjustedRange = resolveDeleteRange(index, length, operation);
        int adjustedIndex = adjustedRange[0];
        int adjustedLength = adjustedRange[1];
        
        // 从数组中删除
        int endIndex = Math.min(adjustedIndex + adjustedLength, array.size());
        for (int i = endIndex - 1; i >= adjustedIndex; i--) {
            if (i < array.size()) {
                array.remove(i);
            }
        }
        
        // 标记对应的Items为删除
        markItemsDeleted(adjustedIndex, adjustedLength);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.DELETE, adjustedIndex, null, adjustedLength)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied delete operation: index={}, length={}, adjustedIndex={}, adjustedLength={}", 
                 index, length, adjustedIndex, adjustedLength);
    }
    
    /**
     * YATA算法 - 解决插入位置冲突
     */
    private int resolveInsertPosition(int index, Operation operation) {
        // 简化版本的YATA算法
        List<Item> validItems = getValidItems();
        
        if (index >= validItems.size()) {
            return array.size();
        }
        
        // 查找冲突操作并排序
        List<Operation> conflictingOps = findConflictingOperations(index, operation);
        
        if (conflictingOps.isEmpty()) {
            return Math.min(index, array.size());
        }
        
        conflictingOps.sort((op1, op2) -> {
            int clockCompare = Long.compare(op1.getClock(), op2.getClock());
            if (clockCompare != 0) return clockCompare;
            return op1.getClientId().compareTo(op2.getClientId());
        });
        
        int adjustedIndex = index;
        for (Operation conflictOp : conflictingOps) {
            if (conflictOp.getClock() < operation.getClock() || 
                (conflictOp.getClock() == operation.getClock() && 
                 conflictOp.getClientId().compareTo(operation.getClientId()) < 0)) {
                adjustedIndex += conflictOp.getContentLength();
            }
        }
        
        return Math.min(adjustedIndex, array.size());
    }
    
    /**
     * YATA算法 - 解决删除范围冲突
     */
    private int[] resolveDeleteRange(int index, int length, Operation operation) {
        int adjustedIndex = Math.min(index, array.size());
        int adjustedLength = Math.min(length, array.size() - adjustedIndex);
        
        return new int[]{adjustedIndex, adjustedLength};
    }
    
    /**
     * 查找冲突的操作
     */
    private List<Operation> findConflictingOperations(int index, Operation currentOp) {
        // 简化版本 - 实际需要在文档级别维护操作历史
        return new ArrayList<>();
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
                item.getClientId(), item.getClock(), "yarray",
                doc.getGuid(), null, 0, 1, "sync"
            );
        } else {
            return Operation.createInsert(
                item.getClientId(), item.getClock(), "yarray",
                doc.getGuid(), item.getParentKey(), 0, item.getContent(), "sync"
            );
        }
    }
    
    @Override
    public int size() {
        return array.size();
    }
    
    @Override
    public YType clone() {
        YArray cloned = new YArray(doc, name);
        cloned.array.addAll(array);
        // 复制items和其他状态
        cloned.items.addAll(items);
        cloned.length = length;
        return cloned;
    }
    
    @Override
    public Object toJSON() {
        List<Object> json = new ArrayList<>();
        for (Object element : array) {
            if (element instanceof YType) {
                json.add(((YType) element).toJSON());
            } else {
                json.add(element);
            }
        }
        return json;
    }
    
    /**
     * 获取增量变更
     */
    public List<YTypeDelta> toDelta() {
        if (array.isEmpty()) {
            return new ArrayList<>();
        }
        
        return List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, 0, new ArrayList<>(array), array.size())
        );
    }
    
    /**
     * 从增量应用变更
     */
    public void applyDelta(List<YTypeDelta> deltas) {
        transact("applyDelta", () -> {
            Transaction transaction = createTransaction("applyDelta");
            
            int index = 0;
            for (YTypeDelta delta : deltas) {
                switch (delta.getType()) {
                    case INSERT:
                        if (delta.getContent() instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> elements = (List<Object>) delta.getContent();
                            for (Object element : elements) {
                                insertAt(index, element, transaction);
                                array.add(index, element);
                                index++;
                            }
                        } else {
                            insertAt(index, delta.getContent(), transaction);
                            array.add(index, delta.getContent());
                            index++;
                        }
                        break;
                    case DELETE:
                        deleteRange(index, delta.getLength(), transaction);
                        int endIndex = Math.min(index + delta.getLength(), array.size());
                        for (int i = endIndex - 1; i >= index; i--) {
                            if (i < array.size()) {
                                array.remove(i);
                            }
                        }
                        break;
                    case RETAIN:
                        index += delta.getLength();
                        break;
                }
            }
        });
    }
    
    @Override
    public String toString() {
        return String.format("YArray{length=%d, elements=%s}", array.size(), array);
    }
}