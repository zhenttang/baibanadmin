package com.yunke.backend.notification.service;

import com.yunke.backend.notification.domain.entity.MailQueue;
import com.yunke.backend.notification.domain.entity.MailTemplate;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 邮件服务接口
 * 对应Node.js版本的邮件服务
 * 参考: /packages/backend/server/src/core/mail/mailer.ts
 */
public interface MailService {

    /**
     * 发送测试邮件
     */
    Mono<Boolean> sendTestMail(String to);

    /**
     * 发送模板邮件
     */
    Mono<Boolean> sendTemplateEmail(String to, String name, String templateName, 
                                  Map<String, Object> variables, 
                                  MailQueue.MailPriority priority);

    /**
     * 发送带名称的邮件
     */
    Mono<Boolean> sendMailWithName(String to, String name, String subject, String content);

    /**
     * 发送普通邮件
     */
    Mono<Boolean> sendMail(String to, String subject, String content);

    /**
     * 发送带名称的邮件（重载）
     */
    Mono<Boolean> sendMail(String to, String toName, String subject, String htmlContent);

    /**
     * 尝试发送邮件（不抛出异常）
     */
    Mono<Boolean> trySendMail(String to, String subject, String htmlContent);

    /**
     * 发送模板邮件（简化版）
     */
    Mono<Boolean> sendTemplatedMail(String to, String templateName, Map<String, Object> variables);

    /**
     * 发送模板邮件（带名称）
     */
    Mono<Boolean> sendTemplatedMail(String to, String toName, String templateName, Map<String, Object> variables);

    /**
     * 将邮件加入队列
     */
    Mono<MailQueue> queueMail(String to, String subject, String htmlContent);

    /**
     * 将邮件加入队列（带优先级）
     */
    Mono<MailQueue> queueMail(String to, String subject, String htmlContent, MailQueue.MailPriority priority);

    /**
     * 将模板邮件加入队列
     */
    Mono<MailQueue> queueTemplatedMail(String to, String templateName, Map<String, Object> variables);

    /**
     * 将模板邮件加入队列（带优先级）
     */
    Mono<MailQueue> queueTemplatedMail(String to, String templateName, Map<String, Object> variables, MailQueue.MailPriority priority);

    /**
     * 将模板邮件加入队列（带名称和优先级）
     */
    Mono<MailQueue> queueTemplatedMail(String to, String toName, String templateName, Map<String, Object> variables, MailQueue.MailPriority priority);

    /**
     * 处理邮件队列（无参数）
     */
    Mono<Integer> processMailQueue();

    /**
     * 处理邮件队列
     */
    Mono<Integer> processMailQueue(int batchSize);

    /**
     * 清理过期邮件队列
     */
    Mono<Integer> cleanupMailQueue(int days);

    /**
     * 获取模板
     */
    Mono<Optional<MailTemplate>> getTemplate(String templateName);

    /**
     * 渲染模板
     */
    Mono<String> renderTemplate(String templateName, Map<String, Object> variables);

    /**
     * 渲染邮件主题
     */
    Mono<String> renderSubject(String templateName, Map<String, Object> variables);

    /**
     * 从模板获取主题
     */
    String getSubjectFromTemplate(String templateName);
    
    // 新增方法
    
    /**
     * 重试失败的邮件
     */
    Mono<Integer> retryFailedMails();
    
    /**
     * 取消邮件
     */
    Mono<Boolean> cancelMail(Integer mailId);
    
    /**
     * 批量取消邮件
     */
    Mono<Integer> cancelMails(List<Integer> mailIds);
    
    /**
     * 清理已发送邮件
     */
    Mono<Integer> cleanupSentMails(int daysOld);
    
    /**
     * 清理发送失败的邮件
     */
    Mono<Integer> cleanupFailedMails(int daysOld);
    
    /**
     * 创建邮件模板
     */
    Mono<MailTemplate> createTemplate(MailTemplate template);
    
    /**
     * 更新邮件模板
     */
    Mono<MailTemplate> updateTemplate(String templateName, MailTemplate template);
    
    /**
     * 删除邮件模板
     */
    Mono<Boolean> deleteTemplate(String templateName);
    
    /**
     * 启用邮件模板
     */
    Mono<Boolean> enableTemplate(String templateName);
    
    /**
     * 禁用邮件模板
     */
    Mono<Boolean> disableTemplate(String templateName);
    
    /**
     * 发送用户注册邮件
     */
    Mono<Boolean> sendUserSignUpMail(String to, String username, String verificationUrl);
    
    /**
     * 发送邮箱验证邮件
     */
    Mono<Boolean> sendEmailVerificationMail(String to, String username, String verificationUrl);
    
    /**
     * 发送密码重置邮件
     */
    Mono<Boolean> sendPasswordResetMail(String to, String username, String resetUrl);
    
    /**
     * 发送密码变更邮件
     */
    Mono<Boolean> sendPasswordChangeMail(String to, String username);
    
    /**
     * 发送邮箱变更邮件
     */
    Mono<Boolean> sendEmailChangeMail(String to, String oldEmail, String newEmail);
    
    /**
     * 发送工作区邀请邮件
     */
    Mono<Boolean> sendWorkspaceInvitationMail(String to, String inviterName, String workspaceName, String invitationUrl);

    /**
     * 发送工作空间邀请
     */
    Mono<Void> sendWorkspaceInvitation(String workspaceId, String inviterId, String email, String inviteId);

    /**
     * 发送邀请接受邮件
     */
    Mono<Boolean> sendInvitationAcceptedMail(String to, String accepterName, String workspaceName);
    
    /**
     * 发送成员离开邮件
     */
    Mono<Boolean> sendMemberLeaveMail(String to, String memberName, String workspaceName);
    
    /**
     * 发送成员移除邮件
     */
    Mono<Boolean> sendMemberRemovedMail(String to, String memberName, String workspaceName);
    
    /**
     * 发送所有权转移邮件
     */
    Mono<Boolean> sendOwnershipTransferMail(String to, String newOwnerName, String workspaceName);
    
    /**
     * 发送文档提及邮件
     */
    Mono<Boolean> sendDocumentMentionMail(String to, String mentionerName, String documentTitle, String documentUrl);
    
    /**
     * 发送文档共享邮件
     */
    Mono<Boolean> sendDocumentShareMail(String to, String sharerName, String documentTitle, String documentUrl);
    
    /**
     * 发送文档评论邮件
     */
    Mono<Boolean> sendDocumentCommentMail(String to, String commenterName, String documentTitle, String commentContent, String documentUrl);
    
    /**
     * 发送欢迎邮件
     */
    Mono<Boolean> sendWelcomeMail(String to, String username);
    
    /**
     * 发送维护通知邮件
     */
    Mono<Boolean> sendMaintenanceMail(String to, String maintenanceTime, String estimatedDuration);
    
    /**
     * 发送安全警报邮件
     */
    Mono<Boolean> sendSecurityAlertMail(String to, String alertType, String alertDetails);
    
    /**
     * 发送账户锁定邮件
     */
    Mono<Boolean> sendAccountLockedMail(String to, String lockReason, String unlockUrl);
    
    /**
     * 获取邮件统计信息
     */
    Mono<Map<String, Object>> getMailStatistics();
    
    /**
     * 获取队列状态
     */
    Mono<Map<String, Object>> getQueueStatus();
    
    /**
     * 获取邮件历史
     */
    Mono<List<MailQueue>> getMailHistory(String recipientEmail, int limit);
    
    /**
     * 获取失败的邮件
     */
    Mono<List<MailQueue>> getFailedMails(int limit);
    
    /**
     * 测试邮件配置
     */
    Mono<Boolean> testMailConfiguration();
    
    /**
     * 批量发送邮件
     */
    Mono<Integer> batchSendMails(List<String> recipients, String subject, String htmlContent);
    
    /**
     * 批量发送模板邮件
     */
    Mono<Integer> batchSendTemplatedMails(List<String> recipients, String templateName, Map<String, Object> variables);
    
    /**
     * 批量将邮件加入队列
     */
    Mono<Integer> batchQueueMails(List<String> recipients, String subject, String htmlContent);
    
    /**
     * 批量将模板邮件加入队列
     */
    Mono<Integer> batchQueueTemplatedMails(List<String> recipients, String templateName, Map<String, Object> variables);
    
    /**
     * 邮件服务是否可用
     */
    boolean isMailServiceAvailable();
    
    /**
     * 重新加载配置
     */
    Mono<Boolean> reloadConfiguration();
    
    /**
     * 获取配置状态
     */
    Mono<Map<String, Object>> getConfigurationStatus();
}