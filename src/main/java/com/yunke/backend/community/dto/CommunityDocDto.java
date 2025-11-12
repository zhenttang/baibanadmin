package com.yunke.backend.community.dto;

import com.yunke.backend.community.enums.CommunityPermission;
import java.time.LocalDateTime;

/**
 * 社区文档数据传输对象
 */
public record CommunityDocDto(
    String id,
    String title,
    String description,
    String authorId,
    String authorName,
    LocalDateTime sharedAt,
    Integer viewCount,
    CommunityPermission permission,
    String workspaceId
) {}