package com.yunke.backend.notification.dto;

import com.yunke.backend.notification.domain.entity.MailQueue;
import com.yunke.backend.notification.domain.entity.MailTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 邮件相关DTO类
 * 对应Node.js版本的邮件DTO
 */
public class MailDto {

    // ==================== 邮件发送DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMailInput {
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String to;
        
        private String toName;
        
        @NotBlank(message = "邮件主题不能为空")
        private String subject;
        
        @NotBlank(message = "邮件内容不能为空")
        private String htmlContent;
        
        private String textContent;
        
        @Builder.Default
        private MailQueue.MailPriority priority = MailQueue.MailPriority.NORMAL;
        
        @Builder.Default
        private Boolean queueMode = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendTemplatedMailInput {
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String to;
        
        private String toName;
        
        @NotBlank(message = "模板名称不能为空")
        private String templateName;
        
        @NotNull(message = "模板变量不能为空")
        private Map<String, Object> variables;
        
        @Builder.Default
        private MailQueue.MailPriority priority = MailQueue.MailPriority.NORMAL;
        
        @Builder.Default
        private Boolean queueMode = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSendMailInput {
        @NotNull(message = "收件人列表不能为空")
        private List<String> recipients;
        
        @NotBlank(message = "邮件主题不能为空")
        private String subject;
        
        @NotBlank(message = "邮件内容不能为空")
        private String htmlContent;
        
        @Builder.Default
        private MailQueue.MailPriority priority = MailQueue.MailPriority.NORMAL;
        
        @Builder.Default
        private Boolean queueMode = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSendTemplatedMailInput {
        @NotNull(message = "收件人列表不能为空")
        private List<String> recipients;
        
        @NotBlank(message = "模板名称不能为空")
        private String templateName;
        
        @NotNull(message = "模板变量不能为空")
        private Map<String, Object> variables;
        
        @Builder.Default
        private MailQueue.MailPriority priority = MailQueue.MailPriority.NORMAL;
        
        @Builder.Default
        private Boolean queueMode = true;
    }

    // ==================== 邮件队列DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailQueueInfo {
        private Integer id;
        private String mailType;
        private String recipientEmail;
        private String recipientName;
        private String subject;
        private MailQueue.MailStatus status;
        private MailQueue.MailPriority priority;
        private Integer retryCount;
        private Integer maxRetries;
        private LocalDateTime nextRetryAt;
        private String errorMessage;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueProcessResult {
        private Integer totalProcessed;
        private Integer successCount;
        private Integer failureCount;
        private List<String> failureReasons;
        private LocalDateTime processTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueStatusDto {
        private Long pendingCount;
        private Long highPriorityCount;
        private Long urgentCount;
        private Long processingCount;
        private Boolean configValid;
        private Boolean serviceAvailable;
        private LocalDateTime lastProcessTime;
    }

    // ==================== 邮件模板DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailTemplateInfo {
        private Integer id;
        private String name;
        private String displayName;
        private MailTemplate.TemplateCategory category;
        private String subjectTemplate;
        private String language;
        private Boolean enabled;
        private Integer version;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTemplateInput {
        @NotBlank(message = "模板名称不能为空")
        private String name;
        
        @NotBlank(message = "显示名称不能为空")
        private String displayName;
        
        @NotNull(message = "模板分类不能为空")
        private MailTemplate.TemplateCategory category;
        
        @NotBlank(message = "主题模板不能为空")
        private String subjectTemplate;
        
        @NotBlank(message = "HTML模板不能为空")
        private String htmlTemplate;
        
        private String textTemplate;
        
        @Builder.Default
        private String language = "zh-CN";
        
        @Builder.Default
        private Boolean enabled = true;
        
        private String description;
        private String defaultVariables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTemplateInput {
        private String displayName;
        private String subjectTemplate;
        private String htmlTemplate;
        private String textTemplate;
        private String description;
        private String defaultVariables;
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRenderInput {
        @NotBlank(message = "模板名称不能为空")
        private String templateName;
        
        @NotNull(message = "模板变量不能为空")
        private Map<String, Object> variables;
        
        @Builder.Default
        private Boolean renderSubject = true;
        
        @Builder.Default
        private Boolean renderContent = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRenderResult {
        private String subject;
        private String htmlContent;
        private String textContent;
        private String templateName;
        private Map<String, Object> usedVariables;
    }

    // ==================== 用户邮件DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSignUpMailInput {
        @NotBlank(message = "用户邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @NotBlank(message = "验证链接不能为空")
        private String verificationUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailVerificationMailInput {
        @NotBlank(message = "用户邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @NotBlank(message = "验证链接不能为空")
        private String verificationUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetMailInput {
        @NotBlank(message = "用户邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @NotBlank(message = "重置链接不能为空")
        private String resetUrl;
    }

    // ==================== 工作空间邮件DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceInvitationMailInput {
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "邀请者名称不能为空")
        private String inviterName;
        
        @NotBlank(message = "工作空间名称不能为空")
        private String workspaceName;
        
        @NotBlank(message = "邀请链接不能为空")
        private String invitationUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceMemberMailInput {
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "成员名称不能为空")
        private String memberName;
        
        @NotBlank(message = "工作空间名称不能为空")
        private String workspaceName;
    }

    // ==================== 文档邮件DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentMentionMailInput {
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "提及者名称不能为空")
        private String mentionerName;
        
        @NotBlank(message = "文档标题不能为空")
        private String documentTitle;
        
        @NotBlank(message = "文档链接不能为空")
        private String documentUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentCommentMailInput {
        @NotBlank(message = "收件人邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "评论者名称不能为空")
        private String commenterName;
        
        @NotBlank(message = "文档标题不能为空")
        private String documentTitle;
        
        @NotBlank(message = "评论内容不能为空")
        private String commentContent;
        
        @NotBlank(message = "文档链接不能为空")
        private String documentUrl;
    }

    // ==================== 统计DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailStatisticsDto {
        private Map<String, Long> statusCounts;
        private Long todayTotal;
        private Long todaySent;
        private Long todayFailed;
        private Map<String, Long> typeCounts;
        private Map<String, Long> priorityCounts;
        private Double successRate;
        private LocalDateTime lastProcessTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStatisticsDto {
        private Long totalTemplates;
        private Long enabledTemplates;
        private Map<String, Long> categoryCounts;
        private Map<String, Long> languageCounts;
        private List<String> mostUsedTemplates;
    }

    // ==================== 响应DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailOperationResponse {
        private Boolean success;
        private String message;
        private String recipientEmail;
        private String mailType;
        private LocalDateTime operationTime;
        private Map<String, Object> additionalInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperationResult {
        private Integer totalCount;
        private Integer successCount;
        private Integer failureCount;
        private List<String> successEmails;
        private List<String> failureEmails;
        private Map<String, String> errors;
        private LocalDateTime operationTime;
    }

    // ==================== 配置DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailConfigurationStatus {
        private Boolean enabled;
        private String host;
        private Integer port;
        private String sender;
        private Boolean configValid;
        private Boolean testMode;
        private Boolean connectionHealthy;
        private LocalDateTime lastTestTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestMailInput {
        @NotBlank(message = "测试邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String testEmail;
        
        private String customSubject;
        private String customContent;
    }

    // ==================== 搜索和过滤DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailQueueSearchInput {
        private String recipientEmail;
        private String mailType;
        private MailQueue.MailStatus status;
        private MailQueue.MailPriority priority;
        private LocalDateTime createdAfter;
        private LocalDateTime createdBefore;
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDirection;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateSearchInput {
        private String keyword;
        private MailTemplate.TemplateCategory category;
        private String language;
        private Boolean enabled;
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDirection;
    }

    // ==================== 清理操作DTO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleanupInput {
        private Integer daysOld;
        private Boolean includeSent;
        private Boolean includeFailed;
        private Boolean includeCancelled;
        private Boolean dryRun;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleanupResult {
        private Integer totalCleaned;
        private Integer sentMailsCleaned;
        private Integer failedMailsCleaned;
        private Integer cancelledMailsCleaned;
        private LocalDateTime cleanupTime;
        private Boolean wasDryRun;
    }
}