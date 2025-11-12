package com.yunke.backend.workspace.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 工作空间成员状态枚举
 * 对应 Prisma Schema 中的 WorkspaceMemberStatus
 */
public enum WorkspaceMemberStatus {
    /**
     * 等待被邀请者接受邀请
     */
    PENDING("Pending"),
    
    /**
     * 等待管理员审核并接受链接邀请
     */
    UNDER_REVIEW("UnderReview"),
    
    /**
     * 团队工作空间的临时状态。在邀请和账单支付之间有时间间隔
     */
    ALLOCATING_SEAT("AllocatingSeat"),
    
    /**
     * 用户成为活跃的工作空间成员需要更多席位
     */
    NEED_MORE_SEAT("NeedMoreSeat"),
    
    /**
     * 激活工作空间成员
     */
    ACCEPTED("Accepted"),
    
    /**
     * @deprecated 已废弃
     */
    @Deprecated
    NEED_MORE_SEAT_AND_REVIEW("NeedMoreSeatAndReview");

    private final String value;

    WorkspaceMemberStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static WorkspaceMemberStatus fromValue(String value) {
        for (WorkspaceMemberStatus status : WorkspaceMemberStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown WorkspaceMemberStatus: " + value);
    }
} 