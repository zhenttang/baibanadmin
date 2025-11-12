package com.yunke.backend.user.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户配额实体
 * 对应Node.js版本的用户配额管理
 * 参考: /packages/backend/server/src/core/quota/types.ts
 */
@Entity
@Table(name = "user_quotas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /**
     * 配额计划名称
     */
    @Column(name = "plan_name", nullable = false)
    @Builder.Default
    private String planName = "free_plan_v1";

    /**
     * 单个文件大小限制（字节）
     */
    @Column(name = "blob_limit", nullable = false)
    @Builder.Default
    private Long blobLimit = 10L * 1024 * 1024; // 10MB

    /**
     * 总存储空间限制（字节）
     */
    @Column(name = "storage_quota", nullable = false)
    @Builder.Default
    private Long storageQuota = 10L * 1024 * 1024 * 1024; // 10GB

    /**
     * 历史记录保留期限（天）
     */
    @Column(name = "history_period", nullable = false)
    @Builder.Default
    private Integer historyPeriod = 7; // 7天

    /**
     * 成员数量限制
     */
    @Column(name = "member_limit", nullable = false)
    @Builder.Default
    private Integer memberLimit = 3; // 3个成员

    /**
     * AI Copilot 操作限制
     */
    @Column(name = "copilot_action_limit", nullable = false)
    @Builder.Default
    private Integer copilotActionLimit = 10; // 10次

    /**
     * 是否为付费计划
     */
    @Column(name = "is_pro", nullable = false)
    @Builder.Default
    private Boolean isPro = false;

    /**
     * 是否为终身计划
     */
    @Column(name = "is_lifetime", nullable = false)
    @Builder.Default
    private Boolean isLifetime = false;

    /**
     * 计划到期时间
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 用户配额计划枚举
     */
    public enum QuotaPlan {
        FREE_PLAN_V1("free_v1", false, "Free Plan"),
        PRO_PLAN_V1("pro_v1", false, "Pro Plan"),
        LIFETIME_PRO_PLAN_V1("lifetime_pro_v1", true, "Lifetime Pro Plan"),
        TEAM_PLAN_V1("team_v1", false, "Team Plan");

        private final String planName;
        private final boolean lifetime;
        private final String displayName;

        QuotaPlan(String planName, boolean lifetime, String displayName) {
            this.planName = planName;
            this.lifetime = lifetime;
            this.displayName = displayName;
        }

        public String getPlanName() {
            return planName;
        }

        public boolean isLifetime() {
            return lifetime;
        }
        
        public String getDisplayName() {
            return displayName;
        }

        public static QuotaPlan fromValue(String value) {
            for (QuotaPlan plan : QuotaPlan.values()) {
                if (plan.getPlanName().equals(value)) {
                    return plan;
                }
            }
            return FREE_PLAN_V1; // 默认返回免费计划
        }
        
        public String getValue() {
            return planName;
        }
        
        public boolean isPro() {
            return this != FREE_PLAN_V1;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取配额计划枚举
     */
    public QuotaPlan getQuotaPlan() {
        return QuotaPlan.fromValue(planName);
    }

    /**
     * 设置配额计划
     */
    public void setQuotaPlan(QuotaPlan plan) {
        this.planName = plan.getValue();
        this.isPro = plan.isPro();
        this.isLifetime = plan == QuotaPlan.LIFETIME_PRO_PLAN_V1;
        
        // 根据计划设置默认配额
        setDefaultQuotaByPlan(plan);
    }

    /**
     * 根据计划设置默认配额
     */
    private void setDefaultQuotaByPlan(QuotaPlan plan) {
        switch (plan) {
            case FREE_PLAN_V1:
                this.blobLimit = 10L * 1024 * 1024; // 10MB
                this.storageQuota = 10L * 1024 * 1024 * 1024; // 10GB
                this.historyPeriod = 7; // 7天
                this.memberLimit = 3;
                this.copilotActionLimit = 10;
                break;
            case PRO_PLAN_V1:
            case LIFETIME_PRO_PLAN_V1:
                this.blobLimit = 100L * 1024 * 1024; // 100MB
                this.storageQuota = 100L * 1024 * 1024 * 1024; // 100GB
                this.historyPeriod = 30; // 30天
                this.memberLimit = 10;
                this.copilotActionLimit = 500;
                break;
            case TEAM_PLAN_V1:
                this.blobLimit = 500L * 1024 * 1024; // 500MB
                this.storageQuota = 100L * 1024 * 1024 * 1024; // 100GB
                this.historyPeriod = 365; // 365天
                this.memberLimit = 100;
                this.copilotActionLimit = 5000;
                break;
        }
    }

    /**
     * 检查配额是否已过期
     */
    public boolean isExpired() {
        return expiredAt != null && expiredAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查配额是否有效
     */
    public boolean isValid() {
        return !isExpired() && (isLifetime || isPro || planName.equals("free_plan_v1"));
    }

    /**
     * 获取格式化的存储配额（人类可读）
     */
    public String getFormattedStorageQuota() {
        return formatBytes(storageQuota);
    }

    /**
     * 获取格式化的文件大小限制（人类可读）
     */
    public String getFormattedBlobLimit() {
        return formatBytes(blobLimit);
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * 计算配额使用率
     */
    public double getUsageRate(long usedStorage) {
        if (storageQuota == 0) return 0.0;
        return Math.min(1.0, (double) usedStorage / storageQuota);
    }

    /**
     * 检查是否接近配额限制
     */
    public boolean isNearLimit(long usedStorage, double threshold) {
        return getUsageRate(usedStorage) >= threshold;
    }

    /**
     * 获取剩余存储空间
     */
    public long getRemainingStorage(long usedStorage) {
        return Math.max(0, storageQuota - usedStorage);
    }

    /**
     * 检查单个文件是否超出限制
     */
    public boolean isBlobSizeExceeded(long blobSize) {
        return blobSize > blobLimit;
    }

    /**
     * 检查总存储是否超出限制
     */
    public boolean isStorageExceeded(long usedStorage) {
        return usedStorage > storageQuota;
    }

    /**
     * 检查新增存储后是否会超出限制
     */
    public boolean wouldExceedStorage(long usedStorage, long additionalSize) {
        return (usedStorage + additionalSize) > storageQuota;
    }

    /**
     * 创建免费版配额
     */
    public static UserQuota createFreeQuota(String userId) {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .build();
        quota.setQuotaPlan(QuotaPlan.FREE_PLAN_V1);
        return quota;
    }

    /**
     * 创建专业版配额
     */
    public static UserQuota createProQuota(String userId, LocalDateTime expiredAt) {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .expiredAt(expiredAt)
                .build();
        quota.setQuotaPlan(QuotaPlan.PRO_PLAN_V1);
        return quota;
    }

    /**
     * 创建终身专业版配额
     */
    public static UserQuota createLifetimeProQuota(String userId) {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .build();
        quota.setQuotaPlan(QuotaPlan.LIFETIME_PRO_PLAN_V1);
        return quota;
    }

    /**
     * 创建团队版配额
     */
    public static UserQuota createTeamQuota(String userId, LocalDateTime expiredAt) {
        UserQuota quota = UserQuota.builder()
                .userId(userId)
                .expiredAt(expiredAt)
                .build();
        quota.setQuotaPlan(QuotaPlan.TEAM_PLAN_V1);
        return quota;
    }

    /**
     * 更新配额
     */
    public UserQuota updateQuota(UserQuota newQuota) {
        this.storageQuota = newQuota.getStorageQuota(); // Assuming newQuota has getters
        this.blobLimit = newQuota.getBlobLimit(); // Assuming newQuota has getters
        this.historyPeriod = newQuota.getHistoryPeriod(); // Assuming newQuota has getters
        this.memberLimit = newQuota.getMemberLimit(); // Assuming newQuota has getters
        this.copilotActionLimit = newQuota.getCopilotActionLimit(); // Assuming newQuota has getters
        this.isPro = newQuota.getIsPro(); // Assuming newQuota has getters
        this.isLifetime = newQuota.getIsLifetime(); // Assuming newQuota has getters
        this.expiredAt = newQuota.getExpiredAt(); // Assuming newQuota has getters
        this.planName = newQuota.getPlanName(); // Assuming newQuota has getters
        return this;
    }

    /**
     * 获取当前计划
     */
    public QuotaPlan getPlan() {
        return QuotaPlan.fromValue(planName);
    }

    /**
     * 切换到新计划
     */
    public UserQuota switchToPlan(QuotaPlan newPlan) {
        this.planName = newPlan.getValue();
        this.isPro = newPlan.isPro();
        this.isLifetime = newPlan == QuotaPlan.LIFETIME_PRO_PLAN_V1;
        
        // 根据新计划更新限制
        setDefaultQuotaByPlan(newPlan);
        return this;
    }

    /**
     * 根据计划更新限制
     */
    private void updateLimitsBasedOnPlan(QuotaPlan plan) {
        // 这里根据不同计划设置不同的限制
        switch (plan) {
            case FREE_PLAN_V1:
                this.blobLimit = 10L * 1024 * 1024; // 10MB
                this.storageQuota = 10L * 1024 * 1024 * 1024; // 10GB
                this.historyPeriod = 7; // 7天
                this.memberLimit = 3;
                this.copilotActionLimit = 10;
                break;
            case PRO_PLAN_V1:
            case LIFETIME_PRO_PLAN_V1:
                this.blobLimit = 100L * 1024 * 1024; // 100MB
                this.storageQuota = 100L * 1024 * 1024 * 1024; // 100GB
                this.historyPeriod = 30; // 30天
                this.memberLimit = 10;
                this.copilotActionLimit = 500;
                break;
            case TEAM_PLAN_V1:
                this.blobLimit = 500L * 1024 * 1024; // 500MB
                this.storageQuota = 100L * 1024 * 1024 * 1024; // 100GB
                this.historyPeriod = 365; // 365天
                this.memberLimit = 100;
                this.copilotActionLimit = 5000;
                break;
        }
    }

    /**
     * 获取计划名称
     */
    public String getPlanName() {
        return planName;
    }
}