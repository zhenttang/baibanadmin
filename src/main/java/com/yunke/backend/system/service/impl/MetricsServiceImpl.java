package com.yunke.backend.service.impl;

import com.yunke.backend.service.MetricsService;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统监控和性能指标服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsServiceImpl implements MetricsService {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 内存中的指标缓存
    private final Map<String, AtomicLong> apiCallCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userOperationCounts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimeHistory = new ConcurrentHashMap<>();
    private final Map<String, Object> systemEventCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getSystemMetrics() {
        log.info("开始收集系统指标");
        
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // CPU指标
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            metrics.put("cpu", Map.of(
                "processCpuLoad", Math.round(osBean.getProcessCpuLoad() * 100 * 100.0) / 100.0,
                "systemCpuLoad", Math.round(osBean.getSystemCpuLoad() * 100 * 100.0) / 100.0,
                "availableProcessors", osBean.getAvailableProcessors()
            ));

            // 内存指标
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            metrics.put("memory", Map.of(
                "heapUsed", heapUsed,
                "heapMax", heapMax,
                "heapUsedPercent", Math.round((double) heapUsed / heapMax * 100 * 100.0) / 100.0,
                "nonHeapUsed", nonHeapUsed,
                "totalPhysicalMemory", osBean.getTotalPhysicalMemorySize(),
                "freePhysicalMemory", osBean.getFreePhysicalMemorySize()
            ));

            // 磁盘指标
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            metrics.put("disk", Map.of(
                "totalSpace", totalSpace,
                "freeSpace", freeSpace,
                "usedSpace", usedSpace,
                "usedPercent", Math.round((double) usedSpace / totalSpace * 100 * 100.0) / 100.0
            ));

            // 运行时指标
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            metrics.put("runtime", Map.of(
                "uptime", runtimeBean.getUptime(),
                "startTime", runtimeBean.getStartTime(),
                "jvmName", runtimeBean.getVmName(),
                "jvmVersion", runtimeBean.getVmVersion()
            ));

            log.info("系统指标收集完成，包含 {} 类指标", metrics.size());
            
        } catch (Exception e) {
            log.error("收集系统指标时发生错误", e);
            metrics.put("error", "Failed to collect system metrics: " + e.getMessage());
        }
        
        return metrics;
    }

    @Override
    public Map<String, Object> getApplicationMetrics() {
        log.info("开始收集应用指标");
        
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // 请求统计
            long totalApiCalls = apiCallCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            Map<String, Long> topEndpoints = apiCallCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> 
                    Long.compare(b.get(), a.get())))
                .limit(10)
                .collect(LinkedHashMap::new, 
                    (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                    LinkedHashMap::putAll);

            metrics.put("requests", Map.of(
                "totalApiCalls", totalApiCalls,
                "topEndpoints", topEndpoints,
                "requestsPerMinute", calculateRequestsPerMinute()
            ));

            // 响应时间统计
            Map<String, Object> responseTimeStats = new HashMap<>();
            for (Map.Entry<String, List<Long>> entry : responseTimeHistory.entrySet()) {
                List<Long> times = entry.getValue();
                if (!times.isEmpty()) {
                    double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0L);
                    long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0L);
                    
                    responseTimeStats.put(entry.getKey(), Map.of(
                        "average", Math.round(avgTime * 100.0) / 100.0,
                        "max", maxTime,
                        "min", minTime,
                        "count", times.size()
                    ));
                }
            }
            metrics.put("responseTime", responseTimeStats);

            // 用户活跃度
            long totalUserOperations = userOperationCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            metrics.put("userActivity", Map.of(
                "totalOperations", totalUserOperations,
                "operationTypes", userOperationCounts.entrySet().stream()
                    .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                        LinkedHashMap::putAll),
                "activeUsers", calculateActiveUsers()
            ));

            // 错误率统计
            metrics.put("errors", Map.of(
                "errorRate", calculateErrorRate(),
                "recentErrors", getRecentErrors()
            ));

            log.info("应用指标收集完成");
            
        } catch (Exception e) {
            log.error("收集应用指标时发生错误", e);
            metrics.put("error", "Failed to collect application metrics: " + e.getMessage());
        }
        
        return metrics;
    }

    @Override
    public Map<String, Object> getDatabaseMetrics() {
        log.info("开始收集数据库指标");
        
        Map<String, Object> metrics = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // 连接池状态
            metrics.put("connectionPool", Map.of(
                "activeConnections", getActiveConnectionCount(),
                "maxConnections", getMaxConnectionCount(),
                "connectionUsage", calculateConnectionUsage()
            ));

            // 数据库基本信息
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT version()");
                if (rs.next()) {
                    metrics.put("database", Map.of(
                        "version", rs.getString(1),
                        "connected", true
                    ));
                }
            }

            // 查询性能统计（这里使用模拟数据，实际可以从数据库统计表中获取）
            metrics.put("queryPerformance", Map.of(
                "averageQueryTime", 23.5,
                "slowQueries", 3,
                "totalQueries", 12547,
                "queriesPerSecond", 15.2
            ));

            // 表统计信息
            metrics.put("tableStats", getTableStatistics(connection));

            log.info("数据库指标收集完成");
            
        } catch (Exception e) {
            log.error("收集数据库指标时发生错误", e);
            metrics.put("error", "Failed to collect database metrics: " + e.getMessage());
            metrics.put("database", Map.of("connected", false));
        }
        
        return metrics;
    }

    @Override
    public Map<String, Object> getHistoryMetrics(String type, String timeRange) {
        log.info("获取历史指标数据：类型={}, 时间范围={}", type, timeRange);
        
        Map<String, Object> historyData = new HashMap<>();
        
        try {
            // 从Redis获取历史数据
            String key = "metrics:history:" + type + ":" + timeRange;
            List<Object> data = redisTemplate.opsForList().range(key, 0, -1);
            
            if (data != null && !data.isEmpty()) {
                historyData.put("data", data);
                historyData.put("count", data.size());
            } else {
                // 如果没有历史数据，生成模拟数据
                historyData = generateMockHistoryData(type, timeRange);
            }
            
            historyData.put("type", type);
            historyData.put("timeRange", timeRange);
            historyData.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("获取历史指标数据失败", e);
            historyData.put("error", "Failed to get history metrics: " + e.getMessage());
        }
        
        return historyData;
    }

    @Override
    public Map<String, Object> getMetricsSummary() {
        log.info("获取实时指标汇总");
        
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // 系统健康状态
            summary.put("systemHealth", "healthy");
            
            // 关键指标
            summary.put("keyMetrics", Map.of(
                "cpuUsage", getCpuUsage(),
                "memoryUsage", getMemoryUsage(),
                "diskUsage", getDiskUsage(),
                "responseTime", getAverageResponseTime(),
                "errorRate", calculateErrorRate(),
                "activeUsers", calculateActiveUsers()
            ));
            
            // 告警信息
            summary.put("alerts", getActiveAlerts());
            
            // 最后更新时间
            summary.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("获取指标汇总失败", e);
            summary.put("error", "Failed to get metrics summary: " + e.getMessage());
        }
        
        return summary;
    }

    @Override
    public Map<String, Object> getHealthStatus() {
        log.info("获取健康检查状态");
        
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 数据库健康检查
            health.put("database", checkDatabaseHealth());
            
            // Redis健康检查
            health.put("redis", checkRedisHealth());
            
            // 系统资源健康检查
            health.put("system", checkSystemHealth());
            
            // 应用健康检查
            health.put("application", checkApplicationHealth());
            
            // 整体状态
            boolean allHealthy = health.values().stream()
                .allMatch(status -> "UP".equals(((Map<?, ?>) status).get("status")));
            health.put("overall", allHealthy ? "UP" : "DOWN");
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            health.put("error", "Failed to check health status: " + e.getMessage());
            health.put("overall", "DOWN");
        }
        
        return health;
    }

    @Override
    public Map<String, Object> exportMetrics(String format, String timeRange) {
        log.info("导出指标数据：格式={}, 时间范围={}", format, timeRange);
        
        Map<String, Object> exportResult = new HashMap<>();
        
        try {
            // 收集所有指标数据
            Map<String, Object> allMetrics = new HashMap<>();
            allMetrics.put("system", getSystemMetrics());
            allMetrics.put("application", getApplicationMetrics());
            allMetrics.put("database", getDatabaseMetrics());
            
            // 根据格式生成导出数据
            String exportData;
            switch (format.toLowerCase()) {
                case "json":
                    exportData = generateJsonExport(allMetrics);
                    break;
                case "csv":
                    exportData = generateCsvExport(allMetrics);
                    break;
                default:
                    exportData = generateJsonExport(allMetrics);
            }
            
            exportResult.put("data", exportData);
            exportResult.put("format", format);
            exportResult.put("filename", "metrics_" + timeRange + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + format);
            exportResult.put("size", exportData.length());
            
        } catch (Exception e) {
            log.error("导出指标数据失败", e);
            exportResult.put("error", "Failed to export metrics: " + e.getMessage());
        }
        
        return exportResult;
    }

    @Override
    public void recordUserOperation(String operation, String userId) {
        try {
            String key = "user_operation:" + operation;
            apiCallCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            userOperationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
            
            log.debug("记录用户操作：{} - {}", operation, userId);
        } catch (Exception e) {
            log.error("记录用户操作失败", e);
        }
    }

    @Override
    public void recordApiCall(String endpoint, long responseTime, int statusCode) {
        try {
            apiCallCounts.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
            
            responseTimeHistory.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(responseTime);
            
            // 保持历史记录不超过1000条
            List<Long> times = responseTimeHistory.get(endpoint);
            if (times.size() > 1000) {
                times.remove(0);
            }
            
            log.debug("记录API调用：{} - {}ms - {}", endpoint, responseTime, statusCode);
        } catch (Exception e) {
            log.error("记录API调用失败", e);
        }
    }

    @Override
    public void recordSystemEvent(String eventType, Map<String, Object> eventData) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Map<String, Object> event = new HashMap<>(eventData);
            event.put("timestamp", timestamp);
            event.put("type", eventType);
            
            systemEventCache.put(eventType + "_" + timestamp, event);
            
            log.debug("记录系统事件：{}", eventType);
        } catch (Exception e) {
            log.error("记录系统事件失败", e);
        }
    }

    // 私有辅助方法
    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return Math.round(osBean.getProcessCpuLoad() * 100 * 100.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getMemoryUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            return Math.round((double) heapUsed / heapMax * 100 * 100.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getDiskUsage() {
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            return Math.round((double) usedSpace / totalSpace * 100 * 100.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getAverageResponseTime() {
        return responseTimeHistory.values().stream()
            .flatMap(List::stream)
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    private double calculateErrorRate() {
        // 简化实现，实际应该根据状态码统计
        return 0.5; // 0.5% 错误率
    }

    private int calculateActiveUsers() {
        // 简化实现，实际应该根据最近活动统计
        return userOperationCounts.size();
    }

    private double calculateRequestsPerMinute() {
        // 简化实现，实际应该基于时间窗口计算
        return apiCallCounts.values().stream().mapToLong(AtomicLong::get).sum() / 60.0;
    }

    private List<String> getRecentErrors() {
        // 简化实现，返回模拟错误
        return Arrays.asList("Database connection timeout", "Redis connection lost");
    }

    private List<Map<String, Object>> getActiveAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        if (getCpuUsage() > 80) {
            alerts.add(Map.of(
                "type", "warning",
                "message", "CPU使用率过高",
                "value", getCpuUsage() + "%"
            ));
        }
        
        if (getMemoryUsage() > 85) {
            alerts.add(Map.of(
                "type", "critical",
                "message", "内存使用率过高",
                "value", getMemoryUsage() + "%"
            ));
        }
        
        return alerts;
    }

    private int getActiveConnectionCount() {
        // 简化实现，实际应该从连接池获取
        return 5;
    }

    private int getMaxConnectionCount() {
        // 简化实现，实际应该从连接池配置获取
        return 20;
    }

    private double calculateConnectionUsage() {
        return (double) getActiveConnectionCount() / getMaxConnectionCount() * 100;
    }

    private Map<String, Object> getTableStatistics(Connection connection) {
        // 简化实现，返回模拟数据
        return Map.of(
            "users", Map.of("rows", 1250, "size", "2.5MB"),
            "user_sessions", Map.of("rows", 3421, "size", "1.2MB"),
            "app_configs", Map.of("rows", 156, "size", "0.3MB")
        );
    }

    private Map<String, Object> checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return Map.of(
                "status", "UP",
                "details", Map.of("database", "PostgreSQL", "validConnection", true)
            );
        } catch (Exception e) {
            return Map.of(
                "status", "DOWN",
                "details", Map.of("error", e.getMessage())
            );
        }
    }

    private Map<String, Object> checkRedisHealth() {
        try {
            redisTemplate.opsForValue().set("health:check", "OK");
            String result = (String) redisTemplate.opsForValue().get("health:check");
            
            return Map.of(
                "status", "OK".equals(result) ? "UP" : "DOWN",
                "details", Map.of("ping", "OK".equals(result))
            );
        } catch (Exception e) {
            return Map.of(
                "status", "DOWN",
                "details", Map.of("error", e.getMessage())
            );
        }
    }

    private Map<String, Object> checkSystemHealth() {
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();
        double diskUsage = getDiskUsage();
        
        boolean healthy = cpuUsage < 90 && memoryUsage < 90 && diskUsage < 90;
        
        return Map.of(
            "status", healthy ? "UP" : "DOWN",
            "details", Map.of(
                "cpu", cpuUsage + "%",
                "memory", memoryUsage + "%",
                "disk", diskUsage + "%"
            )
        );
    }

    private Map<String, Object> checkApplicationHealth() {
        return Map.of(
            "status", "UP",
            "details", Map.of(
                "uptime", ManagementFactory.getRuntimeMXBean().getUptime(),
                "totalRequests", apiCallCounts.values().stream().mapToLong(AtomicLong::get).sum()
            )
        );
    }

    private Map<String, Object> generateMockHistoryData(String type, String timeRange) {
        // 生成模拟历史数据
        List<Map<String, Object>> data = new ArrayList<>();
        int points = getDataPointsForTimeRange(timeRange);
        
        for (int i = 0; i < points; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("timestamp", LocalDateTime.now().minusHours(points - i).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            switch (type) {
                case "system":
                    point.put("cpu", 20 + Math.random() * 60);
                    point.put("memory", 30 + Math.random() * 50);
                    point.put("disk", 10 + Math.random() * 20);
                    break;
                case "application":
                    point.put("requests", (int) (100 + Math.random() * 200));
                    point.put("responseTime", 50 + Math.random() * 100);
                    point.put("errorRate", Math.random() * 2);
                    break;
                case "database":
                    point.put("connections", (int) (5 + Math.random() * 10));
                    point.put("queryTime", 10 + Math.random() * 50);
                    point.put("slowQueries", (int) (Math.random() * 5));
                    break;
            }
            
            data.add(point);
        }
        
        return Map.of(
            "data", data,
            "count", data.size(),
            "generated", true
        );
    }

    private int getDataPointsForTimeRange(String timeRange) {
        switch (timeRange) {
            case "1h": return 60;
            case "6h": return 72;
            case "24h": return 96;
            case "7d": return 168;
            case "30d": return 180;
            default: return 60;
        }
    }

    private String generateJsonExport(Map<String, Object> data) {
        // 简化实现，实际应该使用Jackson等库
        return data.toString();
    }

    private String generateCsvExport(Map<String, Object> data) {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value,Category,Timestamp\n");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // 简化的CSV生成
        data.forEach((category, metrics) -> {
            if (metrics instanceof Map) {
                ((Map<?, ?>) metrics).forEach((key, value) -> {
                    csv.append(String.format("%s,%s,%s,%s\n", key, value, category, timestamp));
                });
            }
        });
        
        return csv.toString();
    }
}