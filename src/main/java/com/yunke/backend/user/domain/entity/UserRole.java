package com.yunke.backend.user.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户角色实体
 */
@Entity
@Table(name = "user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    /**
     * 角色名称
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;
    
    /**
     * 角色是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 角色分配者ID
     */
    @Column(name = "assigned_by")
    private String assignedBy;
    
    /**
     * 角色分配时间
     */
    @CreationTimestamp
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    /**
     * 角色过期时间（可选）
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * 最后更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 角色枚举
     */
    public enum Role {
        /**
         * 超级管理员
         */
        SUPER_ADMIN("super_admin", "超级管理员"),
        
        /**
         * 管理员
         */
        ADMIN("admin", "管理员"),
        
        /**
         * 版主
         */
        MODERATOR("moderator", "版主"),
        
        /**
         * 普通用户
         */
        USER("user", "普通用户");
        
        private final String code;
        private final String displayName;
        
        Role(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}