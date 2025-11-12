package com.yunke.backend.user.controller;

import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.system.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 用户配额控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserQuotaController {

    private final QuotaService quotaService;

    /**
     * 获取当前认证用户ID
     */
    private Mono<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.empty();
        }

        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return Mono.just(userDetails.getUserId());
    }

    /**
     * 获取当前用户的配额信息
     * GET /api/user/quota
     */
    @GetMapping("/quota")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentUserQuota() {
        log.info("获取当前用户配额信息");
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("获取用户配额: userId={}", userId);
                    return quotaService.getUserQuotaWithUsage(userId)
                            .map(quotaData -> ResponseEntity.ok(quotaData))
                            .onErrorResume(e -> {
                                log.error("获取用户配额失败: userId={}", userId, e);
                                return Mono.just(ResponseEntity.status(500).body(Map.of(
                                        "error", "获取用户配额失败",
                                        "message", e.getMessage()
                                )));
                            });
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(Map.of("error", "未授权"))));
    }

    /**
     * 获取指定用户的配额信息（管理员功能）
     * GET /api/user/{userId}/quota
     */
    @GetMapping("/{userId}/quota")
    public Mono<ResponseEntity<Map<String, Object>>> getUserQuota(@PathVariable String userId) {
        log.info("获取指定用户配额信息: userId={}", userId);
        
        // TODO: 添加管理员权限检查
        return quotaService.getUserQuotaWithUsage(userId)
                .map(quotaData -> ResponseEntity.ok(quotaData))
                .onErrorResume(e -> {
                    log.error("获取用户配额失败: userId={}", userId, e);
                    return Mono.just(ResponseEntity.status(500).body(Map.of(
                            "error", "获取用户配额失败",
                            "message", e.getMessage()
                    )));
                });
    }
}