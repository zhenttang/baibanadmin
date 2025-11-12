package com.yunke.backend.monitor;

import com.yunke.backend.infrastructure.config.AffineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 健康检查控制器
 * 提供自定义健康检查端点
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:8081", 
    "http://localhost:8082",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:8082",
    "http://c.yckeji0316.cn",
    "http://c.yckeji0316.cn",
    "http://f.yckeji0316.cn",
    "http://f.yckeji0316.cn",
    "http://b.yckeji0316.cn",
    "https://b.yckeji0316.cn",
    // 添加新的域名支持
    "http://ykweb.yckeji0316.cn",
    "https://ykweb.yckeji0316.cn",
    "http://ykmodile.yckeji0316.cn",
    "https://ykmodile.yckeji0316.cn",
    "http://ykadmin.yckeji0316.cn:8080",
    "https://ykadmin.yckeji0316.cn:8080"
}, allowCredentials = "true")
public class HealthCheckController {

    private final Map<String, HealthIndicator> healthIndicators;
    private final AffineConfig affineConfig;
    private final MetricsCollector metricsCollector;
    
    // 注入AI配置
    @Value("${affine.copilot.enabled:false}")
    private boolean copilotEnabled;
    
    @Value("${affine.payment.enabled:false}")
    private boolean paymentEnabled;

    /**
     * 基础健康检查
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 基础信息
            response.put("status", "UP");
            response.put("timestamp", Instant.now().toString());
            response.put("version", "0.21.0");
            response.put("environment", System.getProperty("spring.profiles.active", "dev"));
            
            // 服务配置
            response.put("flavor", affineConfig.getServer().getFlavor());
            response.put("deployment", affineConfig.getServer().getDeployment());
            
            // 添加特性列表 - 这是前端需要的关键信息
            response.put("features", getEnabledFeatures());
            
            // 实时指标
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("activeConnections", metricsCollector.getActiveConnections());
            metrics.put("activeWebsockets", metricsCollector.getActiveWebsockets());
            metrics.put("queueSize", metricsCollector.getQueueSize());
            response.put("metrics", metrics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Health check failed", e);
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 详细健康检查 - 前端调用的主要端点
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        log.info("🩺 收到详细健康检查请求");
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        
        boolean allHealthy = true;
        
        try {
            // 检查所有健康指标
            for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
                String name = entry.getKey();
                HealthIndicator indicator = entry.getValue();
                
                try {
                    Health health = indicator.health();
                    components.put(name, Map.of(
                        "status", health.getStatus().getCode(),
                        "details", health.getDetails()
                    ));
                    
                    if (!health.getStatus().equals(org.springframework.boot.actuate.health.Status.UP)) {
                        allHealthy = false;
                    }
                } catch (Exception e) {
                    components.put(name, Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                    ));
                    allHealthy = false;
                }
            }
            
            response.put("status", allHealthy ? "UP" : "DOWN");
            response.put("components", components);
            response.put("timestamp", Instant.now().toString());
            
            // 🔥 关键修复：添加前端需要的服务器配置信息
            response.put("version", "0.21.0");
            response.put("serverName", "AFFiNE");
            response.put("flavor", affineConfig.getServer().getFlavor());
            response.put("deployment", affineConfig.getServer().getDeployment());
            
            // 🔥 最重要：添加特性列表，前端据此判断AI功能是否可用
            response.put("features", getEnabledFeatures());
            
            // 添加服务器能力配置（兼容前端ServerFeatureRecord类型）
            Map<String, Boolean> capabilities = new HashMap<>();
            capabilities.put("copilot", copilotEnabled);
            capabilities.put("payment", paymentEnabled);
            capabilities.put("oauth", false); // 根据实际配置设置
            response.put("capabilities", capabilities);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Detailed health check failed", e);
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            // 即使出错也要返回基本的特性信息
            response.put("features", getEnabledFeatures());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 就绪检查
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查关键服务是否就绪
            boolean ready = checkDatabaseReady() && checkRedisReady();
            
            response.put("status", ready ? "READY" : "NOT_READY");
            response.put("timestamp", Instant.now().toString());
            response.put("checks", Map.of(
                "database", checkDatabaseReady(),
                "redis", checkRedisReady()
            ));
            
            return ResponseEntity.status(ready ? 200 : 503).body(response);
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            response.put("status", "NOT_READY");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 存活检查
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 基础存活检查
            response.put("status", "ALIVE");
            response.put("timestamp", Instant.now().toString());
            response.put("uptime", getUptime());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Liveness check failed", e);
            response.put("status", "DEAD");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * 🔥 新增：获取启用的特性列表
     * 这是前端判断AI功能是否可用的关键方法
     */
    private List<String> getEnabledFeatures() {
        List<String> features = new ArrayList<>();
        
        // 根据配置添加特性
        if (copilotEnabled) {
            features.add("copilot");
            log.info("✅ Copilot feature is enabled and exposed to frontend");
        } else {
            log.warn("⚠️ Copilot feature is disabled in configuration");
        }
        
        if (paymentEnabled) {
            features.add("payment");
        }
        
        // 可以根据其他配置添加更多特性
        // features.add("oauth");
        // features.add("collaboration");
        
        log.info("🌐 Exposing features to frontend: {}", features);
        
        return features;
    }

    /**
     * 检查数据库是否就绪
     */
    private boolean checkDatabaseReady() {
        try {
            HealthIndicator dbIndicator = healthIndicators.get("databaseHealthIndicator");
            if (dbIndicator != null) {
                Health health = dbIndicator.health();
                return health.getStatus().equals(org.springframework.boot.actuate.health.Status.UP);
            }
            return false;
        } catch (Exception e) {
            log.warn("Database readiness check failed", e);
            return false;
        }
    }

    /**
     * 检查 Redis 是否就绪
     */
    private boolean checkRedisReady() {
        try {
            HealthIndicator redisIndicator = healthIndicators.get("redisHealthIndicator");
            if (redisIndicator != null) {
                Health health = redisIndicator.health();
                return health.getStatus().equals(org.springframework.boot.actuate.health.Status.UP);
            }
            return false;
        } catch (Exception e) {
            log.warn("Redis readiness check failed", e);
            return false;
        }
    }

    /**
     * 获取系统运行时间
     */
    private String getUptime() {
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        return String.format("%d ms", uptime);
    }
}