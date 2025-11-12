package com.yunke.backend.admin.controller;

import com.yunke.backend.document.dto.DuplicateFileDto;
import com.yunke.backend.storage.dto.*;
import com.yunke.backend.storage.service.StorageManagementService;
import com.yunke.backend.system.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 存储管理增强控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/storage-management")
@RequiredArgsConstructor
@Tag(name = "存储管理", description = "存储服务管理和监控API")
@PreAuthorize("hasRole('ADMIN')")
public class StorageManagementController {
    
    private final StorageManagementService storageManagementService;
    
    // ================== 文件浏览和管理 ==================
    
    @GetMapping("/browse")
    @Operation(summary = "浏览存储目录", description = "获取指定路径下的文件和目录列表")
    public ResponseEntity<Map<String, Object>> browseDirectory(
            @Parameter(description = "目录路径") @RequestParam(defaultValue = "/") String path,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "页面大小") @RequestParam(defaultValue = "50") Integer size) {
        try {
            StorageDirectoryDto directory = storageManagementService.browseDirectory(path, page, size);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", directory,
                    "message", "目录浏览成功"
            ));
        } catch (Exception e) {
            log.error("浏览目录失败: {}", path, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "浏览目录失败: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索文件", description = "根据条件搜索文件")
    public ResponseEntity<Map<String, Object>> searchFiles(
            @Parameter(description = "搜索关键词") @RequestParam String query,
            @Parameter(description = "搜索路径") @RequestParam(defaultValue = "/") String path,
            @Parameter(description = "文件类型") @RequestParam(required = false) String fileType,
            @Parameter(description = "最小文件大小") @RequestParam(required = false) Long minSize,
            @Parameter(description = "最大文件大小") @RequestParam(required = false) Long maxSize,
            @Parameter(description = "开始日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<StorageFileDto> files = storageManagementService.searchFiles(
                    query, path, fileType, minSize, maxSize, startDate, endDate);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", files,
                    "count", files.size(),
                    "message", "文件搜索成功"
            ));
        } catch (Exception e) {
            log.error("文件搜索失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "文件搜索失败: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/file-details")
    @Operation(summary = "获取文件详细信息")
    public ResponseEntity<Map<String, Object>> getFileDetails(
            @Parameter(description = "文件路径") @RequestParam String filePath) {
        try {
            StorageFileDto fileDetails = storageManagementService.getFileDetails(filePath);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", fileDetails,
                    "message", "获取文件详情成功"
            ));
        } catch (Exception e) {
            log.error("获取文件详情失败: {}", filePath, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取文件详情失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/presigned-download-url")
    @Operation(summary = "生成预签名下载URL")
    public ResponseEntity<Map<String, Object>> generateDownloadUrl(
            @RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            Integer expirationMinutes = (Integer) request.getOrDefault("expirationMinutes", 60);
            
            String downloadUrl = storageManagementService.generatePresignedDownloadUrl(filePath, expirationMinutes);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "downloadUrl", downloadUrl,
                    "expirationMinutes", expirationMinutes,
                    "message", "生成下载URL成功"
            ));
        } catch (Exception e) {
            log.error("生成下载URL失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "生成下载URL失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/presigned-upload-url")
    @Operation(summary = "生成预签名上传URL")
    public ResponseEntity<Map<String, Object>> generateUploadUrl(
            @RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            String contentType = (String) request.get("contentType");
            Integer expirationMinutes = (Integer) request.getOrDefault("expirationMinutes", 60);
            
            String uploadUrl = storageManagementService.generatePresignedUploadUrl(filePath, contentType, expirationMinutes);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "uploadUrl", uploadUrl,
                    "expirationMinutes", expirationMinutes,
                    "message", "生成上传URL成功"
            ));
        } catch (Exception e) {
            log.error("生成上传URL失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "生成上传URL失败: " + e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/files")
    @Operation(summary = "删除文件或目录")
    public ResponseEntity<Map<String, Object>> deleteFiles(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> paths = (List<String>) request.get("paths");
            Map<String, Object> result = storageManagementService.deleteFiles(paths);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "result", result,
                    "message", "文件删除操作完成"
            ));
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "删除文件失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/move")
    @Operation(summary = "移动/重命名文件")
    public ResponseEntity<Map<String, Object>> moveFile(
            @RequestBody Map<String, String> request) {
        try {
            String sourcePath = request.get("sourcePath");
            String targetPath = request.get("targetPath");
            boolean success = storageManagementService.moveFile(sourcePath, targetPath);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "文件移动成功" : "文件移动失败"
            ));
        } catch (Exception e) {
            log.error("移动文件失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "移动文件失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/copy")
    @Operation(summary = "复制文件")
    public ResponseEntity<Map<String, Object>> copyFile(
            @RequestBody Map<String, String> request) {
        try {
            String sourcePath = request.get("sourcePath");
            String targetPath = request.get("targetPath");
            boolean success = storageManagementService.copyFile(sourcePath, targetPath);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "文件复制成功" : "文件复制失败"
            ));
        } catch (Exception e) {
            log.error("复制文件失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "复制文件失败: " + e.getMessage()
            ));
        }
    }
    
    // ================== 存储监控和统计 ==================
    
    @GetMapping("/usage-statistics")
    @Operation(summary = "获取存储使用统计")
    public ResponseEntity<StorageUsageStatsDto> getUsageStatistics() {
        try {
            StorageUsageStatsDto stats = storageManagementService.getUsageStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取存储统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/performance")
    @Operation(summary = "获取存储性能数据")
    public ResponseEntity<List<StoragePerformanceDto>> getPerformanceData(
            @Parameter(description = "开始时间") @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<StoragePerformanceDto> data = storageManagementService.getPerformanceData(startTime, endTime);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("获取性能数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/performance/current")
    @Operation(summary = "获取实时性能指标")
    public ResponseEntity<StoragePerformanceDto> getCurrentPerformance() {
        try {
            StoragePerformanceDto performance = storageManagementService.getCurrentPerformance();
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            log.error("获取实时性能数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/file-type-statistics")
    @Operation(summary = "获取文件类型统计")
    public ResponseEntity<Map<String, StorageUsageStatsDto.FileTypeStats>> getFileTypeStatistics() {
        try {
            Map<String, StorageUsageStatsDto.FileTypeStats> stats = storageManagementService.getFileTypeStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取文件类型统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/cost-analysis")
    @Operation(summary = "获取存储成本分析")
    public ResponseEntity<StorageUsageStatsDto.StorageCostInfo> getCostAnalysis(
            @Parameter(description = "开始日期") @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            StorageUsageStatsDto.StorageCostInfo costInfo = storageManagementService.getCostAnalysis(startDate, endDate);
            return ResponseEntity.ok(costInfo);
        } catch (Exception e) {
            log.error("获取成本分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ================== 存储迁移 ==================
    
    @PostMapping("/migration")
    @Operation(summary = "创建存储迁移任务")
    public ResponseEntity<Map<String, Object>> createMigrationTask(
            @RequestBody Map<String, Object> request) {
        try {
            String sourceProvider = (String) request.get("sourceProvider");
            String targetProvider = (String) request.get("targetProvider");
            // 这里需要反序列化MigrationOptions，简化处理
            StorageMigrationDto.MigrationOptions options = new StorageMigrationDto.MigrationOptions();
            
            StorageMigrationDto task = storageManagementService.createMigrationTask(sourceProvider, targetProvider, options);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "task", task,
                    "message", "迁移任务创建成功"
            ));
        } catch (Exception e) {
            log.error("创建迁移任务失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "创建迁移任务失败: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/migration/{taskId}")
    @Operation(summary = "获取迁移任务状态")
    public ResponseEntity<StorageMigrationDto> getMigrationTask(@PathVariable String taskId) {
        try {
            StorageMigrationDto task = storageManagementService.getMigrationTask(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            log.error("获取迁移任务失败: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/migration")
    @Operation(summary = "获取所有迁移任务")
    public ResponseEntity<List<StorageMigrationDto>> getAllMigrationTasks() {
        try {
            List<StorageMigrationDto> tasks = storageManagementService.getAllMigrationTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("获取迁移任务列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/migration/{taskId}/pause")
    @Operation(summary = "暂停迁移任务")
    public ResponseEntity<Map<String, Object>> pauseMigrationTask(@PathVariable String taskId) {
        try {
            boolean success = storageManagementService.pauseMigrationTask(taskId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "任务暂停成功" : "任务暂停失败"
            ));
        } catch (Exception e) {
            log.error("暂停迁移任务失败: {}", taskId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "暂停任务失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/migration/{taskId}/resume")
    @Operation(summary = "恢复迁移任务")
    public ResponseEntity<Map<String, Object>> resumeMigrationTask(@PathVariable String taskId) {
        try {
            boolean success = storageManagementService.resumeMigrationTask(taskId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "任务恢复成功" : "任务恢复失败"
            ));
        } catch (Exception e) {
            log.error("恢复迁移任务失败: {}", taskId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "恢复任务失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/migration/{taskId}/cancel")
    @Operation(summary = "取消迁移任务")
    public ResponseEntity<Map<String, Object>> cancelMigrationTask(@PathVariable String taskId) {
        try {
            boolean success = storageManagementService.cancelMigrationTask(taskId);
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "任务取消成功" : "任务取消失败"
            ));
        } catch (Exception e) {
            log.error("取消迁移任务失败: {}", taskId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "取消任务失败: " + e.getMessage()
            ));
        }
    }
    
    // ================== 存储优化 ==================
    
    @PostMapping("/detect-duplicates")
    @Operation(summary = "检测重复文件")
    public ResponseEntity<List<DuplicateFileDto>> detectDuplicateFiles(
            @RequestBody Map<String, String> request) {
        try {
            String path = request.getOrDefault("path", "/");
            List<DuplicateFileDto> duplicates = storageManagementService.detectDuplicateFiles(path);
            return ResponseEntity.ok(duplicates);
        } catch (Exception e) {
            log.error("检测重复文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/optimization-recommendations")
    @Operation(summary = "获取存储优化建议")
    public ResponseEntity<List<StorageOptimizationDto>> getOptimizationRecommendations() {
        try {
            List<StorageOptimizationDto> recommendations = storageManagementService.getOptimizationRecommendations();
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("获取优化建议失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "执行存储清理")
    public ResponseEntity<StorageCleanupReportDto> performStorageCleanup(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> cleanupTypeNames = (List<String>) request.get("cleanupTypes");
            Boolean dryRun = (Boolean) request.getOrDefault("dryRun", true);
            
            // 转换枚举类型
            List<StorageCleanupReportDto.CleanupType> cleanupTypes = cleanupTypeNames.stream()
                    .map(StorageCleanupReportDto.CleanupType::valueOf)
                    .toList();
            
            StorageCleanupReportDto report = storageManagementService.performStorageCleanup(cleanupTypes, dryRun);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("执行存储清理失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/providers")
    @Operation(summary = "获取支持的存储提供商")
    public ResponseEntity<List<StorageProviderDto>> getSupportedProviders() {
        try {
            List<StorageProviderDto> providers = storageManagementService.getSupportedProviders();
            return ResponseEntity.ok(providers);
        } catch (Exception e) {
            log.error("获取存储提供商失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/test-connection")
    @Operation(summary = "测试存储连接")
    public ResponseEntity<StorageTestResultDto> testStorageConnection(
            @Valid @RequestBody StorageConfigDto config) {
        try {
            StorageTestResultDto result = storageManagementService.testStorageConnection(config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试存储连接失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/validate-config")
    @Operation(summary = "验证存储配置")
    public ResponseEntity<Map<String, Object>> validateStorageConfig(
            @Valid @RequestBody StorageConfigDto config) {
        try {
            Map<String, Object> result = storageManagementService.validateStorageConfig(config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("验证存储配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "valid", false,
                    "message", "验证失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/switch-provider")
    @Operation(summary = "切换存储提供商")
    public ResponseEntity<Map<String, Object>> switchStorageProvider(
            @RequestBody Map<String, Object> request) {
        try {
            String newProvider = (String) request.get("provider");
            // 这里需要反序列化StorageConfigDto，简化处理
            StorageConfigDto config = new StorageConfigDto();
            
            Map<String, Object> result = storageManagementService.switchStorageProvider(newProvider, config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("切换存储提供商失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "切换失败: " + e.getMessage()
            ));
        }
    }
}