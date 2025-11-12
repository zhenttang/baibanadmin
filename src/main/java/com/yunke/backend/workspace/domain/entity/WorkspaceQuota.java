package com.yunke.backend.workspace.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 工作空间配额实体
 * 对应Node.js版本的工作空间配额管理
 * 参考: /packages/backend/server/src/core/quota/types.ts
 */
@Entity
@Table(name = "workspace_quotas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 工作空间ID
     */
    @Column(name = "workspace_id", nullable = false, unique = true)
    private String workspaceId;

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
     * 席位数量（团队版）
     */
    @Column(name = "seat_count", nullable = false)
    @Builder.Default
    private Integer seatCount = 1;

    /**
     * 每席位存储配额（字节）
     */
    @Column(name = "per_seat_quota", nullable = false)
    @Builder.Default
    private Long perSeatQuota = 20L * 1024 * 1024 * 1024; // 20GB per seat

    /**
     * 是否为付费计划
     */
    @Column(name = "is_pro", nullable = false)
    @Builder.Default
    private Boolean isPro = false;

    /**
     * 是否为团队计划
     */
    @Column(name = "is_team", nullable = false)
    @Builder.Default
    private Boolean isTeam = false;

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
     * 工作空间配额计划枚举
     */
    public enum WorkspaceQuotaPlan {
        FREE_PLAN_V1("free_plan_v1", "免费版工作空间", false, false),
        PRO_PLAN_V1("pro_plan_v1", "专业版工作空间", true, false),
        TEAM_PLAN_V1("team_plan_v1", "团队版工作空间", true, true),
        ENTERPRISE_PLAN_V1("enterprise_plan_v1", "企业版工作空间", true, true);

        private final String value;
        private final String displayName;
        private final boolean isPro;
        private final boolean isTeam;

        WorkspaceQuotaPlan(String value, String displayName, boolean isPro, boolean isTeam) {
            this.value = value;
            this.displayName = displayName;
            this.isPro = isPro;
            this.isTeam = isTeam;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }
        
        public String getPlanName() {
            return value;
        }

        public boolean isPro() {
            return isPro;
        }

        public boolean isTeam() {
            return isTeam;
        }

        public static WorkspaceQuotaPlan fromValue(String value) {
            for (WorkspaceQuotaPlan plan : values()) {
                if (plan.value.equals(value)) {
                    return plan;
                }
            }
            throw new IllegalArgumentException("Unknown workspace quota plan: " + value);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取配额计划
     */
    public WorkspaceQuotaPlan getPlan() {
        return WorkspaceQuotaPlan.fromValue(planName);
    }

    /**
     * 设置配额计划
     */
    public void setQuotaPlan(WorkspaceQuotaPlan plan) {
        this.planName = plan.getValue();
        this.isPro = plan.isPro();
        this.isTeam = plan.isTeam();
        
        // 根据计划设置默认配额
        setDefaultQuotaByPlan(plan);
    }

    /**
     * 根据计划设置默认配额
     */
    private void setDefaultQuotaByPlan(WorkspaceQuotaPlan plan) {
        switch (plan) {
            case FREE_PLAN_V1:
                this.blobLimit = 10L * 1024 * 1024; // 10MB
                this.storageQuota = 10L * 1024 * 1024 * 1024; // 10GB
                this.historyPeriod = 7; // 7天
                this.memberLimit = 3;
                this.copilotActionLimit = 10;
                this.seatCount = 1;
                this.perSeatQuota = 10L * 1024 * 1024 * 1024; // 10GB
                break;
            case PRO_PLAN_V1:
                this.blobLimit = 100L * 1024 * 1024; // 100MB
                this.storageQuota = 100L * 1024 * 1024 * 1024; // 100GB
                this.historyPeriod = 30; // 30天
                this.memberLimit = 10;
                this.copilotActionLimit = 500;
                this.seatCount = 1;
                this.perSeatQuota = 100L * 1024 * 1024 * 1024; // 100GB
                break;
            case TEAM_PLAN_V1:
                this.blobLimit = 500L * 1024 * 1024; // 500MB
                this.storageQuota = 1000L * 1024 * 1024 * 1024; // 1TB
                this.historyPeriod = 365; // 365天
                this.memberLimit = 100;
                this.copilotActionLimit = 5000;
                this.seatCount = 5; // 默认5个席位
                this.perSeatQuota = 20L * 1024 * 1024 * 1024; // 20GB per seat
                break;
            case ENTERPRISE_PLAN_V1:
                this.blobLimit = 1024L * 1024 * 1024; // 1GB
                this.storageQuota = 10L * 1024 * 1024 * 1024 * 1024; // 10TB
                this.historyPeriod = 730; // 2年
                this.memberLimit = 1000;
                this.copilotActionLimit = 50000;
                this.seatCount = 50; // 默认50个席位
                this.perSeatQuota = 100L * 1024 * 1024 * 1024; // 100GB per seat
                break;
        }
    }

    /**
     * 计算总存储配额
     */
    public Long calculateTotalStorageQuota() {
        return this.storageQuota;
    }

    /**
     * 更新配额
     */
    public WorkspaceQuota updateQuota(WorkspaceQuota newQuota) {
        this.storageQuota = newQuota.getStorageQuota();
        this.blobLimit = newQuota.getBlobLimit();
        this.memberLimit = newQuota.getMemberLimit();
        this.historyPeriod = newQuota.getHistoryPeriod();
        this.planName = newQuota.getPlanName();
        this.expiredAt = newQuota.getExpiredAt();
        this.copilotActionLimit = newQuota.getCopilotActionLimit();
        this.seatCount = newQuota.getSeatCount();
        this.perSeatQuota = newQuota.getPerSeatQuota();
        this.isPro = newQuota.getIsPro();
        this.isTeam = newQuota.getIsTeam();
        return this;
    }

    /**
     * 检查座位可用性
     */
    public boolean checkSeatAvailability(int requestedSeats) {
        return this.memberLimit >= requestedSeats;
    }

    /**
     * 获取计划名称
     */
    public String getPlanName() {
        return this.planName;
    }

    /**
     * 切换到新计划
     */
    public WorkspaceQuota switchToPlan(WorkspaceQuotaPlan newPlan) {
        this.planName = newPlan.getValue();
        // 根据新计划更新限制
        updateLimitsBasedOnPlan(newPlan);
        return this;
    }

    /**
     * 根据计划更新限制
     */
    private void updateLimitsBasedOnPlan(WorkspaceQuotaPlan plan) {
        // 这里根据不同计划设置不同的限制
        switch (plan) {
            case FREE_PLAN_V1:
                this.storageQuota = 10L * 1024 * 1024 * 1024; // 10GB
                this.blobLimit = 10L * 1024 * 1024; // 10MB
                this.memberLimit = 3;
                this.historyPeriod = 7; // 7天
                break;
            case PRO_PLAN_V1:
                this.storageQuota = 100L * 1024 * 1024 * 1024; // 100GB
                this.blobLimit = 100L * 1024 * 1024; // 100MB
                this.memberLimit = 10;
                this.historyPeriod = 30; // 30天
                break;
            case TEAM_PLAN_V1:
                this.storageQuota = 1000L * 1024 * 1024 * 1024; // 1TB
                this.blobLimit = 500L * 1024 * 1024; // 500MB
                this.memberLimit = 100;
                this.historyPeriod = 365; // 365天
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
        return !isExpired() && (isPro || planName.equals("free_plan_v1"));
    }

    /**
     * 计算总的存储配额（基础配额 + 席位配额）
     */
    public long getTotalStorageQuota() {
        if (isTeam && seatCount > 1) {
            return storageQuota + (seatCount - 1) * perSeatQuota;
        }
        return storageQuota;
    }

    /**
     * 获取有效席位数量
     */
    public int getEffectiveSeatCount() {
        return isTeam ? Math.max(1, seatCount) : 1;
    }

    /**
     * 检查是否可以添加更多席位
     */
    public boolean canAddSeats(int additionalSeats) {
        if (!isTeam) return false;
        return (seatCount + additionalSeats) <= memberLimit;
    }

    /**
     * 添加席位
     */
    public boolean addSeats(int additionalSeats) {
        if (!canAddSeats(additionalSeats)) {
            return false;
        }
        this.seatCount += additionalSeats;
        return true;
    }

    /**
     * 移除席位
     */
    public boolean removeSeats(int seatsToRemove) {
        if (!isTeam || seatsToRemove <= 0) return false;
        int newSeatCount = seatCount - seatsToRemove;
        if (newSeatCount < 1) return false; // 至少保留1个席位
        
        this.seatCount = newSeatCount;
        return true;
    }

    /**
     * 获取格式化的存储配额（人类可读）
     */
    public String getFormattedTotalStorageQuota() {
        return formatBytes(getTotalStorageQuota());
    }

    /**
     * 获取格式化的文件大小限制（人类可读）
     */
    public String getFormattedBlobLimit() {
        return formatBytes(blobLimit);
    }

    /**
     * 获取格式化的每席位配额（人类可读）
     */
    public String getFormattedPerSeatQuota() {
        return formatBytes(perSeatQuota);
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
        long totalQuota = getTotalStorageQuota();
        if (totalQuota == 0) return 0.0;
        return Math.min(1.0, (double) usedStorage / totalQuota);
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
        return Math.max(0, getTotalStorageQuota() - usedStorage);
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
        return usedStorage > getTotalStorageQuota();
    }

    /**
     * 检查新增存储后是否会超出限制
     */
    public boolean wouldExceedStorage(long usedStorage, long additionalSize) {
        return (usedStorage + additionalSize) > getTotalStorageQuota();
    }

    /**
     * 检查成员数量是否超出限制
     */
    public boolean isMemberLimitExceeded(int currentMemberCount) {
        return currentMemberCount > memberLimit;
    }

    /**
     * 检查是否可以添加更多成员
     */
    public boolean canAddMembers(int currentMemberCount, int additionalMembers) {
        return (currentMemberCount + additionalMembers) <= memberLimit;
    }

    /**
     * 创建免费版工作空间配额
     */
    public static WorkspaceQuota createFreeQuota(String workspaceId) {
        WorkspaceQuota quota = WorkspaceQuota.builder()
                .workspaceId(workspaceId)
                .build();
        quota.setQuotaPlan(WorkspaceQuotaPlan.FREE_PLAN_V1);
        return quota;
    }

    /**
     * 创建专业版工作空间配额
     */
    public static WorkspaceQuota createProQuota(String workspaceId, LocalDateTime expiredAt) {
        WorkspaceQuota quota = WorkspaceQuota.builder()
                .workspaceId(workspaceId)
                .expiredAt(expiredAt)
                .build();
        quota.setQuotaPlan(WorkspaceQuotaPlan.PRO_PLAN_V1);
        return quota;
    }

    /**
     * 创建团队版工作空间配额
     */
    public static WorkspaceQuota createTeamQuota(String workspaceId, int seatCount, LocalDateTime expiredAt) {
        WorkspaceQuota quota = WorkspaceQuota.builder()
                .workspaceId(workspaceId)
                .seatCount(seatCount)
                .expiredAt(expiredAt)
                .build();
        quota.setQuotaPlan(WorkspaceQuotaPlan.TEAM_PLAN_V1);
        return quota;
    }

    /**
     * 创建企业版工作空间配额
     */
    public static WorkspaceQuota createEnterpriseQuota(String workspaceId, int seatCount, LocalDateTime expiredAt) {
        WorkspaceQuota quota = WorkspaceQuota.builder()
                .workspaceId(workspaceId)
                .seatCount(seatCount)
                .expiredAt(expiredAt)
                .build();
        quota.setQuotaPlan(WorkspaceQuotaPlan.ENTERPRISE_PLAN_V1);
        return quota;
    }
}