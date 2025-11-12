package com.yunke.backend.controller;

import com.yunke.backend.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统监控和性能指标控制器
 */
@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * 获取系统指标
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        log.info("=== 获取系统指标 ===");
        
        try {
            Map<String, Object> systemMetrics = metricsService.getSystemMetrics();
            log.info("系统指标获取成功，包含 {} 项指标", systemMetrics.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", systemMetrics,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("获取系统指标失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "获取系统指标失败",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取应用指标
     */
    @GetMapping("/application")
    public ResponseEntity<Map<String, Object>> getApplicationMetrics() {
        log.info("=== 获取应用指标 ===");
        
        try {
            Map<String, Object> applicationMetrics = metricsService.getApplicationMetrics();
            log.info("应用指标获取成功，包含 {} 项指标", applicationMetrics.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", applicationMetrics,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("获取应用指标失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "获取应用指标失败",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取数据库指标
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> getDatabaseMetrics() {
        log.info("=== 获取数据库指标 ===");
        
        try {
            Map<String, Object> databaseMetrics = metricsService.getDatabaseMetrics();
            log.info("数据库指标获取成功，包含 {} 项指标", databaseMetrics.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", databaseMetrics,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("获取数据库指标失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "获取数据库指标失败",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取历史数据
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistoryMetrics(
            @RequestParam(defaultValue = "system") String type,
            @RequestParam(defaultValue = "1h") String timeRange) {
        
        log.info("=== 获取历史指标数据：类型={}, 时间范围={} ===", type, timeRange);
        
        try {
            Map<String, Object> historyData = metricsService.getHistoryMetrics(type, timeRange);
            log.info("历史指标数据获取成功");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", historyData,
                "type", type,
                "timeRange", timeRange,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("获取历史指标数据失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "获取历史指标数据失败",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取实时指标汇总
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        log.info("=== 获取实时指标汇总 ===");
        
        try {
            Map<String, Object> summary = metricsService.getMetricsSummary();
            log.info("实时指标汇总获取成功");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", summary,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("获取实时指标汇总失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "获取实时指标汇总失败",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取健康检查状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        log.info("=== 获取健康检查状态 ===");
        
        try {
            Map<String, Object> healthStatus = metricsService.getHealthStatus();
            log.info("健康检查状态获取成功");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", healthStatus,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("获取健康检查状态失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "获取健康检查状态失败",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 导出指标数据
     */
    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> exportMetrics(
            @RequestBody Map<String, Object> exportRequest) {
        
        String format = (String) exportRequest.getOrDefault("format", "json");
        String timeRange = (String) exportRequest.getOrDefault("timeRange", "1h");
        
        log.info("=== 导出指标数据：格式={}, 时间范围={} ===", format, timeRange);
        
        try {
            Map<String, Object> exportResult = metricsService.exportMetrics(format, timeRange);
            log.info("指标数据导出成功");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", exportResult,
                "format", format,
                "timeRange", timeRange,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("导出指标数据失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "导出指标数据失败",
                "message", e.getMessage()
            ));
        }
    }
}