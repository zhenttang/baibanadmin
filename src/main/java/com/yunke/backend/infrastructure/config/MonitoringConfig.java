package com.yunke.backend.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控配置
 * 对应 Node.js 版本的 OpenTelemetry 配置
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MonitoringConfig {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AffineConfig affineConfig;
    
    // 应用启动时间
    private final Instant startTime = Instant.now();
    
    // 请求计数器
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * Prometheus 注册表
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * 自定义指标配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().commonTags(
                "application", "yunke-java-backend",
                "environment", System.getProperty("spring.profiles.active", "dev"),
                "version", "0.21.0",
                "flavor", affineConfig.getServer().getFlavor().name(),
                "deployment", affineConfig.getServer().getDeployment().name()
            );
        };
    }

    /**
     * JVM 指标
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    @Bean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }

    /**
     * 数据库健康检查
     */
    @Bean
    public HealthIndicator databaseHealthIndicator() {
        return () -> {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    return org.springframework.boot.actuate.health.Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("url", affineConfig.getDatabase().getUrl())
                            .withDetail("status", "Connected")
                            .build();
                } else {
                    return org.springframework.boot.actuate.health.Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("error", "Connection validation failed")
                            .build();
                }
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Redis 健康检查
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                String pong = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .ping();
                
                if ("PONG".equals(pong)) {
                    return org.springframework.boot.actuate.health.Health.up()
                            .withDetail("redis", "Connected")
                            .withDetail("host", affineConfig.getRedis().getHost())
                            .withDetail("port", affineConfig.getRedis().getPort())
                            .withDetail("database", affineConfig.getRedis().getDatabase())
                            .build();
                } else {
                    return org.springframework.boot.actuate.health.Health.down()
                            .withDetail("redis", "Ping failed")
                            .withDetail("response", pong)
                            .build();
                }
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("redis", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * 应用健康检查
     */
    @Bean
    public HealthIndicator applicationHealthIndicator() {
        return () -> {
            try {
                Duration uptime = Duration.between(startTime, Instant.now());
                
                // 检查基本系统状态
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                double memoryUsage = (double) usedMemory / totalMemory;
                
                // 内存使用率超过 90% 时报警
                Status status = memoryUsage > 0.9 ? Status.DOWN : Status.UP;
                
                return org.springframework.boot.actuate.health.Health.status(status)
                        .withDetail("uptime", uptime.toString())
                        .withDetail("memory", Map.of(
                                "total", totalMemory,
                                "free", freeMemory,
                                "used", usedMemory,
                                "usage", String.format("%.2f%%", memoryUsage * 100)
                        ))
                        .withDetail("processors", runtime.availableProcessors())
                        .withDetail("requests", requestCount.get())
                        .withDetail("errors", errorCount.get())
                        .build();
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * 存储健康检查
     */
    @Bean
    public HealthIndicator storageHealthIndicator() {
        return () -> {
            try {
                AffineConfig.StorageConfig storageConfig = affineConfig.getStorage();
                
                switch (storageConfig.getProvider()) {
                    case LOCAL:
                        // 检查本地存储路径
                        java.nio.file.Path localPath = java.nio.file.Paths.get(storageConfig.getLocalPath());
                        if (java.nio.file.Files.exists(localPath) && java.nio.file.Files.isWritable(localPath)) {
                            return org.springframework.boot.actuate.health.Health.up()
                                    .withDetail("storage", "Local")
                                    .withDetail("path", storageConfig.getLocalPath())
                                    .withDetail("writable", true)
                                    .build();
                        } else {
                            return org.springframework.boot.actuate.health.Health.down()
                                    .withDetail("storage", "Local")
                                    .withDetail("path", storageConfig.getLocalPath())
                                    .withDetail("error", "Path not writable")
                                    .build();
                        }
                    case S3:
                    case R2:
                    case COS:
                        // 对于云存储，检查配置是否完整
                        boolean configValid = storageConfig.getAccessKeyId() != null && 
                                             storageConfig.getSecretAccessKey() != null &&
                                             storageConfig.getBucket() != null;
                        
                        if (configValid) {
                            return org.springframework.boot.actuate.health.Health.up()
                                    .withDetail("storage", storageConfig.getProvider().name())
                                    .withDetail("bucket", storageConfig.getBucket())
                                    .withDetail("region", storageConfig.getRegion())
                                    .build();
                        } else {
                            return org.springframework.boot.actuate.health.Health.down()
                                    .withDetail("storage", storageConfig.getProvider().name())
                                    .withDetail("error", "Missing configuration")
                                    .build();
                        }
                    default:
                        return org.springframework.boot.actuate.health.Health.unknown()
                                .withDetail("storage", "Unknown provider")
                                .build();
                }
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("storage", "Health check failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * AI 服务健康检查
     */
    @Bean
    public HealthIndicator aiHealthIndicator() {
        return () -> {
            try {
                AffineConfig.CopilotConfig copilotConfig = affineConfig.getCopilot();
                
                if (!copilotConfig.isEnabled()) {
                    return org.springframework.boot.actuate.health.Health.up()
                            .withDetail("ai", "Disabled")
                            .build();
                }
                
                // 检查 AI 服务配置
                boolean openaiConfigured = copilotConfig.getOpenai().isEnabled() && 
                                         copilotConfig.getOpenai().getApiKey() != null;
                boolean anthropicConfigured = copilotConfig.getAnthropic().isEnabled() && 
                                            copilotConfig.getAnthropic().getApiKey() != null;
                boolean googleConfigured = copilotConfig.getGoogle().isEnabled() && 
                                         copilotConfig.getGoogle().getApiKey() != null;
                
                if (openaiConfigured || anthropicConfigured || googleConfigured) {
                    return org.springframework.boot.actuate.health.Health.up()
                            .withDetail("ai", "Configured")
                            .withDetail("openai", openaiConfigured)
                            .withDetail("anthropic", anthropicConfigured)
                            .withDetail("google", googleConfigured)
                            .build();
                } else {
                    return org.springframework.boot.actuate.health.Health.down()
                            .withDetail("ai", "Enabled but not configured")
                            .build();
                }
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("ai", "Health check failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * 获取请求计数器
     */
    public AtomicLong getRequestCount() {
        return requestCount;
    }

    /**
     * 获取错误计数器
     */
    public AtomicLong getErrorCount() {
        return errorCount;
    }
}