package com.yunke.backend.document.dto;

/**
 * Default role payload exposing both mask and legacy smallint role.
 */
public record DocDefaultRoleDto(
        String role,
        int permissionMask,
        int legacyDefaultRole
) {}
