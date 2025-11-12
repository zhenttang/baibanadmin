package com.yunke.backend.payment.repository;

import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AFFiNE支付订单Repository
 */
@Repository
public interface AFFiNEPaymentOrderRepository extends JpaRepository<AFFiNEPaymentOrder, String> {
    
    /**
     * 根据Jeepay订单号查找订单
     */
    Optional<AFFiNEPaymentOrder> findByJeepayOrderNo(String jeepayOrderNo);
    
    /**
     * 根据用户ID查找订单列表
     */
    Page<AFFiNEPaymentOrder> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 根据用户ID和状态查找订单
     */
    List<AFFiNEPaymentOrder> findByUserIdAndStatus(String userId, PaymentStatus status);
    
    /**
     * 根据状态查找订单
     */
    List<AFFiNEPaymentOrder> findByStatus(PaymentStatus status);
    
    /**
     * 查找过期的待支付订单
     */
    @Query("SELECT o FROM AFFiNEPaymentOrder o WHERE o.status = :status AND o.expireTime < :expireTime")
    List<AFFiNEPaymentOrder> findExpiredOrders(@Param("status") PaymentStatus status, 
                                               @Param("expireTime") LocalDateTime expireTime);
    
    /**
     * 统计用户支付成功的订单数量
     */
    @Query("SELECT COUNT(o) FROM AFFiNEPaymentOrder o WHERE o.userId = :userId AND o.status = :status")
    Long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") PaymentStatus status);
    
    /**
     * 统计用户支付总金额
     */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM AFFiNEPaymentOrder o WHERE o.userId = :userId AND o.status = :status")
    Long sumAmountByUserIdAndStatus(@Param("userId") String userId, @Param("status") PaymentStatus status);
    
    /**
     * 查找指定时间范围内的订单
     */
    @Query("SELECT o FROM AFFiNEPaymentOrder o WHERE o.createdAt BETWEEN :startTime AND :endTime ORDER BY o.createdAt DESC")
    List<AFFiNEPaymentOrder> findOrdersBetween(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
}