package com.yunke.backend.controller;

import com.yunke.backend.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 性能监控控制器
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final PerformanceService performanceService;

    /**
     * 获取特定服务的性能指标
     */
    @GetMapping("/metrics/{serviceName}")
    public Mono<ResponseEntity<PerformanceService.PerformanceMetrics>> getMetrics(
            @PathVariable String serviceName) {
        
        return performanceService.getMetrics(serviceName)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有服务的性能指标
     */
    @GetMapping("/metrics")
    public Mono<ResponseEntity<Map<String, PerformanceService.PerformanceMetrics>>> getAllMetrics() {
        return performanceService.getAllMetrics()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> getHealthStatus() {
        return performanceService.getHealthStatus()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * 获取错误统计
     */
    @GetMapping("/errors")
    public Mono<ResponseEntity<Map<String, Object>>> getErrorStats() {
        return performanceService.getErrorStats()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * 记录自定义指标
     */
    @PostMapping("/metrics/custom")
    public Mono<ResponseEntity<Map<String, Object>>> recordCustomMetric(
            @RequestBody CustomMetricRequest request) {
        
        return performanceService.recordCustomMetric(request.name(), request.value())
                .<ResponseEntity<Map<String, Object>>>then(Mono.fromCallable(() -> {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Custom metric recorded successfully"
                    ));
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of(
                        "error", "Failed to record custom metric"
                )));
    }

    // 请求数据类
    public record CustomMetricRequest(String name, Object value) {}
}