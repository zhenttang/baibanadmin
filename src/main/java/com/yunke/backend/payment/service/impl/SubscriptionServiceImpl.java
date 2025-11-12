package com.yunke.backend.payment.service.impl;

import com.yunke.backend.payment.service.SubscriptionService.SubscriptionPlanDto;
import com.yunke.backend.payment.service.SubscriptionService.SubscriptionStatsDto;
import com.yunke.backend.payment.service.SubscriptionService.RevenueStatsDto;
import com.yunke.backend.payment.service.SubscriptionService.UsageStatsDto;
import com.yunke.backend.payment.dto.SubscriptionDto;
import com.yunke.backend.payment.dto.payment.InvoiceDto;
import com.yunke.backend.payment.domain.entity.Invoice;
import com.yunke.backend.payment.domain.entity.Subscription;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.payment.PaymentProvider;
import com.yunke.backend.payment.PaymentProviderManager;
import com.yunke.backend.payment.repository.InvoiceRepository;
import com.yunke.backend.payment.repository.SubscriptionRepository;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.payment.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订阅服务实现
 * 对应Node.js版本的SubscriptionService实现
 * 参考: /packages/backend/server/src/plugins/payment/service.ts
 */
@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentProviderManager paymentProviderManager;

    // ==================== 订阅管理 ====================

    @Override
    public Mono<SubscriptionDto> createSubscription(SubscriptionDto.CreateSubscriptionInput input, String userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(user -> {
                    // 检查是否已有活跃订阅
                    return Mono.fromCallable(() -> subscriptionRepository.findActiveSubscriptionByUserId(userId))
                            .flatMap(optionalSubscription -> {
                                if (optionalSubscription.isPresent()) {
                                    return Mono.error(new RuntimeException("用户已有活跃订阅"));
                                } else {
                                    return createNewSubscription(input, user);
                                }
                            });
                });
    }

    private Mono<SubscriptionDto> createNewSubscription(SubscriptionDto.CreateSubscriptionInput input, User user) {
        // 简化版本：直接创建订阅
        return Mono.fromCallable(() -> {
            Subscription subscription = new Subscription();
            subscription.setTargetId(user.getId());
            subscription.setUserId(user.getId());
            subscription.setStripeSubscriptionId("stripe-sub-" + UUID.randomUUID().toString());
            subscription.setPlan(input.getPlan());
            subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
            subscription.setBillingInterval(Subscription.BillingInterval.MONTH);
            subscription.setQuantity(input.getQuantity());
            subscription.setCurrentPeriodStart(LocalDateTime.now());
            subscription.setCurrentPeriodEnd(input.getTrialEnd() != null ? input.getTrialEnd() : LocalDateTime.now().plusMonths(1));
            subscription.setCreatedAt(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());

            return subscriptionRepository.save(subscription);
        }).map(this::convertToDto);
    }

    private Mono<String> ensureCustomerExists(User user, PaymentProvider provider) {
        if (user.getStripeCustomerId() != null) {
            return Mono.just(user.getStripeCustomerId());
        }

        // 简化版本：生成模拟客户ID
        String customerId = "cus_" + UUID.randomUUID().toString();
        user.setStripeCustomerId(customerId);
        return Mono.fromCallable(() -> userRepository.save(user))
                .map(savedUser -> customerId);
    }

    @Override
    public Mono<SubscriptionDto> getUserSubscription(String userId) {
        return Mono.fromCallable(() -> subscriptionRepository.findActiveSubscriptionByUserId(userId))
                .flatMap(optionalSubscription -> optionalSubscription.map(Mono::just).orElse(Mono.empty()))
                .map(this::convertToDto);
    }

    @Override
    public Mono<SubscriptionDto> getWorkspaceSubscription(String workspaceId) {
        return Mono.fromCallable(() -> subscriptionRepository.findActiveSubscriptionByWorkspaceId(workspaceId))
                .flatMap(optionalSubscription -> optionalSubscription.map(Mono::just).orElse(Mono.empty()))
                .map(this::convertToDto);
    }

    @Override
    public Mono<SubscriptionDto> updateSubscription(String subscriptionId, SubscriptionDto.UpdateSubscriptionInput input, String userId) {
        return Mono.fromCallable(() -> subscriptionRepository.findById(subscriptionId))
                .flatMap(optionalSubscription -> optionalSubscription.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("订阅不存在")))
                .filter(subscription -> subscription.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("无权限修改此订阅")))
                .flatMap(subscription -> {
                    // 简化版本：直接更新本地订阅
                    subscription.setQuantity(input.getQuantity());
                    subscription.setCancelAtPeriodEnd(input.getCancelAtPeriodEnd());
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return Mono.fromCallable(() -> subscriptionRepository.save(subscription));
                })
                .map(this::convertToDto);
    }

    @Override
    public Mono<SubscriptionDto> cancelSubscription(String subscriptionId, SubscriptionDto.CancelSubscriptionInput input, String userId) {
        return Mono.fromCallable(() -> subscriptionRepository.findById(subscriptionId))
                .flatMap(optionalSubscription -> optionalSubscription.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("订阅不存在")))
                .filter(subscription -> subscription.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("无权限取消此订阅")))
                .flatMap(subscription -> {
                    // 简化版本：直接取消本地订阅
                    subscription.cancel();
                    subscription.setCancelReason(input.getReason());
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return Mono.fromCallable(() -> subscriptionRepository.save(subscription));
                })
                .map(this::convertToDto);
    }

    @Override
    public Mono<SubscriptionDto> reactivateSubscription(String subscriptionId, String userId) {
        return Mono.fromCallable(() -> subscriptionRepository.findById(subscriptionId))
                .flatMap(optionalSubscription -> optionalSubscription.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("订阅不存在")))
                .filter(subscription -> subscription.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("无权限重新激活此订阅")))
                .flatMap(subscription -> {
                    // 简化版本：直接重新激活本地订阅
                    subscription.reactivate();
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return Mono.fromCallable(() -> subscriptionRepository.save(subscription));
                })
                .map(this::convertToDto);
    }

    @Override
    public Mono<List<SubscriptionDto>> getSubscriptionHistory(String userId) {
        return Mono.fromCallable(() -> subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .map(subscriptions -> subscriptions.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()));
    }

    // ==================== 支付管理 ====================

    @Override
    public Mono<com.yunke.backend.payment.dto.PaymentProviderDtos.PaymentIntentDto> createPaymentIntent(com.yunke.backend.payment.dto.PaymentProviderDtos.CreatePaymentIntentInput input, String userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(user -> {
                    // 简化版本：创建模拟的PaymentIntent
                    com.yunke.backend.payment.dto.PaymentProviderDtos.PaymentIntentDto paymentIntent = com.yunke.backend.payment.dto.PaymentProviderDtos.PaymentIntentDto.builder()
                            .id("pi_" + UUID.randomUUID().toString())
                            .amount(input.getAmount())
                            .currency(input.getCurrency())
                            .status("requires_payment_method")
                            .build();
                    return Mono.just(paymentIntent);
                });
    }

    @Override
    public Mono<com.yunke.backend.payment.dto.PaymentProviderDtos.PaymentIntentDto> confirmPayment(String paymentIntentId, com.yunke.backend.payment.dto.PaymentProviderDtos.ConfirmPaymentIntentInput input, String userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(user -> {
                    // 简化版本：返回已确认的PaymentIntent
                    com.yunke.backend.payment.dto.PaymentProviderDtos.PaymentIntentDto paymentIntent = com.yunke.backend.payment.dto.PaymentProviderDtos.PaymentIntentDto.builder()
                            .id(paymentIntentId)
                            .status("succeeded")
                            .build();
                    return Mono.just(paymentIntent);
                });
    }

    @Override
    public Mono<com.yunke.backend.payment.dto.PaymentProviderDtos.CheckoutSessionDto> createCheckoutSession(com.yunke.backend.payment.dto.PaymentProviderDtos.CreateCheckoutSessionInput input, String userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(user -> {
                    // 简化版本：创建模拟的CheckoutSession
                    com.yunke.backend.payment.dto.PaymentProviderDtos.CheckoutSessionDto checkoutSession = com.yunke.backend.payment.dto.PaymentProviderDtos.CheckoutSessionDto.builder()
                            .id("cs_" + UUID.randomUUID().toString())
                            .url("https://checkout.stripe.com/mock-session")
                            .status("open")
                            .build();
                    return Mono.just(checkoutSession);
                });
    }

    @Override
    public Mono<com.yunke.backend.payment.dto.PaymentProviderDtos.CustomerPortalSessionDto> createCustomerPortalSession(String userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()))
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(user -> {
                    // 简化版本：创建模拟的CustomerPortalSession
                    com.yunke.backend.payment.dto.PaymentProviderDtos.CustomerPortalSessionDto portalSession = com.yunke.backend.payment.dto.PaymentProviderDtos.CustomerPortalSessionDto.builder()
                            .id("bps_" + UUID.randomUUID().toString())
                            .url("https://billing.stripe.com/mock-portal")
                            .build();
                    return Mono.just(portalSession);
                });
    }

    // ==================== 发票管理 ====================

    @Override
    public Mono<List<InvoiceDto>> getUserInvoices(String userId) {
        return Mono.fromCallable(() -> invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .map(invoices -> invoices.stream()
                        .map(this::convertToInvoiceDto)
                        .collect(Collectors.toList()));
    }

    @Override
    public Mono<InvoiceDto> getInvoice(String invoiceId, String userId) {
        return Mono.fromCallable(() -> invoiceRepository.findById(invoiceId))
                .flatMap(optionalInvoice -> optionalInvoice.map(Mono::just).orElse(Mono.empty()))
                .filter(invoice -> invoice.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("发票不存在或无权限访问")))
                .map(this::convertToInvoiceDto);
    }

    @Override
    public Mono<InvoiceDto> payInvoice(String invoiceId, String userId) {
        return Mono.fromCallable(() -> invoiceRepository.findById(invoiceId))
                .flatMap(optionalInvoice -> optionalInvoice.map(Mono::just).orElse(Mono.empty()))
                .filter(invoice -> invoice.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("发票不存在或无权限访问")))
                .flatMap(invoice -> {
                    if (invoice.isPaid()) {
                        return Mono.error(new RuntimeException("发票已支付"));
                    }
                    
                    // 简化版本：直接标记为已支付
                    invoice.markAsPaid();
                    invoice.setUpdatedAt(LocalDateTime.now());
                    return Mono.fromCallable(() -> invoiceRepository.save(invoice))
                            .flatMap(paidInvoice -> {
                                invoice.markAsPaid();
                                invoice.setUpdatedAt(LocalDateTime.now());
                                return Mono.fromCallable(() -> invoiceRepository.save(invoice));
                            })
                            .cast(Invoice.class)
                            .map(this::convertToInvoiceDto);
                });
    }

    @Override
    public Mono<String> downloadInvoice(String invoiceId, String userId) {
        return Mono.fromCallable(() -> invoiceRepository.findById(invoiceId))
                .flatMap(optionalInvoice -> optionalInvoice.map(Mono::just).orElse(Mono.empty()))
                .filter(invoice -> invoice.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("发票不存在或无权限访问")))
                .flatMap(invoice -> {
                    // 简化版本：返回模拟下载URL
                    return Mono.just("https://mock-invoice-download.com/invoice/" + invoice.getStripeInvoiceId());
                });
    }

    // ==================== 价格和产品 ====================

    @Override
    public Mono<List<com.yunke.backend.payment.dto.PaymentProviderDtos.PriceDto>> getPrices() {
        // 简化版本：返回空列表
        return Mono.just(List.of());
    }

    @Override
    public Mono<List<com.yunke.backend.payment.dto.PaymentProviderDtos.ProductDto>> getProducts() {
        // 简化版本：返回空列表
        return Mono.just(List.of());
    }

    @Override
    public Mono<List<SubscriptionPlanDto>> getSubscriptionPlans() {
        // 预定义的订阅计划
        List<SubscriptionPlanDto> plans = List.of(
                new SubscriptionPlanDto(
                        Subscription.SubscriptionPlan.FREE,
                        "Free",
                        "免费计划，适合个人使用",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of("基础文档编辑", "3GB存储空间", "基础协作功能"),
                        false
                ),
                new SubscriptionPlanDto(
                        Subscription.SubscriptionPlan.PRO,
                        "Pro",
                        "专业计划，适合专业用户",
                        new BigDecimal("8.99"),
                        new BigDecimal("89.99"),
                        List.of("无限文档", "100GB存储空间", "高级协作功能", "AI功能", "优先支持"),
                        true
                ),
                new SubscriptionPlanDto(
                        Subscription.SubscriptionPlan.TEAM,
                        "Team",
                        "团队计划，适合团队协作",
                        new BigDecimal("12.99"),
                        new BigDecimal("129.99"),
                        List.of("所有Pro功能", "团队管理", "高级权限控制", "团队分析", "专属支持"),
                        false
                ),
                new SubscriptionPlanDto(
                        Subscription.SubscriptionPlan.ENTERPRISE,
                        "Enterprise",
                        "企业计划，适合大型企业",
                        new BigDecimal("29.99"),
                        new BigDecimal("299.99"),
                        List.of("所有Team功能", "SSO集成", "高级安全", "自定义部署", "企业级支持"),
                        false
                )
        );
        
        return Mono.just(plans);
    }

    // ==================== 退款管理 ====================

    @Override
    public Mono<com.yunke.backend.payment.dto.PaymentProviderDtos.RefundDto> createRefund(com.yunke.backend.payment.dto.PaymentProviderDtos.CreateRefundInput input, String userId) {
        // 简化版本：创建模拟退款
        com.yunke.backend.payment.dto.PaymentProviderDtos.RefundDto refund = com.yunke.backend.payment.dto.PaymentProviderDtos.RefundDto.builder()
                .id("re_" + UUID.randomUUID().toString())
                .amount(input.getAmount())
                .currency("usd")
                .status("succeeded")
                .build();
        return Mono.just(refund);
    }

    @Override
    public Mono<com.yunke.backend.payment.dto.PaymentProviderDtos.RefundDto> getRefund(String refundId, String userId) {
        // 简化版本：返回模拟退款信息
        com.yunke.backend.payment.dto.PaymentProviderDtos.RefundDto refund = com.yunke.backend.payment.dto.PaymentProviderDtos.RefundDto.builder()
                .id(refundId)
                .amount(BigDecimal.valueOf(100))
                .currency("usd")
                .status("succeeded")
                .build();
        return Mono.just(refund);
    }

    // ==================== 功能检查 ====================

    @Override
    public Mono<Boolean> hasFeature(String userId, String featureName) {
        return getUserSubscription(userId)
                .map(subscription -> {
                    Subscription.SubscriptionPlan plan = subscription.getPlan();
                    return hasFeatureForPlan(plan, featureName);
                })
                .defaultIfEmpty(hasFeatureForPlan(Subscription.SubscriptionPlan.FREE, featureName));
    }

    @Override
    public Mono<Boolean> workspaceHasFeature(String workspaceId, String featureName) {
        return getWorkspaceSubscription(workspaceId)
                .map(subscription -> {
                    Subscription.SubscriptionPlan plan = subscription.getPlan();
                    return hasFeatureForPlan(plan, featureName);
                })
                .defaultIfEmpty(hasFeatureForPlan(Subscription.SubscriptionPlan.FREE, featureName));
    }

    @Override
    public Mono<List<String>> getUserFeatures(String userId) {
        return getUserSubscription(userId)
                .map(subscription -> getFeaturesForPlan(subscription.getPlan()))
                .defaultIfEmpty(getFeaturesForPlan(Subscription.SubscriptionPlan.FREE));
    }

    @Override
    public Mono<List<String>> getWorkspaceFeatures(String workspaceId) {
        return getWorkspaceSubscription(workspaceId)
                .map(subscription -> getFeaturesForPlan(subscription.getPlan()))
                .defaultIfEmpty(getFeaturesForPlan(Subscription.SubscriptionPlan.FREE));
    }

    private boolean hasFeatureForPlan(Subscription.SubscriptionPlan plan, String featureName) {
        return switch (plan) {
            case FREE -> isFreeFeature(featureName);
            case PRO -> isProFeature(featureName);
            case TEAM -> isTeamFeature(featureName);
            case ENTERPRISE -> true;
        };
    }

    private List<String> getFeaturesForPlan(Subscription.SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> List.of("basic_editing", "basic_storage", "basic_collaboration");
            case PRO -> List.of("basic_editing", "basic_storage", "basic_collaboration", 
                              "unlimited_docs", "extended_storage", "ai_features", "priority_support");
            case TEAM -> List.of("basic_editing", "basic_storage", "basic_collaboration", 
                              "unlimited_docs", "extended_storage", "ai_features", "priority_support",
                              "team_management", "advanced_permissions", "team_analytics");
            case ENTERPRISE -> List.of("basic_editing", "basic_storage", "basic_collaboration", 
                                     "unlimited_docs", "extended_storage", "ai_features", "priority_support",
                                     "team_management", "advanced_permissions", "team_analytics",
                                     "sso_integration", "advanced_security", "custom_deployment", "enterprise_support");
        };
    }

    private boolean isFreeFeature(String featureName) {
        return List.of("basic_editing", "basic_storage", "basic_collaboration").contains(featureName);
    }

    private boolean isProFeature(String featureName) {
        return isFreeFeature(featureName) || 
               List.of("unlimited_docs", "extended_storage", "ai_features", "priority_support").contains(featureName);
    }

    private boolean isTeamFeature(String featureName) {
        return isProFeature(featureName) || 
               List.of("team_management", "advanced_permissions", "team_analytics").contains(featureName);
    }

    // ==================== 统计和监控 ====================

    @Override
    public Mono<SubscriptionStatsDto> getSubscriptionStats() {
        return Mono.fromCallable(() -> {
            Object[] rawStats = subscriptionRepository.getSubscriptionStatsRaw();
            if (rawStats == null || rawStats.length < 4) {
                return new SubscriptionStatsDto(0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            
            Long totalSubscriptions = ((Number) rawStats[0]).longValue();
            Long activeSubscriptions = ((Number) rawStats[1]).longValue();
            Long trialingSubscriptions = ((Number) rawStats[2]).longValue();
            Long canceledSubscriptions = ((Number) rawStats[3]).longValue();
            
            return new SubscriptionStatsDto(
                    totalSubscriptions,
                    activeSubscriptions,
                    trialingSubscriptions,
                    canceledSubscriptions,
                    BigDecimal.ZERO, // monthlyRecurringRevenue - 需要单独计算
                    BigDecimal.ZERO  // annualRecurringRevenue - 需要单独计算
            );
        });
    }

    @Override
    public Mono<RevenueStatsDto> getRevenueStats(String period) {
        return Mono.fromCallable(() -> {
            Object[] rawStats = subscriptionRepository.getRevenueStatsRaw(period);
            if (rawStats == null || rawStats.length < 5) {
                return new RevenueStatsDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
            }
            
            // 注意：当前查询返回的是占位符值，实际应该计算真实收入数据
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal monthlyRevenue = BigDecimal.ZERO;
            BigDecimal yearlyRevenue = BigDecimal.ZERO;
            BigDecimal averageOrderValue = BigDecimal.ZERO;
            Long totalOrders = 0L;
            
            return new RevenueStatsDto(
                    totalRevenue,
                    monthlyRevenue,
                    yearlyRevenue,
                    averageOrderValue,
                    totalOrders
            );
        });
    }

    @Override
    public Mono<UsageStatsDto> getUserUsageStats(String userId) {
        return getUserSubscription(userId)
                .flatMap(subscription -> {
                    // 这里可以集成实际的使用量统计
                    return Mono.just(new UsageStatsDto(
                            userId,
                            subscription.getPlan(),
                            0L, // documentsUsed - 需要从实际使用情况计算
                            0L, // storageUsed - 需要从实际使用情况计算
                            0L, // apiCalls - 需要从实际使用情况计算
                            0L, // bandwidthUsed - 需要从实际使用情况计算
                            true // withinLimits - 需要根据实际使用情况判断
                    ));
                })
                .defaultIfEmpty(new UsageStatsDto(
                        userId,
                        Subscription.SubscriptionPlan.FREE,
                        0L, 0L, 0L, 0L, true
                ));
    }

    // ==================== Webhook处理 ====================

    /**
     * 处理支付成功事件
     * 
     * @param paymentIntentId 支付意图ID
     * @return 操作完成信号
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<Void> handlePaymentSucceeded(String paymentIntentId) {
        // TODO: 实现支付成功处理逻辑
        // 需要：1. 查询支付意图信息 2. 激活订阅 3. 发送通知邮件 4. 更新用户配额
        return Mono.empty();
    }

    /**
     * 处理支付失败事件
     * 
     * @param paymentIntentId 支付意图ID
     * @param reason 失败原因
     * @return 操作完成信号
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<Void> handlePaymentFailed(String paymentIntentId, String reason) {
        // TODO: 实现支付失败处理逻辑
        // 需要：1. 查询支付意图信息 2. 暂停订阅 3. 发送通知邮件 4. 记录失败原因
        return Mono.empty();
    }

    /**
     * 处理订阅更新事件
     * 
     * @param subscriptionId 订阅ID
     * @return 操作完成信号
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<Void> handleSubscriptionUpdated(String subscriptionId) {
        // TODO: 实现订阅更新处理逻辑
        // 需要：1. 从支付提供商获取最新订阅状态 2. 更新本地订阅记录 3. 更新用户配额
        return Mono.empty();
    }

    /**
     * 处理发票支付成功事件
     * 
     * @param invoiceId 发票ID
     * @return 操作完成信号
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<Void> handleInvoicePaymentSucceeded(String invoiceId) {
        // TODO: 实现发票支付成功处理逻辑
        // 需要：1. 查询发票信息 2. 更新发票状态 3. 发送确认邮件
        return Mono.empty();
    }

    /**
     * 处理发票支付失败事件
     * 
     * @param invoiceId 发票ID
     * @return 操作完成信号
     * @deprecated 功能待实现，当前返回空
     */
    @Deprecated
    @Override
    public Mono<Void> handleInvoicePaymentFailed(String invoiceId) {
        // TODO: 实现发票支付失败处理逻辑
        // 需要：1. 查询发票信息 2. 更新发票状态 3. 发送失败通知邮件
        return Mono.empty();
    }

    // ==================== 辅助方法 ====================

    private SubscriptionDto convertToDto(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .workspaceId(subscription.getWorkspaceId())
                .targetId(subscription.getTargetId())
                .stripeSubscriptionId(subscription.getStripeSubscriptionId())
                .plan(subscription.getPlan())
                .status(subscription.getStatus())
                .recurring(subscription.getBillingInterval())
                .quantity(subscription.getQuantity())
                .start(subscription.getStart())
                .end(subscription.getEnd())
                .nextBillAt(subscription.getNextBillAt())
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .trialStart(subscription.getTrialStart())
                .trialEnd(subscription.getTrialEnd())
                .canceledAt(subscription.getCanceledAt())
                .cancelReason(subscription.getCancelReason())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    private InvoiceDto convertToInvoiceDto(Invoice invoice) {
        return InvoiceDto.builder()
                .id(invoice.getId())
                .subscriptionId(invoice.getSubscriptionId() != null ? invoice.getSubscriptionId().toString() : null)
                .customerId(invoice.getUserId())
                .number(invoice.getStripeInvoiceId())
                .status(invoice.getStatus().name())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .createdAt(invoice.getCreatedAt())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .description(invoice.getReason())
                .receiptUrl(invoice.getLink())
                .hostedInvoiceUrl(invoice.getLink())
                .metadata(invoice.getMetadata())
                .build();
    }
}
