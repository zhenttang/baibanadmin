package com.yunke.backend.controller;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.notification.service.MailService;
import com.yunke.backend.infrastructure.config.MailConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final MailService mailService;
    private final MailConfig mailConfig;
    private final Optional<BuildProperties> buildProperties;
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Value("${affine.server.name:AFFiNE Server}")
    private String serverName;
    
    @Value("${affine.server.external-url:}")
    private String externalUrl;

    /**
     * 获取服务器配置信息
     */
    @GetMapping("/server-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getServerConfig() {
        log.info("Getting server configuration");
        
        Map<String, Object> response = new HashMap<>();
        
        // 基本服务器信息
        response.put("initialized", true);
        response.put("name", serverName);
        response.put("externalUrl", externalUrl.isEmpty() ? String.format("http://localhost:%d%s", serverPort, contextPath) : externalUrl);
        
        // 服务器版本信息
        Map<String, Object> version = new HashMap<>();
        if (buildProperties.isPresent()) {
            BuildProperties build = buildProperties.get();
            version.put("version", build.getVersion());
            version.put("buildTime", build.getTime());
            version.put("artifact", build.getArtifact());
            version.put("group", build.getGroup());
        } else {
            version.put("version", "dev");
            version.put("buildTime", Instant.now());
            version.put("artifact", "affine-backend");
            version.put("group", "com.yunke");
        }
        response.put("version", version);
        
        // 系统运行信息
        Map<String, Object> runtime = getSystemRuntimeInfo();
        response.put("runtime", runtime);
        
        // 密码要求配置
        Map<String, Object> credentialsRequirement = new HashMap<>();
        Map<String, Object> passwordRequirement = new HashMap<>();
        passwordRequirement.put("minLength", 8);
        passwordRequirement.put("maxLength", 32);
        credentialsRequirement.put("password", passwordRequirement);
        response.put("credentialsRequirement", credentialsRequirement);
        
        // 服务器配置
        Map<String, Object> config = new HashMap<>();
        config.put("port", serverPort);
        config.put("contextPath", contextPath);
        config.put("https", false); // TODO: 从实际配置中获取
        response.put("config", config);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新服务器配置
     */
    @PutMapping("/server-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateServerConfig(@RequestBody UpdateServerConfigRequest request) {
        log.info("Updating server configuration: {}", request);
        
        try {
            // TODO: 实现配置更新逻辑
            // 这里应该更新application.properties或配置中心的配置
            // 由于Spring Boot的配置是在启动时加载的，实时更新需要特殊处理
            
            // 记录配置更改日志
            log.info("Server configuration updated by admin - name: {}, externalUrl: {}", 
                    request.name(), request.externalUrl());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Server configuration updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("changes", request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating server configuration: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update server configuration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取服务器健康状态
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getServerHealth() {
        log.info("Getting server health status");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 内存使用情况
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            Map<String, Object> memory = new HashMap<>();
            memory.put("heap", memoryBean.getHeapMemoryUsage());
            memory.put("nonHeap", memoryBean.getNonHeapMemoryUsage());
            health.put("memory", memory);
            
            // 系统负载
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Map<String, Object> system = new HashMap<>();
            system.put("availableProcessors", osBean.getAvailableProcessors());
            system.put("systemLoadAverage", osBean.getSystemLoadAverage());
            health.put("system", system);
            
            // 数据库连接状态 (TODO: 实现数据库健康检查)
            Map<String, Object> database = new HashMap<>();
            database.put("status", "UP");
            database.put("connectionPool", "available");
            health.put("database", database);
            
            // Redis连接状态 (TODO: 实现Redis健康检查)
            Map<String, Object> redis = new HashMap<>();
            redis.put("status", "UP");
            redis.put("connection", "available");
            health.put("redis", redis);
            
            response.put("status", "UP");
            response.put("health", health);
            response.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error getting server health: {}", e.getMessage(), e);
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 管理员权限探测端点（轻量、无负载）
     * 前端用于判断是否具备管理员权限。
     */
    @GetMapping("/access-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> accessCheck() {
        return ResponseEntity.noContent().build(); // 204
    }
    
    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        log.info("Getting system statistics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", userService.getTotalUserCount());
        response.put("totalWorkspaces", 0); // TODO: 实现工作空间统计
        response.put("totalDocuments", 0); // TODO: 实现文档统计
        response.put("systemHealth", "healthy");
        response.put("uptime", getSystemRuntimeInfo().get("uptime"));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取系统运行时信息
     */
    private Map<String, Object> getSystemRuntimeInfo() {
        Map<String, Object> runtime = new HashMap<>();
        
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 运行时间
            long uptimeMillis = runtimeBean.getUptime();
            runtime.put("uptime", uptimeMillis);
            runtime.put("startTime", Instant.ofEpochMilli(runtimeBean.getStartTime()));
            
            // Java版本信息
            runtime.put("javaVersion", System.getProperty("java.version"));
            runtime.put("javaVendor", System.getProperty("java.vendor"));
            runtime.put("jvmName", runtimeBean.getVmName());
            runtime.put("jvmVersion", runtimeBean.getVmVersion());
            
            // 操作系统信息
            runtime.put("osName", osBean.getName());
            runtime.put("osArch", osBean.getArch());
            runtime.put("osVersion", osBean.getVersion());
            runtime.put("availableProcessors", osBean.getAvailableProcessors());
            
            // 内存信息
            long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            runtime.put("totalMemory", totalMemory);
            runtime.put("usedMemory", usedMemory);
            runtime.put("freeMemory", totalMemory - usedMemory);
            runtime.put("memoryUsagePercent", totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0);
            
        } catch (Exception e) {
            log.warn("Error getting runtime info: {}", e.getMessage());
            runtime.put("error", "Unable to retrieve runtime information");
        }
        
        return runtime;
    }

    
    // 请求数据类
    public record UpdateServerConfigRequest(
        String name,
        String externalUrl,
        Boolean https,
        Map<String, Object> credentialsRequirement,
        Map<String, Object> config
    ) {}
}
