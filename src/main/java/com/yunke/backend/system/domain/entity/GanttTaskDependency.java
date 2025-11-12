package com.yunke.backend.system.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 甘特图任务依赖关系实体
 * 存储任务之间的依赖关系和约束条件
 * 
 * @author AFFiNE Development Team
 */
@Entity
@Table(name = "gantt_task_dependencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttTaskDependency {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;
    
    @Column(name = "doc_id", nullable = false, length = 36)
    private String docId;
    
    @Column(name = "from_task_id", nullable = false, length = 36)
    private String fromTaskId;
    
    @Column(name = "to_task_id", nullable = false, length = 36)
    private String toTaskId;
    
    /**
     * 依赖类型枚举
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false)
    private DependencyType dependencyType;
    
    /**
     * 延迟天数（可以为负数）
     */
    @Column(name = "lag_days")
    private Integer lagDays;
    
    /**
     * 是否允许拖拽时自动调整
     */
    @Column(name = "is_flexible")
    private Boolean isFlexible;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 依赖类型枚举
     */
    public enum DependencyType {
        /**
         * 完成-开始：前置任务完成后，后续任务开始
         */
        FINISH_TO_START("finish-to-start"),
        
        /**
         * 开始-开始：前置任务开始后，后续任务开始
         */  
        START_TO_START("start-to-start"),
        
        /**
         * 完成-完成：前置任务完成后，后续任务完成
         */
        FINISH_TO_FINISH("finish-to-finish"),
        
        /**
         * 开始-完成：前置任务开始后，后续任务完成
         */
        START_TO_FINISH("start-to-finish");
        
        private final String value;
        
        DependencyType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * 根据字符串值获取枚举
         */
        public static DependencyType fromValue(String value) {
            for (DependencyType type : DependencyType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown dependency type: " + value);
        }
    }
    
    /**
     * 验证依赖关系是否有效
     */
    public boolean isValid() {
        // 任务不能依赖自己
        if (fromTaskId != null && fromTaskId.equals(toTaskId)) {
            return false;
        }
        
        // 依赖类型不能为空
        if (dependencyType == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取依赖关系的描述
     */
    public String getDescription() {
        String lagDescription = "";
        if (lagDays != null && lagDays != 0) {
            lagDescription = lagDays > 0 ? " (+" + lagDays + "天)" : " (" + lagDays + "天)";
        }
        
        switch (dependencyType) {
            case FINISH_TO_START:
                return "完成后开始" + lagDescription;
            case START_TO_START:
                return "同时开始" + lagDescription;
            case FINISH_TO_FINISH:
                return "同时完成" + lagDescription;
            case START_TO_FINISH:
                return "开始后完成" + lagDescription;
            default:
                return "未知依赖" + lagDescription;
        }
    }
}