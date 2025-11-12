package com.yunke.backend.admin.controller;

import com.yunke.backend.security.dto.OAuthConfigDto;
import com.yunke.backend.security.dto.OAuthTestResultDto;
import com.yunke.backend.security.dto.OAuthStatisticsDto;
import com.yunke.backend.system.service.impl.OAuthConfigServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * OAuth配置管理控制器
 */
@RestController
@RequestMapping("/api/admin/oauth")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class OAuthConfigController {

    private final OAuthConfigServiceImpl oauthConfigService;

    /**
     * 获取所有OAuth提供商配置
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        try {
            List<OAuthConfigDto> configs = oauthConfigService.getAllConfigs();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "configs", configs,
                "message", "获取OAuth配置成功"
            ));
        } catch (Exception e) {
            log.error("获取OAuth配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "FETCH_CONFIGS_FAILED",
                "message", "获取OAuth配置失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取特定提供商的配置
     */
    @GetMapping("/providers/{provider}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String provider) {
        try {
            OAuthConfigDto config = oauthConfigService.getConfig(provider);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "config", config,
                "message", "获取" + provider + "配置成功"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "INVALID_PROVIDER",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("获取{}配置失败", provider, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "FETCH_CONFIG_FAILED",
                "message", "获取配置失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 更新OAuth配置
     */
    @PutMapping("/providers/{provider}")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String provider,
            @Valid @RequestBody OAuthConfigDto configDto) {
        try {
            oauthConfigService.updateConfig(provider, configDto);
            
            log.info("OAuth配置更新成功: {} by user", provider);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", provider + "配置更新成功"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "INVALID_PROVIDER",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("更新{}配置失败", provider, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "UPDATE_CONFIG_FAILED",
                "message", "更新配置失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 测试OAuth连接
     */
    @PostMapping("/providers/{provider}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable String provider) {
        try {
            OAuthTestResultDto result = oauthConfigService.testConnection(provider);
            
            log.info("OAuth连接测试完成: {} - {}", provider, result.getSuccess() ? "成功" : "失败");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "result", result,
                "message", "OAuth连接测试完成"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "INVALID_PROVIDER",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("OAuth连接测试失败: {}", provider, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "TEST_CONNECTION_FAILED",
                "message", "连接测试失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取回调URL
     */
    @GetMapping("/callback-url")
    public ResponseEntity<Map<String, Object>> getCallbackUrl(@RequestParam(required = false) String provider) {
        try {
            String callbackUrl = oauthConfigService.generateCallbackUrl(provider);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "callbackUrl", callbackUrl,
                "message", "获取回调URL成功"
            ));
        } catch (Exception e) {
            log.error("获取{}回调URL失败", provider, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "GET_CALLBACK_URL_FAILED",
                "message", "获取回调URL失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 验证回调URL
     */
    @PostMapping("/validate-callback")
    public ResponseEntity<Map<String, Object>> validateCallback(@RequestBody Map<String, String> request) {
        try {
            String callbackUrl = request.get("callbackUrl");
            if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "INVALID_CALLBACK_URL",
                    "message", "回调URL不能为空"
                ));
            }
            
            // 简单的URL格式验证
            boolean isValid = callbackUrl.startsWith("http://") || callbackUrl.startsWith("https://");
            boolean isSecure = callbackUrl.startsWith("https://");
            
            Map<String, Object> validation = Map.of(
                "isValid", isValid,
                "isSecure", isSecure,
                "suggestion", isSecure ? "回调URL配置正确" : "建议使用HTTPS回调URL以提高安全性"
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "validation", validation,
                "message", "回调URL验证完成"
            ));
        } catch (Exception e) {
            log.error("验证回调URL失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "VALIDATE_CALLBACK_FAILED",
                "message", "验证回调URL失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取OAuth统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            OAuthStatisticsDto statistics = oauthConfigService.getStatistics();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "statistics", statistics,
                "message", "获取OAuth统计信息成功"
            ));
        } catch (Exception e) {
            log.error("获取OAuth统计信息失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "GET_STATISTICS_FAILED",
                "message", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 批量启用/禁用OAuth提供商
     */
    @PostMapping("/providers/batch-toggle")
    public ResponseEntity<Map<String, Object>> batchToggle(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> providers = (List<String>) request.get("providers");
            Boolean enabled = (Boolean) request.get("enabled");
            
            if (providers == null || providers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "INVALID_REQUEST",
                    "message", "提供商列表不能为空"
                ));
            }
            
            int successCount = 0;
            for (String provider : providers) {
                try {
                    OAuthConfigDto config = oauthConfigService.getConfig(provider);
                    config.setEnabled(enabled);
                    oauthConfigService.updateConfig(provider, config);
                    successCount++;
                } catch (Exception e) {
                    log.warn("批量操作失败: {}", provider, e);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "processedCount", successCount,
                "totalCount", providers.size(),
                "message", String.format("批量操作完成，成功处理 %d/%d 个提供商", successCount, providers.size())
            ));
        } catch (Exception e) {
            log.error("批量操作失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "BATCH_OPERATION_FAILED",
                "message", "批量操作失败: " + e.getMessage()
            ));
        }
    }
}