package com.yunke.backend.community.dto;

import com.yunke.backend.community.enums.CommunityPermission;

/**
 * 分享文档到社区请求对象
 */
public record ShareToCommunityRequest(
    String title,
    String description,
    CommunityPermission permission
) {}