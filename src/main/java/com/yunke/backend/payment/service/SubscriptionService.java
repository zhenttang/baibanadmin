package com.yunke.backend.payment.service;

import com.yunke.backend.payment.dto.SubscriptionDto;
import com.yunke.backend.payment.dto.PaymentProviderDtos;
import com.yunke.backend.payment.dto.payment.InvoiceDto;
import com.yunke.backend.payment.domain.entity.Subscription;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 订阅服务接口
 * 对应Node.js版本的SubscriptionService
 */
public interface SubscriptionService {

    // ==================== 订阅管理 ====================

    /**
     * 创建订阅
     */
    Mono<SubscriptionDto> createSubscription(SubscriptionDto.CreateSubscriptionInput input, String userId);

    /**
     * 获取用户订阅
     */
    Mono<SubscriptionDto> getUserSubscription(String userId);

    /**
     * 获取工作空间订阅
     */
    Mono<SubscriptionDto> getWorkspaceSubscription(String workspaceId);

    /**
     * 更新订阅
     */
    Mono<SubscriptionDto> updateSubscription(String subscriptionId, SubscriptionDto.UpdateSubscriptionInput input, String userId);

    /**
     * 取消订阅
     */
    Mono<SubscriptionDto> cancelSubscription(String subscriptionId, SubscriptionDto.CancelSubscriptionInput input, String userId);

    /**
     * 重新激活订阅
     */
    Mono<SubscriptionDto> reactivateSubscription(String subscriptionId, String userId);

    /**
     * 获取订阅历史
     */
    Mono<List<SubscriptionDto>> getSubscriptionHistory(String userId);

    // ==================== 支付管理 ====================

    /**
     * 创建支付意图
     */
    Mono<PaymentProviderDtos.PaymentIntentDto> createPaymentIntent(PaymentProviderDtos.CreatePaymentIntentInput input, String userId);

    /**
     * 确认支付
     */
    Mono<PaymentProviderDtos.PaymentIntentDto> confirmPayment(String paymentIntentId, PaymentProviderDtos.ConfirmPaymentIntentInput input, String userId);

    /**
     * 创建结账会话
     */
    Mono<PaymentProviderDtos.CheckoutSessionDto> createCheckoutSession(PaymentProviderDtos.CreateCheckoutSessionInput input, String userId);

    /**
     * 创建客户门户会话
     */
    Mono<PaymentProviderDtos.CustomerPortalSessionDto> createCustomerPortalSession(String userId);

    // ==================== 发票管理 ====================

    /**
     * 获取用户发票
     */
    Mono<List<InvoiceDto>> getUserInvoices(String userId);

    /**
     * 获取发票详情
     */
    Mono<InvoiceDto> getInvoice(String invoiceId, String userId);

    /**
     * 支付发票
     */
    Mono<InvoiceDto> payInvoice(String invoiceId, String userId);

    /**
     * 下载发票
     */
    Mono<String> downloadInvoice(String invoiceId, String userId);

    // ==================== 价格和产品 ====================

    /**
     * 获取所有价格
     */
    Mono<List<PaymentProviderDtos.PriceDto>> getPrices();

    /**
     * 获取所有产品
     */
    Mono<List<PaymentProviderDtos.ProductDto>> getProducts();

    /**
     * 获取订阅计划
     */
    Mono<List<SubscriptionPlanDto>> getSubscriptionPlans();

    // ==================== 退款管理 ====================

    /**
     * 创建退款
     */
    Mono<PaymentProviderDtos.RefundDto> createRefund(PaymentProviderDtos.CreateRefundInput input, String userId);

    /**
     * 获取退款信息
     */
    Mono<PaymentProviderDtos.RefundDto> getRefund(String refundId, String userId);

    // ==================== 功能检查 ====================

    /**
     * 检查用户是否有某个功能
     */
    Mono<Boolean> hasFeature(String userId, String featureName);

    /**
     * 检查工作空间是否有某个功能
     */
    Mono<Boolean> workspaceHasFeature(String workspaceId, String featureName);

    /**
     * 获取用户可用功能列表
     */
    Mono<List<String>> getUserFeatures(String userId);

    /**
     * 获取工作空间可用功能列表
     */
    Mono<List<String>> getWorkspaceFeatures(String workspaceId);

    // ==================== 统计和监控 ====================

    /**
     * 获取订阅统计
     */
    Mono<SubscriptionStatsDto> getSubscriptionStats();

    /**
     * 获取收入统计
     */
    Mono<RevenueStatsDto> getRevenueStats(String period);

    /**
     * 获取用户使用情况
     */
    Mono<UsageStatsDto> getUserUsageStats(String userId);

    // ==================== Webhook处理 ====================

    /**
     * 处理支付成功Webhook
     */
    Mono<Void> handlePaymentSucceeded(String paymentIntentId);

    /**
     * 处理支付失败Webhook
     */
    Mono<Void> handlePaymentFailed(String paymentIntentId, String reason);

    /**
     * 处理订阅更新Webhook
     */
    Mono<Void> handleSubscriptionUpdated(String subscriptionId);

    /**
     * 处理发票支付成功Webhook
     */
    Mono<Void> handleInvoicePaymentSucceeded(String invoiceId);

    /**
     * 处理发票支付失败Webhook
     */
    Mono<Void> handleInvoicePaymentFailed(String invoiceId);

    // ==================== 数据传输对象 ====================

    record SubscriptionPlanDto(
            Subscription.SubscriptionPlan plan,
            String name,
            String description,
            java.math.BigDecimal monthlyPrice,
            java.math.BigDecimal yearlyPrice,
            List<String> features,
            Boolean popular
    ) {}

    record SubscriptionStatsDto(
            Long totalSubscriptions,
            Long activeSubscriptions,
            Long trialingSubscriptions,
            Long canceledSubscriptions,
            java.math.BigDecimal monthlyRecurringRevenue,
            java.math.BigDecimal annualRecurringRevenue
    ) {}

    record RevenueStatsDto(
            java.math.BigDecimal totalRevenue,
            java.math.BigDecimal monthlyRevenue,
            java.math.BigDecimal yearlyRevenue,
            java.math.BigDecimal averageOrderValue,
            Long totalOrders
    ) {}

    record UsageStatsDto(
            String userId,
            Subscription.SubscriptionPlan currentPlan,
            Long documentsUsed,
            Long storageUsed,
            Long apiCalls,
            Long bandwidthUsed,
            Boolean withinLimits
    ) {}
}
