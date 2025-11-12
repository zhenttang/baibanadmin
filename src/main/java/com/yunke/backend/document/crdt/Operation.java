package com.yunke.backend.document.crdt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * CRDT操作 - 表示对文档的单个原子操作
 * 
 * 完全对应YJS的Operation结构
 * 包含操作类型、位置、内容等信息
 */
@Slf4j
@Data
public class Operation {
    
    public enum Type {
        INSERT,    // 插入操作
        DELETE,    // 删除操作
        RETAIN,    // 保留操作（用于格式化）
        FORMAT,    // 格式化操作
        EMBED      // 嵌入操作（图片、链接等）
    }
    
    private final String id;           // 操作唯一ID
    private final String clientId;     // 客户端ID
    private final long clock;          // 逻辑时钟
    private final Type type;           // 操作类型
    private final String typeName;     // 目标类型名称（如"text", "map", "array"）
    private final String parentId;     // 父对象ID
    private final Object key;          // 键（用于Map类型）
    private final int index;           // 索引（用于Array类型）
    private final Object content;      // 操作内容
    private final Object attributes;   // 属性信息
    private final long timestamp;      // 创建时间戳
    private final String origin;       // 操作来源
    
    /**
     * 创建插入操作
     */
    public static Operation createInsert(String clientId, long clock, String typeName, 
                                       String parentId, Object key, int index, 
                                       Object content, String origin) {
        return new Operation(
            generateId(clientId, clock),
            clientId, clock, Type.INSERT, typeName, parentId,
            key, index, content, null, System.currentTimeMillis(), origin
        );
    }
    
    /**
     * 创建删除操作
     */
    public static Operation createDelete(String clientId, long clock, String typeName,
                                       String parentId, Object key, int index,
                                       int length, String origin) {
        return new Operation(
            generateId(clientId, clock),
            clientId, clock, Type.DELETE, typeName, parentId,
            key, index, length, null, System.currentTimeMillis(), origin
        );
    }
    
    /**
     * 创建格式化操作
     */
    public static Operation createFormat(String clientId, long clock, String typeName,
                                       String parentId, int index, int length,
                                       Object attributes, String origin) {
        return new Operation(
            generateId(clientId, clock),
            clientId, clock, Type.FORMAT, typeName, parentId,
            null, index, length, attributes, System.currentTimeMillis(), origin
        );
    }
    
    /**
     * 创建保留操作
     */
    public static Operation createRetain(String clientId, long clock, String typeName,
                                       int length, Object attributes, String origin) {
        return new Operation(
            generateId(clientId, clock),
            clientId, clock, Type.RETAIN, typeName, null,
            null, 0, length, attributes, System.currentTimeMillis(), origin
        );
    }
    
    /**
     * 生成操作ID
     */
    private static String generateId(String clientId, long clock) {
        return String.format("%s:%d:%s", clientId, clock, UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * 检查操作是否可以与另一个操作合并
     */
    public boolean canMergeWith(Operation other) {
        if (!this.clientId.equals(other.clientId)) {
            return false;
        }
        
        if (this.clock + 1 != other.clock) {
            return false;
        }
        
        if (this.type != other.type || !this.typeName.equals(other.typeName)) {
            return false;
        }
        
        if (!Objects.equals(this.parentId, other.parentId)) {
            return false;
        }
        
        // 只有相同类型的相邻操作才能合并
        switch (this.type) {
            case INSERT:
                return this.index + getContentLength() == other.index;
            case DELETE:
                return this.index == other.index;
            case RETAIN:
                return Objects.equals(this.attributes, other.attributes);
            default:
                return false;
        }
    }
    
    /**
     * 与另一个操作合并
     */
    public Operation mergeWith(Operation other) {
        if (!canMergeWith(other)) {
            throw new IllegalArgumentException("Cannot merge operations");
        }
        
        switch (this.type) {
            case INSERT:
                Object mergedContent = mergeContent(this.content, other.content);
                return Operation.createInsert(
                    this.clientId, this.clock, this.typeName, this.parentId,
                    this.key, this.index, mergedContent, this.origin
                );
                
            case DELETE:
                int mergedLength = getContentLength() + other.getContentLength();
                return Operation.createDelete(
                    this.clientId, this.clock, this.typeName, this.parentId,
                    this.key, this.index, mergedLength, this.origin
                );
                
            case RETAIN:
                int retainLength = getContentLength() + other.getContentLength();
                return Operation.createRetain(
                    this.clientId, this.clock, this.typeName,
                    retainLength, this.attributes, this.origin
                );
                
            default:
                throw new UnsupportedOperationException("Cannot merge operation type: " + this.type);
        }
    }
    
    /**
     * 合并内容
     */
    private Object mergeContent(Object content1, Object content2) {
        if (content1 instanceof String && content2 instanceof String) {
            return (String) content1 + (String) content2;
        }
        
        // 对于其他类型，可以扩展合并逻辑
        throw new UnsupportedOperationException("Cannot merge content types");
    }
    
    /**
     * 获取内容长度
     */
    public int getContentLength() {
        if (content == null) {
            return 0;
        }
        
        if (content instanceof String) {
            return ((String) content).length();
        }
        
        if (content instanceof Integer) {
            return (Integer) content;
        }
        
        return 1; // 默认长度
    }
    
    /**
     * 检查操作是否为空
     */
    public boolean isEmpty() {
        return getContentLength() == 0;
    }
    
    /**
     * 创建逆操作
     */
    public Operation inverse() {
        switch (this.type) {
            case INSERT:
                return Operation.createDelete(
                    this.clientId, this.clock, this.typeName, this.parentId,
                    this.key, this.index, getContentLength(), this.origin
                );
                
            case DELETE:
                // 删除操作的逆操作需要原始内容，这里简化处理
                throw new UnsupportedOperationException("Delete inverse requires original content");
                
            default:
                throw new UnsupportedOperationException("Cannot create inverse for operation type: " + this.type);
        }
    }
    
    /**
     * 检查是否为NoOp操作
     */
    public boolean isNoOp() {
        return isEmpty() || (type == Type.RETAIN && attributes == null);
    }
    
    /**
     * 转换操作（针对并发冲突）
     */
    public Operation transform(Operation other, boolean ownOp) {
        // 这里实现OT算法的核心逻辑
        // 根据操作类型和位置关系进行变换
        
        if (!this.typeName.equals(other.typeName) || !Objects.equals(this.parentId, other.parentId)) {
            return this; // 不同对象的操作不需要变换
        }
        
        switch (this.type) {
            case INSERT:
                return transformInsert(other, ownOp);
            case DELETE:
                return transformDelete(other, ownOp);
            case RETAIN:
                return transformRetain(other, ownOp);
            default:
                return this;
        }
    }
    
    private Operation transformInsert(Operation other, boolean ownOp) {
        switch (other.type) {
            case INSERT:
                if (this.index <= other.index || (this.index == other.index && ownOp)) {
                    return this;
                } else {
                    return Operation.createInsert(
                        this.clientId, this.clock, this.typeName, this.parentId,
                        this.key, other.getContentLength() + this.index, this.content, this.origin
                    );
                }
                
            case DELETE:
                if (this.index <= other.index) {
                    return this;
                } else {
                    return Operation.createInsert(
                        this.clientId, this.clock, this.typeName, this.parentId,
                        this.key, this.index - other.getContentLength(), this.content, this.origin
                    );
                }
                
            default:
                return this;
        }
    }
    
    private Operation transformDelete(Operation other, boolean ownOp) {
        switch (other.type) {
            case INSERT:
                if (this.index < other.index) {
                    return this;
                } else {
                    return Operation.createDelete(
                        this.clientId, this.clock, this.typeName, this.parentId,
                        this.key, this.index + other.getContentLength(), this.getContentLength(), this.origin
                    );
                }
                
            case DELETE:
                if (this.index <= other.index) {
                    return this;
                } else {
                    return Operation.createDelete(
                        this.clientId, this.clock, this.typeName, this.parentId,
                        this.key, this.index - other.getContentLength(), this.getContentLength(), this.origin
                    );
                }
                
            default:
                return this;
        }
    }
    
    private Operation transformRetain(Operation other, boolean ownOp) {
        // Retain操作的变换逻辑
        return this;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Operation operation = (Operation) obj;
        return Objects.equals(id, operation.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Operation{id='%s', type=%s, client='%s', clock=%d, target='%s', index=%d, content='%s'}", 
                           id, type, clientId, clock, typeName, index, content);
    }
}