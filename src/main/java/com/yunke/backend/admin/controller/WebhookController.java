package com.yunke.backend.admin.controller;

import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.system.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook控制器
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * 注册Webhook
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerWebhook(
            @RequestBody RegisterWebhookRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        try {
            WebhookService.Webhook webhook = webhookService.registerWebhook(
                    request.url(),
                    request.events(),
                    request.secret()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("webhook", webhook);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to register webhook", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有Webhook
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllWebhooks() {
        List<WebhookService.Webhook> webhooks = webhookService.getAllWebhooks();
        
        Map<String, Object> response = new HashMap<>();
        response.put("webhooks", webhooks);
        response.put("count", webhooks.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除Webhook
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteWebhook(@PathVariable String id) {
        try {
            webhookService.deleteWebhook(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete webhook", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 测试Webhook
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable String id) {
        try {
            // 发送测试事件 (fire-and-forget)
            Map<String, Object> testPayload = Map.of(
                    "event", "webhook.test",
                    "message", "This is a test webhook event",
                    "timestamp", java.time.Instant.now().toString()
            );
            
            webhookService.triggerEvent("webhook.test", testPayload)
                .doOnError(e -> log.warn("Failed to trigger test webhook", e))
                .subscribe();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test webhook sent");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to test webhook", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 触发自定义事件（仅用于测试）
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerEvent(@RequestBody TriggerEventRequest request) {
        try {
            // 触发自定义事件 (fire-and-forget)
            webhookService.triggerEvent(request.event(), request.payload())
                .doOnError(e -> log.warn("Failed to trigger webhook event", e))
                .subscribe();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Event triggered successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to trigger event", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 请求数据类
    public record RegisterWebhookRequest(String url, List<String> events, String secret) {}
    public record TriggerEventRequest(String event, Map<String, Object> payload) {}
}