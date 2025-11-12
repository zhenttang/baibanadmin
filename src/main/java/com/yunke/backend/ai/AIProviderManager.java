package com.yunke.backend.ai;

import com.yunke.backend.ai.domain.entity.CopilotSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI提供商管理器
 * 统一管理多个AI提供商
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderManager {

    private final List<AIProvider> providers;

    /**
     * 根据提供商类型获取提供商
     */
    public Optional<AIProvider> getProvider(CopilotSession.AIProvider providerType) {
        return providers.stream()
                .filter(provider -> provider.getProviderType() == providerType)
                .findFirst();
    }

    /**
     * 获取默认提供商（第一个可用的）
     */
    public Mono<Optional<AIProvider>> getDefaultProvider() {
        return getAvailableProviders()
                .map(availableProviders -> 
                        availableProviders.isEmpty() ? 
                                Optional.<AIProvider>empty() : 
                                Optional.of(availableProviders.get(0))
                );
    }

    /**
     * 获取所有可用的提供商
     */
    public Mono<List<AIProvider>> getAvailableProviders() {
        return reactor.core.publisher.Flux.fromIterable(providers)
                .filterWhen(AIProvider::isAvailable)
                .collectList()
                .doOnNext(availableProviders -> 
                        log.debug("Found {} available AI providers", availableProviders.size())
                );
    }

    /**
     * 获取所有提供商
     */
    public List<AIProvider> getAllProviders() {
        return providers;
    }

    /**
     * 检查提供商是否存在且可用
     */
    public Mono<Boolean> isProviderAvailable(CopilotSession.AIProvider providerType) {
        return getProvider(providerType)
                .map(AIProvider::isAvailable)
                .orElse(Mono.just(false));
    }

    /**
     * 获取提供商状态信息
     */
    public Mono<Map<CopilotSession.AIProvider, AIProvider.ProviderStatus>> getProviderStatuses() {
        return reactor.core.publisher.Flux.fromIterable(providers)
                .flatMap(provider -> 
                        provider.isAvailable()
                                .map(available -> Map.entry(
                                        provider.getProviderType(),
                                        new AIProvider.ProviderStatus(
                                                available,
                                                available ? "available" : "unavailable",
                                                "1.0",
                                                0L,
                                                available ? null : "API key not configured or invalid"
                                        )
                                ))
                                .onErrorReturn(Map.entry(
                                        provider.getProviderType(),
                                        new AIProvider.ProviderStatus(
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
    public Mono<Optional<AIProvider>> getRecommendedProvider() {
        return getAvailableProviders()
                .map(availableProviders -> {
                    if (availableProviders.isEmpty()) {
                        return Optional.<AIProvider>empty();
                    }

                    // 优先级排序：OpenAI > Anthropic > Google > Others
                    return availableProviders.stream()
                            .min((p1, p2) -> {
                                int priority1 = getProviderPriority(p1.getProviderType());
                                int priority2 = getProviderPriority(p2.getProviderType());
                                return Integer.compare(priority1, priority2);
                            });
                });
    }

    /**
     * 根据功能获取最佳提供商
     */
    public Mono<Optional<AIProvider>> getBestProviderForCapability(String capability) {
        return getAvailableProviders()
                .map(availableProviders -> 
                        availableProviders.stream()
                                .filter(provider -> {
                                    try {
                                        return provider.getSupportedModels()
                                                .block()
                                                .stream()
                                                .anyMatch(model -> model.capabilities().contains(capability));
                                    } catch (Exception e) {
                                        return false;
                                    }
                                })
                                .findFirst()
                );
    }

    /**
     * 验证提供商配置
     */
    public Mono<Map<CopilotSession.AIProvider, Boolean>> validateProviderConfigs() {
        return reactor.core.publisher.Flux.fromIterable(providers)
                .flatMap(provider -> {
                    Map<String, Object> config = provider.getProviderConfig();
                    String apiKey = (String) config.get("apiKey");
                    
                    if (apiKey == null || "***".equals(apiKey) || apiKey.trim().isEmpty()) {
                        return Mono.just(Map.entry(provider.getProviderType(), false));
                    }
                    
                    return provider.validateApiKey(apiKey)
                            .map(valid -> Map.entry(provider.getProviderType(), valid))
                            .onErrorReturn(Map.entry(provider.getProviderType(), false));
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * 更新提供商配置
     */
    public void updateProviderConfig(CopilotSession.AIProvider providerType, Map<String, Object> config) {
        getProvider(providerType).ifPresent(provider -> {
            provider.setProviderConfig(config);
            log.info("Updated configuration for provider: {}", providerType);
        });
    }

    /**
     * 获取所有提供商的配置信息
     */
    public Map<CopilotSession.AIProvider, Map<String, Object>> getAllProviderConfigs() {
        return providers.stream()
                .collect(Collectors.toMap(
                        AIProvider::getProviderType,
                        AIProvider::getProviderConfig
                ));
    }

    private int getProviderPriority(CopilotSession.AIProvider providerType) {
        return switch (providerType) {
            case OPENAI -> 1;
            case ANTHROPIC -> 2;
            case GOOGLE -> 3;
            case AZURE_OPENAI -> 4;
            case OLLAMA -> 5;
        };
    }
}