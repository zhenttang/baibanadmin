package com.yunke.backend.service.impl;

import com.yunke.backend.service.PluginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 插件服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluginServiceImpl implements PluginService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 插件缓存
    private final Map<String, Plugin> pluginCache = new ConcurrentHashMap<>();
    
    // 插件钩子注册表
    private final Map<String, Map<String, PluginHook>> pluginHooks = new ConcurrentHashMap<>();

    private static final String PLUGIN_KEY_PREFIX = "plugin:";
    private static final String PLUGIN_LIST_KEY = "plugins:all";

    @Override
    public Plugin installPlugin(String name, String version, String source) {
        log.info("Installing plugin: {} version {}", name, version);
        
        String id = UUID.randomUUID().toString();
        Plugin plugin = new Plugin(
                id,
                name,
                version,
                "Plugin: " + name, // 简化的描述
                "AFFiNE", // 默认作者
                source,
                PluginStatus.INSTALLED,
                new HashMap<>(), // 空配置
                new ArrayList<>(), // 空钩子列表
                Instant.now(),
                Instant.now()
        );
        
        try {
            // 保存到Redis
            String key = PLUGIN_KEY_PREFIX + id;
            String pluginJson = objectMapper.writeValueAsString(plugin);
            redisTemplate.opsForValue().set(key, pluginJson, Duration.ofDays(365));
            
            // 添加到列表
            redisTemplate.opsForSet().add(PLUGIN_LIST_KEY, id);
            
            // 更新缓存
            pluginCache.put(id, plugin);
            
            log.info("Plugin installed successfully: {} ({})", name, id);
            return plugin;
        } catch (Exception e) {
            log.error("Failed to install plugin", e);
            throw new RuntimeException("Failed to install plugin", e);
        }
    }

    @Override
    public void uninstallPlugin(String id) {
        log.info("Uninstalling plugin: {}", id);
        
        try {
            // 先禁用插件
            disablePlugin(id);
            
            // 从Redis删除
            String key = PLUGIN_KEY_PREFIX + id;
            redisTemplate.delete(key);
            redisTemplate.opsForSet().remove(PLUGIN_LIST_KEY, id);
            
            // 从缓存删除
            pluginCache.remove(id);
            
            // 删除钩子
            pluginHooks.remove(id);
            
            log.info("Plugin uninstalled successfully: {}", id);
        } catch (Exception e) {
            log.error("Failed to uninstall plugin", e);
            throw new RuntimeException("Failed to uninstall plugin", e);
        }
    }

    @Override
    public void enablePlugin(String id) {
        log.info("Enabling plugin: {}", id);
        
        Optional<Plugin> pluginOpt = getPlugin(id);
        if (pluginOpt.isEmpty()) {
            throw new IllegalArgumentException("Plugin not found: " + id);
        }
        
        Plugin plugin = pluginOpt.get();
        if (plugin.status() == PluginStatus.ENABLED) {
            log.warn("Plugin already enabled: {}", id);
            return;
        }
        
        try {
            // 更新状态
            Plugin updatedPlugin = new Plugin(
                    plugin.id(),
                    plugin.name(),
                    plugin.version(),
                    plugin.description(),
                    plugin.author(),
                    plugin.source(),
                    PluginStatus.ENABLED,
                    plugin.config(),
                    plugin.hooks(),
                    plugin.installedAt(),
                    Instant.now()
            );
            
            savePlugin(updatedPlugin);
            
            log.info("Plugin enabled successfully: {}", id);
        } catch (Exception e) {
            log.error("Failed to enable plugin", e);
            throw new RuntimeException("Failed to enable plugin", e);
        }
    }

    @Override
    public void disablePlugin(String id) {
        log.info("Disabling plugin: {}", id);
        
        Optional<Plugin> pluginOpt = getPlugin(id);
        if (pluginOpt.isEmpty()) {
            throw new IllegalArgumentException("Plugin not found: " + id);
        }
        
        Plugin plugin = pluginOpt.get();
        if (plugin.status() == PluginStatus.DISABLED) {
            log.warn("Plugin already disabled: {}", id);
            return;
        }
        
        try {
            // 更新状态
            Plugin updatedPlugin = new Plugin(
                    plugin.id(),
                    plugin.name(),
                    plugin.version(),
                    plugin.description(),
                    plugin.author(),
                    plugin.source(),
                    PluginStatus.DISABLED,
                    plugin.config(),
                    plugin.hooks(),
                    plugin.installedAt(),
                    Instant.now()
            );
            
            savePlugin(updatedPlugin);
            
            log.info("Plugin disabled successfully: {}", id);
        } catch (Exception e) {
            log.error("Failed to disable plugin", e);
            throw new RuntimeException("Failed to disable plugin", e);
        }
    }

    @Override
    public List<Plugin> getAllPlugins() {
        log.debug("Getting all plugins");
        
        try {
            // 如果缓存为空，从Redis加载
            if (pluginCache.isEmpty()) {
                loadPluginsFromRedis();
            }
            
            return new ArrayList<>(pluginCache.values());
        } catch (Exception e) {
            log.error("Failed to get all plugins", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Optional<Plugin> getPlugin(String id) {
        log.debug("Getting plugin: {}", id);
        
        // 先从缓存获取
        Plugin plugin = pluginCache.get(id);
        if (plugin != null) {
            return Optional.of(plugin);
        }
        
        // 从Redis获取
        try {
            String key = PLUGIN_KEY_PREFIX + id;
            Object pluginJson = redisTemplate.opsForValue().get(key);
            
            if (pluginJson != null) {
                plugin = objectMapper.readValue(pluginJson.toString(), Plugin.class);
                pluginCache.put(id, plugin);
                return Optional.of(plugin);
            }
        } catch (Exception e) {
            log.error("Failed to get plugin from Redis", e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Plugin> getEnabledPlugins() {
        return getAllPlugins().stream()
                .filter(plugin -> plugin.status() == PluginStatus.ENABLED)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> executeHook(String hookName, Map<String, Object> context) {
        log.debug("Executing hook: {} with context: {}", hookName, context.keySet());
        
        Map<String, Object> results = new HashMap<>();
        
        // 获取所有已启用的插件
        List<Plugin> enabledPlugins = getEnabledPlugins();
        
        for (Plugin plugin : enabledPlugins) {
            try {
                // 检查插件是否注册了这个钩子
                Map<String, PluginHook> hooks = pluginHooks.get(plugin.id());
                if (hooks != null && hooks.containsKey(hookName)) {
                    PluginHook hook = hooks.get(hookName);
                    Map<String, Object> result = hook.execute(context);
                    results.put(plugin.id(), result);
                    
                    log.debug("Hook executed successfully: {} -> {}", hookName, plugin.name());
                }
            } catch (Exception e) {
                log.error("Failed to execute hook {} for plugin {}", hookName, plugin.name(), e);
                results.put(plugin.id(), Map.of("error", e.getMessage()));
            }
        }
        
        return results;
    }

    @Override
    public void registerHook(String pluginId, String hookName, PluginHook hook) {
        log.info("Registering hook: {} for plugin: {}", hookName, pluginId);
        
        pluginHooks.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>())
                .put(hookName, hook);
        
        log.info("Hook registered successfully: {} -> {}", hookName, pluginId);
    }

    /**
     * 保存插件到Redis和缓存
     */
    private void savePlugin(Plugin plugin) throws Exception {
        String key = PLUGIN_KEY_PREFIX + plugin.id();
        String pluginJson = objectMapper.writeValueAsString(plugin);
        redisTemplate.opsForValue().set(key, pluginJson, Duration.ofDays(365));
        
        pluginCache.put(plugin.id(), plugin);
    }

    /**
     * 从Redis加载插件
     */
    private void loadPluginsFromRedis() {
        try {
            Set<Object> pluginIds = redisTemplate.opsForSet().members(PLUGIN_LIST_KEY);
            if (pluginIds == null) {
                return;
            }
            
            for (Object pluginId : pluginIds) {
                String key = PLUGIN_KEY_PREFIX + pluginId.toString();
                Object pluginJson = redisTemplate.opsForValue().get(key);
                
                if (pluginJson != null) {
                    Plugin plugin = objectMapper.readValue(pluginJson.toString(), Plugin.class);
                    pluginCache.put(plugin.id(), plugin);
                }
            }
            
            log.info("Loaded {} plugins from Redis", pluginCache.size());
        } catch (Exception e) {
            log.error("Failed to load plugins from Redis", e);
        }
    }
}