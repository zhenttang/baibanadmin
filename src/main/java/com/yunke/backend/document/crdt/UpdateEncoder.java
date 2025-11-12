package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 更新编码器 - 将操作序列化为二进制格式
 * 
 * 完全对应YJS的UpdateEncoder实现
 * 用于网络传输和持久化存储
 */
@Slf4j
public class UpdateEncoder {
    
    private final ByteArrayOutputStream buffer;
    private final List<Operation> operations;
    
    public UpdateEncoder() {
        this.buffer = new ByteArrayOutputStream();
        this.operations = new ArrayList<>();
    }
    
    /**
     * 写入操作
     */
    public void writeOperation(Operation operation) {
        operations.add(operation);
        
        try {
            // 写入操作头
            writeVarUint(operation.getType().ordinal());
            writeString(operation.getClientId());
            writeVarUint(operation.getClock());
            writeString(operation.getTypeName());
            
            // 写入父对象信息
            if (operation.getParentId() != null) {
                writeBoolean(true);
                writeString(operation.getParentId());
            } else {
                writeBoolean(false);
            }
            
            // 写入键信息
            if (operation.getKey() != null) {
                writeBoolean(true);
                writeObject(operation.getKey());
            } else {
                writeBoolean(false);
            }
            
            // 写入索引和内容
            writeVarInt(operation.getIndex());
            writeObject(operation.getContent());
            
            // 写入属性信息
            if (operation.getAttributes() != null) {
                writeBoolean(true);
                writeObject(operation.getAttributes());
            } else {
                writeBoolean(false);
            }
            
            // 写入时间戳和来源
            writeVarUint(operation.getTimestamp());
            writeString(operation.getOrigin() != null ? operation.getOrigin() : "");
            
        } catch (IOException e) {
            log.error("Failed to encode operation", e);
            throw new RuntimeException("Failed to encode operation", e);
        }
    }
    
    /**
     * 写入状态向量
     */
    public void writeStateVector(StateVector stateVector) {
        try {
            writeVarUint(stateVector.size());
            
            for (String clientId : stateVector.getClientIds()) {
                writeString(clientId);
                Long clock = stateVector.get(clientId);
                writeVarUint(clock != null ? clock : 0);
            }
            
        } catch (IOException e) {
            log.error("Failed to encode state vector", e);
            throw new RuntimeException("Failed to encode state vector", e);
        }
    }
    
    /**
     * 写入可变长度无符号整数
     */
    private void writeVarUint(long value) throws IOException {
        while (value >= 0x80) {
            buffer.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buffer.write((int) (value & 0x7F));
    }
    
    /**
     * 写入可变长度有符号整数
     */
    private void writeVarInt(int value) throws IOException {
        // ZigZag编码
        long encoded = (value << 1) ^ (value >> 31);
        writeVarUint(encoded);
    }
    
    /**
     * 写入字符串
     */
    private void writeString(String str) throws IOException {
        if (str == null) {
            writeVarUint(0);
            return;
        }
        
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarUint(bytes.length);
        buffer.write(bytes);
    }
    
    /**
     * 写入布尔值
     */
    private void writeBoolean(boolean value) throws IOException {
        buffer.write(value ? 1 : 0);
    }
    
    /**
     * 写入对象
     */
    private void writeObject(Object obj) throws IOException {
        if (obj == null) {
            buffer.write(0); // NULL_TYPE
            return;
        }
        
        if (obj instanceof String) {
            buffer.write(1); // STRING_TYPE
            writeString((String) obj);
        } else if (obj instanceof Integer) {
            buffer.write(2); // INT_TYPE
            writeVarInt((Integer) obj);
        } else if (obj instanceof Long) {
            buffer.write(3); // LONG_TYPE
            writeVarUint((Long) obj);
        } else if (obj instanceof Boolean) {
            buffer.write(4); // BOOLEAN_TYPE
            writeBoolean((Boolean) obj);
        } else if (obj instanceof byte[]) {
            buffer.write(5); // BYTES_TYPE
            byte[] bytes = (byte[]) obj;
            writeVarUint(bytes.length);
            buffer.write(bytes);
        } else {
            // 复杂对象序列化为JSON字符串
            buffer.write(6); // JSON_TYPE
            writeString(obj.toString());
        }
    }
    
    /**
     * 写入原始字节
     */
    public void writeBytes(byte[] bytes) {
        try {
            buffer.write(bytes);
        } catch (IOException e) {
            log.error("Failed to write bytes", e);
            throw new RuntimeException("Failed to write bytes", e);
        }
    }
    
    /**
     * 获取当前缓冲区大小
     */
    public int size() {
        return buffer.size();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return buffer.size() == 0;
    }
    
    /**
     * 编码为字节数组
     */
    public byte[] encode() {
        try {
            // 写入头部信息
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            
            // 写入版本号
            header.write(1);
            
            // 写入操作数量
            writeVarUintToStream(header, operations.size());
            
            // 合并头部和数据
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            result.write(header.toByteArray());
            result.write(buffer.toByteArray());
            
            return result.toByteArray();
            
        } catch (IOException e) {
            log.error("Failed to encode update", e);
            throw new RuntimeException("Failed to encode update", e);
        }
    }
    
    /**
     * 辅助方法：向流写入VarUint
     */
    private void writeVarUintToStream(ByteArrayOutputStream stream, long value) throws IOException {
        while (value >= 0x80) {
            stream.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        stream.write((int) (value & 0x7F));
    }
    
    /**
     * 重置编码器
     */
    public void reset() {
        buffer.reset();
        operations.clear();
    }
    
    /**
     * 获取已编码的操作列表
     */
    public List<Operation> getOperations() {
        return new ArrayList<>(operations);
    }
    
    @Override
    public String toString() {
        return String.format("UpdateEncoder{operations=%d, size=%d bytes}", 
                           operations.size(), buffer.size());
    }
}