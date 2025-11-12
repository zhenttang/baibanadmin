package com.yunke.backend.service.impl;

import com.yunke.backend.monitor.MetricsCollector;
import com.yunke.backend.service.PerformanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能监控服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceServiceImpl implements PerformanceService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    // 内存指标存储
    private final Map<String, LongAdder> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> responseTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> maxResponseTimes = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, Object> customMetrics = new ConcurrentHashMap<>();

    // Redis键前缀
    private static final String METRICS_PREFIX = "metrics:";
    private static final String ERRORS_PREFIX = "errors:";
    private static final String CUSTOM_METRICS_PREFIX = "custom_metrics:";

    @Override
    public Mono<Void> recordRequest(String endpoint, long responseTime, boolean success) {
        log.debug("Recording request: endpoint={}, responseTime={}, success={}", endpoint, responseTime, success);
        
        return Mono.fromCallable(() -> {
            // 更新内存指标
            requestCounts.computeIfAbsent(endpoint, k -> new LongAdder()).increment();
            responseTimes.computeIfAbsent(endpoint, k -> new LongAdder()).add(responseTime);
            
            // 更新最大响应时间
            maxResponseTimes.computeIfAbsent(endpoint, k -> new AtomicLong(0))
                    .updateAndGet(current -> Math.max(current, responseTime));
            
            if (!success) {
                errorCounts.computeIfAbsent(endpoint, k -> new LongAdder()).increment();
            }
            
            // 持久化到Redis
            String key = METRICS_PREFIX + endpoint;
            Map<String, Object> metrics = Map.of(
                    "requestCount", requestCounts.get(endpoint).sum(),
                    "totalResponseTime", responseTimes.get(endpoint).sum(),
                    "maxResponseTime", maxResponseTimes.get(endpoint).get(),
                    "errorCount", errorCounts.getOrDefault(endpoint, new LongAdder()).sum(),
                    "lastUpdate", Instant.now().toEpochMilli()
            );
            
            return new MetricsData(key, metrics);
        })
        .flatMap(data -> {
            try {
                String metricsJson = objectMapper.writeValueAsString(data.metrics);
                return reactiveRedisTemplate.opsForValue()
                    .set(data.key, metricsJson, Duration.ofHours(24))
                    .thenReturn((Void) null);
            } catch (Exception e) {
                log.warn("Failed to persist metrics to Redis", e);
                return Mono.empty();
            }
        })
        .then();
    }

    @Override
    public Mono<PerformanceMetrics> getMetrics(String serviceName) {
        log.debug("Getting metrics for service: {}", serviceName);
        
        return Mono.fromCallable(() -> {
            long requestCount = requestCounts.getOrDefault(serviceName, new LongAdder()).sum();
            long totalResponseTime = responseTimes.getOrDefault(serviceName, new LongAdder()).sum();
            long maxResponseTime = maxResponseTimes.getOrDefault(serviceName, new AtomicLong(0)).get();
            long errorCount = errorCounts.getOrDefault(serviceName, new LongAdder()).sum();
            
            double averageResponseTime = requestCount > 0 ? (double) totalResponseTime / requestCount : 0.0;
            double errorRate = requestCount > 0 ? (double) errorCount / requestCount : 0.0;
            
            // 获取系统指标
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long memoryUsage = memoryBean.getHeapMemoryUsage().getUsed();
            
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getProcessCpuLoad();
            
            // 获取自定义指标
            Map<String, Object> serviceCustomMetrics = new HashMap<>();
            customMetrics.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(serviceName + ":"))
                    .forEach(entry -> {
                        String key = entry.getKey().substring(serviceName.length() + 1);
                        serviceCustomMetrics.put(key, entry.getValue());
                    });
            
            return new PerformanceMetrics(
                    serviceName,
                    requestCount,
                    averageResponseTime,
                    maxResponseTime,
                    errorRate,
                    memoryUsage,
                    cpuUsage,
                    serviceCustomMetrics
            );
        });
    }

    @Override
    public Mono<Map<String, PerformanceMetrics>> getAllMetrics() {
        log.debug("Getting all performance metrics");
        
        return Mono.fromCallable(() -> {
            Map<String, PerformanceMetrics> allMetrics = new HashMap<>();
            
            // 获取所有已记录的服务
            for (String service : requestCounts.keySet()) {
                PerformanceMetrics metrics = getMetrics(service).block();
                if (metrics != null) {
                    allMetrics.put(service, metrics);
                }
            }
            
            return allMetrics;
        });
    }

    @Override
    public Mono<Void> recordCustomMetric(String name, Object value) {
        log.debug("Recording custom metric: {}={}", name, value);
        
        return Mono.fromCallable(() -> {
            customMetrics.put(name, value);
            
            // 持久化到Redis
            String key = CUSTOM_METRICS_PREFIX + name;
            Map<String, Object> valueData = Map.of(
                    "value", value,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            return new CustomMetricData(key, valueData);
        })
        .flatMap(data -> {
            try {
                String valueJson = objectMapper.writeValueAsString(data.valueData);
                return reactiveRedisTemplate.opsForValue()
                    .set(data.key, valueJson, Duration.ofHours(24))
                    .thenReturn((Void) null);
            } catch (Exception e) {
                log.warn("Failed to persist custom metric to Redis", e);
                return Mono.empty();
            }
        })
        .then();
    }

    @Override
    public Mono<Map<String, Object>> getHealthStatus() {
        log.debug("Getting system health status");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> healthStatus = new HashMap<>();
            
            // 系统基本信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
            
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getProcessCpuLoad() * 100;
            
            healthStatus.put("status", "UP");
            healthStatus.put("timestamp", Instant.now().toEpochMilli());
            
            // 内存信息
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("total", totalMemory);
            memoryInfo.put("free", freeMemory);
            memoryInfo.put("used", usedMemory);
            memoryInfo.put("usagePercent", memoryUsagePercent);
            healthStatus.put("memory", memoryInfo);
            
            // CPU信息
            Map<String, Object> cpuInfo = new HashMap<>();
            cpuInfo.put("usage", cpuUsage);
            cpuInfo.put("cores", osBean.getAvailableProcessors());
            healthStatus.put("cpu", cpuInfo);
            
            // 服务统计
            Map<String, Object> serviceStats = new HashMap<>();
            serviceStats.put("totalServices", requestCounts.size());
            serviceStats.put("totalRequests", requestCounts.values().stream().mapToLong(LongAdder::sum).sum());
            serviceStats.put("totalErrors", errorCounts.values().stream().mapToLong(LongAdder::sum).sum());
            healthStatus.put("services", serviceStats);
            
            // 健康状态评估
            if (memoryUsagePercent > 90 || cpuUsage > 90) {
                healthStatus.put("status", "CRITICAL");
            } else if (memoryUsagePercent > 70 || cpuUsage > 70) {
                healthStatus.put("status", "WARNING");
            }
            
            return healthStatus;
        });
    }

    @Override
    public Mono<Void> recordError(String endpoint, String errorType, String message) {
        log.debug("Recording error: endpoint={}, type={}, message={}", endpoint, errorType, message);
        
        return Mono.fromCallable(() -> {
            errorCounts.computeIfAbsent(endpoint, k -> new LongAdder()).increment();
            
            // 记录错误详情到Redis
            String key = ERRORS_PREFIX + endpoint + ":" + Instant.now().toEpochMilli();
            Map<String, Object> errorInfo = Map.of(
                    "endpoint", endpoint,
                    "errorType", errorType,
                    "message", message,
                    "timestamp", Instant.now().toEpochMilli()
            );
            
            return new ErrorData(key, errorInfo);
        })
        .flatMap(data -> {
            try {
                String errorJson = objectMapper.writeValueAsString(data.errorInfo);
                return reactiveRedisTemplate.opsForValue()
                    .set(data.key, errorJson, Duration.ofDays(7))
                    .thenReturn((Void) null);
            } catch (Exception e) {
                log.warn("Failed to persist error to Redis", e);
                return Mono.empty();
            }
        })
        .then();
    }

    @Override
    public Mono<Map<String, Object>> getErrorStats() {
        log.debug("Getting error statistics");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> errorStats = new HashMap<>();
            
            // 按端点统计错误
            Map<String, Long> errorsByEndpoint = new HashMap<>();
            for (Map.Entry<String, LongAdder> entry : errorCounts.entrySet()) {
                errorsByEndpoint.put(entry.getKey(), entry.getValue().sum());
            }
            
            // 计算总错误数
            long totalErrors = errorCounts.values().stream().mapToLong(LongAdder::sum).sum();
            long totalRequests = requestCounts.values().stream().mapToLong(LongAdder::sum).sum();
            double overallErrorRate = totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0;
            
            errorStats.put("totalErrors", totalErrors);
            errorStats.put("totalRequests", totalRequests);
            errorStats.put("overallErrorRate", overallErrorRate);
            errorStats.put("errorsByEndpoint", errorsByEndpoint);
            errorStats.put("timestamp", Instant.now().toEpochMilli());
            
            return errorStats;
        });
    }
    
    /**
     * 指标数据内部类
     */
    private record MetricsData(String key, Map<String, Object> metrics) {}
    
    /**
     * 自定义指标数据内部类
     */
    private record CustomMetricData(String key, Map<String, Object> valueData) {}
    
    /**
     * 错误数据内部类
     */
    private record ErrorData(String key, Map<String, Object> errorInfo) {}
}