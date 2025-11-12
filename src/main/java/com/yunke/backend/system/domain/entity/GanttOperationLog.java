package com.yunke.backend.system.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 甘特图操作日志实体
 * 记录甘特图的所有操作，用于协作和版本控制
 * 
 * @author AFFiNE Development Team
 */
@Entity
@Table(name = "gantt_operation_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttOperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;
    
    @Column(name = "doc_id", nullable = false, length = 36)
    private String docId;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    /**
     * 操作类型枚举
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;
    
    /**
     * 操作数据 (JSON格式)
     * 存储具体的操作内容和变更数据
     */
    @Column(name = "operation_data", columnDefinition = "JSON", nullable = false)
    private String operationData;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        /**
         * 配置更新：时间轴配置、显示配置等
         */
        CONFIG_UPDATE("config_update"),
        
        /**
         * 添加依赖关系
         */
        DEPENDENCY_ADD("dependency_add"),
        
        /**
         * 移除依赖关系
         */
        DEPENDENCY_REMOVE("dependency_remove"),
        
        /**
         * 任务更新：时间、进度、状态等
         */
        TASK_UPDATE("task_update");
        
        private final String value;
        
        OperationType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * 根据字符串值获取枚举
         */
        public static OperationType fromValue(String value) {
            for (OperationType type : OperationType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown operation type: " + value);
        }
    }
    
    /**
     * 获取操作数据对象
     */
    public <T> T getOperationDataObject(Class<T> clazz) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(this.operationData, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse operation data", e);
        }
    }
    
    /**
     * 设置操作数据对象
     */
    public void setOperationDataObject(Object data) {
        try {
            this.operationData = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize operation data", e);
        }
    }
    
    /**
     * 获取操作描述
     */
    public String getOperationDescription() {
        switch (operationType) {
            case CONFIG_UPDATE:
                return "更新甘特图配置";
            case DEPENDENCY_ADD:
                return "添加任务依赖关系";
            case DEPENDENCY_REMOVE:
                return "删除任务依赖关系";
            case TASK_UPDATE:
                return "更新任务信息";
            default:
                return "未知操作";
        }
    }
}