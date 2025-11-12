package com.yunke.backend.document.domain.entity;

/**
 * 文档角色枚举
 */
public enum DocRole {
    OWNER(10),      // 拥有者，有完全控制权限
    EDITOR(20),     // 编辑者，可以编辑文档内容
    COMMENTER(30),  // 评论者，可以添加评论但不能编辑内容
    VIEWER(40),     // 查看者，只能查看文档
    NONE(0);       // 无权限
    
    private final int value;
    
    DocRole(int value) {
        this.value = value;
    }
    
    /**
     * 获取角色对应的整数值
     * @return 角色值
     */
    public int getValue() {
        return value;
    }
    
    /**
     * 从整数值获取对应的角色枚举
     * @param value 角色值
     * @return 对应的DocRole枚举
     */
    public static DocRole fromValue(Integer value) {
        if (value == null) {
            return NONE;
        }
        
        switch (value) {
            case 10:
                return OWNER;
            case 20:
                return EDITOR;
            case 30:
                return COMMENTER;
            case 40:
                return VIEWER;
            default:
                return NONE;
        }
    }
    
    /**
     * 从Short值获取对应的角色枚举
     * @param value 角色值
     * @return 对应的DocRole枚举
     */
    public static DocRole fromValue(Short value) {
        return fromValue(value != null ? value.intValue() : null);
    }
    
    /**
     * 从字符串获取对应的角色枚举
     * @param value 角色字符串
     * @return 对应的DocRole枚举
     */
    public static DocRole fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }
        
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
} 