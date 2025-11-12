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
 * OpenAI提供商实现
 * 对应Node.js版本的OpenAIProvider
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIProvider implements AIProvider {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${affine.copilot.openai.api-key:}")
    private String apiKey;

    @Value("${affine.copilot.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${affine.copilot.openai.timeout:30}")
    private int timeoutSeconds;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();
        }
        return webClient;
    }

    @Override
    public CopilotSession.AIProvider getProviderType() {
        return CopilotSession.AIProvider.OPENAI;
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public Mono<Boolean> isAvailable() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Mono.just(false);
        }

        return getWebClient()
                .get()
                .uri("/models")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> true)
                .onErrorReturn(false);
    }

    @Override
    public Mono<List<AIModel>> getSupportedModels() {
        return getWebClient()
                .get()
                .uri("/models")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseModelsResponse)
                .onErrorReturn(getDefaultModels());
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request) {
        log.debug("DeepSeek chat completion request: model={}, messages={}", 
                request.getModel(), request.getMessages().size());
        
        // 记录完整的请求内容以便调试
        log.info("DeepSeek request body: {}", request);

        return getWebClient()
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> {
                    int totalTokens = 0;
                    if (response.getUsage() != null && response.getUsage().getTotalTokens() != null) {
                        totalTokens = response.getUsage().getTotalTokens();
                    }
                    log.info("DeepSeek response received successfully:");
                    log.info("Response ID: {}", response.getId());
                    log.info("Model: {}", response.getModel());
                    log.info("Choices count: {}", response.getChoices() != null ? response.getChoices().size() : 0);
                    log.info("Total tokens: {}", totalTokens);
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        var choice = response.getChoices().get(0);
                        log.info("First choice content: {}", choice.getMessage() != null ? choice.getMessage().getContent() : "null");
                        log.info("Finish reason: {}", choice.getFinishReason());
                    }
                })
                .doOnError(error -> {
                    log.error("DeepSeek request failed", error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        var webError = (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        log.error("HTTP Status: {}", webError.getStatusCode());
                        log.error("Response Headers: {}", webError.getHeaders());
                        log.error("Response body: {}", webError.getResponseBodyAsString());
                    }
                    if (error instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                        log.error("JSON parsing error: {}", error.getMessage());
                    }
                    if (error.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                        log.error("JSON parsing error in cause: {}", error.getCause().getMessage());
                    }
                });
    }

    @Override
    public Flux<ChatCompletionResponse> streamChatCompletion(ChatCompletionRequest request) {
        // 设置stream为true
        request.setStream(true);

        log.info("=== DeepSeek 流式请求开始 ===");
        log.info("DeepSeek streaming request: model={}, messages={}, stream={}", 
                request.getModel(), request.getMessages().size(), request.getStream());

        return getWebClient()
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnNext(line -> {
                    log.debug("原始流式数据行: '{}'", line);
                    log.debug("行长度: {}, 是否以'data: '开头: {}, 是否等于'data: [DONE]': {}", 
                             line.length(), line.startsWith("data: "), line.equals("data: [DONE]"));
                })
                .filter(line -> {
                    // 修复：调整过滤逻辑，更详细地检查数据格式
                    if (line.trim().isEmpty()) {
                        log.debug("跳过空行");
                        return false;
                    }
                    
                    if (line.startsWith("data: ")) {
                        if (line.equals("data: [DONE]")) {
                            log.debug("收到完成标记: '{}'", line);
                            return false;
                        } else {
                            log.debug("有效的SSE数据行: '{}'", line);
                            return true;
                        }
                    } else {
                        // 检查是否是直接的JSON数据（没有SSE前缀）
                        String trimmed = line.trim();
                        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                            log.debug("检测到直接JSON格式（非SSE）: '{}'", line);
                            return true;
                        } else {
                            log.debug("过滤掉无效行: '{}'", line);
                            return false;
                        }
                    }
                })
                .map(line -> {
                    String content;
                    if (line.startsWith("data: ")) {
                        content = line.substring(6); // 移除 "data: " 前缀
                        log.debug("从SSE格式提取的JSON内容: '{}'", content);
                    } else {
                        content = line; // 直接使用JSON数据
                        log.debug("直接使用的JSON内容: '{}'", content);
                    }
                    return content;
                })
                .flatMap(this::parseStreamResponse)
                .doOnNext(response -> {
                    log.debug("成功解析的流式响应: {}", response);
                    // 重新启用内容提取日志，确保能看到实际内容
                    String content = extractContentFromStreamResponse(response);
                    log.info("=== 流式响应内容: '{}' ===", content);
                })
                .doOnError(error -> {
                    log.error("DeepSeek streaming request failed", error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        var webError = (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        log.error("HTTP Status: {}", webError.getStatusCode());
                        log.error("Response body: {}", webError.getResponseBodyAsString());
                    }
                })
                .doOnComplete(() -> log.info("=== DeepSeek 流式响应完成 ==="));
    }

    @Override
    public Mono<Integer> countTokens(String text, String model) {
        // 简化实现：大致估算token数量
        // 实际实现应该使用tiktoken或类似库
        return Mono.fromCallable(() -> {
            // 简单估算：1 token ≈ 4 字符（英文）
            int estimatedTokens = (int) Math.ceil(text.length() / 4.0);
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
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build()
                .get()
                .uri("/models")
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
        // 重置WebClient以应用新配置
        this.webClient = null;
    }

    private List<AIModel> parseModelsResponse(String response) {
        try {
            // 简化解析，实际应该解析完整的OpenAI models响应
            return getDefaultModels();
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI models response", e);
            return getDefaultModels();
        }
    }

    private List<AIModel> getDefaultModels() {
        return List.of(
                new AIModel(
                        "deepseek-chat",
                        "DeepSeek Chat",
                        "DeepSeek's advanced chat model",
                        32768,
                        0.0014,
                        0.0028,
                        true,
                        false,
                        List.of("chat", "completion")
                ),
                new AIModel(
                        "deepseek-reasoner",
                        "DeepSeek Reasoner",
                        "DeepSeek's reasoning model",
                        32768,
                        0.0055,
                        0.0055,
                        true,
                        false,
                        List.of("chat", "completion", "reasoning")
                ),
                new AIModel(
                        "gpt-4",
                        "GPT-4",
                        "Most capable GPT-4 model",
                        8192,
                        0.03,
                        0.06,
                        true,
                        false,
                        List.of("chat", "completion", "reasoning")
                ),
                new AIModel(
                        "gpt-3.5-turbo",
                        "GPT-3.5 Turbo",
                        "Fast and efficient GPT-3.5 model",
                        16385,
                        0.001,
                        0.002,
                        true,
                        false,
                        List.of("chat", "completion")
                )
        );
    }

    private AIModel getDefaultModelInfo(String modelName) {
        return new AIModel(
                modelName,
                modelName,
                "OpenAI model",
                4096,
                0.01,
                0.03,
                true,
                false,
                List.of("chat", "completion")
        );
    }

    private Mono<ChatCompletionResponse> parseStreamResponse(String jsonLine) {
        try {
            if (jsonLine.trim().isEmpty()) {
                log.debug("跳过空行");
                return Mono.empty();
            }
            
            log.debug("正在解析JSON: '{}'", jsonLine);
            ChatCompletionResponse response = objectMapper.readValue(jsonLine, ChatCompletionResponse.class);
            log.debug("JSON解析成功: {}", response);
            return Mono.just(response);
        } catch (Exception e) {
            log.error("流式响应解析失败 - JSON: '{}', Error: {}", jsonLine, e.getMessage());
            // 对于流式响应，解析失败时返回空而不是错误，避免中断整个流
            return Mono.empty();
        }
    }

    private String extractContentFromStreamResponse(ChatCompletionResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatCompletionResponse.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                return choice.getDelta().getContent();
            }
        }
        return "";
    }
}