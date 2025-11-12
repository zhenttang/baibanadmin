package com.yunke.backend.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件服务接口
 */
public interface PluginService {

    /**
     * 安装插件
     */
    Plugin installPlugin(String name, String version, String source);

    /**
     * 卸载插件
     */
    void uninstallPlugin(String id);

    /**
     * 启用插件
     */
    void enablePlugin(String id);

    /**
     * 禁用插件
     */
    void disablePlugin(String id);

    /**
     * 获取所有插件
     */
    List<Plugin> getAllPlugins();

    /**
     * 根据ID获取插件
     */
    Optional<Plugin> getPlugin(String id);

    /**
     * 获取已启用的插件
     */
    List<Plugin> getEnabledPlugins();

    /**
     * 执行插件钩子
     */
    Map<String, Object> executeHook(String hookName, Map<String, Object> context);

    /**
     * 注册插件钩子
     */
    void registerHook(String pluginId, String hookName, PluginHook hook);

    /**
     * 插件记录
     */
    record Plugin(
            String id,
            String name,
            String version,
            String description,
            String author,
            String source,
            PluginStatus status,
            Map<String, Object> config,
            List<String> hooks,
            java.time.Instant installedAt,
            java.time.Instant updatedAt
    ) {}

    /**
     * 插件状态
     */
    enum PluginStatus {
        INSTALLED,
        ENABLED,
        DISABLED,
        ERROR
    }

    /**
     * 插件钩子接口
     */
    @FunctionalInterface
    interface PluginHook {
        Map<String, Object> execute(Map<String, Object> context);
    }
}