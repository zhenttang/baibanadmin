package com.yunke.backend.security.constants;

/**
 * 权限动作常量类
 * 
 * <p>用于消除代码中的魔法字符串，统一管理权限动作名称。
 * 所有权限检查相关的代码应使用此类中的常量，而不是硬编码字符串。</p>
 * 
 * @author System
 * @since 2024-12-19
 */
public final class PermissionActions {
    
    private PermissionActions() {
        // 工具类，禁止实例化
    }
    
    // ==================== 通用权限动作 ====================
    
    /** 读取/查看权限 */
    public static final String READ = "read";
    
    /** 查看权限（read的别名） */
    public static final String VIEW = "view";
    
    /** 写入权限 */
    public static final String WRITE = "write";
    
    /** 编辑权限 */
    public static final String EDIT = "edit";
    
    /** 修改权限 */
    public static final String MODIFY = "modify";
    
    /** 更新权限 */
    public static final String UPDATE = "update";
    
    /** 创建权限 */
    public static final String CREATE = "create";
    
    /** 添加权限 */
    public static final String ADD = "add";
    
    /** 删除权限 */
    public static final String DELETE = "delete";
    
    /** 移除权限（delete的别名） */
    public static final String REMOVE = "remove";
    
    // ==================== 工作空间权限动作 ====================
    
    /** 更新工作空间设置 */
    public static final String UPDATE_SETTINGS = "update_settings";
    
    /** 管理用户权限 */
    public static final String MANAGE_USERS = "manage_users";
    
    /** 管理权限 */
    public static final String MANAGE = "manage";
    
    /** 管理员权限 */
    public static final String ADMIN = "admin";
    
    // ==================== 文档权限动作 ====================
    
    /** 评论权限 */
    public static final String COMMENT = "comment";
    
    /** 导出权限 */
    public static final String EXPORT = "export";
    
    /** 分享权限 */
    public static final String SHARE = "share";
    
    /** 邀请权限 */
    public static final String INVITE = "invite";
    
    // ==================== Blob权限动作 ====================
    
    /** Blob读取权限 */
    public static final String BLOB_READ = "Workspace.Blobs.Read";
    
    /** Blob写入权限 */
    public static final String BLOB_WRITE = "Workspace.Blobs.Write";
    
    /** Blob删除权限 */
    public static final String BLOB_DELETE = "Workspace.Blobs.Delete";
    
    /** Blob列表权限 */
    public static final String BLOB_LIST = "Workspace.Blobs.List";
    
    /** Blob元数据读取权限 */
    public static final String BLOB_METADATA_READ = "Workspace.Blobs.Metadata.Read";
    
    /** Blob搜索权限 */
    public static final String BLOB_SEARCH = "Workspace.Blobs.Search";
    
    /** 工作空间读取权限（通用） */
    public static final String WORKSPACE_READ = "Workspace.Read";
    
    /** 列表权限 */
    public static final String LIST = "list";
    
    // ==================== 文档权限动作 ====================
    
    /** 创建文档权限 */
    public static final String DOC_CREATE = "Workspace.CreateDoc";
    
    /** 文档读取权限 */
    public static final String DOC_READ = "Doc.Read";
    
    /** 文档更新权限 */
    public static final String DOC_UPDATE = "Doc.Update";
    
    /** 文档删除权限 */
    public static final String DOC_DELETE = "Doc.Delete";
    
    /** 文档恢复权限 */
    public static final String DOC_RESTORE = "Doc.Restore";
    
    /** 文档发布权限 */
    public static final String DOC_PUBLISH = "Doc.Publish";
    
    /** 文档用户管理权限 */
    public static final String DOC_USERS_MANAGE = "Doc.Users.Manage";
    
    /** 文档用户读取权限 */
    public static final String DOC_USERS_READ = "Doc.Users.Read";
    
    // ==================== 辅助方法 ====================
    
    /**
     * 规范化权限动作字符串（转为小写并去除空格）
     * 
     * @param action 原始权限动作字符串
     * @return 规范化后的权限动作字符串
     */
    public static String normalize(String action) {
        return action != null ? action.toLowerCase().trim() : READ;
    }
    
    /**
     * 检查是否为读取类权限
     * 
     * @param action 权限动作
     * @return 如果是读取类权限返回true
     */
    public static boolean isReadAction(String action) {
        String normalized = normalize(action);
        return READ.equals(normalized) || VIEW.equals(normalized);
    }
    
    /**
     * 检查是否为写入类权限
     * 
     * @param action 权限动作
     * @return 如果是写入类权限返回true
     */
    public static boolean isWriteAction(String action) {
        String normalized = normalize(action);
        return WRITE.equals(normalized) || 
               EDIT.equals(normalized) || 
               MODIFY.equals(normalized) || 
               CREATE.equals(normalized) || 
               ADD.equals(normalized);
    }
    
    /**
     * 检查是否为删除类权限
     * 
     * @param action 权限动作
     * @return 如果是删除类权限返回true
     */
    public static boolean isDeleteAction(String action) {
        String normalized = normalize(action);
        return DELETE.equals(normalized) || REMOVE.equals(normalized);
    }
    
    /**
     * 检查是否为管理类权限
     * 
     * @param action 权限动作
     * @return 如果是管理类权限返回true
     */
    public static boolean isManageAction(String action) {
        String normalized = normalize(action);
        return MANAGE.equals(normalized) || 
               ADMIN.equals(normalized) || 
               MANAGE_USERS.equals(normalized);
    }
}

