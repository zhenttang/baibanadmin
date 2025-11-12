package com.yunke.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户配额DTO - 完全按照AFFiNE的UserQuota类型实现
 * 对应: /packages/backend/server/src/core/quota/types.ts - UserQuota
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaDto {

    /**
     * 配额计划名称
     */
    private String name;

    /**
     * 单个文件大小限制（字节）
     */
    private Long blobLimit;

    /**
     * 总存储空间限制（字节）
     */
    private Long storageQuota;

    /**
     * 历史记录保留期限（天）
     */
    private Integer historyPeriod;

    /**
     * 成员数量限制
     */
    private Integer memberLimit;

    /**
     * AI Copilot 操作限制
     */
    private Integer copilotActionLimit;

    /**
     * 已使用的存储空间（字节）
     * 用于UserQuotaWithUsage类型
     */
    private Long usedStorageQuota;

    /**
     * 是否为付费计划
     */
    private Boolean isPro;

    /**
     * 是否为终身计划
     */
    private Boolean isLifetime;

    /**
     * 计算使用率
     */
    public double getUsageRate() {
        if (storageQuota == null || storageQuota == 0 || usedStorageQuota == null) {
            return 0.0;
        }
        return Math.min(1.0, (double) usedStorageQuota / storageQuota);
    }

    /**
     * 获取剩余存储空间
     */
    public Long getRemainingStorage() {
        if (storageQuota == null || usedStorageQuota == null) {
            return null;
        }
        return Math.max(0, storageQuota - usedStorageQuota);
    }

    /**
     * 检查是否接近配额限制
     */
    public boolean isNearLimit(double threshold) {
        return getUsageRate() >= threshold;
    }

    /**
     * 检查是否超出存储限制
     */
    public boolean isStorageExceeded() {
        if (storageQuota == null || usedStorageQuota == null) {
            return false;
        }
        return usedStorageQuota > storageQuota;
    }

    /**
     * 检查文件大小是否超出限制
     */
    public boolean isBlobSizeExceeded(long blobSize) {
        if (blobLimit == null) {
            return false;
        }
        return blobSize > blobLimit;
    }

    /**
     * 格式化存储大小为人类可读格式
     */
    public String getFormattedStorageQuota() {
        return formatBytes(storageQuota);
    }

    /**
     * 格式化已使用存储为人类可读格式
     */
    public String getFormattedUsedStorage() {
        return formatBytes(usedStorageQuota);
    }

    /**
     * 格式化Blob限制为人类可读格式
     */
    public String getFormattedBlobLimit() {
        return formatBytes(blobLimit);
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes < 1024) {
            return (bytes != null ? bytes : 0) + " B";
        }
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(unit, exp), pre);
    }
}