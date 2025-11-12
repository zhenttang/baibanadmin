package com.yunke.backend.admin.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户管理DTO - 包含更详细的用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50个字符之间")
    private String name;
    
    /**
     * 用户名（登录用）
     */
    private String username;
    
    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 语言偏好
     */
    private String locale;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 是否已注册
     */
    private Boolean registered;
    
    /**
     * 邮箱是否已验证
     */
    private Boolean emailVerified;
    
    /**
     * 是否启用MFA
     */
    private Boolean mfaEnabled;
    
    /**
     * 是否为管理员
     */
    private Boolean isAdmin;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 注册时间
     */
    private LocalDateTime registeredAt;
    
    /**
     * 邮箱验证时间
     */
    private LocalDateTime emailVerifiedAt;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 最后登录IP
     */
    private String lastLoginIp;
    
    /**
     * 用户特性列表
     */
    private List<String> features;
    
    /**
     * 工作空间数量
     */
    private Integer workspaceCount;
    
    /**
     * 文档数量
     */
    private Integer documentCount;
    
    /**
     * 存储使用量（字节）
     */
    private Long storageUsed;
    
    /**
     * 账户状态
     */
    private String status;
    
    /**
     * Stripe客户ID
     */
    private String stripeCustomerId;
}