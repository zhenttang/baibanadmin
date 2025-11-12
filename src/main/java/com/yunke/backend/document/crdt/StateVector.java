package com.yunke.backend.document.crdt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态向量 - 追踪每个客户端的最新时钟
 * 
 * 完全对应YJS的StateVector实现
 * 用于增量同步和冲突检测
 */
@Slf4j
@Data
public class StateVector {
    
    private final Map<String, Long> clocks;
    
    /**
     * 创建空的状态向量
     */
    public StateVector() {
        this.clocks = new ConcurrentHashMap<>();
    }
    
    /**
     * 从现有映射创建状态向量
     */
    public StateVector(Map<String, Long> clocks) {
        this.clocks = new ConcurrentHashMap<>(clocks);
    }
    
    /**
     * 获取客户端的时钟值
     */
    public Long get(String clientId) {
        return clocks.get(clientId);
    }
    
    /**
     * 设置客户端的时钟值
     */
    public void set(String clientId, long clock) {
        clocks.put(clientId, clock);
    }
    
    /**
     * 更新客户端的时钟值（只增不减）
     */
    public void update(String clientId, long clock) {
        clocks.merge(clientId, clock, Math::max);
    }
    
    /**
     * 检查是否包含指定客户端
     */
    public boolean contains(String clientId) {
        return clocks.containsKey(clientId);
    }
    
    /**
     * 获取所有客户端ID
     */
    public Set<String> getClientIds() {
        return new HashSet<>(clocks.keySet());
    }
    
    /**
     * 合并另一个状态向量
     */
    public void merge(StateVector other) {
        for (Map.Entry<String, Long> entry : other.clocks.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 检查是否比另一个状态向量更新
     */
    public boolean isNewerThan(StateVector other) {
        for (Map.Entry<String, Long> entry : clocks.entrySet()) {
            String clientId = entry.getKey();
            Long otherClock = other.get(clientId);
            
            if (otherClock == null || entry.getValue() > otherClock) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否包含指定操作
     */
    public boolean contains(String clientId, long clock) {
        Long currentClock = get(clientId);
        return currentClock != null && currentClock >= clock;
    }
    
    /**
     * 计算与另一个状态向量的差异
     */
    public StateVector diff(StateVector other) {
        StateVector result = new StateVector();
        
        // 添加我们有但对方没有的
        for (Map.Entry<String, Long> entry : clocks.entrySet()) {
            String clientId = entry.getKey();
            Long otherClock = other.get(clientId);
            
            if (otherClock == null) {
                result.set(clientId, entry.getValue());
            } else if (entry.getValue() > otherClock) {
                result.set(clientId, entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 创建副本
     */
    public StateVector copy() {
        return new StateVector(clocks);
    }
    
    /**
     * 清空状态向量
     */
    public void clear() {
        clocks.clear();
    }
    
    /**
     * 获取向量大小
     */
    public int size() {
        return clocks.size();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return clocks.isEmpty();
    }
    
    /**
     * 编码为字节数组
     */
    public byte[] encode() {
        // 简化版本，实际应该使用更紧凑的编码
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : clocks.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }
        return sb.toString().getBytes();
    }
    
    /**
     * 从字节数组解码
     */
    public static StateVector decode(byte[] data) {
        StateVector result = new StateVector();
        String str = new String(data);
        
        if (str.isEmpty()) {
            return result;
        }
        
        String[] pairs = str.split(";");
        for (String pair : pairs) {
            if (!pair.isEmpty()) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    String clientId = parts[0];
                    long clock = Long.parseLong(parts[1]);
                    result.set(clientId, clock);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StateVector that = (StateVector) obj;
        return clocks.equals(that.clocks);
    }
    
    @Override
    public int hashCode() {
        return clocks.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("StateVector{clients=%d, clocks=%s}", 
                           clocks.size(), clocks);
    }
}