package com.yunke.backend.ai;

import com.yunke.backend.ai.dto.ChatCompletionRequest;
import com.yunke.backend.ai.dto.ChatCompletionResponse;
import com.yunke.backend.ai.domain.entity.CopilotSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI提供商接口
 * 支持多个AI提供商的统一抽象
 */
public interface AIProvider {

    /**
     * 获取提供商类型
     */
    CopilotSession.AIProvider getProviderType();

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 检查提供商是否可用
     */
    Mono<Boolean> isAvailable();

    /**
     * 获取支持的模型列表
     */
    Mono<java.util.List<AIModel>> getSupportedModels();

    /**
     * 发送聊天完成请求
     */
    Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request);

    /**
     * 发送流式聊天完成请求
     */
    Flux<ChatCompletionResponse> streamChatCompletion(ChatCompletionRequest request);

    /**
     * 计算token数量
     */
    Mono<Integer> countTokens(String text, String model);

    /**
     * 获取模型信息
     */
    Mono<AIModel> getModelInfo(String modelName);

    /**
     * 验证API密钥
     */
    Mono<Boolean> validateApiKey(String apiKey);

    /**
     * 获取提供商配置
     */
    Map<String, Object> getProviderConfig();

    /**
     * 设置提供商配置
     */
    void setProviderConfig(Map<String, Object> config);

    /**
     * AI模型信息
     */
    record AIModel(
            String id,
            String name,
            String description,
            int maxTokens,
            double costPerInputToken,
            double costPerOutputToken,
            boolean supportsStreaming,
            boolean supportsImages,
            java.util.List<String> capabilities
    ) {}

    /**
     * 提供商状态
     */
    record ProviderStatus(
            boolean available,
            String status,
            String version,
            long responseTime,
            String errorMessage
    ) {}
}