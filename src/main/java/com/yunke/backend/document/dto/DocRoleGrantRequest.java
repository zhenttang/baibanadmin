package com.yunke.backend.document.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.Collections;
import java.util.List;

/**
 * Request payload for granting document roles.
 */
public record DocRoleGrantRequest(
        List<String> userIds,
        @JsonAlias("userId") String singleUserId,
        String role,
        Integer permissionMask,
        Short type
) {
    /**
     * Normalizes userIds so legacy single-user payloads keep working.
     */
    public List<String> resolvedUserIds() {
        if (userIds != null && !userIds.isEmpty()) {
            return userIds;
        }
        if (singleUserId != null && !singleUserId.isBlank()) {
            return List.of(singleUserId);
        }
        return Collections.emptyList();
    }
}
