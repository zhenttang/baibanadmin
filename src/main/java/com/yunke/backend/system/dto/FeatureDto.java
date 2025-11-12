package com.yunke.backend.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 特性开关相关DTO类
 * 对应Node.js版本的特性DTO
 */
public class FeatureDto {

    // ==================== 基础特性DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureInfo {
        private Integer id;
        private String name;
        private String displayName;
        private String description;
        private Integer type;
        private Integer version;
        private Boolean enabled;
        private Map<String, Object> configs;
        private Boolean deprecated;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFeatureInput {
        @NotBlank(message = "特性名称不能为空")
        private String name;
        
        private String description;
        
        @NotNull(message = "特性类型不能为空")
        private Integer type;
        
        private Map<String, Object> configs;
        
        @Builder.Default
        private Boolean enabled = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateFeatureInput {
        private String description;
        private Map<String, Object> configs;
        private Boolean enabled;
    }

    // ==================== 用户特性DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFeatureInfo {
        private Integer id;
        private String userId;
        private String featureName;
        private String displayName;
        private Integer type;
        private String reason;
        private Boolean activated;
        private LocalDateTime createdAt;
        private LocalDateTime expiredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddUserFeatureInput {
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
        
        @NotBlank(message = "添加原因不能为空")
        private String reason;
        
        private LocalDateTime expiredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUserFeatureInput {
        @NotNull(message = "用户ID列表不能为空")
        private List<String> userIds;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
        
        @NotBlank(message = "操作原因不能为空")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFeatureOperationInput {
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SwitchUserPlanInput {
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        private String fromPlan;
        
        @NotBlank(message = "目标计划不能为空")
        private String toPlan;
        
        @NotBlank(message = "切换原因不能为空")
        private String reason;
    }

    // ==================== 工作区特性DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceFeatureInfo {
        private Integer id;
        private String workspaceId;
        private String featureName;
        private String displayName;
        private Integer type;
        private String reason;
        private Boolean activated;
        private Map<String, Object> configs;
        private LocalDateTime createdAt;
        private LocalDateTime expiredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddWorkspaceFeatureInput {
        @NotBlank(message = "工作区ID不能为空")
        private String workspaceId;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
        
        @NotBlank(message = "添加原因不能为空")
        private String reason;
        
        private Map<String, Object> configs;
        private LocalDateTime expiredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchWorkspaceFeatureInput {
        @NotNull(message = "工作区ID列表不能为空")
        private List<String> workspaceIds;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
        
        @NotBlank(message = "操作原因不能为空")
        private String reason;
        
        private Map<String, Object> configs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceFeatureOperationInput {
        @NotBlank(message = "工作区ID不能为空")
        private String workspaceId;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateWorkspaceFeatureConfigInput {
        @NotBlank(message = "工作区ID不能为空")
        private String workspaceId;
        
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
        
        @NotNull(message = "配置不能为空")
        private Map<String, Object> configs;
    }

    // ==================== 管理员操作DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOperationInput {
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotBlank(message = "操作原因不能为空")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EarlyAccessOperationInput {
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotBlank(message = "早期访问类型不能为空")
        private String accessType; // "app" 或 "ai"
        
        @NotBlank(message = "操作原因不能为空")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckEarlyAccessInput {
        @NotBlank(message = "邮箱不能为空")
        private String email;
        
        @NotBlank(message = "早期访问类型不能为空")
        private String accessType; // "app" 或 "ai"
    }

    // ==================== 查询和统计DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureUsageStatsDto {
        private Long totalFeatures;
        private Long enabledFeatures;
        private Long deprecatedFeatures;
        private Long totalUserFeatures;
        private Long totalWorkspaceFeatures;
        private Long adminUsers;
        private Long earlyAccessUsers;
        private Map<String, Long> userFeatureStats;
        private Map<String, Long> workspaceFeatureStats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFeatureListDto {
        private String userId;
        private List<String> activeFeatures;
        private List<UserFeatureInfo> allFeatures;
        private Map<String, Object> quotaInfo;
        private Boolean isAdmin;
        private List<String> earlyAccessTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceFeatureListDto {
        private String workspaceId;
        private List<String> activeFeatures;
        private List<WorkspaceFeatureInfo> allFeatures;
        private Map<String, Object> quotaInfo;
        private Boolean hasTeamPlan;
        private Boolean hasEnterpriseFeatures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureCheckResult {
        private String featureName;
        private Boolean hasFeature;
        private Boolean isActive;
        private Boolean isExpired;
        private LocalDateTime expiredAt;
        private Map<String, Object> config;
    }

    // ==================== A/B测试DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperimentAssignmentInput {
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotBlank(message = "实验特性不能为空")
        private String experimentFeature;
        
        @NotNull(message = "分配概率不能为空")
        private Double probability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperimentOperationInput {
        @NotBlank(message = "实验名称不能为空")
        private String experimentName;
        
        @NotNull(message = "是否保留特性不能为空")
        private Boolean retainFeature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperimentStatsDto {
        private String experimentName;
        private Long totalUsers;
        private Long assignedUsers;
        private Double assignmentRate;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean isActive;
    }

    // ==================== 配置和环境DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureConfigDto {
        private List<String> availableUserFeatures;
        private List<String> availableWorkspaceFeatures;
        private List<String> configurableUserFeatures;
        private List<String> configurableWorkspaceFeatures;
        private Boolean isSelfHosted;
        private Map<String, Object> environmentConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnvironmentFeatureCheckInput {
        @NotBlank(message = "特性名称不能为空")
        private String featureName;
        
        private String environment; // "selfhosted" 或 "cloud"
    }

    // ==================== 批量操作结果DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperationResult {
        private Integer successCount;
        private Integer failureCount;
        private List<String> successIds;
        private List<String> failureIds;
        private Map<String, String> errors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleanupResult {
        private Integer expiredUserFeatures;
        private Integer expiredWorkspaceFeatures;
        private Integer totalCleaned;
        private LocalDateTime cleanupTime;
    }

    // ==================== 验证和响应DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureValidationResult {
        private String featureName;
        private Boolean isValid;
        private Boolean isUserFeature;
        private Boolean isWorkspaceFeature;
        private Boolean isQuotaFeature;
        private Boolean isExperimental;
        private String validationMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureOperationResponse {
        private Boolean success;
        private String message;
        private String featureName;
        private String targetId; // userId 或 workspaceId
        private LocalDateTime operationTime;
        private Map<String, Object> additionalInfo;
    }

    // ==================== 缓存操作DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheOperationInput {
        private String cacheType; // "feature", "user", "workspace", "all"
        private String targetId;  // 可选的目标ID
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheOperationResult {
        private String cacheType;
        private String operation; // "refresh", "warmup", "clear"
        private Boolean success;
        private String message;
        private LocalDateTime operationTime;
    }
}