package com.yunke.backend.admin.controller;

import com.yunke.backend.storage.dto.StorageTestResult;
import com.yunke.backend.admin.service.AdminStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 存储服务管理控制器 - 用于管理界面
 */
@RestController
@RequestMapping("/api/admin/storage")
@RequiredArgsConstructor
@Slf4j
public class AdminStorageController {

    private final AdminStorageService adminStorageService;

    /**
     * 获取支持的存储提供商列表
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getStorageProviders() {
        log.info("获取存储提供商列表");
        
        try {
            List<Map<String, Object>> providers = adminStorageService.getSupportedProviders();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "providers", providers
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取存储提供商列表失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to get storage providers",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取当前存储配置
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getStorageConfig() {
        log.info("获取存储配置");
        
        try {
            Map<String, Object> config = adminStorageService.getCurrentConfig();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "config", config
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取存储配置失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to get storage config",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新存储配置
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateStorageConfig(@RequestBody Map<String, Object> config) {
        log.info("更新存储配置: {}", config);
        
        try {
            adminStorageService.updateConfig(config);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Storage configuration updated successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新存储配置失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to update storage config",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 测试存储连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testStorageConnection(@RequestBody Map<String, Object> config) {
        log.info("测试存储连接: {}", config.get("provider"));
        
        try {
            StorageTestResult result = adminStorageService.testConnection(config);
            
            Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "message", result.getMessage(),
                "details", result.getDetails(),
                "responseTime", result.getResponseTime()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("测试存储连接失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Connection test failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 测试文件上传
     */
    @PostMapping("/test-upload")
    public ResponseEntity<Map<String, Object>> testFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "config", required = false) String configJson) {
        log.info("测试文件上传: {}", file.getOriginalFilename());
        
        try {
            StorageTestResult result = adminStorageService.testUpload(file, configJson);
            
            Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "message", result.getMessage(),
                "fileUrl", result.getFileUrl() != null ? result.getFileUrl() : "",
                "fileSize", result.getFileSize(),
                "uploadTime", result.getUploadTime()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("测试文件上传失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Upload test failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取存储使用情况
     */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getStorageUsage() {
        log.info("获取存储使用情况");
        
        try {
            Map<String, Object> usage = adminStorageService.getStorageUsage();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "usage", usage
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取存储使用情况失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to get storage usage",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 验证存储配置
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateStorageConfig(@RequestBody Map<String, Object> config) {
        log.info("验证存储配置");
        
        try {
            Map<String, Object> validationResult = adminStorageService.validateConfig(config);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "validation", validationResult
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("验证存储配置失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Configuration validation failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取存储提供商的配置模板
     */
    @GetMapping("/providers/{provider}/template")
    public ResponseEntity<Map<String, Object>> getProviderTemplate(@PathVariable String provider) {
        log.info("获取存储提供商配置模板: {}", provider);
        
        try {
            Map<String, Object> template = adminStorageService.getProviderTemplate(provider);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "template", template
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取存储提供商配置模板失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to get provider template",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 清理测试文件
     */
    @DeleteMapping("/test-files")
    public ResponseEntity<Map<String, Object>> cleanupTestFiles() {
        log.info("清理测试文件");
        
        try {
            int deletedCount = adminStorageService.cleanupTestFiles();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Test files cleaned up successfully",
                "deletedCount", deletedCount
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理测试文件失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to cleanup test files",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取存储统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStorageStats() {
        log.info("获取存储统计信息");
        
        try {
            Map<String, Object> stats = adminStorageService.getStorageStats();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "stats", stats
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取存储统计信息失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to get storage stats",
                "message", e.getMessage()
            ));
        }
    }
}