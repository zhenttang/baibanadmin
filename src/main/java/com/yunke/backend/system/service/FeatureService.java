package com.yunke.backend.system.service;

import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 特性开关服务接口
 * 对应Node.js版本的FeatureService
 * 参考: /packages/backend/server/src/core/features/service.ts
 */
public interface FeatureService {

    // ==================== 管理员权限管理 ====================

    /**
     * 检查邮箱是否为内部员工
     */
    boolean isStaff(String email);

    /**
     * 检查用户是否为管理员
     */
    Mono<Boolean> isAdmin(String userId);

    /**
     * 添加管理员权限
     */
    Mono<UserFeature> addAdmin(String userId, String reason);

    /**
     * 移除管理员权限
     */
    Mono<Void> removeAdmin(String userId);

    /**
     * 获取所有管理员用户
     */
    Mono<List<UserFeature>> getAllAdmins();

    // ==================== 早期访问管理 ====================

    /**
     * 添加早期访问权限
     */
    Mono<UserFeature> addEarlyAccess(String userId, Feature.EarlyAccessType type, String reason);

    /**
     * 移除早期访问权限
     */
    Mono<Void> removeEarlyAccess(String userId, Feature.EarlyAccessType type);

    /**
     * 检查用户是否有早期访问权限
     */
    Mono<Boolean> isEarlyAccessUser(String userId, Feature.EarlyAccessType type);

    /**
     * 检查邮箱是否可以早期访问
     * 基于白名单或其他策略
     */
    Mono<Boolean> canEarlyAccess(String email, Feature.EarlyAccessType type);

    /**
     * 获取所有早期访问用户
     */
    Mono<List<UserFeature>> getEarlyAccessUsers(Feature.EarlyAccessType type);

    // ==================== 用户特性管理 ====================

    /**
     * 获取用户的所有特性
     */
    Mono<List<UserFeature>> getUserFeatures(String userId);

    /**
     * 获取用户的激活特性
     */
    Mono<List<String>> getUserActiveFeatures(String userId);

    /**
     * 检查用户是否有特定特性
     */
    Mono<Boolean> hasUserFeature(String userId, String featureName);

    /**
     * 为用户添加特性
     */
    Mono<UserFeature> addUserFeature(String userId, String featureName, String reason);

    /**
     * 为用户添加特性 (带过期时间)
     */
    Mono<UserFeature> addUserFeature(String userId, String featureName, String reason, LocalDateTime expiredAt);

    /**
     * 移除用户特性
     */
    Mono<Void> removeUserFeature(String userId, String featureName);

    /**
     * 激活用户特性
     */
    Mono<Void> activateUserFeature(String userId, String featureName);

    /**
     * 停用用户特性
     */
    Mono<Void> deactivateUserFeature(String userId, String featureName);

    /**
     * 切换用户订阅计划
     */
    Mono<Void> switchUserQuota(String userId, String fromPlan, String toPlan, String reason);

    /**
     * 获取用户配额信息
     */
    Mono<Optional<Map<String, Object>>> getUserQuota(String userId);

    // ==================== 工作区特性管理 ====================

    /**
     * 获取工作区的所有特性
     */
    Mono<List<WorkspaceFeature>> getWorkspaceFeatures(String workspaceId);

    /**
     * 获取工作区的激活特性
     */
    Mono<List<String>> getWorkspaceActiveFeatures(String workspaceId);

    /**
     * 检查工作区是否有特定特性
     */
    Mono<Boolean> hasWorkspaceFeature(String workspaceId, String featureName);

    /**
     * 为工作区添加特性
     */
    Mono<WorkspaceFeature> addWorkspaceFeature(String workspaceId, String featureName, String reason);

    /**
     * 为工作区添加特性 (带配置)
     */
    Mono<WorkspaceFeature> addWorkspaceFeature(String workspaceId, String featureName, String reason, Map<String, Object> configs);

    /**
     * 为工作区添加特性 (带配置和过期时间)
     */
    Mono<WorkspaceFeature> addWorkspaceFeature(String workspaceId, String featureName, String reason, Map<String, Object> configs, LocalDateTime expiredAt);

    /**
     * 移除工作区特性
     */
    Mono<Void> removeWorkspaceFeature(String workspaceId, String featureName);

    /**
     * 激活工作区特性
     */
    Mono<Void> activateWorkspaceFeature(String workspaceId, String featureName);

    /**
     * 停用工作区特性
     */
    Mono<Void> deactivateWorkspaceFeature(String workspaceId, String featureName);

    /**
     * 更新工作区特性配置
     */
    Mono<Void> updateWorkspaceFeatureConfig(String workspaceId, String featureName, Map<String, Object> configs);

    /**
     * 获取工作区配额信息
     */
    Mono<Optional<Map<String, Object>>> getWorkspaceQuota(String workspaceId);

    // ==================== 全局特性管理 ====================

    /**
     * 获取所有可用特性
     */
    Mono<List<Feature>> getAllFeatures();

    /**
     * 获取所有启用的特性
     */
    Mono<List<Feature>> getEnabledFeatures();

    /**
     * 创建新特性
     */
    Mono<Feature> createFeature(String name, String description, Map<String, Object> configs, Feature.FeatureType type);

    /**
     * 更新特性配置
     */
    Mono<Feature> updateFeature(String name, String description, Map<String, Object> configs);

    /**
     * 启用特性
     */
    Mono<Void> enableFeature(String name);

    /**
     * 禁用特性
     */
    Mono<Void> disableFeature(String name);

    /**
     * 废弃特性
     */
    Mono<Void> deprecateFeature(String name, Integer version);

    // ==================== 特性检查和验证 ====================

    /**
     * 获取特性
     * 对应: UserFeatureService和WorkspaceFeatureService中使用的方法
     */
    Mono<Feature> get(String featureName);

    /**
     * 获取特性类型
     * 对应: 特性服务中需要的方法
     */
    Integer getFeatureType(String featureName);

    /**
     * 检查特性是否存在且启用
     */
    Mono<Boolean> isFeatureEnabled(String featureName);

    /**
     * 获取特性配置
     */
    Mono<Optional<Map<String, Object>>> getFeatureConfig(String featureName);

    /**
     * 验证特性名称是否有效
     */
    boolean isValidFeatureName(String featureName);

    /**
     * 检查特性是否为用户特性
     */
    boolean isUserFeature(String featureName);

    /**
     * 检查特性是否为工作区特性
     */
    boolean isWorkspaceFeature(String featureName);

    /**
     * 检查特性是否为配额特性
     */
    boolean isQuotaFeature(String featureName);

    // ==================== 批量操作 ====================

    /**
     * 批量为用户添加特性
     */
    Mono<List<UserFeature>> batchAddUserFeatures(List<String> userIds, String featureName, String reason);

    /**
     * 批量为工作区添加特性
     */
    Mono<List<WorkspaceFeature>> batchAddWorkspaceFeatures(List<String> workspaceIds, String featureName, String reason);

    /**
     * 批量激活用户特性
     */
    Mono<Integer> batchActivateUserFeature(List<String> userIds, String featureName);

    /**
     * 批量激活工作区特性
     */
    Mono<Integer> batchActivateWorkspaceFeature(List<String> workspaceIds, String featureName);

    /**
     * 清理过期特性
     */
    Mono<Integer> cleanupExpiredFeatures();

    // ==================== 统计和监控 ====================

    /**
     * 获取特性使用统计
     */
    Mono<Map<String, Object>> getFeatureUsageStats();

    /**
     * 获取用户特性统计
     */
    Mono<Map<String, Long>> getUserFeatureStats();

    /**
     * 获取工作区特性统计
     */
    Mono<Map<String, Long>> getWorkspaceFeatureStats();

    /**
     * 获取特定特性的用户数量
     */
    Mono<Long> countUsersWithFeature(String featureName);

    /**
     * 获取特定特性的工作区数量
     */
    Mono<Long> countWorkspacesWithFeature(String featureName);

    // ==================== 配置管理 ====================

    /**
     * 获取可配置的用户特性列表
     */
    List<String> getConfigurableUserFeatures();

    /**
     * 获取可配置的工作区特性列表
     */
    List<String> getConfigurableWorkspaceFeatures();

    /**
     * 获取可用的用户特性列表
     */
    List<String> getAvailableUserFeatures();

    /**
     * 获取可用的工作区特性列表
     */
    List<String> getAvailableWorkspaceFeatures();

    // ==================== A/B测试支持 ====================

    /**
     * 为用户分配实验性特性
     */
    Mono<Boolean> assignExperimentalFeature(String userId, String featureName, double probability);

    /**
     * 检查用户是否在实验组中
     */
    Mono<Boolean> isUserInExperiment(String userId, String experimentName);

    /**
     * 获取实验组用户列表
     */
    Mono<List<String>> getExperimentUsers(String experimentName);

    /**
     * 结束实验并处理用户特性
     */
    Mono<Void> endExperiment(String experimentName, boolean retainFeature);

    // ==================== 环境相关 ====================

    /**
     * 检查是否为自托管环境
     */
    boolean isSelfHosted();

    /**
     * 获取环境特定的特性配置
     */
    Mono<List<Feature>> getEnvironmentFeatures();

    /**
     * 检查特性是否在当前环境中可用
     */
    Mono<Boolean> isFeatureAvailableInEnvironment(String featureName);

    // ==================== 缓存相关 ====================

    /**
     * 刷新特性缓存
     */
    Mono<Void> refreshFeatureCache();

    /**
     * 刷新用户特性缓存
     */
    Mono<Void> refreshUserFeatureCache(String userId);

    /**
     * 刷新工作区特性缓存
     */
    Mono<Void> refreshWorkspaceFeatureCache(String workspaceId);

    /**
     * 预热特性缓存
     */
    Mono<Void> warmupFeatureCache();
}