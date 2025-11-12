package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 邮件提供商信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailerProviderDto {
    
    /**
     * 提供商标识
     */
    private String provider;
    
    /**
     * 提供商名称
     */
    private String name;
    
    /**
     * 提供商描述
     */
    private String description;
    
    /**
     * 提供商图标URL
     */
    private String icon;
    
    /**
     * 是否支持
     */
    private Boolean supported;
    
    /**
     * 是否推荐使用
     */
    private Boolean recommended;
    
    /**
     * 预设配置
     */
    private Map<String, Object> presetConfig;
    
    /**
     * 配置字段定义
     */
    private List<ConfigFieldDto> configFields;
    
    /**
     * 使用说明
     */
    private String instructions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigFieldDto {
        /**
         * 字段名称
         */
        private String name;
        
        /**
         * 字段标签
         */
        private String label;
        
        /**
         * 字段类型
         */
        private String type;
        
        /**
         * 是否必填
         */
        private Boolean required;
        
        /**
         * 默认值
         */
        private Object defaultValue;
        
        /**
         * 字段描述
         */
        private String description;
        
        /**
         * 占位符文本
         */
        private String placeholder;
        
        /**
         * 验证规则
         */
        private String validation;
    }
}