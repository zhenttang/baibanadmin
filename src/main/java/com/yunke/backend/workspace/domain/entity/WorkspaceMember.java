package com.yunke.backend.workspace.domain.entity;

import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 工作空间成员实体
 */
@Entity
@Table(name = "workspace_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {
    
    @Id
    private String id;
    
    private String workspaceId;
    private String userId;
    
    @Column(name = "role")
    private String role = "Member"; // 默认角色为Member
    
    @Enumerated(EnumType.STRING)
    private WorkspaceMemberStatus status;
    
    private Instant createdAt;
    private Instant updatedAt;
    private Instant acceptedAt;
    
    /**
     * 检查成员是否活跃
     */
    public boolean isActive() {
        return status == WorkspaceMemberStatus.ACCEPTED;
    }
} 