package com.yunke.backend.document.dto;

import java.util.List;

/**
 * Graph-style pagination payload for document roles.
 */
public record DocRolePageDto(
        List<DocRoleEdgeDto> edges,
        DocRolePageInfoDto pageInfo,
        long totalCount
) {
    public record DocRoleEdgeDto(DocRoleNodeDto node) {}

    public record DocRoleNodeDto(
            DocRoleUserDto user,
            String role,
            int permissionMask,
            Short type
    ) {}

    public record DocRoleUserDto(String id, String name, String avatarUrl) {}

    public record DocRolePageInfoDto(boolean hasNextPage, String endCursor) {}
}
