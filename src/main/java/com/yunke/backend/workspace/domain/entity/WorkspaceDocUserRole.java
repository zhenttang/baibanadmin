package com.yunke.backend.workspace.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.Workspace;

@Entity
@Table(name = "workspace_page_user_permissions")
@IdClass(WorkspaceDocUserRole.WorkspaceDocUserRoleId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDocUserRole {

    @Id
    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Id
    @Column(name = "page_id", nullable = false)
    private String docId;

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "type", nullable = false)
    private Short type;

  /**
   * Bitmask permission for fine-grained controls (nullable for compatibility).
   */
  @Column(name = "permission_mask")
  private Integer permissionMask;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", insertable = false, updatable = false)
    private Workspace workspace;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceDocUserRoleId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String workspaceId;
        private String docId;
        private String userId;
    }
} 