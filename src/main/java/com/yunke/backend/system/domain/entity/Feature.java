package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;

/**
 * 特性开关实体
 * 对应Node.js版本的Feature实体
 * 参考: /packages/backend/server/src/core/features/types.ts
 */
@Entity
@Table(
    name = "features",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_feature_name_version", columnNames = {"feature", "version"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 特性名称 (使用字符串存储，保持与Node.js版本兼容)
     */
    @Column(name = "feature", nullable = false)
    private String name;

    /**
     * 特性配置 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    @Builder.Default
    private Map<String, Object> configs = Map.of();

    /**
     * 特性版本 (用于兼容性)
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * 特性类型 (0=功能开关, 1=配额限制)
     */
    @Column(name = "type", nullable = false)
    @Builder.Default
    private Integer type = 0;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 特性描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 废弃版本 (用于标记废弃的特性)
     */
    @Column(name = "deprecated_version", nullable = false)
    @Builder.Default
    private Integer deprecatedVersion = 0;

    /**
     * 废弃类型
     */
    @Column(name = "deprecated_type", nullable = false)
    @Builder.Default
    private Integer deprecatedType = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "feature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserFeature> userFeatures = new ArrayList<>();

    @OneToMany(mappedBy = "feature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkspaceFeature> workspaceFeatures = new ArrayList<>();

    // ==================== 特性名称枚举 ====================

    /**
     * 特性名称枚举
     * 对应Node.js版本的Feature枚举
     */
    public enum FeatureName {
        // 用户特性
        ADMIN("administrator"),
        EARLY_ACCESS("early_access"),
        AI_EARLY_ACCESS("ai_early_access"),
        UNLIMITED_COPILOT("unlimited_copilot"),
        FREE_PLAN("free_plan_v1"),
        PRO_PLAN("pro_plan_v1"),
        LIFETIME_PRO_PLAN("lifetime_pro_plan_v1"),

        // 工作区特性
        UNLIMITED_WORKSPACE("unlimited_workspace"),
        TEAM_PLAN("team_plan_v1"),

        // 实验性特性
        BETA_FEATURE("beta_feature"),
        EXPERIMENTAL_FEATURE("experimental_feature"),

        // AI相关特性
        AI_CHAT("ai_chat"),
        AI_WRITING("ai_writing"),
        AI_DRAWING("ai_drawing"),

        // 协作特性
        REAL_TIME_COLLABORATION("real_time_collaboration"),
        ADVANCED_PERMISSIONS("advanced_permissions"),
        TEAM_MANAGEMENT("team_management"),

        // 存储特性
        EXTENDED_STORAGE("extended_storage"),
        PREMIUM_STORAGE("premium_storage"),

        // 导入导出特性
        ADVANCED_EXPORT("advanced_export"),
        BULK_OPERATIONS("bulk_operations"),

        // 自定义特性
        CUSTOM_THEMES("custom_themes"),
        ADVANCED_SEARCH("advanced_search"),

        // 企业特性
        SSO_INTEGRATION("sso_integration"),
        AUDIT_LOGS("audit_logs"),
        ENTERPRISE_SECURITY("enterprise_security"),

        // 开发者特性
        API_ACCESS("api_access"),
        WEBHOOK_SUPPORT("webhook_support"),
        PLUGIN_SYSTEM("plugin_system");

        private final String value;

        FeatureName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static FeatureName fromValue(String value) {
            for (FeatureName feature : values()) {
                if (feature.value.equals(value)) {
                    return feature;
                }
            }
            throw new IllegalArgumentException("Unknown feature name: " + value);
        }
    }

    // ==================== 特性类型枚举 ====================

    /**
     * 特性类型枚举
     */
    public enum FeatureType {
        FEATURE(0),  // 功能开关
        QUOTA(1);    // 配额限制

        private final int value;

        FeatureType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // ==================== 早期访问类型枚举 ====================

    /**
     * 早期访问类型枚举
     */
    public enum EarlyAccessType {
        APP("app"),
        AI("ai");

        private final String value;

        EarlyAccessType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static EarlyAccessType fromValue(String value) {
            for (EarlyAccessType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown early access type: " + value);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查特性是否为用户特性
     */
    public boolean isUserFeature() {
        try {
            FeatureName featureName = FeatureName.fromValue(this.name);
            return switch (featureName) {
                case ADMIN, EARLY_ACCESS, AI_EARLY_ACCESS, UNLIMITED_COPILOT,
                     FREE_PLAN, PRO_PLAN, LIFETIME_PRO_PLAN, BETA_FEATURE,
                     EXPERIMENTAL_FEATURE, AI_CHAT, AI_WRITING, AI_DRAWING,
                     EXTENDED_STORAGE, PREMIUM_STORAGE, CUSTOM_THEMES,
                     ADVANCED_SEARCH, API_ACCESS -> true;
                default -> false;
            };
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查特性是否为工作区特性
     */
    public boolean isWorkspaceFeature() {
        try {
            FeatureName featureName = FeatureName.fromValue(this.name);
            return switch (featureName) {
                case UNLIMITED_WORKSPACE, TEAM_PLAN, REAL_TIME_COLLABORATION,
                     ADVANCED_PERMISSIONS, TEAM_MANAGEMENT, ADVANCED_EXPORT,
                     BULK_OPERATIONS, SSO_INTEGRATION, AUDIT_LOGS,
                     ENTERPRISE_SECURITY, WEBHOOK_SUPPORT, PLUGIN_SYSTEM -> true;
                default -> false;
            };
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查特性是否为配额相关
     */
    public boolean isQuotaFeature() {
        return this.type == FeatureType.QUOTA.getValue() ||
               FeatureName.FREE_PLAN.getValue().equals(this.name) ||
               FeatureName.PRO_PLAN.getValue().equals(this.name) ||
               FeatureName.LIFETIME_PRO_PLAN.getValue().equals(this.name) ||
               FeatureName.TEAM_PLAN.getValue().equals(this.name) ||
               FeatureName.EXTENDED_STORAGE.getValue().equals(this.name) ||
               FeatureName.PREMIUM_STORAGE.getValue().equals(this.name);
    }

    /**
     * 检查特性是否为实验性特性
     */
    public boolean isExperimentalFeature() {
        return FeatureName.BETA_FEATURE.getValue().equals(this.name) ||
               FeatureName.EXPERIMENTAL_FEATURE.getValue().equals(this.name) ||
               this.name.contains("experimental") ||
               this.name.contains("beta");
    }

    /**
     * 检查特性是否为管理员特性
     */
    public boolean isAdminFeature() {
        return FeatureName.ADMIN.getValue().equals(this.name) ||
               FeatureName.AUDIT_LOGS.getValue().equals(this.name) ||
               FeatureName.ENTERPRISE_SECURITY.getValue().equals(this.name);
    }

    /**
     * 检查特性是否已废弃
     */
    public boolean isDeprecated() {
        return this.deprecatedVersion > 0;
    }

    /**
     * 获取特性的显示名称
     */
    public String getDisplayName() {
        try {
            FeatureName featureName = FeatureName.fromValue(this.name);
            return switch (featureName) {
                case ADMIN -> "管理员权限";
                case EARLY_ACCESS -> "早期访问";
                case AI_EARLY_ACCESS -> "AI早期访问";
                case UNLIMITED_COPILOT -> "无限AI助手";
                case FREE_PLAN -> "免费计划";
                case PRO_PLAN -> "专业计划";
                case LIFETIME_PRO_PLAN -> "终身专业计划";
                case UNLIMITED_WORKSPACE -> "无限工作空间";
                case TEAM_PLAN -> "团队计划";
                case BETA_FEATURE -> "Beta功能";
                case EXPERIMENTAL_FEATURE -> "实验性功能";
                case AI_CHAT -> "AI聊天";
                case AI_WRITING -> "AI写作";
                case AI_DRAWING -> "AI绘图";
                case REAL_TIME_COLLABORATION -> "实时协作";
                case ADVANCED_PERMISSIONS -> "高级权限控制";
                case TEAM_MANAGEMENT -> "团队管理";
                case EXTENDED_STORAGE -> "扩展存储";
                case PREMIUM_STORAGE -> "高级存储";
                case ADVANCED_EXPORT -> "高级导出";
                case BULK_OPERATIONS -> "批量操作";
                case CUSTOM_THEMES -> "自定义主题";
                case ADVANCED_SEARCH -> "高级搜索";
                case SSO_INTEGRATION -> "SSO集成";
                case AUDIT_LOGS -> "审计日志";
                case ENTERPRISE_SECURITY -> "企业安全";
                case API_ACCESS -> "API访问";
                case WEBHOOK_SUPPORT -> "Webhook支持";
                case PLUGIN_SYSTEM -> "插件系统";
            };
        } catch (IllegalArgumentException e) {
            return this.name;
        }
    }
} 