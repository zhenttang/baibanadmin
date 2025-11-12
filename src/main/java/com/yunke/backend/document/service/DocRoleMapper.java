package com.yunke.backend.document.service;

import com.yunke.backend.document.enums.DocPermission;
import org.springframework.stereotype.Component;

/**
 * Maps between friendly role labels and permission bitmasks.
 */
@Component
public class DocRoleMapper {

    private static final int MANAGER_MASK = DocPermission.Read.bit
            | DocPermission.Comment.bit
            | DocPermission.Add.bit
            | DocPermission.Modify.bit
            | DocPermission.Delete.bit
            | DocPermission.Export.bit
            | DocPermission.Share.bit
            | DocPermission.Invite.bit
            | DocPermission.Manage.bit;

    private static final int EDITOR_MASK = DocPermission.Read.bit
            | DocPermission.Comment.bit
            | DocPermission.Add.bit
            | DocPermission.Modify.bit
            | DocPermission.Export.bit;

    private static final int COMMENTER_MASK = DocPermission.Read.bit | DocPermission.Comment.bit;

    private static final int READER_MASK = DocPermission.Read.bit;

    public int resolveMask(String role, Integer permissionMask) {
        if (permissionMask != null) {
            return permissionMask;
        }
        return roleToMask(role);
    }

    public int roleToMask(String role) {
        if (role == null) {
            return 0;
        }
        return switch (role.toLowerCase()) {
            case "manager", "owner", "admin" -> MANAGER_MASK;
            case "editor" -> EDITOR_MASK;
            case "commenter" -> COMMENTER_MASK;
            case "reader", "viewer" -> READER_MASK;
            case "none" -> 0;
            default -> 0;
        };
    }

    public String maskToRole(int mask) {
        if (DocPermission.has(mask, DocPermission.Manage)) {
            return "manager";
        }
        if (DocPermission.has(mask, DocPermission.Modify) || DocPermission.has(mask, DocPermission.Add)) {
            return "editor";
        }
        if (DocPermission.has(mask, DocPermission.Comment)) {
            return "commenter";
        }
        if (DocPermission.has(mask, DocPermission.Read)) {
            return "reader";
        }
        return "none";
    }
}
