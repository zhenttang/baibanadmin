package com.yunke.backend.service;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 性能监控服务接口
 */
public interface PerformanceService {

    /**
     * 性能指标
     */
    record PerformanceMetrics(
            String serviceName,
            long requestCount,
            double averageResponseTime,
            double maxResponseTime,
            double errorRate,
            long memoryUsage,
            double cpuUsage,
            Map<String, Object> customMetrics
    ) {}

    /**
     * 记录请求性能
     */
    Mono<Void> recordRequest(String endpoint, long responseTime, boolean success);

    /**
     * 获取性能指标
     */
    Mono<PerformanceMetrics> getMetrics(String serviceName);

    /**
     * 获取所有服务的性能概览
     */
    Mono<Map<String, PerformanceMetrics>> getAllMetrics();

    /**
     * 记录自定义指标
     */
    Mono<Void> recordCustomMetric(String name, Object value);

    /**
     * 获取系统健康状态
     */
    Mono<Map<String, Object>> getHealthStatus();

    /**
     * 记录错误
     */
    Mono<Void> recordError(String endpoint, String errorType, String message);

    /**
     * 获取错误统计
     */
    Mono<Map<String, Object>> getErrorStats();
}