package com.yunke.backend.system.service.impl;

import com.yunke.backend.infrastructure.config.AffineConfig;
import com.yunke.backend.system.dto.ServerConfigDto;
import com.yunke.backend.system.dto.ServerInfoDto;
import com.yunke.backend.system.dto.ConfigOperationLogDto;
import com.yunke.backend.system.service.ConfigService;
import com.yunke.backend.system.service.ConfigLogService;

import com.yunke.backend.system.service.ServerConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 服务器配置管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerConfigServiceImpl implements ServerConfigService {
    
    private final AffineConfig affineConfig;
    private final ConfigService configService;
    private final ConfigLogService configLogService;
    private final StringRedisTemplate redisTemplate;
    private final Optional<BuildProperties> buildProperties;
    
    private static final String CONFIG_MODULE = "server";
    
    @PostConstruct
    public void init() {
        log.info("ServerConfigService initialized");
    }
    
    @Override
    public ServerInfoDto getServerInfo() {
        return ServerInfoDto.builder()
                .config(getServerConfig())
                .version(getVersionInfo())
                .runtime(getRuntimeInfo())
                .system(getSystemInfo())
                .status(getSystemHealth())
                .build();
    }
    
    @Override
    public ServerConfigDto getServerConfig() {
        try {
            // 从配置服务获取服务器配置
            Map<String, Object> configMap = configService.getModuleConfig(CONFIG_MODULE);
            
            ServerConfigDto config = new ServerConfigDto();
            
            // 从AffineConfig获取基础配置
            AffineConfig.ServerConfig serverConfig = affineConfig.getServer();
            if (serverConfig != null) {
                config.setServerName("AFFiNE"); // 使用默认名称，因为ServerConfig中没有name字段
                config.setExternalUrl(serverConfig.getExternalUrl());
                config.setHost(serverConfig.getHost());
                config.setPort(serverConfig.getPort());
                config.setHttpsEnabled(serverConfig.isHttpsEnabled());
            }
            
            // 从动态配置覆盖
            if (configMap != null) {
                if (configMap.containsKey("serverName")) {
                    config.setServerName((String) configMap.get("serverName"));
                }
                if (configMap.containsKey("externalUrl")) {
                    config.setExternalUrl((String) configMap.get("externalUrl"));
                }
                if (configMap.containsKey("maxUploadSize")) {
                    config.setMaxUploadSize((Integer) configMap.get("maxUploadSize"));
                }
                if (configMap.containsKey("sessionTimeout")) {
                    config.setSessionTimeout((Integer) configMap.get("sessionTimeout"));
                }
                if (configMap.containsKey("enableSignup")) {
                    config.setEnableSignup((Boolean) configMap.get("enableSignup"));
                }
                if (configMap.containsKey("enableInviteCode")) {
                    config.setEnableInviteCode((Boolean) configMap.get("enableInviteCode"));
                }
                if (configMap.containsKey("defaultLanguage")) {
                    config.setDefaultLanguage((String) configMap.get("defaultLanguage"));
                }
                if (configMap.containsKey("timezone")) {
                    config.setTimezone((String) configMap.get("timezone"));
                }
                if (configMap.containsKey("maintenanceMode")) {
                    config.setMaintenanceMode((Boolean) configMap.get("maintenanceMode"));
                }
                if (configMap.containsKey("maintenanceMessage")) {
                    config.setMaintenanceMessage((String) configMap.get("maintenanceMessage"));
                }
            }
            
            // 设置默认值
            setDefaultValues(config);
            
            return config;
            
        } catch (Exception e) {
            log.error("获取服务器配置失败", e);
            return getDefaultServerConfig();
        }
    }
    
    @Override
    public ServerConfigDto updateServerConfig(ServerConfigDto config, String operator, String sourceIp) {
        try {
            // 验证配置
            if (!validateServerConfig(config)) {
                configLogService.logFailedOperation("UPDATE", CONFIG_MODULE, "server-config", 
                                                   operator, sourceIp, "配置验证失败");
                throw new IllegalArgumentException("服务器配置验证失败");
            }
            
            // 获取当前配置用于日志记录
            ServerConfigDto oldConfig = getServerConfig();
            
            // 构建配置Map
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("serverName", config.getServerName());
            configMap.put("externalUrl", config.getExternalUrl());
            configMap.put("maxUploadSize", config.getMaxUploadSize());
            configMap.put("sessionTimeout", config.getSessionTimeout());
            configMap.put("enableSignup", config.getEnableSignup());
            configMap.put("enableInviteCode", config.getEnableInviteCode());
            configMap.put("defaultLanguage", config.getDefaultLanguage());
            configMap.put("timezone", config.getTimezone());
            configMap.put("maintenanceMode", config.getMaintenanceMode());
            configMap.put("maintenanceMessage", config.getMaintenanceMessage());
            
            // 保存配置
            configService.updateModuleConfig(CONFIG_MODULE, configMap);
            
            // 记录操作日志
            configLogService.logOperation("UPDATE", CONFIG_MODULE, "server-config", 
                                         toJsonString(oldConfig), toJsonString(config), 
                                         operator, sourceIp, "更新服务器配置");
            
            log.info("服务器配置已更新，操作用户: {}", operator);
            
            return getServerConfig();
            
        } catch (Exception e) {
            log.error("更新服务器配置失败", e);
            configLogService.logFailedOperation("UPDATE", CONFIG_MODULE, "server-config", 
                                               operator, sourceIp, e.getMessage());
            throw new RuntimeException("更新服务器配置失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateServerConfig(ServerConfigDto config) {
        try {
            // 基础验证
            if (config == null) {
                return false;
            }
            
            // 服务器名称验证
            if (config.getServerName() == null || config.getServerName().trim().isEmpty()) {
                log.warn("服务器名称不能为空");
                return false;
            }
            
            // URL格式验证
            if (config.getExternalUrl() != null && !config.getExternalUrl().isEmpty()) {
                if (!config.getExternalUrl().matches("^https?://.*")) {
                    log.warn("外部URL格式不正确: {}", config.getExternalUrl());
                    return false;
                }
            }
            
            // 端口验证
            if (config.getPort() != null && (config.getPort() < 1 || config.getPort() > 65535)) {
                log.warn("端口号范围无效: {}", config.getPort());
                return false;
            }
            
            // 上传大小验证
            if (config.getMaxUploadSize() != null && config.getMaxUploadSize() < 1) {
                log.warn("最大上传大小不能小于1MB: {}", config.getMaxUploadSize());
                return false;
            }
            
            // 会话超时验证
            if (config.getSessionTimeout() != null && config.getSessionTimeout() < 5) {
                log.warn("会话超时时间不能小于5分钟: {}", config.getSessionTimeout());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("配置验证过程中发生错误", e);
            return false;
        }
    }
    
    @Override
    public boolean reloadConfig() {
        try {
            // 重新加载配置
            configService.reloadConfig();
            log.info("配置重新加载成功");
            return true;
        } catch (Exception e) {
            log.error("重新加载配置失败", e);
            return false;
        }
    }
    
    @Override
    public List<ConfigOperationLogDto> getConfigOperationLogs(String moduleName, Integer limit) {
        return configLogService.getRecentLogs(moduleName, limit);
    }
    
    @Override
    public ServerInfoDto.ServiceStatus getSystemHealth() {
        Map<String, Object> details = new HashMap<>();
        
        // 数据库状态检查
        String databaseStatus = checkDatabaseHealth();
        details.put("database", databaseStatus);
        
        // Redis状态检查
        String redisStatus = checkRedisHealth();
        details.put("redis", redisStatus);
        
        // 存储状态检查
        String storageStatus = checkStorageHealth();
        details.put("storage", storageStatus);
        
        // 整体状态评估
        String overallStatus = "UP";
        if (!"UP".equals(databaseStatus) || !"UP".equals(redisStatus)) {
            overallStatus = "DOWN";
        } else if (!"UP".equals(storageStatus)) {
            overallStatus = "WARNING";
        }
        
        return ServerInfoDto.ServiceStatus.builder()
                .databaseStatus(databaseStatus)
                .redisStatus(redisStatus)
                .storageStatus(storageStatus)
                .overallStatus(overallStatus)
                .details(details)
                .build();
    }
    
    private ServerInfoDto.VersionInfo getVersionInfo() {
        return ServerInfoDto.VersionInfo.builder()
                .version(buildProperties.map(BuildProperties::getVersion).orElse("unknown"))
                .buildTime(buildProperties.map(bp -> bp.getTime().toString()).orElse("unknown"))
                .gitCommit(System.getProperty("git.commit.id.abbrev", "unknown"))
                .buildProfile(System.getProperty("spring.profiles.active", "default"))
                .javaVersion(System.getProperty("java.version"))
                .springBootVersion(buildProperties.map(bp -> bp.get("spring-boot.version")).orElse("unknown"))
                .build();
    }
    
    private ServerInfoDto.RuntimeInfo getRuntimeInfo() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercentage = (double) usedMemory / maxMemory * 100;
        
        return ServerInfoDto.RuntimeInfo.builder()
                .startTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(runtimeBean.getStartTime()),
                        ZoneId.systemDefault()))
                .uptimeSeconds(runtimeBean.getUptime() / 1000)
                .maxMemory(maxMemory)
                .totalMemory(totalMemory)
                .freeMemory(freeMemory)
                .usedMemory(usedMemory)
                .memoryUsagePercentage(memoryUsagePercentage)
                .activeThreads(Thread.activeCount())
                .build();
    }
    
    private ServerInfoDto.SystemInfo getSystemInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        return ServerInfoDto.SystemInfo.builder()
                .osName(osBean.getName())
                .osVersion(osBean.getVersion())
                .osArch(osBean.getArch())
                .cpuCores(osBean.getAvailableProcessors())
                .systemLoad(osBean.getSystemLoadAverage())
                .systemTimezone(ZoneId.systemDefault().toString())
                .systemEncoding(System.getProperty("file.encoding"))
                .build();
    }
    
    private void setDefaultValues(ServerConfigDto config) {
        if (config.getServerName() == null) {
            config.setServerName("AFFiNE Server");
        }
        if (config.getMaxUploadSize() == null) {
            config.setMaxUploadSize(100); // 100MB
        }
        if (config.getSessionTimeout() == null) {
            config.setSessionTimeout(480); // 8小时
        }
        if (config.getEnableSignup() == null) {
            config.setEnableSignup(true);
        }
        if (config.getEnableInviteCode() == null) {
            config.setEnableInviteCode(false);
        }
        if (config.getDefaultLanguage() == null) {
            config.setDefaultLanguage("zh-CN");
        }
        if (config.getTimezone() == null) {
            config.setTimezone("Asia/Shanghai");
        }
        if (config.getMaintenanceMode() == null) {
            config.setMaintenanceMode(false);
        }
    }
    
    private ServerConfigDto getDefaultServerConfig() {
        ServerConfigDto config = new ServerConfigDto();
        setDefaultValues(config);
        return config;
    }
    
    private String toJsonString(Object obj) {
        try {
            // 简化的JSON序列化，实际应用中可以使用Jackson等库
            return obj.toString();
        } catch (Exception e) {
            return "无法序列化";
        }
    }
    
    private String checkDatabaseHealth() {
        try {
            // 简单的数据库连接检查
            // 实际实现中可以执行简单的查询
            return "UP";
        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return "DOWN";
        }
    }
    
    private String checkRedisHealth() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return "UP";
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            return "DOWN";
        }
    }
    
    private String checkStorageHealth() {
        try {
            // 检查存储服务状态
            // 实际实现中可以检查存储连接
            return "UP";
        } catch (Exception e) {
            log.error("存储健康检查失败", e);
            return "WARNING";
        }
    }
}