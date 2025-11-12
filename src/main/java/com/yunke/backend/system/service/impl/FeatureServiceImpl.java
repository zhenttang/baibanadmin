package com.yunke.backend.system.service.impl;

import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;
import com.yunke.backend.system.repository.FeatureRepository;
import com.yunke.backend.user.repository.UserFeatureRepository;
import com.yunke.backend.workspace.repository.WorkspaceFeatureRepository;
import com.yunke.backend.system.service.FeatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 特性开关服务实现
 * 对应Node.js版本的FeatureService实现
 * 参考: /packages/backend/server/src/core/features/service.ts
 */
@Service
@Transactional
@Slf4j
public class FeatureServiceImpl implements FeatureService {

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private UserFeatureRepository userFeatureRepository;

    @Autowired
    private WorkspaceFeatureRepository workspaceFeatureRepository;

    @Value("${affine.selfhosted:false}")
    private boolean selfHosted;

    @Value("${affine.staff.emails:#{null}}")
    private List<String> staffEmails;

    // 预定义的特性配置
    private static final List<String> USER_FEATURES = Arrays.asList(
            "administrator", "early_access", "ai_early_access", "unlimited_copilot",
            "free_plan_v1", "pro_plan_v1", "lifetime_pro_plan_v1", "beta_feature",
            "experimental_feature", "ai_chat", "ai_writing", "ai_drawing",
            "extended_storage", "premium_storage", "custom_themes", "advanced_search", "api_access"
    );

    private static final List<String> WORKSPACE_FEATURES = Arrays.asList(
            "unlimited_workspace", "team_plan_v1", "real_time_collaboration",
            "advanced_permissions", "team_management", "advanced_export",
            "bulk_operations", "sso_integration", "audit_logs",
            "enterprise_security", "webhook_support", "plugin_system"
    );

    // 内部方法，根据环境返回可配置的用户特性
    private List<String> getInternalConfigurableUserFeatures() {
        return selfHosted ?
            Arrays.asList("administrator", "unlimited_copilot") :
            Arrays.asList("early_access", "ai_early_access", "administrator", "unlimited_copilot");
    }

    private static final List<String> CONFIGURABLE_WORKSPACE_FEATURES = Arrays.asList(
            "team_plan_v1", "real_time_collaboration", "advanced_permissions",
            "team_management", "sso_integration", "audit_logs", "enterprise_security"
    );

    // ==================== 管理员权限管理 ====================

    @Override
    public boolean isStaff(String email) {
        if (staffEmails == null || staffEmails.isEmpty()) {
            return false;
        }
        return staffEmails.contains(email);
    }

    @Override
    @Cacheable(value = "userAdminStatus", key = "#userId")
    public Mono<Boolean> isAdmin(String userId) {
        return Mono.fromCallable(() -> userFeatureRepository.isAdmin(userId));
    }

    @Override
    public Mono<UserFeature> addAdmin(String userId, String reason) {
        return Mono.fromCallable(() -> {
            // 检查是否已经是管理员
            if (userFeatureRepository.isAdmin(userId)) {
                throw new IllegalStateException("User is already an administrator");
            }

            UserFeature adminFeature = userFeatureRepository.addAdmin(userId, reason);
            refreshUserFeatureCache(userId);
            log.info("Added admin privilege to user: {}, reason: {}", userId, reason);
            return adminFeature;
        });
    }

    @Override
    @CacheEvict(value = "userAdminStatus", key = "#userId")
    public Mono<Void> removeAdmin(String userId) {
        return Mono.fromRunnable(() -> {
            userFeatureRepository.deleteByUserIdAndName(userId, "administrator");
            log.info("Removed admin privilege from user: {}", userId);
        });
    }

    @Override
    public Mono<List<UserFeature>> getAllAdmins() {
        return Mono.fromCallable(() -> userFeatureRepository.findAdministrators());
    }

    // ==================== 早期访问管理 ====================

    @Override
    public Mono<UserFeature> addEarlyAccess(String userId, Feature.EarlyAccessType type, String reason) {
        return Mono.fromCallable(() -> {
            String featureName = type == Feature.EarlyAccessType.AI ? "ai_early_access" : "early_access";
            
            // 检查是否已经有早期访问权限
            if (userFeatureRepository.hasEarlyAccess(userId, featureName)) {
                throw new IllegalStateException("User already has early access: " + type);
            }

            UserFeature earlyAccess = userFeatureRepository.addEarlyAccess(userId, featureName, reason);
            refreshUserFeatureCache(userId);
            log.info("Added early access {} to user: {}, reason: {}", type, userId, reason);
            return earlyAccess;
        });
    }

    @Override
    public Mono<Void> removeEarlyAccess(String userId, Feature.EarlyAccessType type) {
        return Mono.fromRunnable(() -> {
            String featureName = type == Feature.EarlyAccessType.AI ? "ai_early_access" : "early_access";
            userFeatureRepository.deleteByUserIdAndName(userId, featureName);
            refreshUserFeatureCache(userId);
            log.info("Removed early access {} from user: {}", type, userId);
        });
    }

    @Override
    @Cacheable(value = "userEarlyAccess", key = "#userId + '_' + #type")
    public Mono<Boolean> isEarlyAccessUser(String userId, Feature.EarlyAccessType type) {
        return Mono.fromCallable(() -> {
            String featureName = type == Feature.EarlyAccessType.AI ? "ai_early_access" : "early_access";
            return userFeatureRepository.hasEarlyAccess(userId, featureName);
        });
    }

    @Override
    public Mono<Boolean> canEarlyAccess(String email, Feature.EarlyAccessType type) {
        return Mono.fromCallable(() -> {
            // 员工邮箱自动拥有早期访问权限
            if (isStaff(email)) {
                return true;
            }

            // 检查早期访问特性的白名单配置
            Optional<Feature> earlyAccessFeature = featureRepository.findByName(
                    type == Feature.EarlyAccessType.AI ? "ai_early_access" : "early_access"
            );

            if (earlyAccessFeature.isPresent()) {
                Map<String, Object> configs = earlyAccessFeature.get().getConfigs();
                if (configs.containsKey("whitelist")) {
                    @SuppressWarnings("unchecked")
                    List<String> whitelist = (List<String>) configs.get("whitelist");
                    return whitelist.contains(email);
                }
            }

            return false;
        });
    }

    @Override
    public Mono<List<UserFeature>> getEarlyAccessUsers(Feature.EarlyAccessType type) {
        return Mono.fromCallable(() -> {
            String featureName = type == Feature.EarlyAccessType.AI ? "ai_early_access" : "early_access";
            return userFeatureRepository.findEarlyAccessUsers(featureName);
        });
    }

    // ==================== 用户特性管理 ====================

    @Override
    @Cacheable(value = "userFeatures", key = "#userId")
    public Mono<List<UserFeature>> getUserFeatures(String userId) {
        return Mono.fromCallable(() -> userFeatureRepository.findByUserId(userId));
    }

    @Override
    @Cacheable(value = "userActiveFeatures", key = "#userId")
    public Mono<List<String>> getUserActiveFeatures(String userId) {
        return Mono.fromCallable(() -> 
            userFeatureRepository.findByUserIdAndActivatedTrue(userId)
                    .stream()
                    .map(UserFeature::getName)
                    .filter(USER_FEATURES::contains) // 只返回有效的用户特性
                    .collect(Collectors.toList())
        );
    }

    @Override
    @Cacheable(value = "userFeatureCheck", key = "#userId + '_' + #featureName")
    public Mono<Boolean> hasUserFeature(String userId, String featureName) {
        return Mono.fromCallable(() -> {
            if (!isUserFeature(featureName)) {
                return false;
            }
            return userFeatureRepository.existsActiveAndNotExpired(userId, featureName, LocalDateTime.now());
        });
    }

    @Override
    public Mono<UserFeature> addUserFeature(String userId, String featureName, String reason) {
        return addUserFeature(userId, featureName, reason, null);
    }

    @Override
    public Mono<UserFeature> addUserFeature(String userId, String featureName, String reason, LocalDateTime expiredAt) {
        return Mono.fromCallable(() -> {
            if (!isUserFeature(featureName)) {
                throw new IllegalArgumentException("Invalid user feature: " + featureName);
            }

            // 检查是否已经存在
            if (userFeatureRepository.existsByUserIdAndName(userId, featureName)) {
                throw new IllegalStateException("User already has feature: " + featureName);
            }

            UserFeature userFeature = UserFeature.builder()
                    .userId(userId)
                    .name(featureName)
                    .type(isQuotaFeature(featureName) ? 1 : 0)
                    .reason(reason)
                    .expiredAt(expiredAt)
                    .activated(true)
                    .build();

            UserFeature saved = userFeatureRepository.save(userFeature);
            refreshUserFeatureCache(userId);
            log.info("Added user feature: {} to user: {}, reason: {}", featureName, userId, reason);
            return saved;
        });
    }

    @Override
    @CacheEvict(value = {"userFeatures", "userActiveFeatures", "userFeatureCheck"}, key = "#userId")
    public Mono<Void> removeUserFeature(String userId, String featureName) {
        return Mono.fromRunnable(() -> {
            userFeatureRepository.deleteByUserIdAndName(userId, featureName);
            log.info("Removed user feature: {} from user: {}", featureName, userId);
        });
    }

    @Override
    @CacheEvict(value = {"userActiveFeatures", "userFeatureCheck"}, key = "#userId")
    public Mono<Void> activateUserFeature(String userId, String featureName) {
        return Mono.fromRunnable(() -> {
            int updated = userFeatureRepository.activateFeature(userId, featureName);
            if (updated > 0) {
                log.info("Activated user feature: {} for user: {}", featureName, userId);
            }
        });
    }

    @Override
    @CacheEvict(value = {"userActiveFeatures", "userFeatureCheck"}, key = "#userId")
    public Mono<Void> deactivateUserFeature(String userId, String featureName) {
        return Mono.fromRunnable(() -> {
            int updated = userFeatureRepository.deactivateFeature(userId, featureName);
            if (updated > 0) {
                log.info("Deactivated user feature: {} for user: {}", featureName, userId);
            }
        });
    }

    @Override
    public Mono<Void> switchUserQuota(String userId, String fromPlan, String toPlan, String reason) {
        return Mono.fromRunnable(() -> {
            userFeatureRepository.switchUserPlan(userId, fromPlan, toPlan, reason);
            refreshUserFeatureCache(userId);
            log.info("Switched user plan from {} to {} for user: {}, reason: {}", fromPlan, toPlan, userId, reason);
        });
    }

    @Override
    public Mono<Optional<Map<String, Object>>> getUserQuota(String userId) {
        return Mono.fromCallable(() -> {
            // 先尝试从缓存获取
            Map<String, Object> cached = getUserQuotaCached(userId);
            if (cached != null) {
                return Optional.of(cached);
            }
            
            List<UserFeature> quotaFeatures = userFeatureRepository.findUserQuotaFeatures(userId);
            if (quotaFeatures.isEmpty()) {
                return Optional.empty();
            }

            // 返回最高级别的配额计划
            UserFeature highestPlan = quotaFeatures.stream()
                    .max(Comparator.comparing(this::getPlanPriority))
                    .orElse(null);

            if (highestPlan != null) {
                Map<String, Object> quotaConfig = getDefaultQuotaConfig(highestPlan.getName());
                // 缓存实际的配额数据而不是 Optional
                cacheUserQuota(userId, quotaConfig);
                return Optional.of(quotaConfig);
            }

            return Optional.empty();
        });
    }
    
    /**
     * 缓存用户配额 - 只缓存实际数据，不缓存 Optional
     */
    @Cacheable(value = "userQuota", key = "#userId")
    public Map<String, Object> getUserQuotaCached(String userId) {
        // 这个方法只是用于触发缓存读取，实际逻辑在 getUserQuota 中
        return null;
    }
    
    /**
     * 更新用户配额缓存
     */
    @CachePut(value = "userQuota", key = "#userId")
    public Map<String, Object> cacheUserQuota(String userId, Map<String, Object> quota) {
        return quota;
    }

    // ==================== 工作区特性管理 ====================

    @Override
    @Cacheable(value = "workspaceFeatures", key = "#workspaceId")
    public Mono<List<WorkspaceFeature>> getWorkspaceFeatures(String workspaceId) {
        return Mono.fromCallable(() -> workspaceFeatureRepository.findByWorkspaceId(workspaceId));
    }

    @Override
    @Cacheable(value = "workspaceActiveFeatures", key = "#workspaceId")
    public Mono<List<String>> getWorkspaceActiveFeatures(String workspaceId) {
        return Mono.fromCallable(() ->
            workspaceFeatureRepository.findByWorkspaceIdAndActivatedTrue(workspaceId)
                    .stream()
                    .map(WorkspaceFeature::getName)
                    .filter(WORKSPACE_FEATURES::contains)
                    .collect(Collectors.toList())
        );
    }

    @Override
    @Cacheable(value = "workspaceFeatureCheck", key = "#workspaceId + '_' + #featureName")
    public Mono<Boolean> hasWorkspaceFeature(String workspaceId, String featureName) {
        return Mono.fromCallable(() -> {
            if (!isWorkspaceFeature(featureName)) {
                return false;
            }
            return workspaceFeatureRepository.existsActiveAndNotExpired(workspaceId, featureName, LocalDateTime.now());
        });
    }

    @Override
    public Mono<WorkspaceFeature> addWorkspaceFeature(String workspaceId, String featureName, String reason) {
        return addWorkspaceFeature(workspaceId, featureName, reason, Map.of(), null);
    }

    @Override
    public Mono<WorkspaceFeature> addWorkspaceFeature(String workspaceId, String featureName, String reason, Map<String, Object> configs) {
        return addWorkspaceFeature(workspaceId, featureName, reason, configs, null);
    }

    @Override
    public Mono<WorkspaceFeature> addWorkspaceFeature(String workspaceId, String featureName, String reason, Map<String, Object> configs, LocalDateTime expiredAt) {
        return Mono.fromCallable(() -> {
            if (!isWorkspaceFeature(featureName)) {
                throw new IllegalArgumentException("Invalid workspace feature: " + featureName);
            }

            // 检查是否已经存在
            if (workspaceFeatureRepository.existsByWorkspaceIdAndName(workspaceId, featureName)) {
                throw new IllegalStateException("Workspace already has feature: " + featureName);
            }

            WorkspaceFeature workspaceFeature = WorkspaceFeature.builder()
                    .workspaceId(workspaceId)
                    .name(featureName)
                    .type(isQuotaFeature(featureName) ? 1 : 0)
                    .reason(reason)
                    .configs(configs != null ? configs : Map.of())
                    .expiredAt(expiredAt)
                    .activated(true)
                    .build();

            WorkspaceFeature saved = workspaceFeatureRepository.save(workspaceFeature);
            refreshWorkspaceFeatureCache(workspaceId);
            log.info("Added workspace feature: {} to workspace: {}, reason: {}", featureName, workspaceId, reason);
            return saved;
        });
    }

    @Override
    @CacheEvict(value = {"workspaceFeatures", "workspaceActiveFeatures", "workspaceFeatureCheck"}, key = "#workspaceId")
    public Mono<Void> removeWorkspaceFeature(String workspaceId, String featureName) {
        return Mono.fromRunnable(() -> {
            workspaceFeatureRepository.deleteByWorkspaceIdAndName(workspaceId, featureName);
            log.info("Removed workspace feature: {} from workspace: {}", featureName, workspaceId);
        });
    }

    @Override
    @CacheEvict(value = {"workspaceActiveFeatures", "workspaceFeatureCheck"}, key = "#workspaceId")
    public Mono<Void> activateWorkspaceFeature(String workspaceId, String featureName) {
        return Mono.fromRunnable(() -> {
            int updated = workspaceFeatureRepository.activateFeature(workspaceId, featureName);
            if (updated > 0) {
                log.info("Activated workspace feature: {} for workspace: {}", featureName, workspaceId);
            }
        });
    }

    @Override
    @CacheEvict(value = {"workspaceActiveFeatures", "workspaceFeatureCheck"}, key = "#workspaceId")
    public Mono<Void> deactivateWorkspaceFeature(String workspaceId, String featureName) {
        return Mono.fromRunnable(() -> {
            int updated = workspaceFeatureRepository.deactivateFeature(workspaceId, featureName);
            if (updated > 0) {
                log.info("Deactivated workspace feature: {} for workspace: {}", featureName, workspaceId);
            }
        });
    }

    @Override
    @CacheEvict(value = {"workspaceFeatures", "workspaceFeatureCheck"}, key = "#workspaceId")
    public Mono<Void> updateWorkspaceFeatureConfig(String workspaceId, String featureName, Map<String, Object> configs) {
        return Mono.fromRunnable(() -> {
            int updated = workspaceFeatureRepository.updateFeatureConfig(workspaceId, featureName, configs);
            if (updated > 0) {
                log.info("Updated workspace feature config: {} for workspace: {}", featureName, workspaceId);
            }
        });
    }

    @Override
    @Cacheable(value = "workspaceQuota", key = "#workspaceId")
    public Mono<Optional<Map<String, Object>>> getWorkspaceQuota(String workspaceId) {
        return Mono.fromCallable(() -> {
            List<WorkspaceFeature> quotaFeatures = workspaceFeatureRepository.findWorkspaceQuotaFeatures(workspaceId);
            if (quotaFeatures.isEmpty()) {
                return Optional.empty();
            }

            // 返回最高级别的配额计划
            WorkspaceFeature highestPlan = quotaFeatures.stream()
                    .max(Comparator.comparing(f -> getPlanPriority(f.getName())))
                    .orElse(null);

            if (highestPlan != null) {
                Map<String, Object> quotaConfig = new HashMap<>(getDefaultQuotaConfig(highestPlan.getName()));
                // 合并自定义配置
                quotaConfig.putAll(highestPlan.getConfigs());
                return Optional.of(quotaConfig);
            }

            return Optional.empty();
        });
    }

    // ==================== 全局特性管理 ====================

    @Override
    @Cacheable(value = "allFeatures")
    public Mono<List<Feature>> getAllFeatures() {
        return Mono.fromCallable(() -> featureRepository.findAll());
    }

    @Override
    @Cacheable(value = "enabledFeatures")
    public Mono<List<Feature>> getEnabledFeatures() {
        return Mono.fromCallable(() -> featureRepository.findByEnabledTrue());
    }

    @Override
    public Mono<Feature> createFeature(String name, String description, Map<String, Object> configs, Feature.FeatureType type) {
        return Mono.fromCallable(() -> {
            if (featureRepository.findByName(name).isPresent()) {
                throw new IllegalStateException("Feature already exists: " + name);
            }

            Feature feature = Feature.builder()
                    .name(name)
                    .description(description)
                    .configs(configs != null ? configs : Map.of())
                    .type(type.getValue())
                    .enabled(true)
                    .version(1)
                    .build();

            Feature saved = featureRepository.save(feature);
            refreshFeatureCache();
            log.info("Created feature: {}, type: {}", name, type);
            return saved;
        });
    }

    @Override
    @CacheEvict(value = {"allFeatures", "enabledFeatures", "featureConfig"}, allEntries = true)
    public Mono<Feature> updateFeature(String name, String description, Map<String, Object> configs) {
        return Mono.fromCallable(() -> {
            Optional<Feature> optionalFeature = featureRepository.findByName(name);
            if (optionalFeature.isEmpty()) {
                throw new IllegalArgumentException("Feature not found: " + name);
            }

            Feature feature = optionalFeature.get();
            feature.setDescription(description);
            feature.setConfigs(configs != null ? configs : Map.of());
            feature.setUpdatedAt(LocalDateTime.now());

            Feature saved = featureRepository.save(feature);
            log.info("Updated feature: {}", name);
            return saved;
        });
    }

    @Override
    @CacheEvict(value = {"enabledFeatures"}, allEntries = true)
    public Mono<Void> enableFeature(String name) {
        return Mono.fromRunnable(() -> {
            Optional<Feature> optionalFeature = featureRepository.findByName(name);
            if (optionalFeature.isPresent()) {
                Feature feature = optionalFeature.get();
                feature.setEnabled(true);
                featureRepository.save(feature);
                log.info("Enabled feature: {}", name);
            }
        });
    }

    @Override
    @CacheEvict(value = {"enabledFeatures"}, allEntries = true)
    public Mono<Void> disableFeature(String name) {
        return Mono.fromRunnable(() -> {
            Optional<Feature> optionalFeature = featureRepository.findByName(name);
            if (optionalFeature.isPresent()) {
                Feature feature = optionalFeature.get();
                feature.setEnabled(false);
                featureRepository.save(feature);
                log.info("Disabled feature: {}", name);
            }
        });
    }

    @Override
    @CacheEvict(value = {"allFeatures", "enabledFeatures"}, allEntries = true)
    public Mono<Void> deprecateFeature(String name, Integer version) {
        return Mono.fromRunnable(() -> {
            Optional<Feature> optionalFeature = featureRepository.findByName(name);
            if (optionalFeature.isPresent()) {
                Feature feature = optionalFeature.get();
                feature.setDeprecatedVersion(version);
                featureRepository.save(feature);
                log.info("Deprecated feature: {} at version: {}", name, version);
            }
        });
    }

    // ==================== 特性检查和验证 ====================

    @Override
    @Cacheable(value = "featureEnabled", key = "#featureName")
    public Mono<Boolean> isFeatureEnabled(String featureName) {
        return Mono.fromCallable(() -> featureRepository.isFeatureEnabledByName(featureName));
    }

    @Override
    @Cacheable(value = "featureConfig", key = "#featureName")
    public Mono<Optional<Map<String, Object>>> getFeatureConfig(String featureName) {
        return Mono.fromCallable(() -> 
            featureRepository.findByName(featureName).map(Feature::getConfigs)
        );
    }

    @Override
    public boolean isValidFeatureName(String featureName) {
        try {
            Feature.FeatureName.fromValue(featureName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean isUserFeature(String featureName) {
        return USER_FEATURES.contains(featureName);
    }

    @Override
    public boolean isWorkspaceFeature(String featureName) {
        return WORKSPACE_FEATURES.contains(featureName);
    }

    @Override
    public boolean isQuotaFeature(String featureName) {
        return featureName.contains("_plan_") || featureName.contains("storage");
    }

    // ==================== 配置管理 ====================

    @Override
    public List<String> getConfigurableUserFeatures() {
        return new ArrayList<>(getInternalConfigurableUserFeatures());
    }

    @Override
    public List<String> getConfigurableWorkspaceFeatures() {
        return new ArrayList<>(CONFIGURABLE_WORKSPACE_FEATURES);
    }

    @Override
    public List<String> getAvailableUserFeatures() {
        return new ArrayList<>(USER_FEATURES);
    }

    @Override
    public List<String> getAvailableWorkspaceFeatures() {
        return new ArrayList<>(WORKSPACE_FEATURES);
    }

    @Override
    public boolean isSelfHosted() {
        return selfHosted;
    }

    // ==================== 统计相关 ====================

    @Override
    public Mono<Map<String, Object>> getFeatureUsageStats() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalFeatures", featureRepository.count());
            stats.put("enabledFeatures", featureRepository.countEnabledFeatures());
            stats.put("deprecatedFeatures", featureRepository.countDeprecatedFeatures());
            stats.put("totalUserFeatures", userFeatureRepository.count());
            stats.put("totalWorkspaceFeatures", workspaceFeatureRepository.count());
            stats.put("adminUsers", userFeatureRepository.countAdministrators());
            stats.put("earlyAccessUsers", userFeatureRepository.countEarlyAccessUsers());
            return stats;
        });
    }

    @Override
    public Mono<Map<String, Long>> getUserFeatureStats() {
        return Mono.fromCallable(() -> {
            Map<String, Long> stats = new HashMap<>();
            for (String feature : USER_FEATURES) {
                stats.put(feature, userFeatureRepository.countUsersWithFeature(feature));
            }
            return stats;
        });
    }

    @Override
    public Mono<Map<String, Long>> getWorkspaceFeatureStats() {
        return Mono.fromCallable(() -> {
            Map<String, Long> stats = new HashMap<>();
            for (String feature : WORKSPACE_FEATURES) {
                stats.put(feature, workspaceFeatureRepository.countWorkspacesWithFeature(feature));
            }
            return stats;
        });
    }

    @Override
    public Mono<Long> countUsersWithFeature(String featureName) {
        return Mono.fromCallable(() -> userFeatureRepository.countUsersWithFeature(featureName));
    }

    @Override
    public Mono<Long> countWorkspacesWithFeature(String featureName) {
        return Mono.fromCallable(() -> workspaceFeatureRepository.countWorkspacesWithFeature(featureName));
    }

    // ==================== 缓存相关 ====================

    @Override
    @CacheEvict(value = {"allFeatures", "enabledFeatures", "featureConfig", "featureEnabled"}, allEntries = true)
    public Mono<Void> refreshFeatureCache() {
        return Mono.fromRunnable(() -> log.info("Refreshed feature cache"));
    }

    @Override
    @CacheEvict(value = {"userFeatures", "userActiveFeatures", "userFeatureCheck", "userQuota", "userAdminStatus", "userEarlyAccess"}, key = "#userId")
    public Mono<Void> refreshUserFeatureCache(String userId) {
        return Mono.fromRunnable(() -> log.info("Refreshed user feature cache for: {}", userId));
    }

    @Override
    @CacheEvict(value = {"workspaceFeatures", "workspaceActiveFeatures", "workspaceFeatureCheck", "workspaceQuota"}, key = "#workspaceId")
    public Mono<Void> refreshWorkspaceFeatureCache(String workspaceId) {
        return Mono.fromRunnable(() -> log.info("Refreshed workspace feature cache for: {}", workspaceId));
    }

    @Override
    public Mono<Void> warmupFeatureCache() {
        return Mono.fromRunnable(() -> {
            // 预热常用的特性缓存 (fire-and-forget)
            getAllFeatures()
                .doOnError(e -> log.warn("Failed to warmup all features cache", e))
                .subscribe();
            getEnabledFeatures()
                .doOnError(e -> log.warn("Failed to warmup enabled features cache", e))
                .subscribe();
            log.info("Warmed up feature cache");
        });
    }

    // ==================== 批量操作和A/B测试等其他方法 ====================
    
    // 这些方法的完整实现会很长，这里提供基础框架
    @Override
    public Mono<List<UserFeature>> batchAddUserFeatures(List<String> userIds, String featureName, String reason) {
        return Mono.fromCallable(() -> {
            List<UserFeature> features = new ArrayList<>();
            for (String userId : userIds) {
                try {
                    UserFeature feature = addUserFeature(userId, featureName, reason).block();
                    if (feature != null) {
                        features.add(feature);
                    }
                } catch (Exception e) {
                    log.warn("Failed to add feature {} to user {}: {}", featureName, userId, e.getMessage());
                }
            }
            return features;
        });
    }

    @Override
    public Mono<List<WorkspaceFeature>> batchAddWorkspaceFeatures(List<String> workspaceIds, String featureName, String reason) {
        return Mono.fromCallable(() -> {
            List<WorkspaceFeature> features = new ArrayList<>();
            for (String workspaceId : workspaceIds) {
                try {
                    WorkspaceFeature feature = addWorkspaceFeature(workspaceId, featureName, reason).block();
                    if (feature != null) {
                        features.add(feature);
                    }
                } catch (Exception e) {
                    log.warn("Failed to add feature {} to workspace {}: {}", featureName, workspaceId, e.getMessage());
                }
            }
            return features;
        });
    }

    @Override
    public Mono<Integer> batchActivateUserFeature(List<String> userIds, String featureName) {
        return Mono.fromCallable(() -> userFeatureRepository.batchActivateFeature(userIds, featureName));
    }

    @Override
    public Mono<Integer> batchActivateWorkspaceFeature(List<String> workspaceIds, String featureName) {
        return Mono.fromCallable(() -> workspaceFeatureRepository.batchActivateFeature(workspaceIds, featureName));
    }

    @Override
    public Mono<Integer> cleanupExpiredFeatures() {
        return Mono.fromCallable(() -> {
            LocalDateTime now = LocalDateTime.now();
            int userFeatures = userFeatureRepository.deleteExpiredFeatures(now);
            int workspaceFeatures = workspaceFeatureRepository.deleteExpiredFeatures(now);
            log.info("Cleaned up {} expired user features and {} expired workspace features", userFeatures, workspaceFeatures);
            return userFeatures + workspaceFeatures;
        });
    }

    // A/B测试相关方法的基础实现
    @Override
    public Mono<Boolean> assignExperimentalFeature(String userId, String featureName, double probability) {
        return Mono.fromCallable(() -> {
            // 简单的概率分配算法
            String hash = Integer.toString((userId + featureName).hashCode());
            double userProbability = Math.abs(hash.hashCode() % 100) / 100.0;
            
            if (userProbability < probability) {
                addUserFeature(userId, featureName, "A/B test assignment").block();
                return true;
            }
            return false;
        });
    }

    @Override
    public Mono<Boolean> isUserInExperiment(String userId, String experimentName) {
        return hasUserFeature(userId, experimentName);
    }

    @Override
    public Mono<List<String>> getExperimentUsers(String experimentName) {
        return Mono.fromCallable(() -> 
            userFeatureRepository.findEarlyAccessUsers(experimentName)
                    .stream()
                    .map(UserFeature::getUserId)
                    .collect(Collectors.toList())
        );
    }

    @Override
    public Mono<Void> endExperiment(String experimentName, boolean retainFeature) {
        return Mono.fromRunnable(() -> {
            if (!retainFeature) {
                // 移除所有实验特性
                userFeatureRepository.findEarlyAccessUsers(experimentName)
                        .forEach(uf -> userFeatureRepository.deleteByUserIdAndName(uf.getUserId(), experimentName));
                log.info("Ended experiment {} and removed feature from all users", experimentName);
            } else {
                log.info("Ended experiment {} and retained feature for all users", experimentName);
            }
        });
    }

    @Override
    public Mono<List<Feature>> getEnvironmentFeatures() {
        return Mono.fromCallable(() -> 
            featureRepository.findByEnvironment(selfHosted ? "selfhosted" : "cloud")
        );
    }

    @Override
    public Mono<Boolean> isFeatureAvailableInEnvironment(String featureName) {
        return Mono.fromCallable(() -> {
            Optional<Feature> feature = featureRepository.findByName(featureName);
            if (feature.isEmpty()) {
                return false;
            }

            Map<String, Object> configs = feature.get().getConfigs();
            if (configs.containsKey("environment")) {
                String environment = (String) configs.get("environment");
                return environment.equals(selfHosted ? "selfhosted" : "cloud");
            }

            // 如果没有环境限制，则在所有环境中可用
            return true;
        });
    }

    // ==================== 辅助方法 ====================

    private int getPlanPriority(UserFeature userFeature) {
        return getPlanPriority(userFeature.getName());
    }

    private int getPlanPriority(String planName) {
        return switch (planName) {
            case "free_plan_v1" -> 1;
            case "pro_plan_v1" -> 2;
            case "lifetime_pro_plan_v1" -> 3;
            case "team_plan_v1" -> 4;
            default -> 0;
        };
    }

    private Map<String, Object> getDefaultQuotaConfig(String planName) {
        return switch (planName) {
            case "free_plan_v1" -> Map.of(
                "name", "Free Plan",
                "blobLimit", 10 * 1024 * 1024, // 10MB
                "storageQuota", 10L * 1024 * 1024 * 1024, // 10GB
                "historyPeriod", 7, // 7 days
                "memberLimit", 3,
                "copilotActionLimit", 10
            );
            case "pro_plan_v1" -> Map.of(
                "name", "Pro Plan",
                "blobLimit", 100 * 1024 * 1024, // 100MB
                "storageQuota", 100L * 1024 * 1024 * 1024, // 100GB
                "historyPeriod", 30, // 30 days
                "memberLimit", 1,
                "copilotActionLimit", 500
            );
            case "team_plan_v1" -> Map.of(
                "name", "Team Plan",
                "blobLimit", 100 * 1024 * 1024, // 100MB
                "storageQuota", 1000L * 1024 * 1024 * 1024, // 1TB
                "historyPeriod", 365, // 365 days
                "memberLimit", 100,
                "copilotActionLimit", 5000
            );
            default -> Map.of();
        };
    }

    @Override
    public Integer getFeatureType(String featureName) {
        return isQuotaFeature(featureName) ? Feature.FeatureType.QUOTA.getValue() : Feature.FeatureType.FEATURE.getValue();
    }

    @Override  
    public Mono<Feature> get(String name) {
        return Mono.fromCallable(() -> 
            featureRepository.findByName(name).orElse(null)
        );
    }
}