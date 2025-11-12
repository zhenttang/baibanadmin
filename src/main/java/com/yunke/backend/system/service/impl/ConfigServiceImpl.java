package com.yunke.backend.system.service.impl;

import com.yunke.backend.system.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置服务实现类
 * 使用Redis作为配置存储，内存作为缓存
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 配置前缀
    private static final String CONFIG_PREFIX = "affine:config:";
    
    // 内存缓存（作为Redis的备选方案）
    private final Map<String, Map<String, Object>> memoryCache = new ConcurrentHashMap<>();

    @Override
    @Cacheable(value = "moduleConfig", key = "#module")
    public Map<String, Object> getModuleConfig(String module) {
        log.info("获取模块配置: {}", module);
        
        try {
            String key = CONFIG_PREFIX + module;
            Object config = redisTemplate.opsForValue().get(key);
            
            if (config != null) {
                if (config instanceof Map) {
                    return (Map<String, Object>) config;
                } else {
                    // 尝试转换为Map
                    return objectMapper.convertValue(config, Map.class);
                }
            }
            
            // Redis中没有配置，尝试从内存缓存获取
            Map<String, Object> memoryConfig = memoryCache.get(module);
            if (memoryConfig != null) {
                log.info("从内存缓存获取配置: {}", module);
                return new HashMap<>(memoryConfig);
            }
            
            // 返回默认配置
            Map<String, Object> defaultConfig = getDefaultConfig(module);
            log.info("使用默认配置: {}", module);
            return defaultConfig;
            
        } catch (Exception e) {
            log.error("获取模块配置失败: {}", module, e);
            
            // 发生错误时，尝试从内存缓存获取
            Map<String, Object> memoryConfig = memoryCache.get(module);
            if (memoryConfig != null) {
                return new HashMap<>(memoryConfig);
            }
            
            return getDefaultConfig(module);
        }
    }

    @Override
    @CacheEvict(value = "moduleConfig", key = "#module")
    public void updateModuleConfig(String module, Map<String, Object> config) {
        log.info("更新模块配置: {} = {}", module, config);
        
        try {
            String key = CONFIG_PREFIX + module;
            redisTemplate.opsForValue().set(key, config);
            
            // 同时更新内存缓存
            memoryCache.put(module, new HashMap<>(config));
            
            log.info("模块配置更新成功: {}", module);
        } catch (Exception e) {
            log.error("更新模块配置失败: {}", module, e);
            
            // Redis失败时，至少更新内存缓存
            memoryCache.put(module, new HashMap<>(config));
            log.warn("Redis更新失败，仅更新了内存缓存: {}", module);
            
            throw new RuntimeException("Failed to update module config: " + module, e);
        }
    }

    @Override
    public Map<String, Object> getAllConfig() {
        log.info("获取所有配置");
        
        Map<String, Object> allConfig = new HashMap<>();
        
        try {
            // 从Redis获取所有配置
            String pattern = CONFIG_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    String module = key.substring(CONFIG_PREFIX.length());
                    Object config = redisTemplate.opsForValue().get(key);
                    if (config instanceof Map) {
                        allConfig.put(module, config);
                    } else if (config != null) {
                        allConfig.put(module, objectMapper.convertValue(config, Map.class));
                    }
                }
            }
            
            // 合并内存缓存中的配置
            for (Map.Entry<String, Map<String, Object>> entry : memoryCache.entrySet()) {
                if (!allConfig.containsKey(entry.getKey())) {
                    allConfig.put(entry.getKey(), entry.getValue());
                }
            }
            
        } catch (Exception e) {
            log.error("获取所有配置失败", e);
            // 发生错误时返回内存缓存
            for (Map.Entry<String, Map<String, Object>> entry : memoryCache.entrySet()) {
                allConfig.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
        
        return allConfig;
    }

    @Override
    public Object getConfigValue(String module, String key) {
        Map<String, Object> moduleConfig = getModuleConfig(module);
        return getNestedValue(moduleConfig, key);
    }

    @Override
    @CacheEvict(value = "moduleConfig", key = "#module")
    public void setConfigValue(String module, String key, Object value) {
        log.info("设置配置项: {}.{} = {}", module, key, value);
        
        Map<String, Object> moduleConfig = getModuleConfig(module);
        setNestedValue(moduleConfig, key, value);
        updateModuleConfig(module, moduleConfig);
    }

    @Override
    @CacheEvict(value = "moduleConfig", allEntries = true)
    public void reloadConfig() {
        log.info("重新加载配置");
        
        try {
            // 清除内存缓存
            memoryCache.clear();
            
            // 从Redis重新加载到内存
            String pattern = CONFIG_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    String module = key.substring(CONFIG_PREFIX.length());
                    Object config = redisTemplate.opsForValue().get(key);
                    if (config instanceof Map) {
                        memoryCache.put(module, (Map<String, Object>) config);
                    } else if (config != null) {
                        memoryCache.put(module, objectMapper.convertValue(config, Map.class));
                    }
                }
            }
            
            log.info("配置重新加载完成");
        } catch (Exception e) {
            log.error("重新加载配置失败", e);
            throw new RuntimeException("Failed to reload config", e);
        }
    }

    @Override
    public boolean validateConfig(String module, Map<String, Object> config) {
        log.info("验证配置: {}", module);
        
        try {
            // 基本验证
            if (config == null || config.isEmpty()) {
                return false;
            }
            
            // 模块特定验证
            switch (module) {
                case "storages":
                    return validateStorageConfig(config);
                case "mailer":
                    return validateMailerConfig(config);
                case "auth":
                    return validateAuthConfig(config);
                default:
                    return true; // 其他模块默认通过
            }
        } catch (Exception e) {
            log.error("配置验证失败: {}", module, e);
            return false;
        }
    }

    /**
     * 获取默认配置
     */
    private Map<String, Object> getDefaultConfig(String module) {
        Map<String, Object> defaultConfig = new HashMap<>();
        
        switch (module) {
            case "storages":
                Map<String, Object> blobConfig = new HashMap<>();
                blobConfig.put("provider", "fs");
                blobConfig.put("bucket", "default");
                Map<String, Object> fsConfig = new HashMap<>();
                fsConfig.put("rootPath", "/var/affine/storage");
                fsConfig.put("publicUrlPrefix", "/api/storage");
                blobConfig.put("fs", fsConfig);
                defaultConfig.put("blob", blobConfig);
                break;
            case "server":
                defaultConfig.put("name", "AFFiNE");
                defaultConfig.put("externalUrl", "http://localhost:3000");
                break;
            case "auth":
                defaultConfig.put("allowSignup", true);
                Map<String, Object> passwordReq = new HashMap<>();
                passwordReq.put("min", 8);
                passwordReq.put("max", 128);
                defaultConfig.put("passwordRequirements", passwordReq);
                break;
        }
        
        return defaultConfig;
    }

    /**
     * 获取嵌套值
     */
    private Object getNestedValue(Map<String, Object> map, String key) {
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object nested = map.get(parts[0]);
            if (nested instanceof Map) {
                return getNestedValue((Map<String, Object>) nested, parts[1]);
            }
            return null;
        } else {
            return map.get(key);
        }
    }

    /**
     * 设置嵌套值
     */
    private void setNestedValue(Map<String, Object> map, String key, Object value) {
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object nested = map.get(parts[0]);
            if (!(nested instanceof Map)) {
                nested = new HashMap<String, Object>();
                map.put(parts[0], nested);
            }
            setNestedValue((Map<String, Object>) nested, parts[1], value);
        } else {
            map.put(key, value);
        }
    }

    /**
     * 验证存储配置
     */
    private boolean validateStorageConfig(Map<String, Object> config) {
        if (!config.containsKey("blob")) {
            return false;
        }
        
        Map<String, Object> blobConfig = (Map<String, Object>) config.get("blob");
        String provider = (String) blobConfig.get("provider");
        
        if (provider == null) {
            return false;
        }
        
        switch (provider) {
            case "fs":
                Map<String, Object> fsConfig = (Map<String, Object>) blobConfig.get("fs");
                return fsConfig != null && fsConfig.containsKey("rootPath");
            case "aws-s3":
                Map<String, Object> s3Config = (Map<String, Object>) blobConfig.get("aws-s3");
                return s3Config != null && 
                       s3Config.containsKey("bucket") && 
                       s3Config.containsKey("accessKeyId") && 
                       s3Config.containsKey("secretAccessKey");
            default:
                return true; // 其他提供商暂时默认通过
        }
    }

    /**
     * 验证邮件配置
     */
    private boolean validateMailerConfig(Map<String, Object> config) {
        if (!config.containsKey("SMTP")) {
            return false;
        }
        
        Map<String, Object> smtpConfig = (Map<String, Object>) config.get("SMTP");
        return smtpConfig.containsKey("host") && 
               smtpConfig.containsKey("port") && 
               smtpConfig.containsKey("sender");
    }

    /**
     * 验证认证配置
     */
    private boolean validateAuthConfig(Map<String, Object> config) {
        if (config.containsKey("passwordRequirements")) {
            Map<String, Object> passwordReq = (Map<String, Object>) config.get("passwordRequirements");
            Object min = passwordReq.get("min");
            Object max = passwordReq.get("max");
            
            if (min instanceof Number && max instanceof Number) {
                return ((Number) min).intValue() > 0 && ((Number) max).intValue() > ((Number) min).intValue();
            }
        }
        return true;
    }
}