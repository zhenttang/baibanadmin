package com.yunke.backend.community.enums;

/**
 * 社区文档访问权限枚举
 */
public enum CommunityPermission {
    PUBLIC("所有工作空间成员可见"),
    COLLABORATOR("协作者及以上权限可见"),
    ADMIN("仅管理员和所有者可见"),
    CUSTOM("自定义用户可见");
    
    private final String description;
    
    CommunityPermission(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}