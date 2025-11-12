package com.yunke.backend.security.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 安全事件DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEventDto {
    
    /**
     * 事件ID
     */
    private String id;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 事件级别
     */
    private String severity;
    
    /**
     * 事件描述
     */
    private String description;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 来源IP
     */
    private String sourceIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 事件时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 请求路径
     */
    private String requestPath;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 地理位置信息
     */
    private GeoLocationInfo geoLocation;
    
    /**
     * 事件详情
     */
    private Map<String, Object> details;
    
    /**
     * 是否已处理
     */
    private Boolean handled;
    
    /**
     * 处理结果
     */
    private String resolution;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeoLocationInfo {
        /**
         * 国家
         */
        private String country;
        
        /**
         * 地区
         */
        private String region;
        
        /**
         * 城市
         */
        private String city;
        
        /**
         * ISP
         */
        private String isp;
        
        /**
         * 经度
         */
        private Double longitude;
        
        /**
         * 纬度
         */
        private Double latitude;
    }
}