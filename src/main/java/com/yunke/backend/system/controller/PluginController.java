package com.yunke.backend.controller;

import com.yunke.backend.service.PluginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件控制器
 */
@RestController
@RequestMapping("/api/plugins")
@RequiredArgsConstructor
@Slf4j
public class PluginController {

    private final PluginService pluginService;

    /**
     * 获取所有插件
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllPlugins() {
        List<PluginService.Plugin> plugins = pluginService.getAllPlugins();
        
        Map<String, Object> response = new HashMap<>();
        response.put("plugins", plugins);
        response.put("count", plugins.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取已启用的插件
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledPlugins() {
        List<PluginService.Plugin> plugins = pluginService.getEnabledPlugins();
        
        Map<String, Object> response = new HashMap<>();
        response.put("plugins", plugins);
        response.put("count", plugins.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取插件
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPlugin(@PathVariable String id) {
        Optional<PluginService.Plugin> plugin = pluginService.getPlugin(id);
        
        if (plugin.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("plugin", plugin.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 安装插件
     */
    @PostMapping("/install")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> installPlugin(@RequestBody InstallPluginRequest request) {
        try {
            PluginService.Plugin plugin = pluginService.installPlugin(
                    request.name(),
                    request.version(),
                    request.source()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("plugin", plugin);
            response.put("message", "Plugin installed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to install plugin", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 卸载插件
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> uninstallPlugin(@PathVariable String id) {
        try {
            pluginService.uninstallPlugin(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Plugin uninstalled successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to uninstall plugin", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 启用插件
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> enablePlugin(@PathVariable String id) {
        try {
            pluginService.enablePlugin(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Plugin enabled successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to enable plugin", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 禁用插件
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> disablePlugin(@PathVariable String id) {
        try {
            pluginService.disablePlugin(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Plugin disabled successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to disable plugin", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 执行插件钩子
     */
    @PostMapping("/hooks/{hookName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> executeHook(
            @PathVariable String hookName,
            @RequestBody Map<String, Object> context) {
        
        try {
            Map<String, Object> results = pluginService.executeHook(hookName, context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hookName", hookName);
            response.put("results", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to execute hook", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 请求数据类
    public record InstallPluginRequest(String name, String version, String source) {}
}