package com.yunke.backend.document.dto;

import java.time.Instant;

/**
 * 活跃协作者 DTO
 */
public record ActiveCollaborator(
    String userId,
    String sessionId,
    String name,
    String avatarUrl,
    Instant joinedAt,
    Instant lastActiveAt,
    CollaboratorStatus status
) {
    /**
     * 协作者状态
     */
    public enum CollaboratorStatus {
        ONLINE,
        OFFLINE,
        IDLE
    }
} 