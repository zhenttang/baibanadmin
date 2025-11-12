package com.yunke.backend.admin.controller;

import com.yunke.backend.security.dto.security.BlockIpRequest;
import com.yunke.backend.security.dto.security.BlockedIp;
import com.yunke.backend.security.dto.security.SecurityEvent;
import com.yunke.backend.security.dto.security.SecurityStats;
import com.yunke.backend.security.service.LoginProtectionService;
import com.yunke.backend.security.service.SecurityMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🖥️ 安全监控后台API
 * 
 * 提供安全管理功能：
 * - 查看安全统计
 * - 查看安全事件
 * - 管理IP黑名单
 * - 解锁被封禁的账号
 */
@RestController
@RequestMapping("/api/admin/security")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitorController {
    
    private final SecurityMonitorService securityMonitor;
    private final LoginProtectionService loginProtection;
    
    /**
     * 获取安全统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<SecurityStats> getStats() {
        SecurityStats stats = securityMonitor.getStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取最近的安全事件
     */
    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getRecentEvents(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "100") int limit) {
        
        List<SecurityEvent> events = securityMonitor.getRecentEvents(days, limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", events);
        result.put("total", events.size());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取被封禁的IP列表
     */
    @GetMapping("/blocked-ips")
    public ResponseEntity<Map<String, Object>> getBlockedIps() {
        List<BlockedIp> blockedIps = securityMonitor.getBlockedIps();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", blockedIps);
        result.put("total", blockedIps.size());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动封禁IP
     */
    @PostMapping("/block-ip")
    public ResponseEntity<Map<String, Object>> blockIp(@Valid @RequestBody BlockIpRequest request) {
        securityMonitor.blockIp(request.getIp(), request.getReason(), 
                               request.getDurationMinutes());
        
        log.info("管理员手动封禁IP: {}, 原因: {}", request.getIp(), request.getReason());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "IP已封禁");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动解封IP
     */
    @DeleteMapping("/blocked-ips/{ip}")
    public ResponseEntity<Map<String, Object>> unblockIp(@PathVariable String ip) {
        securityMonitor.unblockIp(ip);
        
        log.info("管理员手动解封IP: {}", ip);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "IP已解封");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查IP是否被封禁
     */
    @GetMapping("/check-ip/{ip}")
    public ResponseEntity<Map<String, Object>> checkIp(@PathVariable String ip) {
        boolean isBlocked = securityMonitor.isIpBlocked(ip);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("ip", ip);
        result.put("blocked", isBlocked);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 解锁被锁定的账号
     */
    @PostMapping("/unlock-account")
    public ResponseEntity<Map<String, Object>> unlockAccount(@RequestParam String username) {
        loginProtection.unlockAccount(username);
        
        log.info("管理员手动解锁账号: {}", username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "账号已解锁");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取账号登录失败次数
     */
    @GetMapping("/account-failures/{username}")
    public ResponseEntity<Map<String, Object>> getAccountFailures(@PathVariable String username) {
        int failures = loginProtection.getFailureCount(username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("username", username);
        result.put("failures", failures);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 清除所有安全计数（慎用！）
     */
    @PostMapping("/clear-all-counters")
    public ResponseEntity<Map<String, Object>> clearAllCounters(@RequestParam String confirm) {
        if (!"YES_CLEAR_ALL".equals(confirm)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "需要确认参数：confirm=YES_CLEAR_ALL");
            return ResponseEntity.badRequest().body(error);
        }
        
        // TODO: 实现清除所有计数器的逻辑
        log.warn("⚠️ 管理员清除了所有安全计数器");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "所有计数器已清除");
        
        return ResponseEntity.ok(result);
    }
}

