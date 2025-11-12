package com.yunke.backend.notification.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知类型枚举
 * 对应 Prisma Schema 中的 NotificationType
 */
public enum NotificationType {
    /**
     * 提及
     */
    MENTION("Mention"),
    
    /**
     * 邀请
     */
    INVITATION("Invitation"),
    
    /**
     * 邀请已接受
     */
    INVITATION_ACCEPTED("InvitationAccepted"),
    
    /**
     * 邀请被阻止
     */
    INVITATION_BLOCKED("InvitationBlocked"),
    
    /**
     * 邀请被拒绝
     */
    INVITATION_REJECTED("InvitationRejected"),
    
    /**
     * 邀请审核请求
     */
    INVITATION_REVIEW_REQUEST("InvitationReviewRequest"),
    
    /**
     * 邀请审核批准
     */
    INVITATION_REVIEW_APPROVED("InvitationReviewApproved"),
    
    /**
     * 邀请审核拒绝
     */
    INVITATION_REVIEW_DECLINED("InvitationReviewDeclined"),

    /**
     * 论坛@提及
     */
    FORUM_MENTION("ForumMention"),

    /**
     * 论坛帖子被回复
     */
    FORUM_POST_REPLIED("ForumPostReplied"),

    /**
     * 论坛回复被点赞
     */
    FORUM_REPLY_LIKED("ForumReplyLiked"),

    /**
     * 论坛帖子被点赞
     */
    FORUM_POST_LIKED("ForumPostLiked"),

    /**
     * 论坛帖子被版主操作
     */
    FORUM_POST_MODERATED("ForumPostModerated");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static NotificationType fromValue(String value) {
        for (NotificationType type : NotificationType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationType: " + value);
    }
} 
