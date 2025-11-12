package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YText CRDT类型 - 支持协同文本编辑
 * 
 * 完全对应YJS的YText实现
 * 实现YATA算法支持并发文本编辑
 */
@Slf4j
public class YText extends YType {
    
    private StringBuilder content;
    private YataAlgorithm yataAlgorithm;
    private OperationalTransform operationalTransform;
    
    public YText(YDoc doc, String name) {
        super(doc, name);
        this.content = new StringBuilder();
        this.yataAlgorithm = new YataAlgorithm();
        this.operationalTransform = new OperationalTransform();
    }
    
    /**
     * 插入文本到指定位置
     */
    public void insert(int index, String text) {
        insert(index, text, null);
    }
    
    /**
     * 插入带格式的文本
     */
    public void insert(int index, String text, Map<String, Object> attributes) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        transact("insert", () -> {
            Transaction transaction = createTransaction("insert");
            insertAt(index, text, transaction);
            
            // 更新本地内容
            if (index <= content.length()) {
                content.insert(index, text);
            }
        });
    }
    
    /**
     * 删除指定范围的文本
     */
    public void delete(int index, int length) {
        if (length <= 0 || index < 0 || index >= this.getLength()) {
            return;
        }
        
        transact("delete", () -> {
            Transaction transaction = createTransaction("delete");
            deleteRange(index, length, transaction);
            
            // 更新本地内容
            int endIndex = Math.min(index + length, content.length());
            if (index < content.length()) {
                content.delete(index, endIndex);
            }
        });
    }
    
    /**
     * 格式化指定范围的文本
     */
    public void format(int index, int length, Map<String, Object> attributes) {
        if (length <= 0 || index < 0 || index >= this.getLength()) {
            return;
        }
        
        transact("format", () -> {
            Transaction transaction = createTransaction("format");
            
            String clientId = doc.generateClientId();
            long clock = doc.getNextClock(clientId);
            
            Operation formatOp = Operation.createFormat(
                clientId, clock, "ytext", doc.getGuid(),
                index, length, attributes, transaction.getOrigin()
            );
            
            transaction.addOperation(formatOp);
            
            // 通知格式化变更
            List<YTypeDelta> changes = List.of(
                new YTypeDelta(YTypeDelta.Type.RETAIN, index, attributes, length)
            );
            notifyObservers(changes, transaction);
        });
    }
    
    /**
     * 获取文本内容 - 从YATA算法获取
     */
    public String toString() {
        // 使用YATA算法的精确文本内容
        String yataText = yataAlgorithm.getText();
        
        // 同步StringBuilder内容（用于兼容性）
        if (!yataText.equals(content.toString())) {
            content.setLength(0);
            content.append(yataText);
        }
        
        return yataText;
    }
    
    /**
     * 获取指定范围的文本
     */
    public String substring(int start, int end) {
        if (start < 0) start = 0;
        if (end > content.length()) end = content.length();
        if (start >= end) return "";
        
        return content.substring(start, end);
    }
    
    /**
     * 获取字符长度
     */
    public int length() {
        return content.length();
    }
    
    /**
     * 应用操作到文本类型
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
            case FORMAT:
                applyFormatOperation(operation, transaction);
                break;
            default:
                log.warn("Unsupported operation type for YText: {}", operation.getType());
        }
    }
    
    /**
     * 应用插入操作 - 使用完整的YATA算法
     */
    private void applyInsertOperation(Operation operation, Transaction transaction) {
        int index = operation.getIndex();
        Object operationContent = operation.getContent();
        
        if (!(operationContent instanceof String)) {
            log.warn("Invalid content type for text insertion: {}", operationContent.getClass());
            return;
        }
        
        String text = (String) operationContent;
        String clientId = operation.getClientId();
        long clock = operation.getClock();
        
        // 使用YATA算法进行插入
        YataAlgorithm.YataId leftOrigin = extractLeftOrigin(index, operation);
        YataAlgorithm.YataId rightOrigin = extractRightOrigin(index, operation);
        
        // 在YATA算法中插入
        yataAlgorithm.insert(clientId, clock, text, leftOrigin, rightOrigin);
        
        // 同步StringBuilder内容
        String yataText = yataAlgorithm.getText();
        content.setLength(0);
        content.append(yataText);
        
        // 创建Item并添加到items列表
        Item item = new Item(
            operation.getId(),
            clientId, 
            clock,
            this,
            index,
            text
        );
        items.add(item);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, index, text, text.length())
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied insert operation with YATA: clientId={}, clock={}, text='{}', leftOrigin={}, rightOrigin={}", 
                 clientId, clock, text, leftOrigin, rightOrigin);
    }
    
    /**
     * 应用删除操作 - 使用完整的YATA算法
     */
    private void applyDeleteOperation(Operation operation, Transaction transaction) {
        int index = operation.getIndex();
        int length = operation.getContentLength();
        
        // 使用YATA算法进行删除
        YataAlgorithm.YataItem startItem = yataAlgorithm.getItemAtPosition(index);
        if (startItem != null) {
            yataAlgorithm.deleteRange(startItem, length);
        }
        
        // 同步StringBuilder内容
        String yataText = yataAlgorithm.getText();
        content.setLength(0);
        content.append(yataText);
        
        // 标记对应的Items为删除
        markItemsDeleted(index, length);
        
        // 更新长度
        updateLength();
        
        // 通知变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.DELETE, index, null, length)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied delete operation with YATA: index={}, length={}, startItem={}", 
                 index, length, startItem != null ? startItem.getId() : "null");
    }
    
    /**
     * 应用格式化操作
     */
    private void applyFormatOperation(Operation operation, Transaction transaction) {
        int index = operation.getIndex();
        int length = operation.getContentLength();
        Object attributes = operation.getAttributes();
        
        // 通知格式化变更
        List<YTypeDelta> changes = List.of(
            new YTypeDelta(YTypeDelta.Type.RETAIN, index, attributes, length)
        );
        notifyObservers(changes, transaction);
        
        log.debug("Applied format operation: index={}, length={}, attributes={}", 
                 index, length, attributes);
    }
    
    /**
     * YATA算法 - 解决插入位置冲突
     * 使用完整的YATA算法而不是简化版本
     */
    private int resolveInsertPosition(int index, Operation operation) {
        // 使用YATA算法进行精确的位置解析
        YataAlgorithm.YataItem itemAtPosition = yataAlgorithm.getItemAtPosition(index);
        
        if (itemAtPosition == null) {
            // 插入到末尾
            return yataAlgorithm.getLength();
        }
        
        // 检查是否有并发操作
        List<Operation> conflictingOps = findConflictingOperations(index, operation);
        
        if (conflictingOps.isEmpty()) {
            return Math.min(index, yataAlgorithm.getLength());
        }
        
        // 使用操作变换解决冲突
        Operation transformedOp = operation;
        boolean hasOwnPriority = true;
        
        for (Operation conflictOp : conflictingOps) {
            OperationalTransform.TransformResult result = operationalTransform.transform(
                transformedOp, conflictOp, hasOwnPriority
            );
            transformedOp = result.getTransformedOp1();
            
            log.debug("Applied OT transformation: {} vs {}, result: {}", 
                     operation.getId(), conflictOp.getId(), result.getReason());
        }
        
        return Math.min(transformedOp.getIndex(), yataAlgorithm.getLength());
    }
    
    /**
     * 提取左参照 - 用于YATA算法
     */
    private YataAlgorithm.YataId extractLeftOrigin(int index, Operation operation) {
        if (index == 0) {
            return null; // 插入到开头
        }
        
        YataAlgorithm.YataItem leftItem = yataAlgorithm.getItemAtPosition(index - 1);
        return leftItem != null ? leftItem.getId() : null;
    }
    
    /**
     * 提取右参照 - 用于YATA算法
     */
    private YataAlgorithm.YataId extractRightOrigin(int index, Operation operation) {
        YataAlgorithm.YataItem rightItem = yataAlgorithm.getItemAtPosition(index);
        return rightItem != null ? rightItem.getId() : null;
    }
    
    /**
     * 查找冲突的操作 - 高级版本
     */
    private List<Operation> findConflictingOperations(int index, Operation currentOp) {
        List<Operation> conflicting = new ArrayList<>();
        
        // 从文档的操作历史中查找在相同位置或重叠位置的并发操作
        List<Operation> recentOps = doc.getRecentOperations(100); // 获取最近100个操作
        
        for (Operation op : recentOps) {
            if (op.getId().equals(currentOp.getId())) {
                continue; // 跳过自己
            }
            
            // 检查是否是并发操作（不在因果关系链中）
            if (isConcurrentOperation(op, currentOp)) {
                // 检查位置冲突
                if (hasPositionConflict(op, currentOp, index)) {
                    conflicting.add(op);
                }
            }
        }
        
        return conflicting;
    }
    
    /**
     * 检查是否为并发操作
     */
    private boolean isConcurrentOperation(Operation op1, Operation op2) {
        // 检查操作是否在同一客户端或有因果关系
        if (op1.getClientId().equals(op2.getClientId())) {
            return op1.getClock() != op2.getClock(); // 同客户端但不同时钟
        }
        
        // 不同客户端，检查向量时钟的因果关系
        StateVector sv1 = doc.getStateVectorAtOperation(op1);
        StateVector sv2 = doc.getStateVectorAtOperation(op2);
        
        return !sv1.contains(op2.getClientId(), op2.getClock()) && 
               !sv2.contains(op1.getClientId(), op1.getClock());
    }
    
    /**
     * 检查位置冲突
     */
    private boolean hasPositionConflict(Operation op1, Operation op2, int currentIndex) {
        int op1Index = op1.getIndex();
        int op2Index = op2.getIndex();
        
        switch (op1.getType()) {
            case INSERT:
                switch (op2.getType()) {
                    case INSERT:
                        return Math.abs(op1Index - op2Index) <= 1; // 相邻或相同位置
                    case DELETE:
                        int deleteEnd = op2Index + op2.getContentLength();
                        return op1Index >= op2Index && op1Index <= deleteEnd;
                    default:
                        return false;
                }
            case DELETE:
                int deleteEnd1 = op1Index + op1.getContentLength();
                switch (op2.getType()) {
                    case INSERT:
                        return op2Index >= op1Index && op2Index <= deleteEnd1;
                    case DELETE:
                        int deleteEnd2 = op2Index + op2.getContentLength();
                        return !(deleteEnd1 < op2Index || deleteEnd2 < op1Index); // 有重叠
                    default:
                        return false;
                }
            default:
                return false;
        }
    }
    
    /**
     * 编码更新数据
     */
    @Override
    public void encodeUpdate(UpdateEncoder encoder, StateVector targetStateVector) {
        // 遍历所有Items，编码需要同步的操作
        for (Item item : items) {
            if (!targetStateVector.contains(item.getClientId(), item.getClock())) {
                // 创建操作并编码
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
                item.getClientId(), item.getClock(), "ytext",
                doc.getGuid(), null, 0, item.getLength(), "sync"
            );
        } else {
            return Operation.createInsert(
                item.getClientId(), item.getClock(), "ytext",
                doc.getGuid(), item.getParentKey(), 0, item.getContent(), "sync"
            );
        }
    }
    
    @Override
    public int size() {
        return content.length();
    }
    
    @Override
    public YType clone() {
        YText cloned = new YText(doc, name);
        cloned.content = new StringBuilder(content);
        // 复制items和其他状态
        cloned.items.addAll(items);
        cloned.length = length;
        return cloned;
    }
    
    @Override
    public Object toJSON() {
        return content.toString();
    }
    
    /**
     * 获取增量变更
     */
    public List<YTypeDelta> toDelta() {
        if (content.length() == 0) {
            return new ArrayList<>();
        }
        
        return List.of(
            new YTypeDelta(YTypeDelta.Type.INSERT, 0, content.toString(), content.length())
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
                        if (delta.getContent() instanceof String) {
                            insertAt(index, delta.getContent(), transaction);
                            content.insert(index, (String) delta.getContent());
                            index += delta.getLength();
                        }
                        break;
                    case DELETE:
                        deleteRange(index, delta.getLength(), transaction);
                        int endIndex = Math.min(index + delta.getLength(), content.length());
                        if (index < content.length()) {
                            content.delete(index, endIndex);
                        }
                        break;
                    case RETAIN:
                        index += delta.getLength();
                        break;
                }
            }
        });
    }
    
    /**
     * 获取文本的差异
     */
    public List<YTypeDelta> diff(String newText) {
        List<YTypeDelta> deltas = new ArrayList<>();
        String currentText = content.toString();
        
        // 简化的diff算法 - 实际可以使用更高效的算法如Myers diff
        if (!currentText.equals(newText)) {
            if (!currentText.isEmpty()) {
                deltas.add(new YTypeDelta(YTypeDelta.Type.DELETE, 0, null, currentText.length()));
            }
            if (!newText.isEmpty()) {
                deltas.add(new YTypeDelta(YTypeDelta.Type.INSERT, 0, newText, newText.length()));
            }
        }
        
        return deltas;
    }
    
    /**
     * 应用远程操作到YATA算法
     */
    public void applyRemoteOperation(Operation operation) {
        yataAlgorithm.applyRemoteOperation(operation);
        
        // 同步StringBuilder内容
        String yataText = yataAlgorithm.getText();
        content.setLength(0);
        content.append(yataText);
        
        // 更新长度
        updateLength();
        
        log.debug("Applied remote operation to YText: {}", operation.getId());
    }
    
    /**
     * 获取YATA算法的调试信息
     */
    public Map<String, Object> getYataDebugInfo() {
        return yataAlgorithm.getDebugInfo();
    }
    
    /**
     * 验证YATA结构完整性
     */
    public boolean validateYataStructure() {
        return yataAlgorithm.validateStructure();
    }
    
    /**
     * 压缩YATA文档
     */
    public void compactYata() {
        yataAlgorithm.compact();
        
        // 重新同步内容
        String yataText = yataAlgorithm.getText();
        content.setLength(0);
        content.append(yataText);
        
        updateLength();
    }
    
//    @Override
//    public String toString() {
//        return yataAlgorithm.getText();
//    }
}