package com.yunke.backend.admin.service.impl;

import com.yunke.backend.storage.dto.StorageTestResult;
import com.yunke.backend.admin.service.AdminStorageService;
import com.yunke.backend.system.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存储服务管理实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStorageServiceImpl implements AdminStorageService {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    
    // 测试文件前缀
    private static final String TEST_FILE_PREFIX = "admin_test_";
    
    // 存储提供商配置
    private static final Map<String, Map<String, Object>> PROVIDER_TEMPLATES = new HashMap<>();
    
    static {
        // 本地文件系统
        Map<String, Object> fsTemplate = new HashMap<>();
        fsTemplate.put("provider", "fs");
        fsTemplate.put("name", "本地文件系统");
        fsTemplate.put("description", "将文件存储在服务器本地磁盘");
        fsTemplate.put("icon", "folder");
        Map<String, Object> fsConfig = new HashMap<>();
        fsConfig.put("rootPath", "/var/affine/storage");
        fsConfig.put("publicUrlPrefix", "/api/storage");
        fsTemplate.put("config", fsConfig);
        Map<String, Object> fsFields = new HashMap<>();
        fsFields.put("rootPath", Map.of("type", "string", "required", true, "label", "存储根路径", "placeholder", "/var/affine/storage"));
        fsFields.put("publicUrlPrefix", Map.of("type", "string", "required", true, "label", "公开URL前缀", "placeholder", "/api/storage"));
        fsTemplate.put("fields", fsFields);
        PROVIDER_TEMPLATES.put("fs", fsTemplate);
        
        // AWS S3
        Map<String, Object> s3Template = new HashMap<>();
        s3Template.put("provider", "aws-s3");
        s3Template.put("name", "Amazon S3");
        s3Template.put("description", "Amazon Simple Storage Service");
        s3Template.put("icon", "cloud");
        Map<String, Object> s3Config = new HashMap<>();
        s3Config.put("region", "us-east-1");
        s3Config.put("bucket", "");
        s3Config.put("accessKeyId", "");
        s3Config.put("secretAccessKey", "");
        s3Config.put("endpoint", "");
        s3Template.put("config", s3Config);
        Map<String, Object> s3Fields = new HashMap<>();
        s3Fields.put("region", Map.of("type", "string", "required", true, "label", "区域", "placeholder", "us-east-1"));
        s3Fields.put("bucket", Map.of("type", "string", "required", true, "label", "存储桶名称"));
        s3Fields.put("accessKeyId", Map.of("type", "string", "required", true, "label", "Access Key ID"));
        s3Fields.put("secretAccessKey", Map.of("type", "password", "required", true, "label", "Secret Access Key"));
        s3Fields.put("endpoint", Map.of("type", "string", "required", false, "label", "自定义端点", "placeholder", "留空使用AWS默认端点"));
        s3Template.put("fields", s3Fields);
        PROVIDER_TEMPLATES.put("aws-s3", s3Template);
        
        // 腾讯云 COS
        Map<String, Object> cosTemplate = new HashMap<>();
        cosTemplate.put("provider", "tencent-cos");
        cosTemplate.put("name", "腾讯云 COS");
        cosTemplate.put("description", "腾讯云对象存储服务");
        cosTemplate.put("icon", "cloud");
        Map<String, Object> cosConfig = new HashMap<>();
        cosConfig.put("region", "ap-beijing");
        cosConfig.put("bucket", "");
        cosConfig.put("secretId", "");
        cosConfig.put("secretKey", "");
        cosTemplate.put("config", cosConfig);
        Map<String, Object> cosFields = new HashMap<>();
        cosFields.put("region", Map.of("type", "string", "required", true, "label", "地域", "placeholder", "ap-beijing"));
        cosFields.put("bucket", Map.of("type", "string", "required", true, "label", "存储桶名称"));
        cosFields.put("secretId", Map.of("type", "string", "required", true, "label", "Secret ID"));
        cosFields.put("secretKey", Map.of("type", "password", "required", true, "label", "Secret Key"));
        cosTemplate.put("fields", cosFields);
        PROVIDER_TEMPLATES.put("tencent-cos", cosTemplate);
        
        // CloudFlare R2
        Map<String, Object> r2Template = new HashMap<>();
        r2Template.put("provider", "cloudflare-r2");
        r2Template.put("name", "CloudFlare R2");
        r2Template.put("description", "CloudFlare R2 对象存储");
        r2Template.put("icon", "cloud");
        Map<String, Object> r2Config = new HashMap<>();
        r2Config.put("accountId", "");
        r2Config.put("bucket", "");
        r2Config.put("accessKeyId", "");
        r2Config.put("secretAccessKey", "");
        r2Template.put("config", r2Config);
        Map<String, Object> r2Fields = new HashMap<>();
        r2Fields.put("accountId", Map.of("type", "string", "required", true, "label", "账户ID"));
        r2Fields.put("bucket", Map.of("type", "string", "required", true, "label", "存储桶名称"));
        r2Fields.put("accessKeyId", Map.of("type", "string", "required", true, "label", "Access Key ID"));
        r2Fields.put("secretAccessKey", Map.of("type", "password", "required", true, "label", "Secret Access Key"));
        r2Template.put("fields", r2Fields);
        PROVIDER_TEMPLATES.put("cloudflare-r2", r2Template);
    }

    @Override
    public List<Map<String, Object>> getSupportedProviders() {
        log.info("获取支持的存储提供商列表");
        return new ArrayList<>(PROVIDER_TEMPLATES.values());
    }

    @Override
    public Map<String, Object> getCurrentConfig() {
        log.info("获取当前存储配置");
        
        try {
            Map<String, Object> storageConfig = configService.getModuleConfig("storages");
            if (storageConfig == null) {
                storageConfig = new HashMap<>();
                // 设置默认配置
                Map<String, Object> blobConfig = new HashMap<>();
                blobConfig.put("provider", "fs");
                blobConfig.put("bucket", "default");
                Map<String, Object> fsConfig = new HashMap<>();
                fsConfig.put("rootPath", "/var/affine/storage");
                fsConfig.put("publicUrlPrefix", "/api/storage");
                blobConfig.put("fs", fsConfig);
                
                storageConfig.put("blob", blobConfig);
            }
            return storageConfig;
        } catch (Exception e) {
            log.error("获取存储配置失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public void updateConfig(Map<String, Object> config) {
        log.info("更新存储配置: {}", config);
        
        try {
            configService.updateModuleConfig("storages", config);
            log.info("存储配置更新成功");
        } catch (Exception e) {
            log.error("更新存储配置失败", e);
            throw new RuntimeException("Failed to update storage configuration", e);
        }
    }

    @Override
    public StorageTestResult testConnection(Map<String, Object> config) {
        log.info("测试存储连接: {}", config.get("provider"));
        
        long startTime = System.currentTimeMillis();
        String provider = (String) config.get("provider");
        
        try {
            switch (provider) {
                case "fs":
                    return testFileSystemConnection(config);
                case "aws-s3":
                    return testS3Connection(config);
                case "tencent-cos":
                    return testCosConnection(config);
                case "cloudflare-r2":
                    return testR2Connection(config);
                default:
                    return StorageTestResult.failure("Unsupported storage provider: " + provider);
            }
        } catch (Exception e) {
            log.error("存储连接测试失败", e);
            long responseTime = System.currentTimeMillis() - startTime;
            StorageTestResult result = StorageTestResult.failure("Connection test failed: " + e.getMessage());
            result.setResponseTime(responseTime);
            result.setProvider(provider);
            return result;
        }
    }

    private StorageTestResult testFileSystemConnection(Map<String, Object> config) {
        String rootPath = (String) config.get("rootPath");
        if (rootPath == null || rootPath.trim().isEmpty()) {
            return StorageTestResult.failure("Root path is required");
        }
        
        try {
            Path path = Paths.get(rootPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            
            if (!Files.isWritable(path)) {
                return StorageTestResult.failure("Root path is not writable: " + rootPath);
            }
            
            // 测试创建临时文件
            Path testFile = path.resolve(TEST_FILE_PREFIX + System.currentTimeMillis() + ".txt");
            Files.write(testFile, "test".getBytes());
            Files.delete(testFile);
            
            Map<String, Object> details = new HashMap<>();
            details.put("rootPath", rootPath);
            details.put("writable", true);
            details.put("exists", true);
            
            StorageTestResult result = StorageTestResult.success("File system connection successful", details);
            result.setProvider("fs");
            return result;
            
        } catch (Exception e) {
            return StorageTestResult.failure("File system test failed: " + e.getMessage());
        }
    }

    private StorageTestResult testS3Connection(Map<String, Object> config) {
        // TODO: 实现S3连接测试
        // 这里需要集成AWS SDK来实际测试S3连接
        log.info("模拟S3连接测试");
        
        String bucket = (String) config.get("bucket");
        String accessKeyId = (String) config.get("accessKeyId");
        String secretAccessKey = (String) config.get("secretAccessKey");
        
        if (bucket == null || accessKeyId == null || secretAccessKey == null) {
            return StorageTestResult.failure("Missing required S3 configuration");
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("bucket", bucket);
        details.put("region", config.get("region"));
        details.put("endpoint", config.get("endpoint"));
        
        StorageTestResult result = StorageTestResult.success("S3 connection test simulated", details);
        result.setProvider("aws-s3");
        result.setResponseTime(500L); // 模拟响应时间
        return result;
    }

    private StorageTestResult testCosConnection(Map<String, Object> config) {
        // TODO: 实现腾讯云COS连接测试
        log.info("模拟腾讯云COS连接测试");
        
        String bucket = (String) config.get("bucket");
        String secretId = (String) config.get("secretId");
        String secretKey = (String) config.get("secretKey");
        
        if (bucket == null || secretId == null || secretKey == null) {
            return StorageTestResult.failure("Missing required COS configuration");
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("bucket", bucket);
        details.put("region", config.get("region"));
        
        StorageTestResult result = StorageTestResult.success("COS connection test simulated", details);
        result.setProvider("tencent-cos");
        result.setResponseTime(600L);
        return result;
    }

    private StorageTestResult testR2Connection(Map<String, Object> config) {
        // TODO: 实现CloudFlare R2连接测试
        log.info("模拟CloudFlare R2连接测试");
        
        String bucket = (String) config.get("bucket");
        String accountId = (String) config.get("accountId");
        String accessKeyId = (String) config.get("accessKeyId");
        String secretAccessKey = (String) config.get("secretAccessKey");
        
        if (bucket == null || accountId == null || accessKeyId == null || secretAccessKey == null) {
            return StorageTestResult.failure("Missing required R2 configuration");
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("bucket", bucket);
        details.put("accountId", accountId);
        
        StorageTestResult result = StorageTestResult.success("R2 connection test simulated", details);
        result.setProvider("cloudflare-r2");
        result.setResponseTime(400L);
        return result;
    }

    @Override
    public StorageTestResult testUpload(MultipartFile file, String configJson) {
        log.info("测试文件上传: {}", file.getOriginalFilename());
        
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> config;
            if (configJson != null && !configJson.trim().isEmpty()) {
                config = objectMapper.readValue(configJson, Map.class);
            } else {
                config = getCurrentConfig();
                // 提取blob配置
                if (config.containsKey("blob")) {
                    config = (Map<String, Object>) config.get("blob");
                }
            }
            
            String provider = (String) config.get("provider");
            String fileName = TEST_FILE_PREFIX + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            
            StorageTestResult result;
            switch (provider) {
                case "fs":
                    result = testFileSystemUpload(file, fileName, config);
                    break;
                default:
                    result = StorageTestResult.success("Upload test simulated for " + provider);
                    result.setFileUrl("/api/storage/test/" + fileName);
                    result.setFileSize(file.getSize());
                    break;
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            result.setResponseTime(responseTime);
            result.setProvider(provider);
            result.setTestType("upload");
            
            return result;
            
        } catch (Exception e) {
            log.error("文件上传测试失败", e);
            long responseTime = System.currentTimeMillis() - startTime;
            StorageTestResult result = StorageTestResult.failure("Upload test failed: " + e.getMessage());
            result.setResponseTime(responseTime);
            result.setFileSize(file.getSize());
            return result;
        }
    }

    private StorageTestResult testFileSystemUpload(MultipartFile file, String fileName, Map<String, Object> config) throws IOException {
        String rootPath = (String) config.get("rootPath");
        if (rootPath == null) {
            rootPath = "/tmp/affine-test";
        }
        
        Path uploadPath = Paths.get(rootPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());
        
        String publicUrlPrefix = (String) config.getOrDefault("publicUrlPrefix", "/api/storage");
        String fileUrl = publicUrlPrefix + "/" + fileName;
        
        StorageTestResult result = StorageTestResult.success("File uploaded successfully to local filesystem");
        result.setFileUrl(fileUrl);
        result.setFileSize(file.getSize());
        return result;
    }

    @Override
    public Map<String, Object> getStorageUsage() {
        log.info("获取存储使用情况");
        
        Map<String, Object> usage = new HashMap<>();
        
        try {
            Map<String, Object> config = getCurrentConfig();
            String provider = "fs"; // 默认提供商
            
            if (config.containsKey("blob")) {
                Map<String, Object> blobConfig = (Map<String, Object>) config.get("blob");
                provider = (String) blobConfig.getOrDefault("provider", "fs");
            }
            
            switch (provider) {
                case "fs":
                    usage = getFileSystemUsage(config);
                    break;
                default:
                    // 其他提供商的使用情况
                    usage.put("provider", provider);
                    usage.put("totalSpace", "Unknown");
                    usage.put("usedSpace", "Unknown");
                    usage.put("freeSpace", "Unknown");
                    usage.put("fileCount", "Unknown");
                    break;
            }
            
            usage.put("provider", provider);
            usage.put("lastUpdated", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("获取存储使用情况失败", e);
            usage.put("error", e.getMessage());
        }
        
        return usage;
    }

    private Map<String, Object> getFileSystemUsage(Map<String, Object> config) {
        Map<String, Object> usage = new HashMap<>();
        
        String rootPath = "/tmp/affine-storage"; // 默认路径
        if (config.containsKey("blob")) {
            Map<String, Object> blobConfig = (Map<String, Object>) config.get("blob");
            if (blobConfig.containsKey("fs")) {
                Map<String, Object> fsConfig = (Map<String, Object>) blobConfig.get("fs");
                rootPath = (String) fsConfig.getOrDefault("rootPath", rootPath);
            }
        }
        
        try {
            Path path = Paths.get(rootPath);
            if (Files.exists(path)) {
                File rootDir = path.toFile();
                long totalSpace = rootDir.getTotalSpace();
                long freeSpace = rootDir.getFreeSpace();
                long usedSpace = totalSpace - freeSpace;
                
                // 计算文件数量
                AtomicLong fileCount = new AtomicLong(0);
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(f -> fileCount.incrementAndGet());
                
                usage.put("totalSpace", formatBytes(totalSpace));
                usage.put("usedSpace", formatBytes(usedSpace));
                usage.put("freeSpace", formatBytes(freeSpace));
                usage.put("fileCount", fileCount.get());
                usage.put("rootPath", rootPath);
                
                // 使用率百分比
                double usagePercent = (double) usedSpace / totalSpace * 100;
                usage.put("usagePercent", Math.round(usagePercent * 100.0) / 100.0);
            } else {
                usage.put("error", "Storage path does not exist: " + rootPath);
            }
        } catch (Exception e) {
            log.error("获取文件系统使用情况失败", e);
            usage.put("error", e.getMessage());
        }
        
        return usage;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public Map<String, Object> validateConfig(Map<String, Object> config) {
        log.info("验证存储配置");
        
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        String provider = (String) config.get("provider");
        if (provider == null || provider.trim().isEmpty()) {
            errors.add("Storage provider is required");
        } else {
            // 验证提供商是否支持
            if (!PROVIDER_TEMPLATES.containsKey(provider)) {
                errors.add("Unsupported storage provider: " + provider);
            } else {
                // 验证提供商特定配置
                Map<String, Object> template = PROVIDER_TEMPLATES.get(provider);
                Map<String, Object> fields = (Map<String, Object>) template.get("fields");
                
                for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
                    String fieldName = fieldEntry.getKey();
                    Map<String, Object> fieldConfig = (Map<String, Object>) fieldEntry.getValue();
                    boolean required = (boolean) fieldConfig.getOrDefault("required", false);
                    
                    if (required && (!config.containsKey(fieldName) || 
                        config.get(fieldName) == null || 
                        config.get(fieldName).toString().trim().isEmpty())) {
                        errors.add("Field '" + fieldName + "' is required for " + provider);
                    }
                }
                
                // 添加提供商特定的验证警告
                switch (provider) {
                    case "fs":
                        String rootPath = (String) config.get("rootPath");
                        if (rootPath != null && !rootPath.startsWith("/")) {
                            warnings.add("Root path should be an absolute path");
                        }
                        break;
                    case "aws-s3":
                        String bucket = (String) config.get("bucket");
                        if (bucket != null && !bucket.matches("^[a-z0-9.-]+$")) {
                            warnings.add("S3 bucket name should only contain lowercase letters, numbers, dots and hyphens");
                        }
                        break;
                }
            }
        }
        
        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        validation.put("provider", provider);
        
        return validation;
    }

    @Override
    public Map<String, Object> getProviderTemplate(String provider) {
        log.info("获取存储提供商配置模板: {}", provider);
        
        if (!PROVIDER_TEMPLATES.containsKey(provider)) {
            throw new IllegalArgumentException("Unsupported storage provider: " + provider);
        }
        
        return new HashMap<>(PROVIDER_TEMPLATES.get(provider));
    }

    @Override
    public int cleanupTestFiles() {
        log.info("清理测试文件");
        
        int deletedCount = 0;
        
        try {
            Map<String, Object> config = getCurrentConfig();
            String provider = "fs";
            
            if (config.containsKey("blob")) {
                Map<String, Object> blobConfig = (Map<String, Object>) config.get("blob");
                provider = (String) blobConfig.getOrDefault("provider", "fs");
            }
            
            if ("fs".equals(provider)) {
                deletedCount = cleanupFileSystemTestFiles(config);
            }
            // TODO: 为其他提供商实现测试文件清理
            
            log.info("成功清理 {} 个测试文件", deletedCount);
            
        } catch (Exception e) {
            log.error("清理测试文件失败", e);
        }
        
        return deletedCount;
    }

    private int cleanupFileSystemTestFiles(Map<String, Object> config) throws IOException {
        String rootPath = "/tmp/affine-storage";
        if (config.containsKey("blob")) {
            Map<String, Object> blobConfig = (Map<String, Object>) config.get("blob");
            if (blobConfig.containsKey("fs")) {
                Map<String, Object> fsConfig = (Map<String, Object>) blobConfig.get("fs");
                rootPath = (String) fsConfig.getOrDefault("rootPath", rootPath);
            }
        }
        
        Path path = Paths.get(rootPath);
        if (!Files.exists(path)) {
            return 0;
        }
        
        AtomicLong deletedCount = new AtomicLong(0);
        Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith(TEST_FILE_PREFIX))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                        deletedCount.incrementAndGet();
                    } catch (IOException e) {
                        log.warn("无法删除测试文件: {}", p, e);
                    }
                });
        
        return (int) deletedCount.get();
    }

    @Override
    public Map<String, Object> getStorageStats() {
        log.info("获取存储统计信息");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Map<String, Object> usage = getStorageUsage();
            stats.putAll(usage);
            
            // 添加其他统计信息
            stats.put("supportedProviders", PROVIDER_TEMPLATES.keySet().size());
            stats.put("testFilesPrefix", TEST_FILE_PREFIX);
            stats.put("lastStatsUpdate", LocalDateTime.now());
            
            // 添加配置状态
            Map<String, Object> config = getCurrentConfig();
            String currentProvider = "fs";
            if (config.containsKey("blob")) {
                Map<String, Object> blobConfig = (Map<String, Object>) config.get("blob");
                currentProvider = (String) blobConfig.getOrDefault("provider", "fs");
            }
            stats.put("currentProvider", currentProvider);
            
            // 验证当前配置
            Map<String, Object> validation = validateConfig(config);
            stats.put("configValid", validation.get("valid"));
            
        } catch (Exception e) {
            log.error("获取存储统计信息失败", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}