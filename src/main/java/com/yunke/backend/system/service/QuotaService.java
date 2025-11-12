package com.yunke.backend.system.service;


import com.yunke.backend.user.domain.entity.UserQuota;
import com.yunke.backend.workspace.domain.entity.WorkspaceQuota;
import com.yunke.backend.system.domain.entity.QuotaUsage;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配额管理服务接口
 * 对应Node.js版本的QuotaService
 * 参考: /packages/backend/server/src/core/quota/service.ts
 */
public interface QuotaService {

    // ==================== 用户配额管理 ====================

    /**
     * 获取用户配额
     */
    Mono<Optional<UserQuota>> getUserQuota(String userId);

    /**
     * 获取用户配额和使用情况
     */
    Mono<Map<String, Object>> getUserQuotaWithUsage(String userId);

    /**
     * 创建用户基础配额
     */
    Mono<UserQuota> setupUserBaseQuota(String userId);

    /**
     * 更新用户配额
     */
    Mono<UserQuota> updateUserQuota(String userId, UserQuota quota);

    /**
     * 切换用户配额计划
     */
    Mono<UserQuota> switchUserQuota(String userId, UserQuota.QuotaPlan fromPlan, 
                                    UserQuota.QuotaPlan toPlan, String reason);

    /**
     * 升级用户到专业版
     */
    Mono<UserQuota> upgradeUserToPro(String userId, LocalDateTime expiredAt);

    /**
     * 升级用户到终身专业版
     */
    Mono<UserQuota> upgradeUserToLifetimePro(String userId);

    /**
     * 降级用户到免费版
     */
    Mono<UserQuota> downgradeUserToFree(String userId);

    /**
     * 获取用户存储使用量
     */
    Mono<Long> getUserStorageUsage(String userId);

    /**
     * 获取用户成员使用量
     */
    Mono<Integer> getUserMemberUsage(String userId);

    /**
     * 获取用户AI操作使用量
     */
    Mono<Integer> getUserCopilotUsage(String userId);

    // ==================== 工作空间配额管理 ====================

    /**
     * 获取工作空间配额
     */
    Mono<Optional<WorkspaceQuota>> getWorkspaceQuota(String workspaceId);

    /**
     * 获取工作空间配额和使用情况
     */
    Mono<Map<String, Object>> getWorkspaceQuotaWithUsage(String workspaceId);

    /**
     * 创建工作空间基础配额
     */
    Mono<WorkspaceQuota> setupWorkspaceBaseQuota(String workspaceId);

    /**
     * 更新工作空间配额
     */
    Mono<WorkspaceQuota> updateWorkspaceQuota(String workspaceId, WorkspaceQuota quota);

    /**
     * 切换工作空间配额计划
     */
    Mono<WorkspaceQuota> switchWorkspaceQuota(String workspaceId, 
                                              WorkspaceQuota.WorkspaceQuotaPlan fromPlan,
                                              WorkspaceQuota.WorkspaceQuotaPlan toPlan, 
                                              String reason);

    /**
     * 升级工作空间到专业版
     */
    Mono<WorkspaceQuota> upgradeWorkspaceToPro(String workspaceId, LocalDateTime expiredAt);

    /**
     * 升级工作空间到团队版
     */
    Mono<WorkspaceQuota> upgradeWorkspaceToTeam(String workspaceId, int seatCount, 
                                                LocalDateTime expiredAt);

    /**
     * 降级工作空间到免费版
     */
    Mono<WorkspaceQuota> downgradeWorkspaceToFree(String workspaceId);

    /**
     * 获取工作空间存储使用量
     */
    Mono<Long> getWorkspaceStorageUsage(String workspaceId);

    /**
     * 获取工作空间成员使用量
     */
    Mono<Integer> getWorkspaceMemberUsage(String workspaceId);

    /**
     * 获取工作空间AI操作使用量
     */
    Mono<Integer> getWorkspaceCopilotUsage(String workspaceId);

    // ==================== 席位管理 ====================

    /**
     * 检查席位配额
     */
    Mono<Boolean> checkSeat(String workspaceId, int requiredSeats);

    /**
     * 尝试检查席位配额（不抛异常）
     */
    Mono<Boolean> tryCheckSeat(String workspaceId, int requiredSeats);

    /**
     * 获取工作空间席位配额
     */
    Mono<Map<String, Object>> getWorkspaceSeatQuota(String workspaceId);

    /**
     * 添加席位
     */
    Mono<Boolean> addSeats(String workspaceId, int additionalSeats);

    /**
     * 移除席位
     */
    Mono<Boolean> removeSeats(String workspaceId, int seatsToRemove);

    /**
     * 设置席位数量
     */
    Mono<Boolean> setSeats(String workspaceId, int totalSeats);

    // ==================== 配额检查 ====================

    /**
     * 检查存储配额
     */
    Mono<Boolean> checkStorageQuota(String targetId, QuotaUsage.UsageType usageType, 
                                    long additionalSize);

    /**
     * 检查文件大小限制
     */
    Mono<Boolean> checkBlobSizeLimit(String targetId, QuotaUsage.UsageType usageType, 
                                     long blobSize);

    /**
     * 检查成员配额
     */
    Mono<Boolean> checkMemberQuota(String targetId, QuotaUsage.UsageType usageType, 
                                   int additionalMembers);

    /**
     * 检查AI操作配额
     */
    Mono<Boolean> checkCopilotQuota(String targetId, QuotaUsage.UsageType usageType, 
                                    int additionalActions);

    /**
     * 获取有效存储配额
     */
    Mono<Long> getEffectiveStorageQuota(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 获取有效成员限制
     */
    Mono<Integer> getEffectiveMemberLimit(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 获取有效AI操作限制
     */
    Mono<Integer> getEffectiveCopilotLimit(String targetId, QuotaUsage.UsageType usageType);

    // ==================== 使用量管理 ====================

    /**
     * 记录存储使用
     */
    Mono<Void> recordStorageUsage(String targetId, QuotaUsage.UsageType usageType, 
                                  long bytes, boolean isIncrease);

    /**
     * 记录成员使用
     */
    Mono<Void> recordMemberUsage(String targetId, QuotaUsage.UsageType usageType, 
                                 int memberCount);

    /**
     * 记录AI操作使用
     */
    Mono<Void> recordCopilotUsage(String targetId, QuotaUsage.UsageType usageType, 
                                  int actions);

    /**
     * 记录文件操作
     */
    Mono<Void> recordFileOperation(String targetId, QuotaUsage.UsageType usageType, 
                                   int fileCount, boolean isIncrease);

    /**
     * 记录文档操作
     */
    Mono<Void> recordDocumentOperation(String targetId, QuotaUsage.UsageType usageType, 
                                       int documentCount, boolean isIncrease);

    /**
     * 获取或创建使用记录
     */
    Mono<QuotaUsage> getOrCreateUsageRecord(String targetId, QuotaUsage.UsageType usageType);

    // ==================== 配额计算器 ====================

    /**
     * 获取用户配额计算器
     */
    Mono<Map<String, Object>> getUserQuotaCalculator(String userId);

    /**
     * 获取工作空间配额计算器
     */
    Mono<Map<String, Object>> getWorkspaceQuotaCalculator(String workspaceId);

    /**
     * 计算配额使用率
     */
    Mono<Map<String, Double>> calculateUsageRates(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 计算剩余配额
     */
    Mono<Map<String, Object>> calculateRemainingQuota(String targetId, QuotaUsage.UsageType usageType);

    // ==================== 格式化功能 ====================

    /**
     * 格式化用户配额信息
     */
    Mono<Map<String, Object>> formatUserQuota(String userId);

    /**
     * 格式化工作空间配额信息
     */
    Mono<Map<String, Object>> formatWorkspaceQuota(String workspaceId);

    /**
     * 格式化存储大小
     */
    String formatStorageSize(long bytes);

    /**
     * 格式化配额限制
     */
    Map<String, String> formatQuotaLimits(UserQuota userQuota);

    /**
     * 格式化配额限制
     */
    Map<String, String> formatQuotaLimits(WorkspaceQuota workspaceQuota);

    // ==================== 配额重置 ====================

    /**
     * 重置配额使用量
     */
    Mono<Integer> resetQuotaUsage();

    /**
     * 重置用户AI操作配额
     */
    Mono<Void> resetUserCopilotQuota(String userId);

    /**
     * 重置工作空间AI操作配额
     */
    Mono<Void> resetWorkspaceCopilotQuota(String workspaceId);

    /**
     * 处理过期配额
     */
    Mono<Integer> handleExpiredQuotas();

    /**
     * 自动降级过期配额
     */
    Mono<Integer> autoDowngradeExpiredQuotas();

    // ==================== 统计和监控 ====================

    /**
     * 获取配额统计信息
     */
    Mono<Map<String, Object>> getQuotaStatistics();

    /**
     * 获取使用量统计信息
     */
    Mono<Map<String, Object>> getUsageStatistics();

    /**
     * 获取配额使用趋势
     */
    Mono<List<Map<String, Object>>> getUsageTrend(QuotaUsage.UsageType usageType, int months);

    /**
     * 获取热门用户排行榜
     */
    Mono<List<Map<String, Object>>> getTopUsersByUsage(String usageType, int limit);

    /**
     * 获取热门工作空间排行榜
     */
    Mono<List<Map<String, Object>>> getTopWorkspacesByUsage(String usageType, int limit);

    /**
     * 获取配额告警列表
     */
    Mono<List<Map<String, Object>>> getQuotaAlerts(double threshold);

    // ==================== 批量操作 ====================

    /**
     * 批量创建用户配额
     */
    Mono<List<UserQuota>> batchCreateUserQuotas(List<String> userIds);

    /**
     * 批量创建工作空间配额
     */
    Mono<List<WorkspaceQuota>> batchCreateWorkspaceQuotas(List<String> workspaceIds);

    /**
     * 批量更新配额计划
     */
    Mono<Integer> batchUpdateUserQuotaPlan(List<String> userIds, UserQuota.QuotaPlan plan);

    /**
     * 批量更新工作空间配额计划
     */
    Mono<Integer> batchUpdateWorkspaceQuotaPlan(List<String> workspaceIds, 
                                                WorkspaceQuota.WorkspaceQuotaPlan plan);

    /**
     * 批量设置配额过期时间
     */
    Mono<Integer> batchSetQuotaExpiration(List<String> targetIds, QuotaUsage.UsageType usageType,
                                          LocalDateTime expiredAt);

    // ==================== 配额验证 ====================

    /**
     * 验证配额配置
     */
    Mono<Boolean> validateQuotaConfiguration(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 修复无效配额
     */
    Mono<Integer> fixInvalidQuotas();

    /**
     * 同步配额和使用量
     */
    Mono<Void> syncQuotaAndUsage(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 清理孤立的使用记录
     */
    Mono<Integer> cleanupOrphanedUsageRecords();

    // ==================== 配额通知 ====================

    /**
     * 检查并发送配额告警
     */
    Mono<Void> checkAndSendQuotaAlerts();

    /**
     * 发送配额即将到期通知
     */
    Mono<Void> sendQuotaExpirationNotifications();

    /**
     * 发送配额超限通知
     */
    Mono<Void> sendQuotaExceededNotification(String targetId, QuotaUsage.UsageType usageType, 
                                             String quotaType);

    /**
     * 发送配额升级建议
     */
    Mono<Void> sendQuotaUpgradeRecommendation(String targetId, QuotaUsage.UsageType usageType);

    // ==================== 高级功能 ====================

    /**
     * 预测配额使用量
     */
    Mono<Map<String, Object>> predictQuotaUsage(String targetId, QuotaUsage.UsageType usageType, 
                                                int futureDays);

    /**
     * 优化配额分配建议
     */
    Mono<Map<String, Object>> getQuotaOptimizationSuggestions(String targetId, 
                                                               QuotaUsage.UsageType usageType);

    /**
     * 计算配额成本
     */
    Mono<Map<String, Object>> calculateQuotaCost(String targetId, QuotaUsage.UsageType usageType);

    /**
     * 导出配额报告
     */
    Mono<String> exportQuotaReport(String targetId, QuotaUsage.UsageType usageType, 
                                   LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 配额健康检查
     */
    Mono<Map<String, Object>> performQuotaHealthCheck();
}