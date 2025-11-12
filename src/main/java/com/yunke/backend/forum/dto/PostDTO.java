package com.yunke.backend.forum.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PostDTO {
    private String id;
    private Long forumId;
    private String forumName;

    // 发帖人信息
    private String userId;
    private String userName;
    private String userAvatar;

    private String title;
    private String content;
    private String contentType;
    private String status; // NORMAL, LOCKED, DELETED, HIDDEN

    private Boolean isSticky;
    private Boolean isEssence;
    private Boolean isLocked;
    private Boolean isHot;

    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer collectCount;

    private BigDecimal hotScore;
    private BigDecimal qualityScore;

    private LocalDateTime lastReplyAt;
    private String lastReplyUserId;
    private String lastReplyUserName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 交互状态（非持久化）
    private Boolean isLiked;
    private Boolean isCollected;
}
