package com.yunke.backend.notification.controller;


import com.yunke.backend.notification.domain.entity.MailQueue;
import com.yunke.backend.notification.domain.entity.MailTemplate;
import com.yunke.backend.notification.dto.MailDto;
import com.yunke.backend.notification.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 邮件REST API控制器
 * 对应Node.js版本的邮件API
 * 参考: /packages/backend/server/src/core/mail/resolver.ts
 */
@RestController
@RequestMapping("/api/mail")
@Slf4j
public class MailController {

    @Autowired
    private MailService mailService;

    // ==================== 邮件发送API ====================

    /**
     * 发送邮件
     * POST /api/mail/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendMail(
            @Valid @RequestBody MailDto.SendMailInput input) {
        
        Mono<Boolean> sendResult = input.getQueueMode() ? 
                mailService.queueMail(input.getTo(), input.getSubject(), input.getHtmlContent(), input.getPriority())
                        .map(queue -> queue != null) :
                mailService.sendMail(input.getTo(), input.getToName(), input.getSubject(), input.getHtmlContent());

        return sendResult.map(success -> {
            MailDto.MailOperationResponse response = MailDto.MailOperationResponse.builder()
                    .success(success)
                    .message(success ? "邮件发送成功" : "邮件发送失败")
                    .recipientEmail(input.getTo())
                    .mailType("direct")
                    .operationTime(LocalDateTime.now())
                    .build();
            
            return success ? 
                    ResponseEntity.ok(response) : 
                    ResponseEntity.badRequest().body(response);
        });
    }

    /**
     * 发送模板邮件
     * POST /api/mail/send-templated
     */
    @PostMapping("/send-templated")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendTemplatedMail(
            @Valid @RequestBody MailDto.SendTemplatedMailInput input) {
        
        Mono<Boolean> sendResult = input.getQueueMode() ?
                mailService.queueTemplatedMail(input.getTo(), input.getToName(), 
                        input.getTemplateName(), input.getVariables(), input.getPriority())
                        .map(queue -> queue != null) :
                mailService.sendTemplatedMail(input.getTo(), input.getToName(), 
                        input.getTemplateName(), input.getVariables());

        return sendResult.map(success -> {
            MailDto.MailOperationResponse response = MailDto.MailOperationResponse.builder()
                    .success(success)
                    .message(success ? "模板邮件发送成功" : "模板邮件发送失败")
                    .recipientEmail(input.getTo())
                    .mailType(input.getTemplateName())
                    .operationTime(LocalDateTime.now())
                    .build();
            
            return success ? 
                    ResponseEntity.ok(response) : 
                    ResponseEntity.badRequest().body(response);
        });
    }

    /**
     * 批量发送邮件
     * POST /api/mail/batch-send
     */
    @PostMapping("/batch-send")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.BatchOperationResult>> batchSendMails(
            @Valid @RequestBody MailDto.BatchSendMailInput input) {
        
        Mono<Integer> sendResult = input.getQueueMode() ?
                mailService.batchQueueMails(input.getRecipients(), input.getSubject(), input.getHtmlContent()) :
                mailService.batchSendMails(input.getRecipients(), input.getSubject(), input.getHtmlContent());

        return sendResult.map(successCount -> {
            int totalCount = input.getRecipients().size();
            int failureCount = totalCount - successCount;
            
            MailDto.BatchOperationResult result = MailDto.BatchOperationResult.builder()
                    .totalCount(totalCount)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .operationTime(LocalDateTime.now())
                    .build();
            
            return ResponseEntity.ok(result);
        });
    }

    /**
     * 批量发送模板邮件
     * POST /api/mail/batch-send-templated
     */
    @PostMapping("/batch-send-templated")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.BatchOperationResult>> batchSendTemplatedMails(
            @Valid @RequestBody MailDto.BatchSendTemplatedMailInput input) {
        
        Mono<Integer> sendResult = input.getQueueMode() ?
                mailService.batchQueueTemplatedMails(input.getRecipients(), 
                        input.getTemplateName(), input.getVariables()) :
                mailService.batchSendTemplatedMails(input.getRecipients(), 
                        input.getTemplateName(), input.getVariables());

        return sendResult.map(successCount -> {
            int totalCount = input.getRecipients().size();
            int failureCount = totalCount - successCount;
            
            MailDto.BatchOperationResult result = MailDto.BatchOperationResult.builder()
                    .totalCount(totalCount)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .operationTime(LocalDateTime.now())
                    .build();
            
            return ResponseEntity.ok(result);
        });
    }

    // ==================== 队列管理API ====================

    /**
     * 处理邮件队列
     * POST /api/mail/process-queue
     */
    @PostMapping("/process-queue")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.QueueProcessResult>> processMailQueue(
            @RequestParam(defaultValue = "50") int batchSize) {
        
        return mailService.processMailQueue(batchSize)
                .map(processed -> {
                    MailDto.QueueProcessResult result = MailDto.QueueProcessResult.builder()
                            .totalProcessed(processed)
                            .processTime(LocalDateTime.now())
                            .build();
                    
                    return ResponseEntity.ok(result);
                });
    }

    /**
     * 重试失败邮件
     * POST /api/mail/retry-failed
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> retryFailedMails() {
        return mailService.retryFailedMails()
                .map(retried -> ResponseEntity.ok(Map.of(
                        "retriedCount", retried,
                        "message", "失败邮件重试操作完成",
                        "operationTime", LocalDateTime.now()
                )));
    }

    /**
     * 取消邮件
     * DELETE /api/mail/queue/{mailId}
     */
    @DeleteMapping("/queue/{mailId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> cancelMail(@PathVariable Integer mailId) {
        return mailService.cancelMail(mailId)
                .map(success -> {
                    Map<String, Object> response = Map.of(
                            "success", success,
                            "message", success ? "邮件取消成功" : "邮件取消失败",
                            "mailId", mailId
                    );
                    
                    return success ? 
                            ResponseEntity.ok(response) : 
                            ResponseEntity.badRequest().body(response);
                });
    }

    /**
     * 清理邮件队列
     * POST /api/mail/cleanup
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.CleanupResult>> cleanupMailQueue(
            @Valid @RequestBody MailDto.CleanupInput input) {
        
        return Mono.zip(
                mailService.cleanupSentMails(input.getDaysOld()),
                mailService.cleanupFailedMails(input.getDaysOld())
        ).map(tuple -> {
            int sentCleaned = tuple.getT1();
            int failedCleaned = tuple.getT2();
            
            MailDto.CleanupResult result = MailDto.CleanupResult.builder()
                    .totalCleaned(sentCleaned + failedCleaned)
                    .sentMailsCleaned(sentCleaned)
                    .failedMailsCleaned(failedCleaned)
                    .cleanupTime(LocalDateTime.now())
                    .wasDryRun(input.getDryRun())
                    .build();
            
            return ResponseEntity.ok(result);
        });
    }

    // ==================== 模板管理API ====================

    /**
     * 获取邮件模板
     * GET /api/mail/templates/{templateName}
     */
    @GetMapping("/templates/{templateName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.MailTemplateInfo>> getTemplate(
            @PathVariable String templateName) {
        
        return mailService.getTemplate(templateName)
                .map(templateOpt -> {
                    if (templateOpt.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }
                    
                    MailTemplate template = templateOpt.get();
                    MailDto.MailTemplateInfo info = convertToTemplateInfo(template);
                    
                    return ResponseEntity.ok(info);
                });
    }

    /**
     * 创建邮件模板
     * POST /api/mail/templates
     */
    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.MailTemplateInfo>> createTemplate(
            @Valid @RequestBody MailDto.CreateTemplateInput input) {
        
        MailTemplate template = MailTemplate.builder()
                .name(input.getName())
                .displayName(input.getDisplayName())
                .category(input.getCategory())
                .subjectTemplate(input.getSubjectTemplate())
                .htmlTemplate(input.getHtmlTemplate())
                .textTemplate(input.getTextTemplate())
                .language(input.getLanguage())
                .enabled(input.getEnabled())
                .description(input.getDescription())
                .defaultVariables(input.getDefaultVariables())
                .build();
        
        return mailService.createTemplate(template)
                .map(created -> {
                    MailDto.MailTemplateInfo info = convertToTemplateInfo(created);
                    return ResponseEntity.status(HttpStatus.CREATED).body(info);
                });
    }

    /**
     * 更新邮件模板
     * PUT /api/mail/templates/{templateName}
     */
    @PutMapping("/templates/{templateName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.MailTemplateInfo>> updateTemplate(
            @PathVariable String templateName,
            @Valid @RequestBody MailDto.UpdateTemplateInput input) {
        
        MailTemplate updateData = MailTemplate.builder()
                .displayName(input.getDisplayName())
                .subjectTemplate(input.getSubjectTemplate())
                .htmlTemplate(input.getHtmlTemplate())
                .textTemplate(input.getTextTemplate())
                .description(input.getDescription())
                .defaultVariables(input.getDefaultVariables())
                .enabled(input.getEnabled())
                .build();
        
        return mailService.updateTemplate(templateName, updateData)
                .map(updated -> {
                    MailDto.MailTemplateInfo info = convertToTemplateInfo(updated);
                    return ResponseEntity.ok(info);
                })
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 删除邮件模板
     * DELETE /api/mail/templates/{templateName}
     */
    @DeleteMapping("/templates/{templateName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> deleteTemplate(
            @PathVariable String templateName) {
        
        return mailService.deleteTemplate(templateName)
                .map(success -> {
                    Map<String, Object> response = Map.of(
                            "success", success,
                            "message", success ? "模板删除成功" : "模板删除失败",
                            "templateName", templateName
                    );
                    
                    return success ? 
                            ResponseEntity.ok(response) : 
                            ResponseEntity.notFound().build();
                });
    }

    /**
     * 渲染邮件模板
     * POST /api/mail/templates/render
     */
    @PostMapping("/templates/render")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.TemplateRenderResult>> renderTemplate(
            @Valid @RequestBody MailDto.TemplateRenderInput input) {
        
        return Mono.zip(
                mailService.renderSubject(input.getTemplateName(), input.getVariables()),
                mailService.renderTemplate(input.getTemplateName(), input.getVariables())
        ).map(tuple -> {
            String subject = tuple.getT1();
            String htmlContent = tuple.getT2();
            
            MailDto.TemplateRenderResult result = MailDto.TemplateRenderResult.builder()
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .templateName(input.getTemplateName())
                    .usedVariables(input.getVariables())
                    .build();
            
            return ResponseEntity.ok(result);
        }).onErrorReturn(ResponseEntity.badRequest().build());
    }

    // ==================== 用户邮件API ====================

    /**
     * 发送用户注册邮件
     * POST /api/mail/user/sign-up
     */
    @PostMapping("/user/sign-up")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendUserSignUpMail(
            @Valid @RequestBody MailDto.UserSignUpMailInput input) {
        
        return mailService.sendUserSignUpMail(input.getEmail(), input.getUsername(), input.getVerificationUrl())
                .map(success -> createOperationResponse(success, input.getEmail(), "user-sign-up"));
    }

    /**
     * 发送邮箱验证邮件
     * POST /api/mail/user/email-verify
     */
    @PostMapping("/user/email-verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendEmailVerificationMail(
            @Valid @RequestBody MailDto.EmailVerificationMailInput input) {
        
        return mailService.sendEmailVerificationMail(input.getEmail(), input.getUsername(), input.getVerificationUrl())
                .map(success -> createOperationResponse(success, input.getEmail(), "email-verify"));
    }

    /**
     * 发送密码重置邮件
     * POST /api/mail/user/password-reset
     */
    @PostMapping("/user/password-reset")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendPasswordResetMail(
            @Valid @RequestBody MailDto.PasswordResetMailInput input) {
        
        return mailService.sendPasswordResetMail(input.getEmail(), input.getUsername(), input.getResetUrl())
                .map(success -> createOperationResponse(success, input.getEmail(), "password-reset"));
    }

    // ==================== 工作空间邮件API ====================

    /**
     * 发送工作空间邀请邮件
     * POST /api/mail/workspace/invitation
     */
    @PostMapping("/workspace/invitation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendWorkspaceInvitationMail(
            @Valid @RequestBody MailDto.WorkspaceInvitationMailInput input) {
        
        return mailService.sendWorkspaceInvitationMail(input.getEmail(), input.getInviterName(), 
                        input.getWorkspaceName(), input.getInvitationUrl())
                .map(success -> createOperationResponse(success, input.getEmail(), "workspace-invitation"));
    }

    /**
     * 发送成员离开通知邮件
     * POST /api/mail/workspace/member-leave
     */
    @PostMapping("/workspace/member-leave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendMemberLeaveMail(
            @Valid @RequestBody MailDto.WorkspaceMemberMailInput input) {
        
        return mailService.sendMemberLeaveMail(input.getEmail(), input.getMemberName(), input.getWorkspaceName())
                .map(success -> createOperationResponse(success, input.getEmail(), "member-leave"));
    }

    // ==================== 文档邮件API ====================

    /**
     * 发送文档提及邮件
     * POST /api/mail/document/mention
     */
    @PostMapping("/document/mention")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendDocumentMentionMail(
            @Valid @RequestBody MailDto.DocumentMentionMailInput input) {
        
        return mailService.sendDocumentMentionMail(input.getEmail(), input.getMentionerName(), 
                        input.getDocumentTitle(), input.getDocumentUrl())
                .map(success -> createOperationResponse(success, input.getEmail(), "document-mention"));
    }

    /**
     * 发送文档评论邮件
     * POST /api/mail/document/comment
     */
    @PostMapping("/document/comment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendDocumentCommentMail(
            @Valid @RequestBody MailDto.DocumentCommentMailInput input) {
        
        return mailService.sendDocumentCommentMail(input.getEmail(), input.getCommenterName(), 
                        input.getDocumentTitle(), input.getCommentContent(), input.getDocumentUrl())
                .map(success -> createOperationResponse(success, input.getEmail(), "document-comment"));
    }

    // ==================== 统计和监控API ====================

    /**
     * 获取邮件统计
     * GET /api/mail/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.MailStatisticsDto>> getMailStatistics() {
        return mailService.getMailStatistics()
                .map(stats -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Long> statusCounts = (Map<String, Long>) stats.get("statusCounts");
                    
                    MailDto.MailStatisticsDto statisticsDto = MailDto.MailStatisticsDto.builder()
                            .statusCounts(statusCounts)
                            .todayTotal((Long) stats.get("todayTotal"))
                            .todaySent((Long) stats.get("todaySent"))
                            .todayFailed((Long) stats.get("todayFailed"))
                            .lastProcessTime(LocalDateTime.now())
                            .build();
                    
                    return ResponseEntity.ok(statisticsDto);
                });
    }

    /**
     * 获取队列状态
     * GET /api/mail/queue/status
     */
    @GetMapping("/queue/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.QueueStatusDto>> getQueueStatus() {
        return mailService.getQueueStatus()
                .map(status -> {
                    MailDto.QueueStatusDto statusDto = MailDto.QueueStatusDto.builder()
                            .pendingCount((Long) status.get("pendingCount"))
                            .highPriorityCount((Long) status.get("highPriorityCount"))
                            .urgentCount((Long) status.get("urgentCount"))
                            .configValid((Boolean) status.get("configValid"))
                            .serviceAvailable((Boolean) status.get("serviceAvailable"))
                            .lastProcessTime(LocalDateTime.now())
                            .build();
                    
                    return ResponseEntity.ok(statusDto);
                });
    }

    /**
     * 获取邮件历史
     * GET /api/mail/history
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<MailDto.MailQueueInfo>>> getMailHistory(
            @RequestParam String recipientEmail,
            @RequestParam(defaultValue = "50") int limit) {
        
        return mailService.getMailHistory(recipientEmail, limit)
                .map(mails -> {
                    List<MailDto.MailQueueInfo> mailInfos = mails.stream()
                            .map(this::convertToQueueInfo)
                            .toList();
                    
                    return ResponseEntity.ok(mailInfos);
                });
    }

    /**
     * 获取失败邮件列表
     * GET /api/mail/failed
     */
    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<MailDto.MailQueueInfo>>> getFailedMails(
            @RequestParam(defaultValue = "50") int limit) {
        
        return mailService.getFailedMails(limit)
                .map(mails -> {
                    List<MailDto.MailQueueInfo> mailInfos = mails.stream()
                            .map(this::convertToQueueInfo)
                            .toList();
                    
                    return ResponseEntity.ok(mailInfos);
                });
    }

    // ==================== 测试和配置API ====================

    /**
     * 测试邮件配置
     * GET /api/mail/test-config
     */
    @GetMapping("/test-config")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> testMailConfiguration() {
        return mailService.testMailConfiguration()
                .map(success -> {
                    Map<String, Object> response = Map.of(
                            "configurationValid", success,
                            "message", success ? "邮件配置正常" : "邮件配置异常",
                            "testTime", LocalDateTime.now()
                    );
                    
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 发送测试邮件
     * POST /api/mail/test
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.MailOperationResponse>> sendTestMail(
            @Valid @RequestBody MailDto.TestMailInput input) {
        
        return mailService.sendTestMail(input.getTestEmail())
                .map(success -> createOperationResponse(success, input.getTestEmail(), "test"));
    }

    /**
     * 获取配置状态
     * GET /api/mail/config/status
     */
    @GetMapping("/config/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<MailDto.MailConfigurationStatus>> getConfigurationStatus() {
        return mailService.getConfigurationStatus()
                .map(config -> {
                    MailDto.MailConfigurationStatus status = MailDto.MailConfigurationStatus.builder()
                            .enabled((Boolean) config.get("enabled"))
                            .host((String) config.get("host"))
                            .port((Integer) config.get("port"))
                            .sender((String) config.get("sender"))
                            .configValid((Boolean) config.get("configValid"))
                            .testMode((Boolean) config.get("testMode"))
                            .lastTestTime(LocalDateTime.now())
                            .build();
                    
                    return ResponseEntity.ok(status);
                });
    }

    // ==================== 辅助方法 ====================

    private ResponseEntity<MailDto.MailOperationResponse> createOperationResponse(
            Boolean success, String email, String mailType) {
        
        MailDto.MailOperationResponse response = MailDto.MailOperationResponse.builder()
                .success(success)
                .message(success ? "邮件发送成功" : "邮件发送失败")
                .recipientEmail(email)
                .mailType(mailType)
                .operationTime(LocalDateTime.now())
                .build();
        
        return success ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.badRequest().body(response);
    }

    private MailDto.MailTemplateInfo convertToTemplateInfo(MailTemplate template) {
        return MailDto.MailTemplateInfo.builder()
                .id(template.getId())
                .name(template.getName())
                .displayName(template.getDisplayName())
                .category(template.getCategory())
                .subjectTemplate(template.getSubjectTemplate())
                .language(template.getLanguage())
                .enabled(template.getEnabled())
                .version(template.getVersion())
                .description(template.getDescription())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .createdBy(template.getCreatedBy())
                .build();
    }

    private MailDto.MailQueueInfo convertToQueueInfo(MailQueue mail) {
        return MailDto.MailQueueInfo.builder()
                .id(mail.getId())
                .mailType(mail.getMailType())
                .recipientEmail(mail.getRecipientEmail())
                .recipientName(mail.getRecipientName())
                .subject(mail.getSubject())
                .status(mail.getStatus())
                .priority(mail.getPriority())
                .retryCount(mail.getRetryCount())
                .maxRetries(mail.getMaxRetries())
                .nextRetryAt(mail.getNextRetryAt())
                .errorMessage(mail.getErrorMessage())
                .sentAt(mail.getSentAt())
                .createdAt(mail.getCreatedAt())
                .updatedAt(mail.getUpdatedAt())
                .build();
    }
}