package com.yunke.backend.notification.service.impl;

import com.yunke.backend.infrastructure.config.MailConfig;
import com.yunke.backend.notification.domain.entity.MailQueue;
import com.yunke.backend.notification.domain.entity.MailTemplate;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.notification.repository.MailQueueRepository;
import com.yunke.backend.notification.repository.MailTemplateRepository;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.workspace.repository.WorkspaceRepository;
import com.yunke.backend.notification.service.MailService;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;

/**
 * 邮件服务实现
 * 对应Node.js版本的邮件服务实现
 * 参考: /packages/backend/server/src/core/mail/mailer.ts
 */
@Service
@Transactional
@Slf4j
public class MailServiceImpl implements MailService {

    @Autowired
    private MailConfig mailConfig;

    @Autowired
    private MailQueueRepository mailQueueRepository;

    @Autowired
    private MailTemplateRepository mailTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TemplateEngine templateEngine;

    private JavaMailSender javaMailSender;

    // 邮箱格式验证
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // ==================== 邮件发送 ====================

    /**
     * 发送简单邮件（内部方法）
     */
    private void sendMailInternal(String to, String subject, String htmlContent, String textContent) {
        // 使用模板引擎渲染内容
        // 实现省略，避免与接口方法冲突
    }

    @Override
    public Mono<Boolean> sendMail(String to, String subject, String htmlContent) {
        return sendMail(to, null, subject, htmlContent);
    }

    @Override
    public Mono<Boolean> sendMail(String to, String toName, String subject, String htmlContent) {
        return Mono.fromCallable(() -> {
            if (!isValidConfiguration()) {
                log.warn("Mail configuration is invalid, skipping email send");
                return false;
            }

            if (!isValidEmail(to)) {
                log.warn("Invalid email address: {}", to);
                return false;
            }

            try {
                MimeMessage message = getMailSender().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailConfig.getFullSender());
                
                if (toName != null && !toName.isEmpty()) {
                    helper.setTo(String.format("%s <%s>", toName, to));
                } else {
                    helper.setTo(to);
                }
                
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                getMailSender().send(message);
                log.info("Email sent successfully to: {} <{}>", toName, to);
                return true;

            } catch (Exception e) {
                log.error("Failed to send email to {} <{}>: {}", toName, to, e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public Mono<Boolean> trySendMail(String to, String subject, String htmlContent) {
        return sendMail(to, subject, htmlContent)
                .onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> sendTemplatedMail(String to, String templateName, Map<String, Object> variables) {
        return sendTemplatedMail(to, null, templateName, variables);
    }

    @Override
    public Mono<Boolean> sendTemplatedMail(String to, String toName, String templateName, Map<String, Object> variables) {
        return getTemplate(templateName)
                .flatMap(templateOpt -> {
                    if (templateOpt.isEmpty()) {
                        log.warn("Template not found: {}", templateName);
                        return Mono.just(false);
                    }

                    MailTemplate template = templateOpt.get();
                    
                    return Mono.zip(
                            renderSubject(templateName, variables),
                            renderTemplate(templateName, variables)
                    ).flatMap(tuple -> {
                        String subject = tuple.getT1();
                        String htmlContent = tuple.getT2();
                        return sendMail(to, toName, subject, htmlContent);
                    });
                });
    }

    /**
     * 发送带名称的邮件
     */
    public Mono<Boolean> sendMailWithName(String to, String name, String subject, String content) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                if (name != null && !name.isEmpty()) {
                    helper.setTo(new InternetAddress(to, name, "UTF-8"));
                } else {
                    helper.setTo(to);
                }
                
                helper.setFrom(mailConfig.getFrom());
                helper.setSubject(subject);
                helper.setText(content, true);
                
                javaMailSender.send(message);
                log.info("Email sent to {} ({}): {}", name, to, subject);
                return true;
            } catch (Exception e) {
                log.error("Failed to send email to {} ({}): {}", name, to, e.getMessage(), e);
                return false;
            }
                });
    }

    // ==================== 队列邮件 ====================

    @Override
    public Mono<MailQueue> queueMail(String to, String subject, String htmlContent) {
        return queueMail(to, subject, htmlContent, MailQueue.MailPriority.NORMAL);
    }

    @Override
    public Mono<MailQueue> queueMail(String to, String subject, String htmlContent, MailQueue.MailPriority priority) {
        return Mono.fromCallable(() -> {
            if (!isValidEmail(to)) {
                throw new IllegalArgumentException("Invalid email address: " + to);
            }

            MailQueue mailQueue = MailQueue.builder()
                    .mailType("direct")
                    .recipientEmail(to)
                    .subject(subject)
                    .parametersMap(Map.of("htmlContent", htmlContent))
                    .priority(priority)
                    .status(MailQueue.MailStatus.PENDING)
                    .build();

            MailQueue saved = mailQueueRepository.save(mailQueue);
            log.info("Queued email for: {}, priority: {}", to, priority);
            return saved;
        });
    }

    @Override
    public Mono<MailQueue> queueTemplatedMail(String to, String templateName, Map<String, Object> variables) {
        return queueTemplatedMail(to, templateName, variables, MailQueue.MailPriority.NORMAL);
    }

    @Override
    public Mono<MailQueue> queueTemplatedMail(String to, String templateName, Map<String, Object> variables, 
                                               MailQueue.MailPriority priority) {
        return queueTemplatedMail(to, null, templateName, variables, priority);
    }

    @Override
    public Mono<MailQueue> queueTemplatedMail(String to, String toName, String templateName, 
                                               Map<String, Object> variables, MailQueue.MailPriority priority) {
        return Mono.fromCallable(() -> {
            if (!isValidEmail(to)) {
                throw new IllegalArgumentException("Invalid email address: " + to);
            }

            // 渲染主题
            String subject = renderSubject(templateName, variables).block();
            
            MailQueue mailQueue = MailQueue.builder()
                    .mailType(templateName)
                    .recipientEmail(to)
                    .recipientName(toName)
                    .subject(subject)
                    .status(MailQueue.MailStatus.PENDING)
                    .priority(priority)
                    .build();
        
            // 设置参数
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                mailQueue.setParameters(objectMapper.writeValueAsString(variables));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize mail parameters", e);
                throw new RuntimeException("Failed to serialize mail parameters", e);
            }

            MailQueue saved = mailQueueRepository.save(mailQueue);
            log.info("Queued templated email for: {}, template: {}, priority: {}", to, templateName, priority);
            return saved;
        });
    }

    /**
     * 发送模板邮件
     */
    @Override
    public Mono<Boolean> sendTemplateEmail(String to, String name, String templateName, 
                                          Map<String, Object> variables, 
                                          MailQueue.MailPriority priority) {
        String toName = name != null ? name : "";
        String subject = getSubjectFromTemplate(templateName);
        
        return Mono.fromCallable(() -> {
            // 创建邮件队列记录
            MailQueue mailQueue = MailQueue.builder()
                    .mailType(templateName)
                    .recipientEmail(to)
                    .recipientName(toName)
                    .subject(subject)
                    .status(MailQueue.MailStatus.PENDING)
                    .priority(priority)
                    .build();
            
            // 设置参数
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                mailQueue.setParameters(objectMapper.writeValueAsString(variables));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize mail parameters", e);
                return false;
            }
            
            MailQueue saved = mailQueueRepository.save(mailQueue);
            log.info("Mail queued: {}", saved.getId());
            return true;
        }).onErrorReturn(false);
    }

    // ==================== 队列管理 ====================

    @Override
    public Mono<Integer> processMailQueue() {
        return processMailQueue(50); // 默认批量处理50封邮件
    }

    @Override
    public Mono<Integer> processMailQueue(int batchSize) {
        return Mono.fromCallable(() -> {
            if (!isValidConfiguration()) {
                log.warn("Mail configuration is invalid, skipping queue processing");
                return 0;
            }

            List<MailQueue> mailsToProcess = mailQueueRepository.findProcessableMails(
                    LocalDateTime.now(), PageRequest.of(0, batchSize)
            );

            if (mailsToProcess.isEmpty()) {
                return 0;
            }

            int processed = 0;
            for (MailQueue mail : mailsToProcess) {
                try {
                    // 标记为处理中
                    mail.setStatus(MailQueue.MailStatus.PROCESSING);
                    mailQueueRepository.save(mail);

                    boolean success = processSingleMail(mail);
                    
                    if (success) {
                        mail.markAsSent();
                        processed++;
                        log.info("Successfully processed mail: {}", mail.getId());
                    } else {
                        mail.incrementRetryCount();
                        mail.calculateNextRetryTime(5); // 5分钟基础延迟
                        mail.markAsFailed("Send failed", null);
                        log.warn("Failed to process mail: {}, retry count: {}", 
                                mail.getId(), mail.getRetryCount());
                    }
                    
                    mailQueueRepository.save(mail);

                } catch (Exception e) {
                    mail.incrementRetryCount();
                    mail.calculateNextRetryTime(5);
                    mail.markAsFailed(e.getMessage(), getStackTrace(e));
                    mailQueueRepository.save(mail);
                    log.error("Error processing mail {}: {}", mail.getId(), e.getMessage(), e);
                }
            }

            log.info("Processed {} mails from queue", processed);
            return processed;
        });
    }

    private boolean processSingleMail(MailQueue mail) {
        try {
            if ("direct".equals(mail.getMailType())) {
                // 直接发送HTML内容
                Map<String, Object> params = mail.getParametersMap();
                String htmlContent = (String) params.get("htmlContent");
                return sendMail(mail.getRecipientEmail(), mail.getRecipientName(), 
                               mail.getSubject(), htmlContent).block();
            } else {
                // 使用模板发送
                return sendTemplatedMail(mail.getRecipientEmail(), mail.getRecipientName(),
                                       mail.getMailType(), mail.getParametersMap()).block();
            }
        } catch (Exception e) {
            log.error("Error in processSingleMail: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Mono<Integer> retryFailedMails() {
        return Mono.fromCallable(() -> {
            List<MailQueue> failedMails = mailQueueRepository.findMailsToRetry(
                    LocalDateTime.now(), PageRequest.of(0, 20)
            );

            int retried = 0;
            for (MailQueue mail : failedMails) {
                mail.setStatus(MailQueue.MailStatus.PENDING);
                mailQueueRepository.save(mail);
                retried++;
            }

            log.info("Reset {} failed mails for retry", retried);
            return retried;
        });
    }

    @Override
    public Mono<Boolean> cancelMail(Integer mailId) {
        return Mono.fromCallable(() -> {
            Optional<MailQueue> mailOpt = mailQueueRepository.findById(mailId);
            if (mailOpt.isPresent()) {
                MailQueue mail = mailOpt.get();
                if (mail.getStatus() == MailQueue.MailStatus.PENDING || 
                    mail.getStatus() == MailQueue.MailStatus.FAILED) {
                    mail.setStatus(MailQueue.MailStatus.CANCELLED);
                    mailQueueRepository.save(mail);
                    log.info("Cancelled mail: {}", mailId);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public Mono<Integer> cancelMails(List<Integer> mailIds) {
        return Mono.fromCallable(() -> {
            int cancelled = mailQueueRepository.updateMailStatus(
                    mailIds, MailQueue.MailStatus.CANCELLED, LocalDateTime.now()
            );
            log.info("Cancelled {} mails", cancelled);
            return cancelled;
        });
    }

    @Override
    public Mono<Integer> cleanupSentMails(int daysOld) {
        return Mono.fromCallable(() -> {
            LocalDateTime threshold = LocalDateTime.now().minusDays(daysOld);
            int deleted = mailQueueRepository.deleteOldSentMails(threshold);
            log.info("Cleaned up {} sent mails older than {} days", deleted, daysOld);
            return deleted;
        });
    }

    @Override
    public Mono<Integer> cleanupFailedMails(int daysOld) {
        return Mono.fromCallable(() -> {
            LocalDateTime threshold = LocalDateTime.now().minusDays(daysOld);
            int deleted = mailQueueRepository.deleteOldFailedMails(threshold);
            log.info("Cleaned up {} failed mails older than {} days", deleted, daysOld);
            return deleted;
        });
    }

    /**
     * 清理过期邮件队列
     */
    @Override
    public Mono<Integer> cleanupMailQueue(int days) {
        return Mono.fromCallable(() -> {
            LocalDateTime threshold = LocalDateTime.now().minusDays(days);
            int deleted = mailQueueRepository.deleteByCreatedAtBefore(threshold);
            log.info("Cleaned up {} old mail queue items older than {} days", deleted, days);
            return deleted;
        });
    }

    // ==================== 模板管理 ====================

    @Override
    public Mono<String> renderTemplate(String templateName, Map<String, Object> variables) {
        return getTemplate(templateName)
                .map(templateOpt -> {
                    if (templateOpt.isEmpty()) {
                        throw new ResourceNotFoundException("MailTemplate", templateName);
                    }

                    MailTemplate template = templateOpt.get();
                    Context context = new Context();
                    
                    // 添加默认变量
                    context.setVariable("baseUrl", mailConfig.getTemplateBaseUrl());
                    context.setVariable("siteName", "AFFiNE");
                    
                    // 添加用户变量
                    if (variables != null) {
                        variables.forEach(context::setVariable);
                    }

                    return templateEngine.process("mail/" + templateName, context);
                });
    }

    @Override
    public Mono<String> renderSubject(String templateName, Map<String, Object> variables) {
        return getTemplate(templateName)
                .map(templateOpt -> {
                    if (templateOpt.isEmpty()) {
                        return "AFFiNE通知";
                    }

                    MailTemplate template = templateOpt.get();
                    String subjectTemplate = template.getSubjectTemplate();
                    
                    if (variables != null && !variables.isEmpty()) {
                        // 简单的模板变量替换
                        for (Map.Entry<String, Object> entry : variables.entrySet()) {
                            String placeholder = "${" + entry.getKey() + "}";
                            subjectTemplate = subjectTemplate.replace(placeholder, 
                                                                    String.valueOf(entry.getValue()));
                        }
                    }
                    
                    return subjectTemplate;
                });
    }

    /**
     * 从模板获取邮件主题
     */
    @Override
    public String getSubjectFromTemplate(String templateName) {
        // 简化实现，根据模板名称返回固定主题
        switch (templateName) {
            case "welcome":
                return "欢迎使用AFFiNE";
            case "password_reset":
                return "重置您的AFFiNE密码";
            case "verification":
                return "验证您的AFFiNE账号";
            case "invitation":
                return "您被邀请加入AFFiNE工作空间";
            default:
                return "AFFiNE通知";
        }
    }

    @Override
    public Mono<Optional<MailTemplate>> getTemplate(String templateName) {
        return Mono.fromCallable(() -> 
            mailTemplateRepository.findLatestEnabledVersionByName(templateName)
        );
    }

    @Override
    public Mono<MailTemplate> createTemplate(MailTemplate template) {
        return Mono.fromCallable(() -> {
            // 设置版本号
            Integer maxVersion = mailTemplateRepository.findMaxVersionByName(template.getName())
                    .orElse(0);
            template.setVersion(maxVersion + 1);
            
            MailTemplate saved = mailTemplateRepository.save(template);
            log.info("Created mail template: {} v{}", template.getName(), template.getVersion());
            return saved;
        });
    }

    @Override
    public Mono<MailTemplate> updateTemplate(String templateName, MailTemplate template) {
        return Mono.fromCallable(() -> {
            Optional<MailTemplate> existingOpt = mailTemplateRepository.findLatestVersionByName(templateName);
            if (existingOpt.isEmpty()) {
                throw new RuntimeException("Template not found: " + templateName);
            }

            MailTemplate existing = existingOpt.get();
            existing.setDisplayName(template.getDisplayName());
            existing.setSubjectTemplate(template.getSubjectTemplate());
            existing.setHtmlTemplate(template.getHtmlTemplate());
            existing.setTextTemplate(template.getTextTemplate());
            existing.setDescription(template.getDescription());
            existing.setDefaultVariables(template.getDefaultVariables());

            MailTemplate saved = mailTemplateRepository.save(existing);
            log.info("Updated mail template: {}", templateName);
            return saved;
        });
    }

    @Override
    public Mono<Boolean> deleteTemplate(String templateName) {
        return Mono.fromCallable(() -> {
            List<MailTemplate> templates = mailTemplateRepository.findAllVersionsByName(templateName);
            if (!templates.isEmpty()) {
                mailTemplateRepository.deleteAll(templates);
                log.info("Deleted all versions of template: {}", templateName);
                return true;
            }
            return false;
        });
    }

    @Override
    public Mono<Boolean> enableTemplate(String templateName) {
        return Mono.fromCallable(() -> {
            Optional<MailTemplate> templateOpt = mailTemplateRepository.findLatestVersionByName(templateName);
            if (templateOpt.isPresent()) {
                MailTemplate template = templateOpt.get();
                template.setEnabled(true);
                mailTemplateRepository.save(template);
                log.info("Enabled template: {}", templateName);
                return true;
            }
            return false;
        });
    }

    @Override
    public Mono<Boolean> disableTemplate(String templateName) {
        return Mono.fromCallable(() -> {
            Optional<MailTemplate> templateOpt = mailTemplateRepository.findLatestVersionByName(templateName);
            if (templateOpt.isPresent()) {
                MailTemplate template = templateOpt.get();
                template.setEnabled(false);
                mailTemplateRepository.save(template);
                log.info("Disabled template: {}", templateName);
                return true;
            }
            return false;
        });
    }

    // ==================== 用户相关邮件 ====================

    @Override
    public Mono<Boolean> sendUserSignUpMail(String to, String username, String verificationUrl) {
        Map<String, Object> variables = Map.of(
                "username", username,
                "verificationUrl", verificationUrl
        );
        return sendTemplatedMail(to, username, MailTemplate.UserTemplates.SIGN_UP, variables);
    }

    @Override
    public Mono<Boolean> sendEmailVerificationMail(String to, String username, String verificationUrl) {
        Map<String, Object> variables = Map.of(
                "username", username,
                "verificationUrl", verificationUrl
        );
        return sendTemplatedMail(to, username, MailTemplate.UserTemplates.EMAIL_VERIFY, variables);
    }

    @Override
    public Mono<Boolean> sendPasswordResetMail(String to, String username, String resetUrl) {
        Map<String, Object> variables = Map.of(
                "username", username,
                "resetUrl", resetUrl
        );
        return sendTemplatedMail(to, username, MailTemplate.UserTemplates.PASSWORD_CHANGE, variables);
    }

    @Override
    public Mono<Boolean> sendPasswordChangeMail(String to, String username) {
        Map<String, Object> variables = Map.of("username", username);
        return sendTemplatedMail(to, username, MailTemplate.UserTemplates.PASSWORD_SET, variables);
    }

    @Override
    public Mono<Boolean> sendEmailChangeMail(String to, String oldEmail, String newEmail) {
        Map<String, Object> variables = Map.of(
                "oldEmail", oldEmail,
                "newEmail", newEmail
        );
        return sendTemplatedMail(to, MailTemplate.UserTemplates.EMAIL_CHANGE, variables);
    }

    // ==================== 工作空间相关邮件 ====================

    @Override
    public Mono<Boolean> sendWorkspaceInvitationMail(String to, String inviterName, String workspaceName, 
                                                      String invitationUrl) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("inviterName", inviterName);
        variables.put("workspaceName", workspaceName);
        variables.put("invitationUrl", invitationUrl);
        
        return sendTemplatedMail(to, "workspace_invitation", variables);
    }

    @Override
    public Mono<Void> sendWorkspaceInvitation(String workspaceId, String inviterId, String email, String inviteId) {
        return Mono.fromCallable(() -> {
            String inviterName = "用户";
            String workspaceName = "工作空间";

            try {
                User inviter = userRepository.findById(inviterId).orElse(null);
                if (inviter != null) {
                    inviterName = inviter.getName();
                }
            } catch (Exception e) {
                log.warn("获取邀请者信息失败: inviterId={}", inviterId, e);
            }

            try {
                Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
                if (workspace != null && workspace.getName() != null) {
                    workspaceName = workspace.getName();
                }
            } catch (Exception e) {
                log.warn("获取工作空间信息失败: workspaceId={}", workspaceId, e);
            }

            String baseUrl = mailConfig.getBaseUrl() != null ? mailConfig.getBaseUrl() : "http://localhost:3000";
            String invitationUrl = baseUrl + "/invite/accept/" + inviteId;

            // 发送邀请邮件 (fire-and-forget)
            sendWorkspaceInvitationMail(email, inviterName, workspaceName, invitationUrl)
                .doOnError(e -> log.warn("Failed to send invitation mail to {}", email, e))
                .subscribe();

            return null;
        }).then();
    }

    @Override
    public Mono<Boolean> sendInvitationAcceptedMail(String to, String accepterName, String workspaceName) {
        Map<String, Object> variables = Map.of(
                "accepterName", accepterName,
                "workspaceName", workspaceName
        );
        return sendTemplatedMail(to, MailTemplate.WorkspaceTemplates.INVITATION_ACCEPTED, variables);
    }

    @Override
    public Mono<Boolean> sendMemberLeaveMail(String to, String memberName, String workspaceName) {
        Map<String, Object> variables = Map.of(
                "memberName", memberName,
                "workspaceName", workspaceName
        );
        return sendTemplatedMail(to, MailTemplate.WorkspaceTemplates.MEMBER_LEAVE, variables);
    }

    @Override
    public Mono<Boolean> sendMemberRemovedMail(String to, String memberName, String workspaceName) {
        Map<String, Object> variables = Map.of(
                "memberName", memberName,
                "workspaceName", workspaceName
        );
        return sendTemplatedMail(to, MailTemplate.WorkspaceTemplates.MEMBER_REMOVED, variables);
    }

    @Override
    public Mono<Boolean> sendOwnershipTransferMail(String to, String newOwnerName, String workspaceName) {
        Map<String, Object> variables = Map.of(
                "newOwnerName", newOwnerName,
                "workspaceName", workspaceName
        );
        return sendTemplatedMail(to, MailTemplate.WorkspaceTemplates.OWNERSHIP_TRANSFERRED, variables);
    }

    // ==================== 文档相关邮件 ====================

    @Override
    public Mono<Boolean> sendDocumentMentionMail(String to, String mentionerName, String documentTitle, 
                                                  String documentUrl) {
        Map<String, Object> variables = Map.of(
                "mentionerName", mentionerName,
                "documentTitle", documentTitle,
                "documentUrl", documentUrl
        );
        return sendTemplatedMail(to, MailTemplate.DocumentTemplates.MENTION, variables);
    }

    @Override
    public Mono<Boolean> sendDocumentShareMail(String to, String sharerName, String documentTitle, 
                                               String documentUrl) {
        Map<String, Object> variables = Map.of(
                "sharerName", sharerName,
                "documentTitle", documentTitle,
                "documentUrl", documentUrl
        );
        return sendTemplatedMail(to, MailTemplate.DocumentTemplates.SHARE, variables);
    }

    @Override
    public Mono<Boolean> sendDocumentCommentMail(String to, String commenterName, String documentTitle, 
                                                  String commentContent, String documentUrl) {
        Map<String, Object> variables = Map.of(
                "commenterName", commenterName,
                "documentTitle", documentTitle,
                "commentContent", commentContent,
                "documentUrl", documentUrl
        );
        return sendTemplatedMail(to, MailTemplate.DocumentTemplates.COMMENT, variables);
    }

    // ==================== 系统邮件 ====================

    @Override
    public Mono<Boolean> sendWelcomeMail(String to, String username) {
        Map<String, Object> variables = Map.of("username", username);
        return sendTemplatedMail(to, username, MailTemplate.SystemTemplates.WELCOME, variables);
    }

    @Override
    public Mono<Boolean> sendMaintenanceMail(String to, String maintenanceTime, String estimatedDuration) {
        Map<String, Object> variables = Map.of(
                "maintenanceTime", maintenanceTime,
                "estimatedDuration", estimatedDuration
        );
        return sendTemplatedMail(to, MailTemplate.SystemTemplates.MAINTENANCE, variables);
    }

    @Override
    public Mono<Boolean> sendSecurityAlertMail(String to, String alertType, String alertDetails) {
        Map<String, Object> variables = Map.of(
                "alertType", alertType,
                "alertDetails", alertDetails
        );
        return sendTemplatedMail(to, MailTemplate.SystemTemplates.SECURITY_ALERT, variables);
    }

    @Override
    public Mono<Boolean> sendAccountLockedMail(String to, String lockReason, String unlockUrl) {
        Map<String, Object> variables = Map.of(
                "lockReason", lockReason,
                "unlockUrl", unlockUrl
        );
        return sendTemplatedMail(to, MailTemplate.SystemTemplates.ACCOUNT_LOCKED, variables);
    }

    // ==================== 统计和监控 ====================

    @Override
    public Mono<Map<String, Object>> getMailStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取状态统计
            List<Object[]> statusCounts = mailQueueRepository.countByStatus();
            Map<String, Long> statusMap = new HashMap<>();
            for (Object[] row : statusCounts) {
                statusMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
            
            // 获取今日统计
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime todayEnd = todayStart.plusDays(1);
            
            stats.put("statusCounts", statusMap);
            stats.put("todayTotal", mailQueueRepository.countByCreatedAtBetween(todayStart, todayEnd));
            stats.put("todaySent", mailQueueRepository.countSentMailsBetween(todayStart, todayEnd));
            stats.put("todayFailed", mailQueueRepository.countFailedMailsBetween(todayStart, todayEnd));
            
            return stats;
        });
    }

    @Override
    public Mono<Map<String, Object>> getQueueStatus() {
        return Mono.fromCallable(() -> {
            Map<String, Object> status = new HashMap<>();
            
            status.put("pendingCount", mailQueueRepository.countPendingMailsByPriority(MailQueue.MailPriority.NORMAL));
            status.put("highPriorityCount", mailQueueRepository.countPendingMailsByPriority(MailQueue.MailPriority.HIGH));
            status.put("urgentCount", mailQueueRepository.countPendingMailsByPriority(MailQueue.MailPriority.URGENT));
            status.put("configValid", isValidConfiguration());
            status.put("serviceAvailable", isMailServiceAvailable());
            
            return status;
        });
    }

    @Override
    public Mono<List<MailQueue>> getMailHistory(String recipientEmail, int limit) {
        return Mono.fromCallable(() -> 
            mailQueueRepository.findMailHistoryByRecipient(
                recipientEmail, PageRequest.of(0, limit)
            ).getContent()
        );
    }

    @Override
    public Mono<List<MailQueue>> getFailedMails(int limit) {
        return Mono.fromCallable(() -> 
            mailQueueRepository.findRecentFailedMails(PageRequest.of(0, limit)).getContent()
        );
    }

    @Override
    public Mono<Boolean> testMailConfiguration() {
        return Mono.fromCallable(() -> {
            try {
                if (!isValidConfiguration()) {
                    return false;
                }
                
                // 测试连接
                JavaMailSenderImpl sender = (JavaMailSenderImpl) getMailSender();
                sender.getSession().getTransport().connect(
                    sender.getHost(),
                    sender.getPort(),
                    sender.getUsername(),
                    sender.getPassword()
                );
                return true;
            } catch (Exception e) {
                log.error("Mail configuration test failed: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * 发送测试邮件
     */
    @Override
    public Mono<Boolean> sendTestMail(String to) {
        String subject = "AFFiNE邮件服务测试";
            
        return Mono.fromCallable(() -> {
            String content = "<h2>邮件测试成功</h2><p>如果您收到此邮件，说明邮件配置正常工作。</p>";
            
            return sendMail(to, subject, content)
                .block(); // 阻塞获取结果
        });
    }

    // ==================== 批量操作 ====================

    @Override
    public Mono<Integer> batchSendMails(List<String> recipients, String subject, String htmlContent) {
        return Mono.fromCallable(() -> {
            int sent = 0;
            for (String recipient : recipients) {
                try {
                    if (sendMail(recipient, subject, htmlContent).block()) {
                        sent++;
                    }
                } catch (Exception e) {
                    log.error("Failed to send mail to {}: {}", recipient, e.getMessage());
                }
            }
            return sent;
        });
    }

    @Override
    public Mono<Integer> batchSendTemplatedMails(List<String> recipients, String templateName, 
                                                  Map<String, Object> variables) {
        return Mono.fromCallable(() -> {
            int sent = 0;
            for (String recipient : recipients) {
                try {
                    if (sendTemplatedMail(recipient, templateName, variables).block()) {
                        sent++;
                    }
                } catch (Exception e) {
                    log.error("Failed to send templated mail to {}: {}", recipient, e.getMessage());
                }
            }
            return sent;
        });
    }

    @Override
    public Mono<Integer> batchQueueMails(List<String> recipients, String subject, String htmlContent) {
        return Mono.fromCallable(() -> {
            int queued = 0;
            for (String recipient : recipients) {
                try {
                    queueMail(recipient, subject, htmlContent).block();
                    queued++;
                } catch (Exception e) {
                    log.error("Failed to queue mail for {}: {}", recipient, e.getMessage());
                }
            }
            return queued;
        });
    }

    @Override
    public Mono<Integer> batchQueueTemplatedMails(List<String> recipients, String templateName, 
                                                   Map<String, Object> variables) {
        return Mono.fromCallable(() -> {
            int queued = 0;
            for (String recipient : recipients) {
                try {
                    queueTemplatedMail(recipient, templateName, variables).block();
                    queued++;
                } catch (Exception e) {
                    log.error("Failed to queue templated mail for {}: {}", recipient, e.getMessage());
                }
            }
            return queued;
        });
    }

    // ==================== 配置管理 ====================

    @Override
    public boolean isMailServiceAvailable() {
        return mailConfig.getEnabled() && isValidConfiguration();
    }

    @Override
    public Mono<Boolean> reloadConfiguration() {
        return Mono.fromCallable(() -> {
            // 重新初始化邮件发送器
            this.javaMailSender = null;
            return isValidConfiguration();
        });
    }

    @Override
    public Mono<Map<String, Object>> getConfigurationStatus() {
        return Mono.fromCallable(() -> {
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", mailConfig.getEnabled());
            status.put("host", mailConfig.getHost());
            status.put("port", mailConfig.getPort());
            status.put("sender", mailConfig.getSender());
            status.put("configValid", isValidConfiguration());
            status.put("testMode", mailConfig.getTestMode());
            return status;
        });
    }

    // ==================== 辅助方法 ====================

    private JavaMailSender getMailSender() {
        if (javaMailSender == null) {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(mailConfig.getHost());
            mailSender.setPort(mailConfig.getPort());
            mailSender.setUsername(mailConfig.getUsername());
            mailSender.setPassword(mailConfig.getPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", mailConfig.getProtocol());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", mailConfig.getEnableStartTls());
            props.put("mail.smtp.ssl.enable", mailConfig.getEnableSsl());
            props.put("mail.smtp.ssl.trust", mailConfig.getHost());
            props.put("mail.smtp.connectiontimeout", mailConfig.getConnectionTimeout());
            props.put("mail.smtp.timeout", mailConfig.getReadTimeout());
            
            if (mailConfig.getIgnoreTls()) {
                props.put("mail.smtp.ssl.checkserveridentity", "false");
                props.put("mail.smtp.ssl.trust", "*");
            }

            this.javaMailSender = mailSender;
        }
        return javaMailSender;
    }

    private boolean isValidConfiguration() {
        return mailConfig.isConfigValid();
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private String getStackTrace(Exception e) {
        return Arrays.toString(e.getStackTrace());
    }
}