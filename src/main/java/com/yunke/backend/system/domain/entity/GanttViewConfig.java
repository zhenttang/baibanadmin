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
import java.util.List;

/**
 * 甘特图视图配置实体
 * 存储甘特图的时间轴配置、显示配置和工作日历配置
 * 
 * @author AFFiNE Development Team
 */
@Entity
@Table(name = "gantt_view_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttViewConfig {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;
    
    @Column(name = "doc_id", nullable = false, length = 36) 
    private String docId;
    
    /**
     * 时间轴配置 (JSON格式)
     * 包含：startDate, endDate, unit, showWeekends, workingDays
     */
    @Column(name = "timeline_config", columnDefinition = "JSON", nullable = false)
    private String timelineConfig;
    
    /**
     * 显示配置 (JSON格式)
     * 包含：showCriticalPath, showProgress, compactMode
     */
    @Column(name = "display_config", columnDefinition = "JSON", nullable = false)
    private String displayConfig;
    
    /**
     * 工作日历配置 (JSON格式，可选)
     * 包含：holidays, specialWorkingDays, timezone等
     */
    @Column(name = "working_calendar", columnDefinition = "JSON")
    private String workingCalendar;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 生成UUID主键
     */
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
    
    /**
     * 获取时间轴配置的便利方法
     */
    public GanttTimelineConfig getTimelineConfigObject() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                this.timelineConfig, GanttTimelineConfig.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse timeline config", e);
        }
    }
    
    /**
     * 设置时间轴配置的便利方法
     */
    public void setTimelineConfigObject(GanttTimelineConfig config) {
        try {
            this.timelineConfig = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize timeline config", e);
        }
    }
    
    /**
     * 获取显示配置的便利方法
     */
    public GanttDisplayConfig getDisplayConfigObject() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                this.displayConfig, GanttDisplayConfig.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse display config", e);
        }
    }
    
    /**
     * 设置显示配置的便利方法
     */
    public void setDisplayConfigObject(GanttDisplayConfig config) {
        try {
            this.displayConfig = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize display config", e);
        }
    }
}