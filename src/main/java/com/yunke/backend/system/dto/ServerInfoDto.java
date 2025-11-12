package com.yunke.backend.system.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 服务器信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerInfoDto {
    
    /**
     * 服务器配置信息
     */
    private ServerConfigDto config;
    
    /**
     * 版本信息
     */
    private VersionInfo version;
    
    /**
     * 运行时信息
     */
    private RuntimeInfo runtime;
    
    /**
     * 系统信息
     */
    private SystemInfo system;
    
    /**
     * 服务状态
     */
    private ServiceStatus status;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VersionInfo {
        /**
         * 应用版本
         */
        private String version;
        
        /**
         * 构建时间
         */
        private String buildTime;
        
        /**
         * Git提交hash
         */
        private String gitCommit;
        
        /**
         * 构建环境
         */
        private String buildProfile;
        
        /**
         * Java版本
         */
        private String javaVersion;
        
        /**
         * Spring Boot版本
         */
        private String springBootVersion;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RuntimeInfo {
        /**
         * 启动时间
         */
        private LocalDateTime startTime;
        
        /**
         * 运行时长（秒）
         */
        private Long uptimeSeconds;
        
        /**
         * JVM最大内存（字节）
         */
        private Long maxMemory;
        
        /**
         * JVM总内存（字节）
         */
        private Long totalMemory;
        
        /**
         * JVM空闲内存（字节）
         */
        private Long freeMemory;
        
        /**
         * JVM已使用内存（字节）
         */
        private Long usedMemory;
        
        /**
         * 内存使用率（百分比）
         */
        private Double memoryUsagePercentage;
        
        /**
         * 活跃线程数
         */
        private Integer activeThreads;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemInfo {
        /**
         * 操作系统名称
         */
        private String osName;
        
        /**
         * 操作系统版本
         */
        private String osVersion;
        
        /**
         * 操作系统架构
         */
        private String osArch;
        
        /**
         * CPU核心数
         */
        private Integer cpuCores;
        
        /**
         * 系统负载（Linux/macOS）
         */
        private Double systemLoad;
        
        /**
         * 系统时区
         */
        private String systemTimezone;
        
        /**
         * 系统编码
         */
        private String systemEncoding;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceStatus {
        /**
         * 数据库状态
         */
        private String databaseStatus;
        
        /**
         * Redis状态
         */
        private String redisStatus;
        
        /**
         * 存储状态
         */
        private String storageStatus;
        
        /**
         * 整体健康状态
         */
        private String overallStatus;
        
        /**
         * 服务详情
         */
        private Map<String, Object> details;
    }
}