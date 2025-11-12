package com.yunke.backend.payment.repository;

import com.yunke.backend.payment.dto.RevenueStatsDto;
import com.yunke.backend.payment.dto.SubscriptionStatsDto;
import com.yunke.backend.payment.domain.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订阅Repository接口
 * 对应Node.js版本的SubscriptionRepository
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * 根据目标ID查找订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.targetId = :targetId ORDER BY s.createdAt DESC")
    List<Subscription> findByTargetId(@Param("targetId") String targetId);

    /**
     * 根据目标ID查找活跃订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.targetId = :targetId AND s.status IN ('ACTIVE', 'TRIALING') ORDER BY s.createdAt DESC")
    List<Subscription> findActiveByTargetId(@Param("targetId") String targetId);

    /**
     * 根据目标ID和计划查找订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.targetId = :targetId AND s.plan = :plan ORDER BY s.createdAt DESC")
    List<Subscription> findByTargetIdAndPlanList(@Param("targetId") String targetId, @Param("plan") Subscription.SubscriptionPlan plan);
    
    /**
     * 根据目标ID和计划查找最新订阅
     */
    default Optional<Subscription> findByTargetIdAndPlan(String targetId, Subscription.SubscriptionPlan plan) {
        List<Subscription> subscriptions = findByTargetIdAndPlanList(targetId, plan);
        return subscriptions.isEmpty() ? Optional.empty() : Optional.of(subscriptions.get(0));
    }

    /**
     * 根据Stripe订阅ID查找
     */
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * 查找即将到期的订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.nextBillAt BETWEEN :start AND :end")
    List<Subscription> findExpiringSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查找试用期即将结束的订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIALING' AND s.trialEnd BETWEEN :start AND :end")
    List<Subscription> findTrialEndingSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查找过期未支付的订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.nextBillAt < :cutoffDate")
    List<Subscription> findOverdueSubscriptions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 统计按计划分组的订阅数量
     */
    @Query("SELECT s.plan, COUNT(s) FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIALING') GROUP BY s.plan")
    List<Object[]> countActiveSubscriptionsByPlan();

    /**
     * 统计按状态分组的订阅数量
     */
    @Query("SELECT s.status, COUNT(s) FROM Subscription s GROUP BY s.status")
    List<Object[]> countSubscriptionsByStatus();

    /**
     * 查找用户的当前有效订阅（列表）
     */
    @Query("SELECT s FROM Subscription s WHERE s.targetId = :targetId AND s.status IN ('ACTIVE', 'TRIALING') AND (s.end IS NULL OR s.end > CURRENT_TIMESTAMP) ORDER BY s.createdAt DESC")
    List<Subscription> findCurrentActiveSubscriptionList(@Param("targetId") String targetId);
    
    /**
     * 查找用户的当前有效订阅
     */
    default Optional<Subscription> findCurrentActiveSubscription(String targetId) {
        List<Subscription> subscriptions = findCurrentActiveSubscriptionList(targetId);
        return subscriptions.isEmpty() ? Optional.empty() : Optional.of(subscriptions.get(0));
    }

    /**
     * 查找计划中取消的订阅
     */
    @Query(value = "SELECT * FROM subscriptions s WHERE s.status = 'ACTIVE' AND JSON_EXTRACT(s.metadata, '$.cancel_at_period_end') = 'true'", nativeQuery = true)
    List<Subscription> findScheduledForCancellation();

    /**
     * 查找需要续费的订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.nextBillAt <= CURRENT_TIMESTAMP")
    List<Subscription> findSubscriptionsForRenewal();

    /**
     * 根据Stripe客户ID查找订阅
     */
    @Query("SELECT s FROM Subscription s JOIN User u ON s.userId = u.id WHERE u.stripeCustomerId = :stripeCustomerId")
    List<Subscription> findByStripeCustomerId(@Param("stripeCustomerId") String stripeCustomerId);

    /**
     * 查找企业版订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.plan = 'ENTERPRISE' AND s.status IN ('ACTIVE', 'TRIALING')")
    List<Subscription> findActiveEnterpriseSubscriptions();

    /**
     * 统计收入（按月）
     */
    @Query("SELECT YEAR(s.start), MONTH(s.start), SUM(s.amount) FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIALING') AND s.start BETWEEN :startDate AND :endDate GROUP BY YEAR(s.start), MONTH(s.start)")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 查找免费用户
     */
    @Query("SELECT DISTINCT s.targetId FROM Subscription s WHERE s.plan = 'FREE' AND s.status = 'ACTIVE'")
    List<String> findFreeUsers();

    /**
     * 查找付费用户
     */
    @Query("SELECT DISTINCT s.targetId FROM Subscription s WHERE s.plan != 'FREE' AND s.status IN ('ACTIVE', 'TRIALING')")
    List<String> findPaidUsers();

    /**
     * 根据用户ID查找活跃订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status IN ('ACTIVE', 'TRIALING') ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") String userId);

    /**
     * 根据工作空间ID查找活跃订阅
     */
    @Query("SELECT s FROM Subscription s WHERE s.workspaceId = :workspaceId AND s.status IN ('ACTIVE', 'TRIALING') ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveSubscriptionByWorkspaceId(@Param("workspaceId") String workspaceId);

    /**
     * 根据用户ID查找订阅历史
     */
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<Subscription> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * 获取订阅统计信息（返回原始数据，由服务层转换为DTO）
     */
    @Query("SELECT " +
           "COUNT(s), " +
           "SUM(CASE WHEN s.status IN ('ACTIVE', 'TRIALING') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.status = 'TRIALING' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.status = 'CANCELED' THEN 1 ELSE 0 END) " +
           "FROM Subscription s")
    Object[] getSubscriptionStatsRaw();

    /**
     * 获取收入统计信息（返回原始数据，由服务层转换为DTO）
     */
    @Query("SELECT 0, 0, 0, 0, 0 FROM Subscription s WHERE 1=1")
    Object[] getRevenueStatsRaw(@Param("period") String period);
}