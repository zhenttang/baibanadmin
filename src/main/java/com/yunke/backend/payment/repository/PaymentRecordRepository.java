package com.yunke.backend.payment.repository;

import com.yunke.backend.payment.domain.entity.PaymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 付费记录Repository接口
 */
@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    /**
     * 根据用户ID查找付费记录
     */
    List<PaymentRecord> findByUserId(String userId);

    /**
     * 根据文档ID查找付费记录
     */
    List<PaymentRecord> findByDocumentId(String documentId);

    /**
     * 根据用户ID和文档ID查找付费记录
     */
    PaymentRecord findByUserIdAndDocumentId(String userId, String documentId);

    /**
     * 检查用户是否已购买文档
     */
    @Query("SELECT COUNT(pr) > 0 FROM PaymentRecord pr WHERE pr.userId = :userId AND pr.documentId = :documentId AND pr.status = 'SUCCESS'")
    boolean hasUserPurchasedDocument(@Param("userId") String userId, @Param("documentId") String documentId);

    /**
     * 根据交易ID查找付费记录
     */
    PaymentRecord findByTransactionId(String transactionId);

    /**
     * 根据支付状态查找记录
     */
    List<PaymentRecord> findByStatus(PaymentRecord.PaymentStatus status);

    /**
     * 根据支付方式查找记录
     */
    List<PaymentRecord> findByPaymentMethod(String paymentMethod);

    /**
     * 根据时间范围查找记录
     */
    List<PaymentRecord> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID和支付状态查找记录
     */
    Page<PaymentRecord> findByUserIdAndStatus(String userId, PaymentRecord.PaymentStatus status, Pageable pageable);

    /**
     * 统计用户的成功支付次数
     */
    @Query("SELECT COUNT(pr) FROM PaymentRecord pr WHERE pr.userId = :userId AND pr.status = 'SUCCESS'")
    Long countSuccessfulPaymentsByUserId(@Param("userId") String userId);

    /**
     * 统计文档的成功购买次数
     */
    @Query("SELECT COUNT(pr) FROM PaymentRecord pr WHERE pr.documentId = :documentId AND pr.status = 'SUCCESS'")
    Long countSuccessfulPurchasesByDocumentId(@Param("documentId") String documentId);

    /**
     * 计算用户的总支付金额
     */
    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PaymentRecord pr WHERE pr.userId = :userId AND pr.status = 'SUCCESS'")
    BigDecimal sumAmountByUserId(@Param("userId") String userId);

    /**
     * 计算文档的总收入
     */
    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PaymentRecord pr WHERE pr.documentId = :documentId AND pr.status = 'SUCCESS'")
    BigDecimal sumAmountByDocumentId(@Param("documentId") String documentId);
    
    /**
     * 计算作者的总收入
     */
    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PaymentRecord pr JOIN CommunityDocument cd ON pr.documentId = cd.id WHERE cd.authorId = :authorId AND pr.status = 'SUCCESS'")
    BigDecimal sumAmountByAuthorId(@Param("authorId") String authorId);

    /**
     * 计算指定时间范围内的总收入
     */
    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PaymentRecord pr WHERE pr.createdAt BETWEEN :startTime AND :endTime AND pr.status = 'SUCCESS'")
    BigDecimal sumAmountByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找用户购买的所有文档ID
     */
    @Query("SELECT pr.documentId FROM PaymentRecord pr WHERE pr.userId = :userId AND pr.status = 'SUCCESS'")
    List<String> findPurchasedDocumentIdsByUserId(@Param("userId") String userId);

    /**
     * 查找购买指定文档的所有用户ID
     */
    @Query("SELECT pr.userId FROM PaymentRecord pr WHERE pr.documentId = :documentId AND pr.status = 'SUCCESS'")
    List<String> findPurchaserIdsByDocumentId(@Param("documentId") String documentId);

    /**
     * 查找待处理的支付记录
     */
    @Query("SELECT pr FROM PaymentRecord pr WHERE pr.status = 'PENDING' AND pr.createdAt < :timeout")
    List<PaymentRecord> findPendingPaymentsBeforeTimeout(@Param("timeout") LocalDateTime timeout);
}