package com.yunke.backend.system.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 甘特图显示配置数据类
 * 
 * @author AFFiNE Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttDisplayConfig {
    
    /**
     * 是否显示关键路径
     */
    private Boolean showCriticalPath;
    
    /**
     * 是否显示进度
     */
    private Boolean showProgress;
    
    /**
     * 紧凑模式
     */
    private Boolean compactMode;
}