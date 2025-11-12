package com.yunke.backend.system.controller;

import com.yunke.backend.system.dto.ServerConfigDto;
import com.yunke.backend.system.dto.ServerInfoDto;
import com.yunke.backend.system.dto.ConfigOperationLogDto;

import com.yunke.backend.system.service.ServerConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 服务器配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/server-config")
@RequiredArgsConstructor
@Tag(name = "服务器配置管理", description = "服务器配置和系统信息管理API")
@PreAuthorize("hasRole('ADMIN')")
public class ServerConfigController {
    
    private final ServerConfigService serverConfigService;
    
    @GetMapping("/server-info")
    @Operation(summary = "获取服务器完整信息", description = "获取包括配置、版本、运行时、系统状态等完整信息")
    public ResponseEntity<ServerInfoDto> getServerInfo() {
        try {
            ServerInfoDto serverInfo = serverConfigService.getServerInfo();
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            log.error("获取服务器信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/server-config")
    @Operation(summary = "获取服务器配置", description = "获取当前服务器配置信息")
    public ResponseEntity<ServerConfigDto> getServerConfig() {
        try {
            ServerConfigDto config = serverConfigService.getServerConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("获取服务器配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/server-config")
    @Operation(summary = "更新服务器配置", description = "更新服务器配置并记录操作日志")
    public ResponseEntity<?> updateServerConfig(
            @Valid @RequestBody ServerConfigDto config,
            Principal principal,
            HttpServletRequest request) {
        try {
            String operator = principal != null ? principal.getName() : "anonymous";
            String sourceIp = getClientIpAddress(request);
            
            ServerConfigDto updatedConfig = serverConfigService.updateServerConfig(config, operator, sourceIp);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "服务器配置更新成功",
                    "data", updatedConfig
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("服务器配置验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "配置验证失败: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("更新服务器配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "配置更新失败: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/server-config/validate")
    @Operation(summary = "验证服务器配置", description = "验证服务器配置的有效性")
    public ResponseEntity<Map<String, Object>> validateServerConfig(
            @Valid @RequestBody ServerConfigDto config) {
        try {
            boolean isValid = serverConfigService.validateServerConfig(config);
            
            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "配置验证通过" : "配置验证失败"
            ));
            
        } catch (Exception e) {
            log.error("验证服务器配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "valid", false,
                    "message", "配置验证过程中发生错误: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/server-config/reload")
    @Operation(summary = "重新加载配置", description = "重新加载系统配置")
    public ResponseEntity<Map<String, Object>> reloadConfig() {
        try {
            boolean success = serverConfigService.reloadConfig();
            
            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "配置重新加载成功" : "配置重新加载失败"
            ));
            
        } catch (Exception e) {
            log.error("重新加载配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "重新加载配置失败: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/config-logs")
    @Operation(summary = "获取配置操作日志", description = "获取配置变更操作日志")
    public ResponseEntity<List<ConfigOperationLogDto>> getConfigLogs(
            @Parameter(description = "模块名称") @RequestParam(required = false) String module,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "50") Integer limit) {
        try {
            List<ConfigOperationLogDto> logs = serverConfigService.getConfigOperationLogs(module, limit);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("获取配置操作日志失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/system-health")
    @Operation(summary = "获取系统健康状态", description = "获取系统各组件的健康状态")
    public ResponseEntity<ServerInfoDto.ServiceStatus> getSystemHealth() {
        try {
            ServerInfoDto.ServiceStatus status = serverConfigService.getSystemHealth();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/server-status")
    @Operation(summary = "获取服务器运行状态", description = "获取服务器运行时状态信息")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        try {
            ServerInfoDto serverInfo = serverConfigService.getServerInfo();
            
            Map<String, Object> status = Map.of(
                    "runtime", serverInfo.getRuntime(),
                    "system", serverInfo.getSystem(),
                    "health", serverInfo.getStatus()
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取服务器状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}