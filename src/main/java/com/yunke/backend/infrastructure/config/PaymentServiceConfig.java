package com.yunke.backend.infrastructure.config;

import com.yunke.backend.payment.repository.AFFiNEPaymentOrderRepository;
import com.yunke.backend.payment.service.AFFiNEPaymentService;

import com.yunke.backend.payment.service.impl.AFFiNEAlipayServiceImpl;
import com.yunke.backend.payment.service.impl.AFFiNEDirectPaymentServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付服务配置 - 支持多种支付方式
 */
@Configuration
public class PaymentServiceConfig {
    
    /**
     * 轻量级支付服务实现 - 无需额外服务端
     */
    @Bean
    @ConditionalOnProperty(name = "affine.payment.provider", havingValue = "direct")
    public AFFiNEPaymentService directPaymentService(
            AFFiNEPaymentOrderRepository paymentOrderRepository,
            com.yunke.backend.payment.service.UserSubscriptionService subscriptionService) {
        return new AFFiNEDirectPaymentServiceImpl(paymentOrderRepository, subscriptionService);
    }
    
    /**
     * 支付宝沙箱支付服务实现 - 真实支付宝集成
     */
    @Bean
    @ConditionalOnProperty(name = "affine.payment.provider", havingValue = "alipay-sandbox", matchIfMissing = true)
    public AFFiNEPaymentService alipayPaymentService(
            AlipayProperties alipayProperties,
            AFFiNEPaymentOrderRepository paymentOrderRepository,
            com.yunke.backend.payment.service.UserSubscriptionService subscriptionService) {
        return new AFFiNEAlipayServiceImpl(alipayProperties, paymentOrderRepository, subscriptionService);
    }
}