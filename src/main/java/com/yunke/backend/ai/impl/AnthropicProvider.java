package com.yunke.backend.ai.impl;

import com.yunke.backend.ai.AIProvider;
import com.yunke.backend.ai.dto.ChatCompletionRequest;
import com.yunke.backend.ai.dto.ChatCompletionResponse;
import com.yunke.backend.ai.domain.entity.CopilotSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Anthropic (Claude) 提供商实现
 * 对应Node.js版本的AnthropicProvider
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicProvider implements AIProvider {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${copilot.anthropic.api-key:}")
    private String apiKey;

    @Value("${copilot.anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${copilot.anthropic.timeout:60}")
    private int timeoutSeconds;

    @Value("${copilot.anthropic.version:2023-06-01}")
    private String apiVersion;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", apiVersion)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();
        }
        return webClient;
    }

    @Override
    public CopilotSession.AIProvider getProviderType() {
        return CopilotSession.AIProvider.ANTHROPIC;
    }

    @Override
    public String getProviderName() {
        return "Anthropic (Claude)";
    }

    @Override
    public Mono<Boolean> isAvailable() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Mono.just(false);
        }

        // Anthropic没有直接的健康检查端点，我们可以尝试发送一个简单请求
        return Mono.just(true); // 简化实现
    }

    @Override
    public Mono<List<AIModel>> getSupportedModels() {
        return Mono.just(getDefaultModels());
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request) {
        log.debug("Anthropic chat completion request: model={}, messages={}", 
                request.getModel(), request.getMessages().size());

        // 转换请求格式为Anthropic API格式
        Map<String, Object> anthropicRequest = convertToAnthropicRequest(request);

        return getWebClient()
                .post()
                .uri("/v1/messages")
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::convertFromAnthropicResponse)
                .doOnSuccess(response -> log.debug("Anthropic response received"))
                .doOnError(error -> log.error("Anthropic request failed", error));
    }

    @Override
    public Flux<ChatCompletionResponse> streamChatCompletion(ChatCompletionRequest request) {
        log.debug("Anthropic streaming chat completion request: model={}, messages={}", 
                request.getModel(), request.getMessages().size());

        Map<String, Object> anthropicRequest = convertToAnthropicRequest(request);
        anthropicRequest.put("stream", true);

        return getWebClient()
                .post()
                .uri("/v1/messages")
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                .map(line -> line.substring(6))
                .flatMap(this::parseStreamResponse)
                .doOnNext(response -> log.debug("Anthropic stream chunk received"))
                .doOnError(error -> log.error("Anthropic streaming request failed", error));
    }

    @Override
    public Mono<Integer> countTokens(String text, String model) {
        // Anthropic token计算比较复杂，这里使用简化估算
        return Mono.fromCallable(() -> {
            // Claude模型的token估算稍有不同
            int estimatedTokens = (int) Math.ceil(text.length() / 3.5);
            return Math.max(1, estimatedTokens);
        });
    }

    @Override
    public Mono<AIModel> getModelInfo(String modelName) {
        return getSupportedModels()
                .map(models -> models.stream()
                        .filter(model -> model.id().equals(modelName))
                        .findFirst()
                        .orElse(getDefaultModelInfo(modelName)));
    }

    @Override
    public Mono<Boolean> validateApiKey(String apiKey) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", apiVersion)
                .build()
                .post()
                .uri("/v1/messages")
                .bodyValue(Map.of(
                        "model", "claude-3-sonnet-20240229",
                        "max_tokens", 1,
                        "messages", List.of(Map.of("role", "user", "content", "test"))
                ))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> true)
                .onErrorReturn(false);
    }

    @Override
    public Map<String, Object> getProviderConfig() {
        return Map.of(
                "apiKey", apiKey != null ? "***" : null,
                "baseUrl", baseUrl,
                "timeout", timeoutSeconds,
                "version", apiVersion,
                "available", apiKey != null && !apiKey.trim().isEmpty()
        );
    }

    @Override
    public void setProviderConfig(Map<String, Object> config) {
        if (config.containsKey("apiKey")) {
            this.apiKey = (String) config.get("apiKey");
        }
        if (config.containsKey("baseUrl")) {
            this.baseUrl = (String) config.get("baseUrl");
        }
        if (config.containsKey("timeout")) {
            this.timeoutSeconds = (Integer) config.get("timeout");
        }
        if (config.containsKey("version")) {
            this.apiVersion = (String) config.get("version");
        }
        this.webClient = null;
    }

    private List<AIModel> getDefaultModels() {
        return List.of(
                new AIModel(
                        "claude-3-opus-20240229",
                        "Claude 3 Opus",
                        "Most powerful Claude 3 model",
                        200000,
                        0.015,
                        0.075,
                        true,
                        true,
                        List.of("chat", "completion", "reasoning", "vision", "analysis")
                ),
                new AIModel(
                        "claude-3-sonnet-20240229",
                        "Claude 3 Sonnet",
                        "Balanced Claude 3 model",
                        200000,
                        0.003,
                        0.015,
                        true,
                        true,
                        List.of("chat", "completion", "reasoning", "vision")
                ),
                new AIModel(
                        "claude-3-haiku-20240307",
                        "Claude 3 Haiku",
                        "Fastest Claude 3 model",
                        200000,
                        0.00025,
                        0.00125,
                        true,
                        true,
                        List.of("chat", "completion", "speed")
                )
        );
    }

    private AIModel getDefaultModelInfo(String modelName) {
        return new AIModel(
                modelName,
                modelName,
                "Anthropic Claude model",
                200000,
                0.003,
                0.015,
                true,
                true,
                List.of("chat", "completion", "reasoning")
        );
    }

    private Map<String, Object> convertToAnthropicRequest(ChatCompletionRequest request) {
        // 转换OpenAI格式的请求到Anthropic格式
        return Map.of(
                "model", request.getModel(),
                "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096,
                "messages", request.getMessages(),
                "temperature", request.getTemperature() != null ? request.getTemperature() : 1.0
        );
    }

    private ChatCompletionResponse convertFromAnthropicResponse(String anthropicResponse) {
        try {
            // 这里需要将Anthropic的响应格式转换为标准的ChatCompletionResponse格式
            // 简化实现，实际需要根据Anthropic API文档进行详细转换
            Map<String, Object> response = objectMapper.readValue(anthropicResponse, Map.class);
            
            ChatCompletionResponse.Builder builder = ChatCompletionResponse.builder()
                    .id((String) response.get("id"))
                    .model((String) response.get("model"));

            // 转换choices
            if (response.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                if (!content.isEmpty() && "text".equals(content.get(0).get("type"))) {
                    String text = (String) content.get(0).get("text");
                    
                    ChatCompletionResponse.Choice choice = ChatCompletionResponse.Choice.builder()
                            .index(0)
                            .message(ChatCompletionResponse.Message.builder()
                                    .role("assistant")
                                    .content(text)
                                    .build())
                            .finishReason((String) response.get("stop_reason"))
                            .build();
                    
                    builder.choices(List.of(choice));
                }
            }

            // 转换usage
            if (response.containsKey("usage")) {
                Map<String, Object> usage = (Map<String, Object>) response.get("usage");
                builder.usage(ChatCompletionResponse.Usage.builder()
                        .promptTokens((Integer) usage.get("input_tokens"))
                        .completionTokens((Integer) usage.get("output_tokens"))
                        .totalTokens((Integer) usage.get("input_tokens") + (Integer) usage.get("output_tokens"))
                        .build());
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to convert Anthropic response", e);
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    private Mono<ChatCompletionResponse> parseStreamResponse(String jsonLine) {
        try {
            if (jsonLine.trim().isEmpty()) {
                return Mono.empty();
            }
            
            // 解析Anthropic流式响应
            return Mono.just(convertFromAnthropicResponse(jsonLine));
        } catch (Exception e) {
            log.debug("Failed to parse stream response line: {}", jsonLine, e);
            return Mono.empty();
        }
    }
}