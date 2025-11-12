package com.yunke.backend.system.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 甘特图时间轴配置数据类
 * 
 * @author AFFiNE Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttTimelineConfig {
    
    /**
     * 开始日期 (Unix 时间戳，毫秒)
     */
    private Long startDate;
    
    /**
     * 结束日期 (Unix 时间戳，毫秒)  
     */
    private Long endDate;
    
    /**
     * 时间单位：day, week, month
     */
    private String unit;
    
    /**
     * 是否显示周末
     */
    private Boolean showWeekends;
    
    /**
     * 工作日数组 [1,2,3,4,5] 表示周一到周五
     */
    private List<Integer> workingDays;
}