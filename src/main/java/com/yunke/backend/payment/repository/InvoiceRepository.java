package com.yunke.backend.payment.repository;

import com.yunke.backend.payment.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 发票Repository接口
 * 对应Node.js版本的InvoiceRepository
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    /**
     * 根据目标ID查找发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.targetId = :targetId ORDER BY i.createdAt DESC")
    List<Invoice> findByTargetId(@Param("targetId") String targetId);

    /**
     * 根据订阅ID查找发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.subscriptionId = :subscriptionId ORDER BY i.createdAt DESC")
    List<Invoice> findBySubscriptionId(@Param("subscriptionId") Integer subscriptionId);

    /**
     * 查找未支付的发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.targetId = :targetId AND i.status = 'OPEN' ORDER BY i.createdAt DESC")
    List<Invoice> findUnpaidInvoices(@Param("targetId") String targetId);

    /**
     * 查找已支付的发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.targetId = :targetId AND i.status = 'PAID' ORDER BY i.createdAt DESC")
    List<Invoice> findPaidInvoices(@Param("targetId") String targetId);

    /**
     * 查找逾期发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'OPEN' AND i.dueDate < CURRENT_TIMESTAMP")
    List<Invoice> findOverdueInvoices();

    /**
     * 查找即将到期的发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'OPEN' AND i.dueDate BETWEEN :start AND :end")
    List<Invoice> findInvoicesDueSoon(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计用户的发票总金额
     */
    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.targetId = :targetId AND i.status = 'PAID'")
    BigDecimal getTotalPaidAmount(@Param("targetId") String targetId);

    /**
     * 统计用户的未支付金额
     */
    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.targetId = :targetId AND i.status = 'OPEN'")
    BigDecimal getTotalUnpaidAmount(@Param("targetId") String targetId);

    /**
     * 查找指定时间范围内的发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.targetId = :targetId AND i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
    List<Invoice> findInvoicesByDateRange(
            @Param("targetId") String targetId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计按状态分组的发票数量
     */
    @Query("SELECT i.status, COUNT(i) FROM Invoice i GROUP BY i.status")
    List<Object[]> countInvoicesByStatus();

    /**
     * 查找最近的发票（列表）
     */
    @Query("SELECT i FROM Invoice i WHERE i.targetId = :targetId ORDER BY i.createdAt DESC")
    List<Invoice> findLatestInvoiceList(@Param("targetId") String targetId);
    
    /**
     * 查找最近的发票
     */
    default Optional<Invoice> findLatestInvoice(String targetId) {
        List<Invoice> invoices = findLatestInvoiceList(targetId);
        return invoices.isEmpty() ? Optional.empty() : Optional.of(invoices.get(0));
    }

    /**
     * 查找周期性发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.subscriptionId IS NOT NULL AND i.periodStart IS NOT NULL AND i.periodEnd IS NOT NULL ORDER BY i.createdAt DESC")
    List<Invoice> findRecurringInvoices();

    /**
     * 查找一次性发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.subscriptionId IS NULL ORDER BY i.createdAt DESC")
    List<Invoice> findOneTimeInvoices();

    /**
     * 统计每月发票金额
     */
    @Query("SELECT YEAR(i.createdAt), MONTH(i.createdAt), SUM(i.amount) FROM Invoice i WHERE i.status = 'PAID' AND i.createdAt BETWEEN :startDate AND :endDate GROUP BY YEAR(i.createdAt), MONTH(i.createdAt)")
    List<Object[]> getMonthlyInvoiceAmount(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 查找重复发票（相同金额和目标ID）
     */
    @Query("SELECT i FROM Invoice i WHERE i.targetId = :targetId AND i.amount = :amount AND i.createdAt BETWEEN :start AND :end")
    List<Invoice> findDuplicateInvoices(
            @Param("targetId") String targetId, 
            @Param("amount") BigDecimal amount,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * 查找有支付错误的发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.lastPaymentError IS NOT NULL AND i.status = 'OPEN'")
    List<Invoice> findInvoicesWithPaymentErrors();

    /**
     * 查找大额发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.amount >= :minAmount ORDER BY i.amount DESC")
    List<Invoice> findLargeInvoices(@Param("minAmount") BigDecimal minAmount);

    /**
     * 统计发票支付成功率
     */
    @Query("SELECT " +
           "SUM(CASE WHEN i.status = 'PAID' THEN 1 ELSE 0 END) as paid, " +
           "COUNT(i) as total " +
           "FROM Invoice i WHERE i.createdAt BETWEEN :startDate AND :endDate")
    Object[] getInvoicePaymentRate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 查找需要重试支付的发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'OPEN' AND i.lastPaymentError IS NOT NULL AND i.updatedAt < :retryAfter")
    List<Invoice> findInvoicesForRetry(@Param("retryAfter") LocalDateTime retryAfter);

    /**
     * 根据货币统计发票
     */
    @Query("SELECT i.currency, COUNT(i), SUM(i.amount) FROM Invoice i WHERE i.status = 'PAID' GROUP BY i.currency")
    List<Object[]> getInvoiceStatsByCurrency();

    /**
     * 根据用户ID查找发票
     */
    @Query("SELECT i FROM Invoice i WHERE i.userId = :userId ORDER BY i.createdAt DESC")
    List<Invoice> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
}