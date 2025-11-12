package com.yunke.backend.notification.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 邮件模板实体
 * 对应Node.js版本的邮件模板系统
 * 参考: /packages/backend/server/src/mails/
 */
@Entity
@Table(name = "mail_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 模板名称（唯一标识）
     */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * 模板显示名称
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * 模板分类
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TemplateCategory category;

    /**
     * 邮件主题模板
     */
    @Column(name = "subject_template", nullable = false)
    private String subjectTemplate;

    /**
     * HTML内容模板
     */
    @Column(name = "html_template", columnDefinition = "TEXT", nullable = false)
    private String htmlTemplate;

    /**
     * 纯文本模板
     */
    @Column(name = "text_template", columnDefinition = "TEXT")
    private String textTemplate;

    /**
     * 模板语言
     */
    @Column(name = "language", nullable = false)
    @Builder.Default
    private String language = "zh-CN";

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 模板版本
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * 模板描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 默认变量（JSON格式）
     */
    @Column(name = "default_variables", columnDefinition = "TEXT")
    private String defaultVariables;

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
     * 创建者
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * 模板分类枚举
     */
    public enum TemplateCategory {
        USER,           // 用户相关
        WORKSPACE,      // 工作空间相关
        TEAM,           // 团队相关
        DOCUMENT,       // 文档相关
        SYSTEM,         // 系统相关
        NOTIFICATION    // 通知相关
    }

    // ==================== 预定义模板常量 ====================

    /**
     * 用户相关模板
     */
    public static final class UserTemplates {
        public static final String SIGN_UP = "user-sign-up";
        public static final String SIGN_IN = "user-sign-in";
        public static final String PASSWORD_SET = "user-password-set";
        public static final String PASSWORD_CHANGE = "user-password-change";
        public static final String EMAIL_VERIFY = "user-email-verify";
        public static final String EMAIL_CHANGE = "user-email-change";
    }

    /**
     * 工作空间相关模板
     */
    public static final class WorkspaceTemplates {
        public static final String INVITATION = "workspace-invitation";
        public static final String INVITATION_ACCEPTED = "workspace-invitation-accepted";
        public static final String MEMBER_LEAVE = "workspace-member-leave";
        public static final String MEMBER_REMOVED = "workspace-member-removed";
        public static final String OWNERSHIP_TRANSFERRED = "workspace-ownership-transferred";
        public static final String OWNERSHIP_RECEIVED = "workspace-ownership-received";
    }

    /**
     * 团队相关模板
     */
    public static final class TeamTemplates {
        public static final String BECOME_ADMIN = "team-become-admin";
        public static final String BECOME_COLLABORATOR = "team-become-collaborator";
        public static final String WORKSPACE_UPGRADED = "team-workspace-upgraded";
        public static final String EXPIRED = "team-expired";
        public static final String DELETE_IN_24H = "team-delete-in-24h";
        public static final String DELETE_IN_1M = "team-delete-in-1m";
    }

    /**
     * 文档相关模板
     */
    public static final class DocumentTemplates {
        public static final String MENTION = "document-mention";
        public static final String SHARE = "document-share";
        public static final String COMMENT = "document-comment";
    }

    /**
     * 系统相关模板
     */
    public static final class SystemTemplates {
        public static final String WELCOME = "system-welcome";
        public static final String MAINTENANCE = "system-maintenance";
        public static final String SECURITY_ALERT = "system-security-alert";
        public static final String ACCOUNT_LOCKED = "system-account-locked";
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查模板是否为用户相关
     */
    public boolean isUserTemplate() {
        return category == TemplateCategory.USER;
    }

    /**
     * 检查模板是否为工作空间相关
     */
    public boolean isWorkspaceTemplate() {
        return category == TemplateCategory.WORKSPACE;
    }

    /**
     * 检查模板是否为团队相关
     */
    public boolean isTeamTemplate() {
        return category == TemplateCategory.TEAM;
    }

    /**
     * 检查模板是否为文档相关
     */
    public boolean isDocumentTemplate() {
        return category == TemplateCategory.DOCUMENT;
    }

    /**
     * 检查模板是否为系统相关
     */
    public boolean isSystemTemplate() {
        return category == TemplateCategory.SYSTEM;
    }

    /**
     * 获取模板的完整标识
     */
    public String getFullName() {
        return String.format("%s:v%d", name, version);
    }

    /**
     * 检查模板是否可用
     */
    public boolean isAvailable() {
        return enabled && htmlTemplate != null && !htmlTemplate.isEmpty();
    }
}