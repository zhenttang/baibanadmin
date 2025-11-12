package com.yunke.backend.payment.service;

import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AFFiNE支付服务接口
 */
public interface AFFiNEPaymentService {
    
    /**
     * 创建支付订单
     */
    Mono<PaymentResult> createPayment(PaymentRequest request);
    
    /**
     * 查询支付状态
     */
    Mono<PaymentStatus> queryPaymentStatus(String orderId);
    
    /**
     * 处理支付回调
     */
    Mono<Void> handlePaymentCallback(String notifyData, String signature);
    
    /**
     * 取消支付订单
     */
    Mono<Boolean> cancelPayment(String orderId, String userId);
    
    /**
     * 退款处理
     */
    Mono<Boolean> refundPayment(String orderId, String userId, String reason);
    
    /**
     * 获取用户支付订单列表
     */
    Mono<List<AFFiNEPaymentOrder>> getUserPaymentOrders(String userId, int page, int size);
    
    /**
     * 验证订单归属
     */
    Mono<Boolean> isOrderOwnedByUser(String orderId, String userId);
    
    /**
     * 清理过期订单
     */
    Mono<Integer> cleanupExpiredOrders();
}