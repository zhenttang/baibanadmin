package com.yunke.backend.document.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * AFFiNE兼容的文档ID解析器
 * 完全参考AFFiNE开源代码的DocID类实现
 * 
 * 支持的ID格式：
 * - workspaceId (workspace root document)
 * - pageId (regular page document)  
 * - workspaceId:space:pageId (space page document)
 * - workspaceId:settings:settingsId (settings document)
 * - db$collectionName (database sync document)
 * - userdata$userId$collectionName (user data document)
 */
@Slf4j
@Getter
public class DocID {
    
    public enum DocVariant {
        WORKSPACE("workspace"),
        PAGE("page"), 
        SPACE("space"),
        SETTINGS("settings"),
        DATABASE_SYNC("db"),
        USER_DATA("userdata"),
        UNKNOWN("unknown");
        
        private final String value;
        
        DocVariant(String value) {
            this.value = value;
        }
        
        public static DocVariant fromString(String value) {
            for (DocVariant variant : values()) {
                if (variant.value.equals(value)) {
                    return variant;
                }
            }
            return UNKNOWN;
        }
    }
    
    private final String raw;
    private final String workspace;
    private final DocVariant variant;
    private final String sub;
    
    /**
     * 静态解析方法，失败时返回null而不是抛异常
     */
    public static DocID parse(String raw) {
        try {
            return new DocID(raw);
        } catch (Exception e) {
            log.debug("Failed to parse DocID: {}, error: {}", raw, e.getMessage());
            return null;
        }
    }
    
    /**
     * 静态解析方法，带工作空间上下文
     */
    public static DocID parse(String raw, String workspaceId) {
        try {
            return new DocID(raw, workspaceId);
        } catch (Exception e) {
            log.debug("Failed to parse DocID: {} with workspace: {}, error: {}", raw, workspaceId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取纯净的GUID，不带任何前缀
     * 对于workspace文档返回workspaceId，对于其他文档返回sub部分
     */
    public String getGuid() {
        return variant == DocVariant.WORKSPACE ? workspace : sub;
    }
    
    /**
     * 获取完整的文档标识符
     */
    public String getFull() {
        if (variant == DocVariant.WORKSPACE) {
            return workspace;
        }
        return workspace + ":" + variant.value + ":" + sub;
    }
    
    /**
     * 是否为工作空间根文档
     */
    public boolean isWorkspace() {
        return variant == DocVariant.WORKSPACE;
    }
    
    /**
     * 是否为数据库同步文档
     */
    public boolean isDatabaseSync() {
        return variant == DocVariant.DATABASE_SYNC;
    }
    
    /**
     * 是否为用户数据文档
     */
    public boolean isUserData() {
        return variant == DocVariant.USER_DATA;
    }
    
    /**
     * 获取数据库集合名称（仅对数据库同步文档有效）
     */
    public String getCollectionName() {
        if (!isDatabaseSync()) {
            throw new IllegalStateException("Not a database sync document");
        }
        return sub;
    }
    
    /**
     * 主构造函数
     */
    public DocID(String raw, String workspaceId) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("Invalid Empty Doc ID");
        }
        
        this.raw = raw;
        
        // 处理数据库同步格式: db$collectionName
        if (raw.startsWith("db$")) {
            String collectionName = raw.substring(3);
            if (collectionName.isEmpty()) {
                throw new IllegalArgumentException("Invalid database sync ID: " + raw);
            }
            this.workspace = workspaceId; // 数据库同步文档使用传入的工作空间ID
            this.variant = DocVariant.DATABASE_SYNC;
            this.sub = collectionName;
            return;
        }
        
        // 处理用户数据格式: userdata$userId$collectionName
        if (raw.startsWith("userdata$")) {
            String[] parts = raw.split("\\$");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid user data ID format: " + raw);
            }
            this.workspace = workspaceId; // 用户数据文档使用传入的工作空间ID
            this.variant = DocVariant.USER_DATA;
            this.sub = parts[1] + "$" + parts[2]; // userId$collectionName
            return;
        }
        
        // 处理标准格式
        String[] parts = raw.split(":");
        
        if (parts.length > 3) {
            // 特殊适配情况: wsId:space:page:pageId
            if (parts.length == 4 && "space".equals(parts[1]) && "page".equals(parts[2])) {
                parts = new String[]{workspaceId != null ? workspaceId : parts[0], "space", parts[3]};
            } else {
                throw new IllegalArgumentException("Invalid format of Doc ID: " + raw);
            }
        } else if (parts.length == 2) {
            // ${variant}:${guid}
            if (workspaceId == null) {
                throw new IllegalArgumentException("Workspace is required for variant:guid format");
            }
            // 重新组织为 [workspaceId, variant, guid]
            String[] newParts = new String[3];
            newParts[0] = workspaceId;
            newParts[1] = parts[0];
            newParts[2] = parts[1];
            parts = newParts;
        } else if (parts.length == 1) {
            // ${ws} or ${pageId}
            if (workspaceId != null && !parts[0].equals(workspaceId)) {
                // 这是一个pageId，需要添加工作空间和variant
                parts = new String[]{workspaceId, "unknown", parts[0]};
            }
            // 否则parts[0]就是workspaceId，保持不变
        }
        
        String workspace = parts.length > 0 ? parts[0] : null;
        if (workspaceId != null) {
            workspace = workspaceId;
        }
        
        String variant = parts.length > 1 ? parts[1] : null;
        String docId = parts.length > 2 ? parts[2] : null;
        
        if (workspace == null || workspace.isEmpty()) {
            throw new IllegalArgumentException("Workspace is required");
        }
        
        if (variant != null) {
            DocVariant docVariant = DocVariant.fromString(variant);
            if (docVariant == DocVariant.UNKNOWN && !variant.equals("unknown")) {
                throw new IllegalArgumentException("Invalid ID variant: " + variant);
            }
            
            if (docId == null || docId.isEmpty()) {
                throw new IllegalArgumentException("ID is required for non-workspace doc");
            }
            
            this.workspace = workspace;
            this.variant = docVariant;
            this.sub = docId;
        } else if (docId != null) {
            throw new IllegalArgumentException("Variant is required for non-workspace doc");
        } else {
            // 纯工作空间ID
            this.workspace = workspace;
            this.variant = DocVariant.WORKSPACE;
            this.sub = null;
        }
    }
    
    /**
     * 简化构造函数，不提供工作空间上下文
     */
    public DocID(String raw) {
        this(raw, null);
    }
    
    /**
     * 修正工作空间ID（用于后续处理）
     */
    public void fixWorkspace(String workspaceId) {
        if (!isWorkspace() && !this.workspace.equals(workspaceId)) {
            // 注意：这里在原AFFiNE代码中是直接修改字段，但Java中字段是final的
            // 实际使用中应该创建新的DocID实例
            log.warn("Cannot fix workspace for immutable DocID. Current: {}, target: {}", this.workspace, workspaceId);
        }
    }
    
    @Override
    public String toString() {
        return getFull();
    }
    
    /**
     * 创建一个修正工作空间后的新DocID
     */
    public DocID withWorkspace(String newWorkspaceId) {
        if (isWorkspace() || this.workspace.equals(newWorkspaceId)) {
            return this;
        }
        
        // 根据当前variant重新构造ID
        String newRaw;
        switch (variant) {
            case DATABASE_SYNC:
                newRaw = "db$" + sub;
                break;
            case USER_DATA:
                newRaw = "userdata$" + sub;
                break;
            default:
                newRaw = newWorkspaceId + ":" + variant.value + ":" + sub;
                break;
        }
        
        return new DocID(newRaw, newWorkspaceId);
    }
}