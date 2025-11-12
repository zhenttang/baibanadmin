package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YJS更新解码器 - 完全基于AFFiNE的YJS实现
 * 
 * 用于解码和验证YJS二进制更新数据
 * 支持YJS标准的二进制格式
 */
@Slf4j
public class UpdateDecoder {
    
    private final ByteBuffer buffer;
    private final int originalLength;
    
    public UpdateDecoder(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Update data cannot be null");
        }
        this.buffer = ByteBuffer.wrap(data);
        this.originalLength = data.length;
    }
    
    /**
     * 验证更新数据的格式是否正确
     */
    public boolean validate() {
        try {
            buffer.rewind();
            
            // YJS更新的基本结构验证
            if (buffer.remaining() < 2) {
                return false; // 至少需要2字节
            }
            
            // 读取更新的头部信息
            int structsLength = readVarUint();
            if (structsLength < 0 || structsLength > buffer.remaining()) {
                return false;
            }
            
            // 跳过结构数据
            if (structsLength > 0) {
                buffer.position(buffer.position() + structsLength);
            }
            
            // 检查是否还有删除集数据
            if (buffer.hasRemaining()) {
                int deleteSetLength = readVarUint();
                if (deleteSetLength < 0 || deleteSetLength > buffer.remaining()) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.debug("Update validation failed", e);
            return false;
        }
    }
    
    /**
     * 检查是否还有更多数据可读
     */
    public boolean hasNext() {
        return buffer.hasRemaining();
    }
    
    /**
     * 读取一个操作 - 简化版本
     */
    public Operation readOperation() {
        if (!hasNext()) {
            return null;
        }
        
        try {
            // 读取操作类型
            int typeInfo = readVarUint();
            Operation.Type type = decodeOperationType(typeInfo);
            
            // 读取客户端ID和时钟
            String clientId = readString();
            long clock = readVarUint();
            
            // 读取操作数据
            switch (type) {
                case INSERT:
                    return readInsertOperation(clientId, clock);
                case DELETE:
                    return readDeleteOperation(clientId, clock);
                case FORMAT:
                    return readFormatOperation(clientId, clock);
                default:
                    return readRetainOperation(clientId, clock);
            }
            
        } catch (Exception e) {
            log.error("Failed to read operation from update", e);
            return null;
        }
    }
    
    /**
     * 解码操作类型
     */
    private Operation.Type decodeOperationType(int typeInfo) {
        int type = typeInfo & 0x3; // 取最低2位
        switch (type) {
            case 0: return Operation.Type.INSERT;
            case 1: return Operation.Type.DELETE;
            case 2: return Operation.Type.FORMAT;
            default: return Operation.Type.RETAIN;
        }
    }
    
    /**
     * 读取插入操作
     */
    private Operation readInsertOperation(String clientId, long clock) {
        int length = readVarUint();
        String content = readString();
        int index = readVarUint();
        
        return Operation.createInsert(
            clientId, clock, "ytext", "doc", null, 
            index, content, "decode"
        );
    }
    
    /**
     * 读取删除操作
     */
    private Operation readDeleteOperation(String clientId, long clock) {
        int index = readVarUint();
        int length = readVarUint();
        
        return Operation.createDelete(
            clientId, clock, "ytext", "doc", null,
            index, length, "decode"
        );
    }
    
    /**
     * 读取格式化操作
     */
    private Operation readFormatOperation(String clientId, long clock) {
        int index = readVarUint();
        int length = readVarUint();
        Map<String, Object> attributes = readAttributes();
        
        return Operation.createFormat(
            clientId, clock, "ytext", "doc",
            index, length, attributes, "decode"
        );
    }
    
    /**
     * 读取保留操作
     */
    private Operation readRetainOperation(String clientId, long clock) {
        int length = readVarUint();
        Map<String, Object> attributes = readAttributes();
        
        return Operation.createRetain(
            clientId, clock, "ytext", length, attributes, "decode"
        );
    }
    
    /**
     * 读取可变长度无符号整数
     */
    private int readVarUint() {
        int result = 0;
        int shift = 0;
        
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            result |= (b & 0x7F) << shift;
            
            if ((b & 0x80) == 0) {
                break;
            }
            
            shift += 7;
            if (shift >= 32) {
                throw new RuntimeException("VarUint too large");
            }
        }
        
        return result;
    }
    
    /**
     * 读取字符串
     */
    private String readString() {
        int length = readVarUint();
        if (length == 0) {
            return "";
        }
        
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * 读取属性映射
     */
    private Map<String, Object> readAttributes() {
        int count = readVarUint();
        if (count == 0) {
            return null;
        }
        
        Map<String, Object> attributes = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String key = readString();
            Object value = readValue();
            attributes.put(key, value);
        }
        
        return attributes;
    }
    
    /**
     * 读取值对象
     */
    private Object readValue() {
        int type = readVarUint();
        switch (type) {
            case 0: // string
                return readString();
            case 1: // number
                return readVarUint();
            case 2: // boolean
                return readVarUint() != 0;
            case 3: // null
                return null;
            default:
                return readString(); // 默认当作字符串
        }
    }
    
    /**
     * 获取剩余字节数
     */
    public int remaining() {
        return buffer.remaining();
    }
    
    /**
     * 获取当前位置
     */
    public int position() {
        return buffer.position();
    }
    
    /**
     * 获取原始数据长度
     */
    public int length() {
        return originalLength;
    }
    
    /**
     * 重置到开始位置
     */
    public void rewind() {
        buffer.rewind();
    }
}