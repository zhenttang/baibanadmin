package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 安全事件实体
 */
@Entity
@Table(name = "security_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 事件类型
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    /**
     * 事件级别 (INFO, WARNING, CRITICAL)
     */
    @Column(name = "severity", nullable = false)
    private String severity;
    
    /**
     * 事件描述
     */
    @Column(name = "description", nullable = false)
    private String description;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private String userId;
    
    /**
     * 用户名
     */
    @Column(name = "username")
    private String username;
    
    /**
     * 来源IP
     */
    @Column(name = "source_ip", nullable = false)
    private String sourceIp;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    /**
     * 事件时间
     */
    @CreationTimestamp
    @Column(name = "event_time", nullable = false, updatable = false)
    private LocalDateTime eventTime;
    
    /**
     * 请求路径
     */
    @Column(name = "request_path")
    private String requestPath;
    
    /**
     * 请求方法
     */
    @Column(name = "request_method")
    private String requestMethod;
    
    /**
     * 国家
     */
    @Column(name = "country")
    private String country;
    
    /**
     * 地区
     */
    @Column(name = "region")
    private String region;
    
    /**
     * 城市
     */
    @Column(name = "city")
    private String city;
    
    /**
     * ISP
     */
    @Column(name = "isp")
    private String isp;
    
    /**
     * 经度
     */
    @Column(name = "longitude")
    private Double longitude;
    
    /**
     * 纬度
     */
    @Column(name = "latitude")
    private Double latitude;
    
    /**
     * 事件详情（JSON格式）
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    /**
     * 是否已处理
     */
    @Column(name = "handled", nullable = false)
    @Builder.Default
    private Boolean handled = false;
    
    /**
     * 处理结果
     */
    @Column(name = "resolution")
    private String resolution;
    
    /**
     * 处理时间
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    /**
     * 处理人
     */
    @Column(name = "resolved_by")
    private String resolvedBy;
}