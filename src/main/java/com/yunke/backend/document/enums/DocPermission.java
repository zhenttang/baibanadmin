package com.yunke.backend.document.enums;

/**
 * Bitmask-based document permissions. Each permission is one bit.
 * This co-exists with legacy boolean permission maps for backward compatibility.
 */
public enum DocPermission {
    Read(1 << 0),
    Comment(1 << 1),
    Add(1 << 2),
    Modify(1 << 3),
    Delete(1 << 4),
    Export(1 << 5),
    Share(1 << 6),
    Invite(1 << 7),
    Manage(1 << 8);

    public final int bit;

    DocPermission(int bit) {
        this.bit = bit;
    }

    public static boolean has(int mask, DocPermission permission) {
        return (mask & permission.bit) == permission.bit;
    }
}


