package com.yunke.backend.system.controller;

import com.yunke.backend.system.dto.FeatureDto;
import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.system.service.FeatureService;
import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 特性开关REST API控制器
 * 对应Node.js版本的特性API
 * 参考: /packages/backend/server/src/core/features/resolver.ts
 */
@RestController
@RequestMapping("/api/features")
@Slf4j
public class FeatureController {

    @Autowired
    private FeatureService featureService;

    // ==================== 全局特性管理 (管理员) ====================

    /**
     * 获取所有特性
     * GET /api/features
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<FeatureDto.FeatureInfo>>> getAllFeatures() {
        return featureService.getAllFeatures()
                .map(features -> features.stream()
                        .map(this::convertToFeatureInfo)
                        .toList())
                .map(ResponseEntity::ok);
    }

    /**
     * 获取启用的特性
     * GET /api/features/enabled
     */
    @GetMapping("/enabled")
    public Mono<ResponseEntity<List<FeatureDto.FeatureInfo>>> getEnabledFeatures() {
        return featureService.getEnabledFeatures()
                .map(features -> features.stream()
                        .map(this::convertToFeatureInfo)
                        .toList())
                .map(ResponseEntity::ok);
    }

    /**
     * 创建新特性
     * POST /api/features
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureInfo>> createFeature(
            @Valid @RequestBody FeatureDto.CreateFeatureInput input) {
        Feature.FeatureType type = input.getType() == 0 ? Feature.FeatureType.FEATURE : Feature.FeatureType.QUOTA;
        return featureService.createFeature(input.getName(), input.getDescription(), input.getConfigs(), type)
                .map(this::convertToFeatureInfo)
                .map(feature -> ResponseEntity.status(HttpStatus.CREATED).body(feature))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * 更新特性
     * PUT /api/features/{name}
     */
    @PutMapping("/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureInfo>> updateFeature(
            @PathVariable String name,
            @Valid @RequestBody FeatureDto.UpdateFeatureInput input) {
        return featureService.updateFeature(name, input.getDescription(), input.getConfigs())
                .map(this::convertToFeatureInfo)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 启用特性
     * POST /api/features/{name}/enable
     */
    @PostMapping("/{name}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> enableFeature(@PathVariable String name) {
        return featureService.enableFeature(name)
                .map(v -> ResponseEntity.ok().<Void>build())
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 禁用特性
     * POST /api/features/{name}/disable
     */
    @PostMapping("/{name}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> disableFeature(@PathVariable String name) {
        return featureService.disableFeature(name)
                .map(v -> ResponseEntity.ok().<Void>build())
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 废弃特性
     * POST /api/features/{name}/deprecate
     */
    @PostMapping("/{name}/deprecate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deprecateFeature(
            @PathVariable String name,
            @RequestParam Integer version) {
        return featureService.deprecateFeature(name, version)
                .map(v -> ResponseEntity.ok().<Void>build())
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    // ==================== 用户特性管理 ====================

    /**
     * 获取用户特性
     * GET /api/features/users/{userId}
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public Mono<ResponseEntity<FeatureDto.UserFeatureListDto>> getUserFeatures(
            @PathVariable String userId) {
        return Mono.zip(
                featureService.getUserActiveFeatures(userId),
                featureService.getUserFeatures(userId),
                featureService.getUserQuota(userId),
                featureService.isAdmin(userId)
        ).flatMap(tuple -> {
            List<String> activeFeatures = tuple.getT1();
            List<String> earlyAccessTypes = tuple.getT2().stream()
                    .filter(uf -> uf.getName().contains("early_access"))
                    .map(uf -> uf.getName().replace("_early_access", "").replace("early_access", "app"))
                    .toList();

            FeatureDto.UserFeatureListDto result = FeatureDto.UserFeatureListDto.builder()
                    .userId(userId)
                    .activeFeatures(activeFeatures)
                    .allFeatures(tuple.getT2().stream()
                            .map(this::convertToUserFeatureInfo)
                            .toList())
                    .quotaInfo(tuple.getT3().orElse(Map.of()))
                    .isAdmin(tuple.getT4())
                    .earlyAccessTypes(earlyAccessTypes)
                    .build();

            return Mono.just(ResponseEntity.ok(result));
        });
    }

    /**
     * 为用户添加特性
     * POST /api/features/users
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> addUserFeature(
            @Valid @RequestBody FeatureDto.AddUserFeatureInput input) {
        return featureService.addUserFeature(input.getUserId(), input.getFeatureName(), input.getReason(), input.getExpiredAt())
                .map(userFeature -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("用户特性添加成功")
                            .featureName(input.getFeatureName())
                            .targetId(input.getUserId())
                            .operationTime(userFeature.getCreatedAt())
                            .build();
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(false)
                            .message("添加失败: " + error.getMessage())
                            .featureName(input.getFeatureName())
                            .targetId(input.getUserId())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }

    /**
     * 移除用户特性
     * DELETE /api/features/users/{userId}/{featureName}
     */
    @DeleteMapping("/users/{userId}/{featureName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> removeUserFeature(
            @PathVariable String userId,
            @PathVariable String featureName) {
        return featureService.removeUserFeature(userId, featureName)
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("用户特性移除成功")
                            .featureName(featureName)
                            .targetId(userId)
                            .build();
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(false)
                            .message("移除失败: " + error.getMessage())
                            .featureName(featureName)
                            .targetId(userId)
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }

    /**
     * 激活用户特性
     * POST /api/features/users/{userId}/{featureName}/activate
     */
    @PostMapping("/users/{userId}/{featureName}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> activateUserFeature(
            @PathVariable String userId,
            @PathVariable String featureName) {
        return featureService.activateUserFeature(userId, featureName)
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("用户特性激活成功")
                            .featureName(featureName)
                            .targetId(userId)
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 停用用户特性
     * POST /api/features/users/{userId}/{featureName}/deactivate
     */
    @PostMapping("/users/{userId}/{featureName}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> deactivateUserFeature(
            @PathVariable String userId,
            @PathVariable String featureName) {
        return featureService.deactivateUserFeature(userId, featureName)
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("用户特性停用成功")
                            .featureName(featureName)
                            .targetId(userId)
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 切换用户计划
     * POST /api/features/users/switch-plan
     */
    @PostMapping("/users/switch-plan")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> switchUserPlan(
            @Valid @RequestBody FeatureDto.SwitchUserPlanInput input) {
        return featureService.switchUserQuota(input.getUserId(), input.getFromPlan(), input.getToPlan(), input.getReason())
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("用户计划切换成功")
                            .featureName(input.getToPlan())
                            .targetId(input.getUserId())
                            .additionalInfo(Map.of("fromPlan", input.getFromPlan(), "toPlan", input.getToPlan()))
                            .build();
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(false)
                            .message("计划切换失败: " + error.getMessage())
                            .targetId(input.getUserId())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }

    /**
     * 检查用户是否有特定特性
     * GET /api/features/users/{userId}/check/{featureName}
     */
    @GetMapping("/users/{userId}/check/{featureName}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public Mono<ResponseEntity<FeatureDto.FeatureCheckResult>> checkUserFeature(
            @PathVariable String userId,
            @PathVariable String featureName) {
        return featureService.hasUserFeature(userId, featureName)
                .map(hasFeature -> {
                    FeatureDto.FeatureCheckResult result = FeatureDto.FeatureCheckResult.builder()
                            .featureName(featureName)
                            .hasFeature(hasFeature)
                            .isActive(hasFeature)
                            .isExpired(false) // 可以进一步实现过期检查
                            .build();
                    return ResponseEntity.ok(result);
                });
    }

    // ==================== 工作区特性管理 ====================

    /**
     * 获取工作区特性
     * GET /api/features/workspaces/{workspaceId}
     */
    @GetMapping("/workspaces/{workspaceId}")
    public Mono<ResponseEntity<FeatureDto.WorkspaceFeatureListDto>> getWorkspaceFeatures(
            @PathVariable String workspaceId) {
        return Mono.zip(
                featureService.getWorkspaceActiveFeatures(workspaceId),
                featureService.getWorkspaceFeatures(workspaceId),
                featureService.getWorkspaceQuota(workspaceId),
                featureService.hasWorkspaceFeature(workspaceId, "team_plan_v1"),
                featureService.hasWorkspaceFeature(workspaceId, "enterprise_security")
        ).map(tuple -> {
            List<String> activeFeatures = tuple.getT1();
            Boolean hasEnterpriseFeatures = tuple.getT5() || 
                    activeFeatures.stream().anyMatch(f -> f.contains("enterprise") || f.contains("sso") || f.contains("audit"));

            FeatureDto.WorkspaceFeatureListDto result = FeatureDto.WorkspaceFeatureListDto.builder()
                    .workspaceId(workspaceId)
                    .activeFeatures(activeFeatures)
                    .allFeatures(tuple.getT2().stream()
                            .map(this::convertToWorkspaceFeatureInfo)
                            .toList())
                    .quotaInfo(tuple.getT3().orElse(Map.of()))
                    .hasTeamPlan(tuple.getT4())
                    .hasEnterpriseFeatures(hasEnterpriseFeatures)
                    .build();

            return ResponseEntity.ok(result);
        });
    }

    /**
     * 为工作区添加特性
     * POST /api/features/workspaces
     */
    @PostMapping("/workspaces")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> addWorkspaceFeature(
            @Valid @RequestBody FeatureDto.AddWorkspaceFeatureInput input) {
        return featureService.addWorkspaceFeature(
                input.getWorkspaceId(), 
                input.getFeatureName(), 
                input.getReason(), 
                input.getConfigs(), 
                input.getExpiredAt())
                .map(workspaceFeature -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("工作区特性添加成功")
                            .featureName(input.getFeatureName())
                            .targetId(input.getWorkspaceId())
                            .operationTime(workspaceFeature.getCreatedAt())
                            .build();
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(false)
                            .message("添加失败: " + error.getMessage())
                            .featureName(input.getFeatureName())
                            .targetId(input.getWorkspaceId())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }

    /**
     * 移除工作区特性
     * DELETE /api/features/workspaces/{workspaceId}/{featureName}
     */
    @DeleteMapping("/workspaces/{workspaceId}/{featureName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> removeWorkspaceFeature(
            @PathVariable String workspaceId,
            @PathVariable String featureName) {
        return featureService.removeWorkspaceFeature(workspaceId, featureName)
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("工作区特性移除成功")
                            .featureName(featureName)
                            .targetId(workspaceId)
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 更新工作区特性配置
     * PUT /api/features/workspaces/config
     */
    @PutMapping("/workspaces/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> updateWorkspaceFeatureConfig(
            @Valid @RequestBody FeatureDto.UpdateWorkspaceFeatureConfigInput input) {
        return featureService.updateWorkspaceFeatureConfig(
                input.getWorkspaceId(), 
                input.getFeatureName(), 
                input.getConfigs())
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("工作区特性配置更新成功")
                            .featureName(input.getFeatureName())
                            .targetId(input.getWorkspaceId())
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 检查工作区是否有特定特性
     * GET /api/features/workspaces/{workspaceId}/check/{featureName}
     */
    @GetMapping("/workspaces/{workspaceId}/check/{featureName}")
    public Mono<ResponseEntity<FeatureDto.FeatureCheckResult>> checkWorkspaceFeature(
            @PathVariable String workspaceId,
            @PathVariable String featureName) {
        return featureService.hasWorkspaceFeature(workspaceId, featureName)
                .map(hasFeature -> {
                    FeatureDto.FeatureCheckResult result = FeatureDto.FeatureCheckResult.builder()
                            .featureName(featureName)
                            .hasFeature(hasFeature)
                            .isActive(hasFeature)
                            .isExpired(false)
                            .build();
                    return ResponseEntity.ok(result);
                });
    }

    // ==================== 管理员和早期访问管理 ====================

    /**
     * 添加管理员权限
     * POST /api/features/admin
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> addAdmin(
            @Valid @RequestBody FeatureDto.AdminOperationInput input) {
        return featureService.addAdmin(input.getUserId(), input.getReason())
                .map(userFeature -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("管理员权限添加成功")
                            .featureName("administrator")
                            .targetId(input.getUserId())
                            .operationTime(userFeature.getCreatedAt())
                            .build();
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(false)
                            .message("添加失败: " + error.getMessage())
                            .featureName("administrator")
                            .targetId(input.getUserId())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }

    /**
     * 移除管理员权限
     * DELETE /api/features/admin/{userId}
     */
    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> removeAdmin(
            @PathVariable String userId) {
        return featureService.removeAdmin(userId)
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("管理员权限移除成功")
                            .featureName("administrator")
                            .targetId(userId)
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 添加早期访问权限
     * POST /api/features/early-access
     */
    @PostMapping("/early-access")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> addEarlyAccess(
            @Valid @RequestBody FeatureDto.EarlyAccessOperationInput input) {
        Feature.EarlyAccessType type = Feature.EarlyAccessType.fromValue(input.getAccessType());
        return featureService.addEarlyAccess(input.getUserId(), type, input.getReason())
                .map(userFeature -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("早期访问权限添加成功")
                            .featureName(userFeature.getName())
                            .targetId(input.getUserId())
                            .operationTime(userFeature.getCreatedAt())
                            .additionalInfo(Map.of("accessType", input.getAccessType()))
                            .build();
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(false)
                            .message("添加失败: " + error.getMessage())
                            .targetId(input.getUserId())
                            .additionalInfo(Map.of("accessType", input.getAccessType()))
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }

    /**
     * 移除早期访问权限
     * DELETE /api/features/early-access/{userId}/{accessType}
     */
    @DeleteMapping("/early-access/{userId}/{accessType}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureOperationResponse>> removeEarlyAccess(
            @PathVariable String userId,
            @PathVariable String accessType) {
        Feature.EarlyAccessType type = Feature.EarlyAccessType.fromValue(accessType);
        return featureService.removeEarlyAccess(userId, type)
                .map(v -> {
                    FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                            .success(true)
                            .message("早期访问权限移除成功")
                            .targetId(userId)
                            .additionalInfo(Map.of("accessType", accessType))
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 检查邮箱早期访问权限
     * POST /api/features/early-access/check
     */
    @PostMapping("/early-access/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkEarlyAccess(
            @Valid @RequestBody FeatureDto.CheckEarlyAccessInput input) {
        Feature.EarlyAccessType type = Feature.EarlyAccessType.fromValue(input.getAccessType());
        return featureService.canEarlyAccess(input.getEmail(), type)
                .map(canAccess -> {
                    Map<String, Boolean> result = Map.of(
                            "canAccess", canAccess,
                            "accessType", input.getAccessType().equals(type.getValue())
                    );
                    return ResponseEntity.ok(result);
                });
    }

    // ==================== 统计和监控 ====================

    /**
     * 获取特性使用统计
     * GET /api/features/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.FeatureUsageStatsDto>> getFeatureStats() {
        return Mono.zip(
                featureService.getFeatureUsageStats(),
                featureService.getUserFeatureStats(),
                featureService.getWorkspaceFeatureStats()
        ).map(tuple -> {
            Map<String, Object> stats = tuple.getT1();
            FeatureDto.FeatureUsageStatsDto result = FeatureDto.FeatureUsageStatsDto.builder()
                    .totalFeatures((Long) stats.get("totalFeatures"))
                    .enabledFeatures((Long) stats.get("enabledFeatures"))
                    .deprecatedFeatures((Long) stats.get("deprecatedFeatures"))
                    .totalUserFeatures((Long) stats.get("totalUserFeatures"))
                    .totalWorkspaceFeatures((Long) stats.get("totalWorkspaceFeatures"))
                    .adminUsers((Long) stats.get("adminUsers"))
                    .earlyAccessUsers((Long) stats.get("earlyAccessUsers"))
                    .userFeatureStats(tuple.getT2())
                    .workspaceFeatureStats(tuple.getT3())
                    .build();
            return ResponseEntity.ok(result);
        });
    }

    /**
     * 获取特性配置信息
     * GET /api/features/config
     */
    @GetMapping("/config")
    public Mono<ResponseEntity<FeatureDto.FeatureConfigDto>> getFeatureConfig() {
        return Mono.fromCallable(() -> {
            FeatureDto.FeatureConfigDto config = FeatureDto.FeatureConfigDto.builder()
                    .availableUserFeatures(featureService.getAvailableUserFeatures())
                    .availableWorkspaceFeatures(featureService.getAvailableWorkspaceFeatures())
                    .configurableUserFeatures(featureService.getConfigurableUserFeatures())
                    .configurableWorkspaceFeatures(featureService.getConfigurableWorkspaceFeatures())
                    .isSelfHosted(featureService.isSelfHosted())
                    .environmentConfig(Map.of(
                            "selfHosted", featureService.isSelfHosted(),
                            "environment", featureService.isSelfHosted() ? "selfhosted" : "cloud"
                    ))
                    .build();
            return ResponseEntity.ok(config);
        });
    }

    // ==================== 批量操作 ====================

    /**
     * 批量为用户添加特性
     * POST /api/features/users/batch
     */
    @PostMapping("/users/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.BatchOperationResult>> batchAddUserFeatures(
            @Valid @RequestBody FeatureDto.BatchUserFeatureInput input) {
        return featureService.batchAddUserFeatures(input.getUserIds(), input.getFeatureName(), input.getReason())
                .map(results -> {
                    List<String> successIds = results.stream().map(uf -> uf.getUserId()).toList();
                    List<String> failureIds = input.getUserIds().stream()
                            .filter(id -> !successIds.contains(id))
                            .toList();

                    FeatureDto.BatchOperationResult result = FeatureDto.BatchOperationResult.builder()
                            .successCount(successIds.size())
                            .failureCount(failureIds.size())
                            .successIds(successIds)
                            .failureIds(failureIds)
                            .build();
                    return ResponseEntity.ok(result);
                });
    }

    /**
     * 批量为工作区添加特性
     * POST /api/features/workspaces/batch
     */
    @PostMapping("/workspaces/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.BatchOperationResult>> batchAddWorkspaceFeatures(
            @Valid @RequestBody FeatureDto.BatchWorkspaceFeatureInput input) {
        return featureService.batchAddWorkspaceFeatures(input.getWorkspaceIds(), input.getFeatureName(), input.getReason())
                .map(results -> {
                    List<String> successIds = results.stream().map(wf -> wf.getWorkspaceId()).toList();
                    List<String> failureIds = input.getWorkspaceIds().stream()
                            .filter(id -> !successIds.contains(id))
                            .toList();

                    FeatureDto.BatchOperationResult result = FeatureDto.BatchOperationResult.builder()
                            .successCount(successIds.size())
                            .failureCount(failureIds.size())
                            .successIds(successIds)
                            .failureIds(failureIds)
                            .build();
                    return ResponseEntity.ok(result);
                });
    }

    /**
     * 清理过期特性
     * POST /api/features/cleanup
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.CleanupResult>> cleanupExpiredFeatures() {
        return featureService.cleanupExpiredFeatures()
                .map(totalCleaned -> {
                    FeatureDto.CleanupResult result = FeatureDto.CleanupResult.builder()
                            .totalCleaned(totalCleaned)
                            .cleanupTime(java.time.LocalDateTime.now())
                            .build();
                    return ResponseEntity.ok(result);
                });
    }

    // ==================== 缓存管理 ====================

    /**
     * 刷新特性缓存
     * POST /api/features/cache/refresh
     */
    @PostMapping("/cache/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<FeatureDto.CacheOperationResult>> refreshCache(
            @RequestBody(required = false) FeatureDto.CacheOperationInput input) {
        
        if (input == null || "feature".equals(input.getCacheType()) || "all".equals(input.getCacheType())) {
            return featureService.refreshFeatureCache()
                    .map(v -> {
                        FeatureDto.CacheOperationResult result = FeatureDto.CacheOperationResult.builder()
                                .cacheType("feature")
                                .operation("refresh")
                                .success(true)
                                .message("特性缓存刷新成功")
                                .operationTime(java.time.LocalDateTime.now())
                                .build();
                        return ResponseEntity.ok(result);
                    });
        } else if ("user".equals(input.getCacheType()) && input.getTargetId() != null) {
            return featureService.refreshUserFeatureCache(input.getTargetId())
                    .map(v -> {
                        FeatureDto.CacheOperationResult result = FeatureDto.CacheOperationResult.builder()
                                .cacheType("user")
                                .operation("refresh")
                                .success(true)
                                .message("用户特性缓存刷新成功")
                                .operationTime(java.time.LocalDateTime.now())
                                .build();
                        return ResponseEntity.ok(result);
                    });
        } else if ("workspace".equals(input.getCacheType()) && input.getTargetId() != null) {
            return featureService.refreshWorkspaceFeatureCache(input.getTargetId())
                    .map(v -> {
                        FeatureDto.CacheOperationResult result = FeatureDto.CacheOperationResult.builder()
                                .cacheType("workspace")
                                .operation("refresh")
                                .success(true)
                                .message("工作区特性缓存刷新成功")
                                .operationTime(java.time.LocalDateTime.now())
                                .build();
                        return ResponseEntity.ok(result);
                    });
        }

        FeatureDto.CacheOperationResult result = FeatureDto.CacheOperationResult.builder()
                .operation("refresh")
                .success(false)
                .message("无效的缓存操作参数")
                .operationTime(java.time.LocalDateTime.now())
                .build();
        return Mono.just(ResponseEntity.badRequest().body(result));
    }

    // ==================== 辅助方法 ====================

    private FeatureDto.FeatureInfo convertToFeatureInfo(Feature feature) {
        return FeatureDto.FeatureInfo.builder()
                .id(feature.getId())
                .name(feature.getName())
                .displayName(feature.getDisplayName())
                .description(feature.getDescription())
                .type(feature.getType())
                .version(feature.getVersion())
                .enabled(feature.getEnabled())
                .configs(feature.getConfigs())
                .deprecated(feature.isDeprecated())
                .createdAt(feature.getCreatedAt())
                .updatedAt(feature.getUpdatedAt())
                .build();
    }

    private FeatureDto.UserFeatureInfo convertToUserFeatureInfo(UserFeature userFeature) {
        return FeatureDto.UserFeatureInfo.builder()
                .id(userFeature.getId())
                .userId(userFeature.getUserId())
                .featureName(userFeature.getName())
                .displayName(getFeatureDisplayName(userFeature.getName()))
                .type(userFeature.getType())
                .reason(userFeature.getReason())
                .activated(userFeature.isActivated())
                .createdAt(userFeature.getCreatedAt())
                .expiredAt(userFeature.getExpiredAt())
                .build();
    }

    private FeatureDto.WorkspaceFeatureInfo convertToWorkspaceFeatureInfo(WorkspaceFeature workspaceFeature) {
        return FeatureDto.WorkspaceFeatureInfo.builder()
                .id(workspaceFeature.getId())
                .workspaceId(workspaceFeature.getWorkspaceId())
                .featureName(workspaceFeature.getName())
                .displayName(getFeatureDisplayName(workspaceFeature.getName()))
                .type(workspaceFeature.getType())
                .reason(workspaceFeature.getReason())
                .activated(workspaceFeature.isActivated())
                .configs(workspaceFeature.getConfigs())
                .createdAt(workspaceFeature.getCreatedAt())
                .expiredAt(workspaceFeature.getExpiredAt())
                .build();
    }

    private String getFeatureDisplayName(String featureName) {
        try {
            Feature.FeatureName name = Feature.FeatureName.fromValue(featureName);
            Feature dummyFeature = Feature.builder().name(featureName).build();
            return dummyFeature.getDisplayName();
        } catch (Exception e) {
            return featureName;
        }
    }

    // ==================== 异常处理 ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<FeatureDto.FeatureOperationResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                .success(false)
                .message("参数错误: " + e.getMessage())
                .operationTime(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<FeatureDto.FeatureOperationResponse> handleIllegalStateException(IllegalStateException e) {
        FeatureDto.FeatureOperationResponse response = FeatureDto.FeatureOperationResponse.builder()
                .success(false)
                .message("状态错误: " + e.getMessage())
                .operationTime(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(response);
    }
}