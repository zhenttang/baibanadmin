package com.yunke.backend.notification.repository;

import com.yunke.backend.notification.domain.entity.MailQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件队列Repository
 * 对应Node.js版本的邮件队列管理
 * 参考: /packages/backend/server/src/core/mail/job.ts
 */
@Repository
public interface MailQueueRepository extends JpaRepository<MailQueue, Integer> {

    // ==================== 基础查询 ====================

    /**
     * 根据状态查询邮件
     */
    List<MailQueue> findByStatus(MailQueue.MailStatus status);

    /**
     * 根据收件人邮箱查询邮件
     */
    List<MailQueue> findByRecipientEmail(String recipientEmail);

    /**
     * 根据邮件类型查询邮件
     */
    List<MailQueue> findByMailType(String mailType);

    /**
     * 根据状态和优先级查询邮件
     */
    List<MailQueue> findByStatusAndPriorityOrderByCreatedAtAsc(
            MailQueue.MailStatus status, 
            MailQueue.MailPriority priority
    );

    // ==================== 队列处理相关 ====================

    /**
     * 查询等待处理的邮件（按优先级和创建时间排序）
     */
    @Query("SELECT m FROM MailQueue m WHERE m.status = 'PENDING' " +
           "ORDER BY " +
           "CASE m.priority " +
           "WHEN 'URGENT' THEN 4 " +
           "WHEN 'HIGH' THEN 3 " +
           "WHEN 'NORMAL' THEN 2 " +
           "WHEN 'LOW' THEN 1 " +
           "END DESC, m.createdAt ASC")
    List<MailQueue> findPendingMailsOrderByPriority(Pageable pageable);

    /**
     * 查询需要重试的邮件
     */
    @Query("SELECT m FROM MailQueue m WHERE m.status = 'FAILED' " +
           "AND m.retryCount < m.maxRetries " +
           "AND (m.nextRetryAt IS NULL OR m.nextRetryAt <= :now) " +
           "ORDER BY m.priority DESC, m.nextRetryAt ASC")
    List<MailQueue> findMailsToRetry(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 查询处理中但超时的邮件
     */
    @Query("SELECT m FROM MailQueue m WHERE m.status = 'PROCESSING' " +
           "AND m.updatedAt < :timeoutThreshold")
    List<MailQueue> findTimeoutProcessingMails(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 查询可以处理的邮件（等待中或需要重试的）
     */
    @Query("SELECT m FROM MailQueue m WHERE " +
           "(m.status = 'PENDING') OR " +
           "(m.status = 'FAILED' AND m.retryCount < m.maxRetries AND " +
           "(m.nextRetryAt IS NULL OR m.nextRetryAt <= :now)) " +
           "ORDER BY " +
           "CASE m.priority " +
           "WHEN 'URGENT' THEN 4 " +
           "WHEN 'HIGH' THEN 3 " +
           "WHEN 'NORMAL' THEN 2 " +
           "WHEN 'LOW' THEN 1 " +
           "END DESC, m.createdAt ASC")
    List<MailQueue> findProcessableMails(@Param("now") LocalDateTime now, Pageable pageable);

    // ==================== 状态更新 ====================

    /**
     * 批量更新邮件状态
     */
    @Modifying
    @Query("UPDATE MailQueue m SET m.status = :newStatus, m.updatedAt = :now " +
           "WHERE m.id IN :ids")
    int updateMailStatus(@Param("ids") List<Integer> ids, 
                        @Param("newStatus") MailQueue.MailStatus newStatus,
                        @Param("now") LocalDateTime now);

    /**
     * 重置超时的处理中邮件状态
     */
    @Modifying
    @Query("UPDATE MailQueue m SET m.status = 'PENDING', m.updatedAt = :now " +
           "WHERE m.status = 'PROCESSING' AND m.updatedAt < :timeoutThreshold")
    int resetTimeoutProcessingMails(@Param("timeoutThreshold") LocalDateTime timeoutThreshold,
                                   @Param("now") LocalDateTime now);

    /**
     * 标记邮件为已发送
     */
    @Modifying
    @Query("UPDATE MailQueue m SET m.status = 'SENT', m.sentAt = :sentAt, " +
           "m.errorMessage = NULL, m.errorStack = NULL, m.updatedAt = :now " +
           "WHERE m.id = :id")
    int markAsSent(@Param("id") Integer id, 
                   @Param("sentAt") LocalDateTime sentAt,
                   @Param("now") LocalDateTime now);

    /**
     * 标记邮件为失败
     */
    @Modifying
    @Query("UPDATE MailQueue m SET m.status = 'FAILED', " +
           "m.errorMessage = :errorMessage, m.errorStack = :errorStack, " +
           "m.retryCount = m.retryCount + 1, m.nextRetryAt = :nextRetryAt, " +
           "m.updatedAt = :now WHERE m.id = :id")
    int markAsFailed(@Param("id") Integer id,
                    @Param("errorMessage") String errorMessage,
                    @Param("errorStack") String errorStack,
                    @Param("nextRetryAt") LocalDateTime nextRetryAt,
                    @Param("now") LocalDateTime now);

    // ==================== 统计查询 ====================

    /**
     * 统计各状态邮件数量
     */
    @Query("SELECT m.status, COUNT(m) FROM MailQueue m GROUP BY m.status")
    List<Object[]> countByStatus();

    /**
     * 统计指定时间段内的邮件数量
     */
    @Query("SELECT COUNT(m) FROM MailQueue m WHERE m.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start, 
                                @Param("end") LocalDateTime end);

    /**
     * 统计指定时间段内已发送的邮件数量
     */
    @Query("SELECT COUNT(m) FROM MailQueue m WHERE m.status = 'SENT' " +
           "AND m.sentAt BETWEEN :start AND :end")
    Long countSentMailsBetween(@Param("start") LocalDateTime start, 
                              @Param("end") LocalDateTime end);

    /**
     * 统计指定时间段内失败的邮件数量
     */
    @Query("SELECT COUNT(m) FROM MailQueue m WHERE m.status = 'FAILED' " +
           "AND m.updatedAt BETWEEN :start AND :end")
    Long countFailedMailsBetween(@Param("start") LocalDateTime start, 
                                @Param("end") LocalDateTime end);

    /**
     * 统计邮件类型分布
     */
    @Query("SELECT m.mailType, COUNT(m) FROM MailQueue m GROUP BY m.mailType")
    List<Object[]> countByMailType();

    /**
     * 统计优先级分布
     */
    @Query("SELECT m.priority, COUNT(m) FROM MailQueue m GROUP BY m.priority")
    List<Object[]> countByPriority();

    // ==================== 清理相关 ====================

    /**
     * 删除指定时间之前已发送的邮件
     */
    @Modifying
    @Query("DELETE FROM MailQueue m WHERE m.status = 'SENT' AND m.sentAt < :threshold")
    int deleteOldSentMails(@Param("threshold") LocalDateTime threshold);

    /**
     * 删除指定时间之前失败且超过最大重试次数的邮件
     */
    @Modifying
    @Query("DELETE FROM MailQueue m WHERE m.status = 'FAILED' " +
           "AND m.retryCount >= m.maxRetries AND m.updatedAt < :threshold")
    int deleteOldFailedMails(@Param("threshold") LocalDateTime threshold);

    /**
     * 取消指定时间之前未处理的邮件
     */
    @Modifying
    @Query("UPDATE MailQueue m SET m.status = 'CANCELLED', m.updatedAt = :now " +
           "WHERE m.status = 'PENDING' AND m.createdAt < :threshold")
    int cancelOldPendingMails(@Param("threshold") LocalDateTime threshold,
                             @Param("now") LocalDateTime now);

    /**
     * 删除指定时间之前创建的所有邮件
     */
    @Modifying
    @Query("DELETE FROM MailQueue m WHERE m.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);

    // ==================== 复杂查询 ====================

    /**
     * 查询指定收件人的邮件历史
     */
    @Query("SELECT m FROM MailQueue m WHERE m.recipientEmail = :email " +
           "ORDER BY m.createdAt DESC")
    Page<MailQueue> findMailHistoryByRecipient(@Param("email") String email, Pageable pageable);

    /**
     * 查询重试次数最多的邮件
     */
    @Query("SELECT m FROM MailQueue m WHERE m.retryCount > 0 " +
           "ORDER BY m.retryCount DESC, m.updatedAt DESC")
    Page<MailQueue> findMostRetriedMails(Pageable pageable);

    /**
     * 查询最近失败的邮件
     */
    @Query("SELECT m FROM MailQueue m WHERE m.status = 'FAILED' " +
           "ORDER BY m.updatedAt DESC")
    Page<MailQueue> findRecentFailedMails(Pageable pageable);

    /**
     * 根据邮件类型和时间范围查询邮件
     */
    @Query("SELECT m FROM MailQueue m WHERE m.mailType = :mailType " +
           "AND m.createdAt BETWEEN :start AND :end " +
           "ORDER BY m.createdAt DESC")
    Page<MailQueue> findByMailTypeAndDateRange(@Param("mailType") String mailType,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              Pageable pageable);

    /**
     * 查询特定优先级的等待邮件数量
     */
    @Query("SELECT COUNT(m) FROM MailQueue m WHERE m.status = 'PENDING' " +
           "AND m.priority = :priority")
    Long countPendingMailsByPriority(@Param("priority") MailQueue.MailPriority priority);

    /**
     * 查询队列中最早的未处理邮件
     */
    @Query("SELECT m FROM MailQueue m WHERE m.status IN ('PENDING', 'FAILED') " +
           "AND (m.nextRetryAt IS NULL OR m.nextRetryAt <= :now) " +
           "ORDER BY m.createdAt ASC")
    Optional<MailQueue> findOldestPendingMail(@Param("now") LocalDateTime now);
}