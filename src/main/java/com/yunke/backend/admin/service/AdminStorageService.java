package com.yunke.backend.admin.service;

import com.yunke.backend.storage.dto.StorageTestResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 存储服务管理接口
 */
public interface AdminStorageService {
    
    /**
     * 获取支持的存储提供商列表
     */
    List<Map<String, Object>> getSupportedProviders();
    
    /**
     * 获取当前存储配置
     */
    Map<String, Object> getCurrentConfig();
    
    /**
     * 更新存储配置
     */
    void updateConfig(Map<String, Object> config);
    
    /**
     * 测试存储连接
     */
    StorageTestResult testConnection(Map<String, Object> config);
    
    /**
     * 测试文件上传
     */
    StorageTestResult testUpload(MultipartFile file, String configJson);
    
    /**
     * 获取存储使用情况
     */
    Map<String, Object> getStorageUsage();
    
    /**
     * 验证存储配置
     */
    Map<String, Object> validateConfig(Map<String, Object> config);
    
    /**
     * 获取存储提供商配置模板
     */
    Map<String, Object> getProviderTemplate(String provider);
    
    /**
     * 清理测试文件
     */
    int cleanupTestFiles();
    
    /**
     * 获取存储统计信息
     */
    Map<String, Object> getStorageStats();
}