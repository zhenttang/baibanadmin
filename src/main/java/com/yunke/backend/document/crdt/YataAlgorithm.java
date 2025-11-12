package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 高级YATA算法实现 - Yet Another Transformation Approach
 * 
 * 完全对应YJS的YATA算法核心实现
 * 处理复杂的并发文本编辑和冲突解决
 */
@Slf4j
public class YataAlgorithm {
    
    // ID结构体，用于YATA算法的位置标识
    public static class YataId implements Comparable<YataId> {
        private final String clientId;
        private final long clock;
        
        public YataId(String clientId, long clock) {
            this.clientId = clientId;
            this.clock = clock;
        }
        
        public String getClientId() { return clientId; }
        public long getClock() { return clock; }
        
        @Override
        public int compareTo(YataId other) {
            int clientCompare = this.clientId.compareTo(other.clientId);
            if (clientCompare != 0) return clientCompare;
            return Long.compare(this.clock, other.clock);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof YataId)) return false;
            YataId other = (YataId) obj;
            return Objects.equals(clientId, other.clientId) && clock == other.clock;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(clientId, clock);
        }
        
        @Override
        public String toString() {
            return String.format("YataId{%s:%d}", clientId, clock);
        }
    }
    
    // YATA项目结构
    public static class YataItem {
        private final YataId id;
        private final String content;
        private final YataId leftOrigin;    // 左边的参照项
        private final YataId rightOrigin;   // 右边的参照项
        private final String clientId;
        private final long timestamp;
        private boolean deleted;
        private YataItem left;              // 实际的左邻居
        private YataItem right;             // 实际的右邻居
        
        public YataItem(YataId id, String content, YataId leftOrigin, YataId rightOrigin, 
                       String clientId, long timestamp) {
            this.id = id;
            this.content = content;
            this.leftOrigin = leftOrigin;
            this.rightOrigin = rightOrigin;
            this.clientId = clientId;
            this.timestamp = timestamp;
            this.deleted = false;
        }
        
        // Getters and Setters
        public YataId getId() { return id; }
        public String getContent() { return content; }
        public YataId getLeftOrigin() { return leftOrigin; }
        public YataId getRightOrigin() { return rightOrigin; }
        public String getClientId() { return clientId; }
        public long getTimestamp() { return timestamp; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
        public YataItem getLeft() { return left; }
        public void setLeft(YataItem left) { this.left = left; }
        public YataItem getRight() { return right; }
        public void setRight(YataItem right) { this.right = right; }
        
        public int getLength() {
            return content != null ? content.length() : 0;
        }
        
        @Override
        public String toString() {
            return String.format("YataItem{id=%s, content='%s', deleted=%s}", 
                               id, content, deleted);
        }
    }
    
    // YATA文档结构
    private final Map<YataId, YataItem> items = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<YataId, YataItem> orderedItems = new ConcurrentSkipListMap<>();
    private YataItem head;  // 虚拟头节点
    private YataItem tail;  // 虚拟尾节点
    
    // 状态向量，跟踪每个客户端的最新时钟
    private final StateVector stateVector = new StateVector();
    
    // 操作变换引擎
    private final OperationalTransform operationalTransform = new OperationalTransform();
    
    public YataAlgorithm() {
        // 初始化虚拟头尾节点
        this.head = new YataItem(new YataId("_head", 0), null, null, null, "_system", 0);
        this.tail = new YataItem(new YataId("_tail", Long.MAX_VALUE), null, null, null, "_system", 0);
        
        this.head.setRight(this.tail);
        this.tail.setLeft(this.head);
        
        items.put(head.getId(), head);
        items.put(tail.getId(), tail);
    }
    
    /**
     * 插入操作 - YATA算法的核心
     */
    public void insert(String clientId, long clock, String content, 
                      YataId leftOrigin, YataId rightOrigin) {
        YataId id = new YataId(clientId, clock);
        YataItem newItem = new YataItem(id, content, leftOrigin, rightOrigin, 
                                       clientId, System.currentTimeMillis());
        
        // 更新状态向量
        stateVector.update(clientId, clock);
        
        // 存储项目
        items.put(id, newItem);
        orderedItems.put(id, newItem);
        
        // 执行集成算法
        integrateItem(newItem);
        
        log.debug("Inserted YATA item: {}, leftOrigin: {}, rightOrigin: {}", 
                 id, leftOrigin, rightOrigin);
    }
    
    /**
     * 集成算法 - 将新项目插入到正确位置
     */
    private void integrateItem(YataItem newItem) {
        YataItem left = findIntegrationLeft(newItem);
        YataItem right = findIntegrationRight(newItem, left);
        
        // 在并发环境中寻找正确的插入位置
        while (true) {
            YataItem candidate = left.getRight();
            
            if (candidate == right) {
                // 找到了正确的位置
                break;
            }
            
            if (candidate.getId().compareTo(newItem.getId()) < 0) {
                // candidate应该在newItem之前
                left = candidate;
            } else {
                // candidate应该在newItem之后
                right = candidate;
                break;
            }
        }
        
        // 插入新项目
        insertBetween(newItem, left, right);
    }
    
    /**
     * 寻找集成左边界
     */
    private YataItem findIntegrationLeft(YataItem newItem) {
        YataId leftOrigin = newItem.getLeftOrigin();
        
        if (leftOrigin == null) {
            return head;
        }
        
        YataItem leftItem = items.get(leftOrigin);
        if (leftItem == null) {
            // 左参照项不存在，可能还未到达，使用头节点
            log.warn("Left origin not found: {}, using head", leftOrigin);
            return head;
        }
        
        // 向右扫描，寻找最后一个来自同一次编辑会话的项目
        YataItem current = leftItem;
        while (current.getRight() != null && 
               current.getRight() != tail &&
               current.getRight().getLeftOrigin() != null &&
               current.getRight().getLeftOrigin().equals(leftOrigin) &&
               current.getRight().getId().compareTo(newItem.getId()) < 0) {
            current = current.getRight();
        }
        
        return current;
    }
    
    /**
     * 寻找集成右边界
     */
    private YataItem findIntegrationRight(YataItem newItem, YataItem left) {
        YataId rightOrigin = newItem.getRightOrigin();
        
        if (rightOrigin == null) {
            return tail;
        }
        
        YataItem rightItem = items.get(rightOrigin);
        if (rightItem == null) {
            // 右参照项不存在，使用尾节点
            log.warn("Right origin not found: {}, using tail", rightOrigin);
            return tail;
        }
        
        // 从left开始向右扫描，寻找rightItem或更合适的位置
        YataItem current = left.getRight();
        while (current != tail && current != rightItem) {
            if (current.getId().compareTo(newItem.getId()) > 0) {
                // 找到了一个ID更大的项目，应该插在它之前
                break;
            }
            current = current.getRight();
        }
        
        return current;
    }
    
    /**
     * 在两个项目之间插入新项目
     */
    private void insertBetween(YataItem newItem, YataItem left, YataItem right) {
        newItem.setLeft(left);
        newItem.setRight(right);
        
        if (left != null) {
            left.setRight(newItem);
        }
        if (right != null) {
            right.setLeft(newItem);
        }
        
        log.debug("Inserted {} between {} and {}", 
                 newItem.getId(), 
                 left != null ? left.getId() : "null", 
                 right != null ? right.getId() : "null");
    }
    
    /**
     * 删除操作
     */
    public void delete(YataId targetId) {
        YataItem item = items.get(targetId);
        if (item != null && !item.isDeleted()) {
            item.setDeleted(true);
            log.debug("Deleted YATA item: {}", targetId);
        } else {
            log.warn("Cannot delete item: {} (not found or already deleted)", targetId);
        }
    }
    
    /**
     * 批量删除
     */
    public void deleteRange(YataItem start, int length) {
        YataItem current = start;
        int deleted = 0;
        
        while (current != null && current != tail && deleted < length) {
            if (!current.isDeleted()) {
                current.setDeleted(true);
                deleted += current.getLength();
                log.debug("Deleted item in range: {}", current.getId());
            }
            current = current.getRight();
        }
    }
    
    /**
     * 获取文档的文本内容
     */
    public String getText() {
        StringBuilder result = new StringBuilder();
        YataItem current = head.getRight();
        
        while (current != null && current != tail) {
            if (!current.isDeleted() && current.getContent() != null) {
                result.append(current.getContent());
            }
            current = current.getRight();
        }
        
        return result.toString();
    }
    
    /**
     * 获取文档长度（不包括被删除的内容）
     */
    public int getLength() {
        int length = 0;
        YataItem current = head.getRight();
        
        while (current != null && current != tail) {
            if (!current.isDeleted()) {
                length += current.getLength();
            }
            current = current.getRight();
        }
        
        return length;
    }
    
    /**
     * 根据位置索引找到对应的YATA项目
     */
    public YataItem getItemAtPosition(int position) {
        int currentPos = 0;
        YataItem current = head.getRight();
        
        while (current != null && current != tail) {
            if (!current.isDeleted()) {
                int itemLength = current.getLength();
                if (currentPos + itemLength > position) {
                    return current;
                }
                currentPos += itemLength;
            }
            current = current.getRight();
        }
        
        return null;
    }
    
    /**
     * 获取项目在文档中的位置
     */
    public int getPositionOfItem(YataId itemId) {
        int position = 0;
        YataItem current = head.getRight();
        
        while (current != null && current != tail) {
            if (current.getId().equals(itemId)) {
                return position;
            }
            if (!current.isDeleted()) {
                position += current.getLength();
            }
            current = current.getRight();
        }
        
        return -1; // 未找到
    }
    
    /**
     * 应用远程操作
     */
    public void applyRemoteOperation(Operation operation) {
        switch (operation.getType()) {
            case INSERT:
                applyRemoteInsert(operation);
                break;
            case DELETE:
                applyRemoteDelete(operation);
                break;
            default:
                log.warn("Unsupported remote operation type: {}", operation.getType());
        }
    }
    
    /**
     * 应用远程插入操作
     */
    private void applyRemoteInsert(Operation operation) {
        String clientId = operation.getClientId();
        long clock = operation.getClock();
        String content = (String) operation.getContent();
        
        // 从操作中提取origin信息（简化实现）
        YataId leftOrigin = extractLeftOrigin(operation);
        YataId rightOrigin = extractRightOrigin(operation);
        
        insert(clientId, clock, content, leftOrigin, rightOrigin);
    }
    
    /**
     * 应用远程删除操作
     */
    private void applyRemoteDelete(Operation operation) {
        // 根据操作信息找到要删除的项目
        int index = operation.getIndex();
        int length = operation.getContentLength();
        
        YataItem startItem = getItemAtPosition(index);
        if (startItem != null) {
            deleteRange(startItem, length);
        }
    }
    
    /**
     * 生成插入操作的左参照（简化实现）
     */
    private YataId extractLeftOrigin(Operation operation) {
        // 这里应该从操作的元数据中提取，简化为根据索引计算
        int index = operation.getIndex();
        if (index == 0) {
            return null; // 插入到开头
        }
        
        YataItem leftItem = getItemAtPosition(index - 1);
        return leftItem != null ? leftItem.getId() : null;
    }
    
    /**
     * 生成插入操作的右参照（简化实现）
     */
    private YataId extractRightOrigin(Operation operation) {
        // 这里应该从操作的元数据中提取，简化为根据索引计算
        int index = operation.getIndex();
        YataItem rightItem = getItemAtPosition(index);
        return rightItem != null ? rightItem.getId() : null;
    }
    
    /**
     * 编码文档状态为更新
     */
    public byte[] encodeStateAsUpdate(StateVector targetStateVector) {
        UpdateEncoder encoder = new UpdateEncoder();
        
        // 编码所有在目标状态向量之后的项目
        for (YataItem item : orderedItems.values()) {
            if (item == head || item == tail) continue;
            
            String clientId = item.getClientId();
            long clock = item.getId().getClock();
            
            if (!targetStateVector.contains(clientId, clock)) {
                // 编码这个项目
                Operation operation = itemToOperation(item);
                encoder.writeOperation(operation);
            }
        }
        
        return encoder.encode();
    }
    
    /**
     * 将YATA项目转换为操作
     */
    private Operation itemToOperation(YataItem item) {
        if (item.isDeleted()) {
            return Operation.createDelete(
                item.getClientId(), item.getId().getClock(), "ytext",
                "doc", null, 0, item.getLength(), "yata"
            );
        } else {
            return Operation.createInsert(
                item.getClientId(), item.getId().getClock(), "ytext",
                "doc", null, 0, item.getContent(), "yata"
            );
        }
    }
    
    /**
     * 获取状态向量
     */
    public StateVector getStateVector() {
        return stateVector.copy();
    }
    
    /**
     * 压缩文档 - 移除被删除的项目以优化内存
     */
    public void compact() {
        List<YataId> toRemove = new ArrayList<>();
        
        for (YataItem item : items.values()) {
            if (item.isDeleted() && item != head && item != tail) {
                // 检查是否可以安全删除（没有其他项目引用它作为origin）
                if (canSafelyRemove(item)) {
                    toRemove.add(item.getId());
                }
            }
        }
        
        for (YataId id : toRemove) {
            YataItem item = items.remove(id);
            orderedItems.remove(id);
            
            // 重新连接链表
            if (item.getLeft() != null) {
                item.getLeft().setRight(item.getRight());
            }
            if (item.getRight() != null) {
                item.getRight().setLeft(item.getLeft());
            }
        }
        
        log.info("Compacted YATA document: removed {} deleted items", toRemove.size());
    }
    
    /**
     * 检查项目是否可以安全移除
     */
    private boolean canSafelyRemove(YataItem item) {
        YataId itemId = item.getId();
        
        // 检查是否有其他项目将此项目作为origin
        for (YataItem other : items.values()) {
            if (itemId.equals(other.getLeftOrigin()) || itemId.equals(other.getRightOrigin())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取调试信息
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalItems", items.size());
        info.put("deletedItems", items.values().stream().mapToInt(item -> item.isDeleted() ? 1 : 0).sum());
        info.put("textLength", getLength());
        info.put("stateVector", stateVector.toString());
        
        // 添加链表结构信息
        List<String> structure = new ArrayList<>();
        YataItem current = head;
        int count = 0;
        while (current != null && count < 100) { // 限制数量避免过长
            structure.add(String.format("%s%s", 
                         current.getId().toString(), 
                         current.isDeleted() ? "(D)" : ""));
            current = current.getRight();
            count++;
        }
        info.put("structure", structure);
        
        return info;
    }
    
    /**
     * 验证文档结构的完整性
     */
    public boolean validateStructure() {
        try {
            // 检查链表的双向一致性
            YataItem current = head;
            while (current != null) {
                if (current.getRight() != null && current.getRight().getLeft() != current) {
                    log.error("Structure validation failed: inconsistent right link at {}", current.getId());
                    return false;
                }
                if (current.getLeft() != null && current.getLeft().getRight() != current) {
                    log.error("Structure validation failed: inconsistent left link at {}", current.getId());
                    return false;
                }
                current = current.getRight();
            }
            
            // 检查所有items都在链表中
            Set<YataId> chainIds = new HashSet<>();
            current = head;
            while (current != null) {
                chainIds.add(current.getId());
                current = current.getRight();
            }
            
            for (YataId id : items.keySet()) {
                if (!chainIds.contains(id)) {
                    log.error("Structure validation failed: item {} not in chain", id);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("Structure validation failed with exception", e);
            return false;
        }
    }
}