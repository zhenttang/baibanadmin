package com.yunke.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 根路径控制器
 * 处理根路径访问，返回服务基本信息
 */
@RestController
public class RootController {

    /**
     * 根路径处理 - 返回API服务信息
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "service", "AFFiNE Backend API",
            "version", "0.21.0", 
            "status", "running",
            "timestamp", Instant.now().toString(),
            "message", "Welcome to AFFiNE API Server",
            "documentation", Map.of(
                "health", "/health - Health check endpoint",
                "health_detailed", "/health/detailed - Detailed health status", 
                "api", "/api/* - Main API endpoints"
            )
        ));
    }
}