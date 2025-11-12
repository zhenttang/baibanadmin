package com.yunke.backend.monitor;

import com.yunke.backend.infrastructure.config.MonitoringConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.Tags;

/**
 * 指标收集器
 * 收集自定义业务指标
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsCollector {

    private final MeterRegistry meterRegistry;
    private final MonitoringConfig monitoringConfig;

    // 基础指标
    private Counter httpRequestsTotal;
    private Counter httpErrorsTotal;
    private Timer httpRequestDuration;
    
    // 业务指标
    private Counter userOperations;
    private Counter workspaceOperations;
    private Counter docOperations;
    private Counter aiOperations;
    
    // 系统指标
    private AtomicLong activeConnections = new AtomicLong(0);
    private AtomicLong activeWebsockets = new AtomicLong(0);
    private AtomicLong queueSize = new AtomicLong(0);

    @PostConstruct
    public void initializeMetrics() {
        log.info("Initializing custom metrics...");
        
        // HTTP 基础指标
        httpRequestsTotal = Counter.builder("http_requests_total")
                .description("Total HTTP requests")
                .register(meterRegistry);
        
        httpErrorsTotal = Counter.builder("http_errors_total")
                .description("Total HTTP errors")
                .register(meterRegistry);
        
        httpRequestDuration = Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration")
                .register(meterRegistry);
        
        // 业务指标
        userOperations = Counter.builder("user_operations_total")
                .description("Total user operations")
                .register(meterRegistry);
        
        workspaceOperations = Counter.builder("workspace_operations_total")
                .description("Total workspace operations")
                .register(meterRegistry);
        
        docOperations = Counter.builder("doc_operations_total")
                .description("Total document operations")
                .register(meterRegistry);
        
        aiOperations = Counter.builder("ai_operations_total")
                .description("Total AI operations")
                .register(meterRegistry);
        
        // 系统指标
        Gauge.builder("active_connections", activeConnections, AtomicLong::get)
                .description("Number of active connections")
                .register(meterRegistry);
        
        Gauge.builder("active_websockets", activeWebsockets, AtomicLong::get)
                .description("Number of active WebSocket connections")
                .register(meterRegistry);
        
        Gauge.builder("queue_size", queueSize, AtomicLong::get)
                .description("Current queue size")
                .register(meterRegistry);
        
        log.info("Custom metrics initialized successfully");
    }

    /**
     * 记录 HTTP 请求
     */
    public void recordHttpRequest(String method, String endpoint, int status, Duration duration) {
        Tags tags = Tags.of(
            "method", method,
            "endpoint", endpoint,
            "status", String.valueOf(status)
        );
        
        httpRequestsTotal.increment();
        
        httpRequestDuration.record(duration.toMillis(), TimeUnit.MILLISECONDS);
        
        // 记录错误
        if (status >= 400) {
            httpErrorsTotal.increment();
            monitoringConfig.getErrorCount().incrementAndGet();
        }
        
        monitoringConfig.getRequestCount().incrementAndGet();
    }

    /**
     * 记录用户操作
     */
    public void recordUserOperation(String operation, String userId) {
        userOperations.increment();
    }

    /**
     * 记录工作空间操作
     */
    public void recordWorkspaceOperation(String operation, String workspaceId) {
        workspaceOperations.increment();
    }

    /**
     * 记录文档操作
     */
    public void recordDocOperation(String operation, String docId) {
        docOperations.increment();
    }

    /**
     * 记录 AI 操作
     */
    public void recordAiOperation(String operation, String provider, String model) {
        aiOperations.increment();
    }

    /**
     * 增加活跃连接数
     */
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    /**
     * 减少活跃连接数
     */
    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    /**
     * 增加活跃 WebSocket 连接数
     */
    public void incrementActiveWebsockets() {
        activeWebsockets.incrementAndGet();
    }

    /**
     * 减少活跃 WebSocket 连接数
     */
    public void decrementActiveWebsockets() {
        activeWebsockets.decrementAndGet();
    }

    /**
     * 设置队列大小
     */
    public void setQueueSize(long size) {
        queueSize.set(size);
    }

    /**
     * 获取活跃连接数
     */
    public long getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * 获取活跃 WebSocket 连接数
     */
    public long getActiveWebsockets() {
        return activeWebsockets.get();
    }

    /**
     * 获取队列大小
     */
    public long getQueueSize() {
        return queueSize.get();
    }
}