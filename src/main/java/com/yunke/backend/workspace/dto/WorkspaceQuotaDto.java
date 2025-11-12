package com.yunke.backend.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作空间配额DTO - 完全按照AFFiNE的WorkspaceQuota类型实现
 * 对应: /packages/backend/server/src/core/quota/types.ts - WorkspaceQuota
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceQuotaDto {

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
     * 用于WorkspaceQuotaWithUsage类型
     */
    private Long usedStorageQuota;

    /**
     * 当前成员数量
     * 用于WorkspaceQuotaWithUsage类型
     */
    private Integer memberCount;

    /**
     * 超出限制的成员数量
     * 用于WorkspaceQuotaWithUsage类型
     */
    private Integer overcapacityMemberCount;

    /**
     * 继承配额的所有者用户ID
     * 如果不为空，表示此工作空间继承所有者的配额
     */
    private String ownerQuota;

    /**
     * 已使用大小 (废弃字段，保持兼容性)
     * @deprecated 使用 usedStorageQuota 替代
     */
    @Deprecated
    private Long usedSize;

    /**
     * 计算存储使用率
     */
    public double getStorageUsageRate() {
        if (storageQuota == null || storageQuota == 0 || usedStorageQuota == null) {
            return 0.0;
        }
        return Math.min(1.0, (double) usedStorageQuota / storageQuota);
    }

    /**
     * 计算成员使用率
     */
    public double getMemberUsageRate() {
        if (memberLimit == null || memberLimit == 0 || memberCount == null) {
            return 0.0;
        }
        return Math.min(1.0, (double) memberCount / memberLimit);
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
     * 获取剩余成员席位
     */
    public Integer getRemainingSeats() {
        if (memberLimit == null || memberCount == null) {
            return null;
        }
        return Math.max(0, memberLimit - memberCount);
    }

    /**
     * 检查是否接近存储限制
     */
    public boolean isNearStorageLimit(double threshold) {
        return getStorageUsageRate() >= threshold;
    }

    /**
     * 检查是否接近成员限制
     */
    public boolean isNearMemberLimit(double threshold) {
        return getMemberUsageRate() >= threshold;
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
     * 检查是否超出成员限制
     */
    public boolean isMemberLimitExceeded() {
        if (memberLimit == null || memberCount == null) {
            return false;
        }
        return memberCount > memberLimit;
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
     * 检查是否为继承配额
     */
    public boolean isInheritedQuota() {
        return ownerQuota != null;
    }

    /**
     * 检查是否有超额成员
     */
    public boolean hasOvercapacityMembers() {
        return overcapacityMemberCount != null && overcapacityMemberCount > 0;
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

    /**
     * 获取配额状态描述
     */
    public String getQuotaStatus() {
        if (isStorageExceeded()) {
            return "Storage Exceeded";
        }
        if (isMemberLimitExceeded()) {
            return "Member Limit Exceeded";
        }
        if (isNearStorageLimit(0.9)) {
            return "Near Storage Limit";
        }
        if (isNearMemberLimit(0.9)) {
            return "Near Member Limit";
        }
        return "Normal";
    }
}