package com.yunke.backend.system.service;

import java.util.Map;

/**
 * 配置服务接口
 */
public interface ConfigService {
    
    /**
     * 获取指定模块的配置
     */
    Map<String, Object> getModuleConfig(String module);
    
    /**
     * 更新指定模块的配置
     */
    void updateModuleConfig(String module, Map<String, Object> config);
    
    /**
     * 获取所有配置
     */
    Map<String, Object> getAllConfig();
    
    /**
     * 获取配置项值
     */
    Object getConfigValue(String module, String key);
    
    /**
     * 设置配置项值
     */
    void setConfigValue(String module, String key, Object value);
    
    /**
     * 重新加载配置
     */
    void reloadConfig();
    
    /**
     * 验证配置
     */
    boolean validateConfig(String module, Map<String, Object> config);
}