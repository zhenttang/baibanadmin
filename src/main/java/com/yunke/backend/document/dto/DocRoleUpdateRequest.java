package com.yunke.backend.document.dto;

/**
 * Request payload for updating document or default roles.
 */
public record DocRoleUpdateRequest(
        String role,
        Integer permissionMask,
        Short type
) {}
