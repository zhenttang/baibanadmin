package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 配额使用记录实体
 * 对应Node.js版本的配额使用统计
 * 参考: /packages/backend/server/src/core/quota/service.ts
 */
@Entity
@Table(name = "quota_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 使用类型（用户或工作空间）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private UsageType usageType;

    /**
     * 目标ID（用户ID或工作空间ID）
     */
    @Column(name = "target_id", nullable = false)
    private String targetId;

    /**
     * 资源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    /**
     * 已使用的存储空间（字节）
     */
    @Column(name = "used_storage", nullable = false)
    @Builder.Default
    private Long usedStorage = 0L;

    /**
     * 已使用的成员数量
     */
    @Column(name = "used_members", nullable = false)
    @Builder.Default
    private Integer usedMembers = 0;

    /**
     * 已使用的AI Copilot操作次数
     */
    @Column(name = "used_copilot_actions", nullable = false)
    @Builder.Default
    private Integer usedCopilotActions = 0;

    /**
     * 已使用的历史记录条数
     */
    @Column(name = "used_history_records", nullable = false)
    @Builder.Default
    private Long usedHistoryRecords = 0L;

    /**
     * 文件数量
     */
    @Column(name = "file_count", nullable = false)
    @Builder.Default
    private Integer fileCount = 0;

    /**
     * 文档数量
     */
    @Column(name = "document_count", nullable = false)
    @Builder.Default
    private Integer documentCount = 0;

    /**
     * 统计周期（月份，格式：YYYY-MM）
     */
    @Column(name = "period", nullable = false)
    private String period;

    /**
     * 重置时间（下次配额重置的时间）
     */
    @Column(name = "reset_at")
    private LocalDateTime resetAt;

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
     * 使用类型枚举
     */
    public enum UsageType {
        USER,       // 用户配额使用
        WORKSPACE   // 工作空间配额使用
    }

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        STORAGE,        // 存储
        MEMBER,         // 成员
        COPILOT,        // AI助手
        HISTORY,        // 历史记录
        FILE,           // 文件
        DOCUMENT        // 文档
    }

    // ==================== 索引定义 ====================

    @Table(name = "quota_usage", indexes = {
            @Index(name = "idx_quota_usage_target_type", columnList = "target_id, usage_type"),
            @Index(name = "idx_quota_usage_period", columnList = "period"),
            @Index(name = "idx_quota_usage_reset", columnList = "reset_at"),
            @Index(name = "idx_quota_usage_resource", columnList = "resource_type")
    })
    public static class QuotaUsageIndex {
        // 索引定义类
    }

    // ==================== 辅助方法 ====================

    /**
     * 增加存储使用量
     */
    public void addStorageUsage(long bytes) {
        this.usedStorage += bytes;
    }

    /**
     * 减少存储使用量
     */
    public void reduceStorageUsage(long bytes) {
        this.usedStorage = Math.max(0, this.usedStorage - bytes);
    }

    /**
     * 增加成员使用量
     */
    public void addMemberUsage(int count) {
        this.usedMembers += count;
    }

    /**
     * 减少成员使用量
     */
    public void reduceMemberUsage(int count) {
        this.usedMembers = Math.max(0, this.usedMembers - count);
    }

    /**
     * 增加AI操作使用量
     */
    public void addCopilotUsage(int actions) {
        this.usedCopilotActions += actions;
    }

    /**
     * 增加历史记录使用量
     */
    public void addHistoryUsage(long records) {
        this.usedHistoryRecords += records;
    }

    /**
     * 增加文件数量
     */
    public void addFileCount(int count) {
        this.fileCount += count;
    }

    /**
     * 减少文件数量
     */
    public void reduceFileCount(int count) {
        this.fileCount = Math.max(0, this.fileCount - count);
    }

    /**
     * 增加文档数量
     */
    public void addDocumentCount(int count) {
        this.documentCount += count;
    }

    /**
     * 减少文档数量
     */
    public void reduceDocumentCount(int count) {
        this.documentCount = Math.max(0, this.documentCount - count);
    }

    /**
     * 检查是否需要重置
     */
    public boolean needsReset() {
        return resetAt != null && LocalDateTime.now().isAfter(resetAt);
    }

    /**
     * 重置使用量（保留累计统计）
     */
    public void reset() {
        // 重置可重置的使用量
        this.usedCopilotActions = 0;
        
        // 计算下次重置时间（下个月的第一天）
        LocalDateTime now = LocalDateTime.now();
        this.resetAt = now.withDayOfMonth(1).plusMonths(1).withHour(0).withMinute(0).withSecond(0);
        this.period = now.getYear() + "-" + String.format("%02d", now.getMonthValue());
    }

    /**
     * 获取格式化的存储使用量
     */
    public String getFormattedStorageUsage() {
        return formatBytes(usedStorage);
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
     * 计算存储使用率
     */
    public double getStorageUsageRate(long totalQuota) {
        if (totalQuota == 0) return 0.0;
        return Math.min(1.0, (double) usedStorage / totalQuota);
    }

    /**
     * 计算成员使用率
     */
    public double getMemberUsageRate(int totalLimit) {
        if (totalLimit == 0) return 0.0;
        return Math.min(1.0, (double) usedMembers / totalLimit);
    }

    /**
     * 计算AI操作使用率
     */
    public double getCopilotUsageRate(int totalLimit) {
        if (totalLimit == 0) return 0.0;
        return Math.min(1.0, (double) usedCopilotActions / totalLimit);
    }

    /**
     * 创建用户配额使用记录
     */
    public static QuotaUsage createUserUsage(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return QuotaUsage.builder()
                .usageType(UsageType.USER)
                .targetId(userId)
                .resourceType(ResourceType.STORAGE)
                .period(now.getYear() + "-" + String.format("%02d", now.getMonthValue()))
                .resetAt(now.withDayOfMonth(1).plusMonths(1).withHour(0).withMinute(0).withSecond(0))
                .build();
    }

    /**
     * 创建工作空间配额使用记录
     */
    public static QuotaUsage createWorkspaceUsage(String workspaceId) {
        LocalDateTime now = LocalDateTime.now();
        return QuotaUsage.builder()
                .usageType(UsageType.WORKSPACE)
                .targetId(workspaceId)
                .resourceType(ResourceType.STORAGE)
                .period(now.getYear() + "-" + String.format("%02d", now.getMonthValue()))
                .resetAt(now.withDayOfMonth(1).plusMonths(1).withHour(0).withMinute(0).withSecond(0))
                .build();
    }

    /**
     * 克隆当前使用记录（用于历史记录）
     */
    public QuotaUsage cloneForHistory() {
        return QuotaUsage.builder()
                .usageType(this.usageType)
                .targetId(this.targetId)
                .resourceType(this.resourceType)
                .usedStorage(this.usedStorage)
                .usedMembers(this.usedMembers)
                .usedCopilotActions(this.usedCopilotActions)
                .usedHistoryRecords(this.usedHistoryRecords)
                .fileCount(this.fileCount)
                .documentCount(this.documentCount)
                .period(this.period)
                .resetAt(this.resetAt)
                .build();
    }

    /**
     * 合并另一个使用记录（用于聚合）
     */
    public void merge(QuotaUsage other) {
        if (!this.targetId.equals(other.targetId) || this.usageType != other.usageType) {
            throw new IllegalArgumentException("Cannot merge usage records with different target or type");
        }
        
        this.usedStorage += other.usedStorage;
        this.usedMembers = Math.max(this.usedMembers, other.usedMembers); // 成员数取最大值
        this.usedCopilotActions += other.usedCopilotActions;
        this.usedHistoryRecords += other.usedHistoryRecords;
        this.fileCount += other.fileCount;
        this.documentCount += other.documentCount;
    }
}