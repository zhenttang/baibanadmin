package com.yunke.backend.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 支付提供商管理器
 * 统一管理多个支付提供商
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderManager {

    private final List<PaymentProvider> providers;

    /**
     * 根据提供商类型获取提供商
     */
    public Optional<PaymentProvider> getProvider(PaymentProvider.PaymentProviderType providerType) {
        return providers.stream()
                .filter(provider -> provider.getProviderType() == providerType)
                .findFirst();
    }

    /**
     * 获取默认提供商（第一个可用的）
     */
    public Mono<Optional<PaymentProvider>> getDefaultProvider() {
        return getAvailableProviders()
                .map(availableProviders -> 
                        availableProviders.isEmpty() ? 
                                Optional.<PaymentProvider>empty() : 
                                Optional.of(availableProviders.get(0))
                );
    }

    /**
     * 获取所有可用的提供商
     */
    public Mono<List<PaymentProvider>> getAvailableProviders() {
        return reactor.core.publisher.Flux.fromIterable(providers)
                .filterWhen(PaymentProvider::isAvailable)
                .collectList()
                .doOnNext(availableProviders -> 
                        log.debug("Found {} available payment providers", availableProviders.size())
                );
    }

    /**
     * 获取所有提供商
     */
    public List<PaymentProvider> getAllProviders() {
        return providers;
    }

    /**
     * 检查提供商是否存在且可用
     */
    public Mono<Boolean> isProviderAvailable(PaymentProvider.PaymentProviderType providerType) {
        return getProvider(providerType)
                .map(PaymentProvider::isAvailable)
                .orElse(Mono.just(false));
    }

    /**
     * 获取提供商状态信息
     */
    public Mono<Map<PaymentProvider.PaymentProviderType, ProviderStatus>> getProviderStatuses() {
        return reactor.core.publisher.Flux.fromIterable(providers)
                .flatMap(provider -> 
                        provider.isAvailable()
                                .map(available -> Map.entry(
                                        provider.getProviderType(),
                                        new ProviderStatus(
                                                available,
                                                available ? "available" : "unavailable",
                                                "1.0",
                                                0L,
                                                available ? null : "API key not configured or invalid"
                                        )
                                ))
                                .onErrorReturn(Map.entry(
                                        provider.getProviderType(),
                                        new ProviderStatus(
                                                false,
                                                "error",
                                                "1.0",
                                                0L,
                                                "Health check failed"
                                        )
                                ))
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * 获取推荐的提供商（基于性能和可用性）
     */
    public Mono<Optional<PaymentProvider>> getRecommendedProvider() {
        return getAvailableProviders()
                .map(availableProviders -> {
                    if (availableProviders.isEmpty()) {
                        return Optional.<PaymentProvider>empty();
                    }

                    // 优先级排序：Stripe > PayPal > 其他
                    return availableProviders.stream()
                            .min((p1, p2) -> {
                                int priority1 = getProviderPriority(p1.getProviderType());
                                int priority2 = getProviderPriority(p2.getProviderType());
                                return Integer.compare(priority1, priority2);
                            });
                });
    }

    /**
     * 根据支付方式获取最佳提供商
     */
    public Mono<Optional<PaymentProvider>> getBestProviderForPaymentMethod(PaymentProvider.PaymentMethodType paymentMethod) {
        return getAvailableProviders()
                .map(availableProviders -> 
                        availableProviders.stream()
                                .filter(provider -> provider.getSupportedPaymentMethods().contains(paymentMethod))
                                .findFirst()
                );
    }

    /**
     * 验证所有提供商配置
     */
    public Mono<Map<PaymentProvider.PaymentProviderType, Boolean>> validateProviderConfigs() {
        return reactor.core.publisher.Flux.fromIterable(providers)
                .flatMap(provider -> 
                        provider.isAvailable()
                                .map(available -> Map.entry(provider.getProviderType(), available))
                                .onErrorReturn(Map.entry(provider.getProviderType(), false))
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * 处理Webhook事件（自动路由到正确的提供商）
     */
    public Mono<Boolean> processWebhookEvent(String providerType, String payload, String signature) {
        try {
            PaymentProvider.PaymentProviderType type = PaymentProvider.PaymentProviderType.valueOf(providerType.toUpperCase());
            return getProvider(type)
                    .map(provider -> {
                        // 这里应该解析payload并调用provider的processWebhookEvent方法
                        // 简化实现
                        return true;
                    })
                    .map(Mono::just)
                    .orElse(Mono.just(false));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown payment provider type: {}", providerType);
            return Mono.just(false);
        }
    }

    /**
     * 获取所有支持的支付方式
     */
    public Mono<List<PaymentProvider.PaymentMethodType>> getAllSupportedPaymentMethods() {
        return getAvailableProviders()
                .map(providers -> providers.stream()
                        .flatMap(provider -> provider.getSupportedPaymentMethods().stream())
                        .distinct()
                        .collect(Collectors.toList()));
    }

    /**
     * 获取提供商统计信息
     */
    public Mono<ProviderStatistics> getProviderStatistics() {
        return Mono.fromCallable(() -> {
            int totalProviders = providers.size();
            
            Map<PaymentProvider.PaymentProviderType, Boolean> availability = validateProviderConfigs().block();
            long availableProviders = availability.values().stream().mapToLong(b -> b ? 1 : 0).sum();
            
            List<PaymentProvider.PaymentMethodType> supportedMethods = getAllSupportedPaymentMethods().block();
            
            return new ProviderStatistics(
                    totalProviders,
                    (int) availableProviders,
                    supportedMethods.size(),
                    supportedMethods
            );
        });
    }

    private int getProviderPriority(PaymentProvider.PaymentProviderType providerType) {
        return switch (providerType) {
            case STRIPE -> 1;
            case PAYPAL -> 2;
            case ALIPAY -> 3;
            case WECHAT_PAY -> 4;
        };
    }

    /**
     * 提供商状态记录
     */
    public record ProviderStatus(
            boolean available,
            String status,
            String version,
            long responseTime,
            String errorMessage
    ) {}

    /**
     * 提供商统计信息记录
     */
    public record ProviderStatistics(
            int totalProviders,
            int availableProviders,
            int supportedPaymentMethods,
            List<PaymentProvider.PaymentMethodType> paymentMethods
    ) {}
}