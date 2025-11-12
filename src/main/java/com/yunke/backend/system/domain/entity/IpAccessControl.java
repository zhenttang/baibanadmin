package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * IP访问控制实体
 */
@Entity
@Table(name = "ip_access_control")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAccessControl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * IP地址或CIDR
     */
    @Column(name = "ip_address", nullable = false, unique = true)
    private String ipAddress;
    
    /**
     * 控制类型 (WHITELIST, BLACKLIST)
     */
    @Column(name = "access_type", nullable = false)
    private String accessType;
    
    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    /**
     * 备注说明
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 创建人
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    /**
     * 最后命中时间
     */
    @Column(name = "last_hit_at")
    private LocalDateTime lastHitAt;
    
    /**
     * 命中次数
     */
    @Column(name = "hit_count")
    @Builder.Default
    private Long hitCount = 0L;
    
    /**
     * 过期时间（可选）
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}