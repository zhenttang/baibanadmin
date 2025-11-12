package com.yunke.backend.ai.service.impl;

import com.yunke.backend.infrastructure.config.AffineConfig;
import com.yunke.backend.ai.service.AiService;

import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.security.constants.PermissionActions;
import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.monitor.MetricsCollector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final AffineConfig affineConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final WorkspaceDocService docService;
    private final PermissionService permissionService;
    private final MetricsCollector metricsCollector;

    @Override
    public Mono<String> complete(String prompt, AiProvider provider, String model) {
        log.debug("AI completion request: provider={}, model={}", provider, model);
        
        return Mono.fromCallable(() -> {
            if (!affineConfig.getCopilot().isEnabled()) {
                throw new RuntimeException("AI service is disabled");
            }
            return provider;
        })
        .flatMap(p -> {
            switch (p) {
                case OPENAI:
                    return completeWithOpenAI(prompt, model);
                case ANTHROPIC:
                    return completeWithAnthropic(prompt, model);
                case GOOGLE:
                    return completeWithGoogle(prompt, model);
                default:
                    return Mono.error(new IllegalArgumentException("Unsupported AI provider: " + p));
            }
        })
        .doOnSuccess(result -> metricsCollector.recordAiOperation("complete", provider.name(), model))
        .doOnError(error -> log.error("AI completion failed", error));
    }

    @Override
    public Flux<String> completeStream(String prompt, AiProvider provider, String model) {
        log.debug("AI streaming completion request: provider={}, model={}", provider, model);
        
        return Mono.fromCallable(() -> {
            if (!affineConfig.getCopilot().isEnabled()) {
                throw new RuntimeException("AI service is disabled");
            }
            return provider;
        })
        .flatMapMany(p -> {
            switch (p) {
                case OPENAI:
                    return completeStreamWithOpenAI(prompt, model);
                case ANTHROPIC:
                    return completeStreamWithAnthropic(prompt, model);
                case GOOGLE:
                    return completeStreamWithGoogle(prompt, model);
                default:
                    return Flux.error(new IllegalArgumentException("Unsupported AI provider: " + p));
            }
        })
        .doOnComplete(() -> metricsCollector.recordAiOperation("complete_stream", provider.name(), model))
        .doOnError(error -> log.error("AI streaming completion failed", error));
    }

    @Override
    public Mono<String> chat(List<ChatMessage> messages, AiProvider provider, String model) {
        log.debug("AI chat request: provider={}, model={}, messages={}", provider, model, messages.size());
        
        return Mono.fromCallable(() -> {
            if (!affineConfig.getCopilot().isEnabled()) {
                throw new RuntimeException("AI service is disabled");
            }
            return provider;
        })
        .flatMap(p -> {
            switch (p) {
                case OPENAI:
                    return chatWithOpenAI(messages, model);
                case ANTHROPIC:
                    return chatWithAnthropic(messages, model);
                case GOOGLE:
                    return chatWithGoogle(messages, model);
                default:
                    return Mono.error(new IllegalArgumentException("Unsupported AI provider: " + p));
            }
        })
        .doOnSuccess(result -> metricsCollector.recordAiOperation("chat", provider.name(), model))
        .doOnError(error -> log.error("AI chat failed", error));
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages, AiProvider provider, String model) {
        log.debug("AI streaming chat request: provider={}, model={}, messages={}", provider, model, messages.size());
        
        return Mono.fromCallable(() -> {
            if (!affineConfig.getCopilot().isEnabled()) {
                throw new RuntimeException("AI service is disabled");
            }
            return provider;
        })
        .flatMapMany(p -> {
            switch (p) {
                case OPENAI:
                    return chatStreamWithOpenAI(messages, model);
                case ANTHROPIC:
                    return chatStreamWithAnthropic(messages, model);
                case GOOGLE:
                    return chatStreamWithGoogle(messages, model);
                default:
                    return Flux.error(new IllegalArgumentException("Unsupported AI provider: " + p));
            }
        })
        .doOnComplete(() -> metricsCollector.recordAiOperation("chat_stream", provider.name(), model))
        .doOnError(error -> log.error("AI streaming chat failed", error));
    }

    @Override
    public Mono<String> summarizeDocument(String content, String language) {
        log.debug("AI document summarization request: language={}, length={}", language, content.length());
        
        String prompt = String.format(
            "Please summarize the following document in %s. Keep it concise and highlight the main points:\n\n%s",
            language, content
        );
        
        return complete(prompt, AiProvider.OPENAI, "gpt-3.5-turbo")
                .doOnSuccess(result -> metricsCollector.recordAiOperation("summarize", "openai", "gpt-3.5-turbo"));
    }

    @Override
    public Mono<String> translateDocument(String content, String sourceLanguage, String targetLanguage) {
        log.debug("AI document translation request: {} -> {}, length={}", 
                sourceLanguage, targetLanguage, content.length());
        
        String prompt = String.format(
            "Please translate the following text from %s to %s. Maintain the original formatting and style:\n\n%s",
            sourceLanguage, targetLanguage, content
        );
        
        return complete(prompt, AiProvider.OPENAI, "gpt-3.5-turbo")
                .doOnSuccess(result -> metricsCollector.recordAiOperation("translate", "openai", "gpt-3.5-turbo"));
    }

    @Override
    public Mono<String> generateCode(String description, String language) {
        log.debug("AI code generation request: language={}, description={}", language, description);
        
        String prompt = String.format(
            "Please generate %s code based on the following description. Include comments and follow best practices:\n\n%s",
            language, description
        );
        
        return complete(prompt, AiProvider.OPENAI, "gpt-4")
                .doOnSuccess(result -> metricsCollector.recordAiOperation("generate_code", "openai", "gpt-4"));
    }

    @Override
    public Mono<List<SearchResult>> smartSearch(String query, String workspaceId, String userId) {
        log.debug("AI smart search request: query={}, workspace={}", query, workspaceId);
        
        return Mono.fromCallable(() -> {
            // 检查工作空间访问权限
            if (!permissionService.hasWorkspaceAccess(userId, workspaceId)) {
                throw new PermissionDeniedException(userId, "workspace", PermissionActions.READ);
            }
            
            // 获取工作空间文档
            return docService.getWorkspaceDocs(workspaceId);
        })
        .flatMap(docs -> {
            // 简化的搜索实现 - 基于关键词匹配
            List<SearchResult> results = docs.stream()
                    .filter(doc -> doc.getTitle().toLowerCase().contains(query.toLowerCase()))
                    .map(doc -> new SearchResult(
                            doc.getId(),
                            doc.getTitle(),
                            "", // 简化实现，不包含内容
                            "document",
                            0.8, // 固定评分
                            Map.of("workspaceId", workspaceId, "updatedAt", doc.getUpdatedAt())
                    ))
                    .collect(Collectors.toList());
            
            return Mono.just(results);
        })
        .doOnSuccess(results -> metricsCollector.recordAiOperation("smart_search", "internal", "search"));
    }

    @Override
    public Mono<List<AiSession>> getSessionHistory(String userId) {
        log.debug("Getting AI session history for user: {}", userId);
        
        String key = "ai_sessions:" + userId;
        return reactiveRedisTemplate.opsForList().range(key, 0, -1)
                .cast(String.class)
                .flatMap(sessionJson -> {
                    try {
                        AiSession session = objectMapper.readValue(sessionJson, AiSession.class);
                        return Mono.just(session);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize AI session", e);
                        return Mono.empty();
                    }
                })
                .collectList();
    }

    @Override
    public Mono<AiSession> createSession(String userId, String title, AiProvider provider) {
        log.info("Creating AI session: user={}, title={}, provider={}", userId, title, provider);
        
        AiSession session = new AiSession(
                UUID.randomUUID().toString(),
                userId,
                title,
                provider,
                getDefaultModel(provider),
                new ArrayList<>(),
                Instant.now(),
                Instant.now()
        );
        
        return Mono.fromCallable(() -> {
            try {
                String sessionJson = objectMapper.writeValueAsString(session);
                String key = "ai_sessions:" + userId;
                String sessionKey = "ai_session:" + session.id();
                
                return new SessionData(key, sessionKey, sessionJson, session);
            } catch (Exception e) {
                throw new RuntimeException("Failed to prepare AI session data", e);
            }
        })
        .flatMap(data -> 
            Mono.when(
                reactiveRedisTemplate.opsForList().leftPush(data.key, data.sessionJson),
                reactiveRedisTemplate.expire(data.key, Duration.ofDays(30)),
                reactiveRedisTemplate.opsForValue().set(data.sessionKey, data.sessionJson, Duration.ofDays(30))
            )
            .thenReturn(data.session)
        );
    }

    @Override
    public Mono<Void> deleteSession(String sessionId) {
        log.info("Deleting AI session: {}", sessionId);
        
        String sessionKey = "ai_session:" + sessionId;
        return reactiveRedisTemplate.delete(sessionKey).then();
    }

    /**
     * OpenAI文本补全
     */
    private Mono<String> completeWithOpenAI(String prompt, String model) {
        if (!affineConfig.getCopilot().getOpenai().isEnabled()) {
            return Mono.error(new RuntimeException("OpenAI is disabled"));
        }
        
        WebClient webClient = webClientBuilder
                .baseUrl(affineConfig.getCopilot().getOpenai().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + affineConfig.getCopilot().getOpenai().getApiKey())
                .build();
        
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "max_tokens", 1000,
                "temperature", 0.7
        );
        
        return webClient.post()
                .uri("/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.path("choices").get(0).path("text").asText())
                .timeout(Duration.ofSeconds(30));
    }

    /**
     * OpenAI流式文本补全
     */
    private Flux<String> completeStreamWithOpenAI(String prompt, String model) {
        // 简化实现 - 将普通补全结果分割为流
        return completeWithOpenAI(prompt, model)
                .flatMapMany(result -> {
                    String[] words = result.split(" ");
                    return Flux.fromArray(words)
                            .delayElements(Duration.ofMillis(50));
                });
    }

    /**
     * OpenAI聊天
     */
    private Mono<String> chatWithOpenAI(List<ChatMessage> messages, String model) {
        if (!affineConfig.getCopilot().getOpenai().isEnabled()) {
            return Mono.error(new RuntimeException("OpenAI is disabled"));
        }
        
        WebClient webClient = webClientBuilder
                .baseUrl(affineConfig.getCopilot().getOpenai().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + affineConfig.getCopilot().getOpenai().getApiKey())
                .build();
        
        List<Map<String, String>> chatMessages = messages.stream()
                .map(msg -> Map.of("role", msg.role(), "content", msg.content()))
                .collect(Collectors.toList());
        
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", chatMessages,
                "max_tokens", 1000,
                "temperature", 0.7
        );
        
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.path("choices").get(0).path("message").path("content").asText())
                .timeout(Duration.ofSeconds(30));
    }

    /**
     * OpenAI流式聊天
     */
    private Flux<String> chatStreamWithOpenAI(List<ChatMessage> messages, String model) {
        // 简化实现 - 将普通聊天结果分割为流
        return chatWithOpenAI(messages, model)
                .flatMapMany(result -> {
                    String[] words = result.split(" ");
                    return Flux.fromArray(words)
                            .delayElements(Duration.ofMillis(50));
                });
    }

    /**
     * Anthropic相关方法（简化实现）
     */
    private Mono<String> completeWithAnthropic(String prompt, String model) {
        return Mono.error(new RuntimeException("Anthropic integration not implemented"));
    }
    
    private Flux<String> completeStreamWithAnthropic(String prompt, String model) {
        return Flux.error(new RuntimeException("Anthropic integration not implemented"));
    }
    
    private Mono<String> chatWithAnthropic(List<ChatMessage> messages, String model) {
        return Mono.error(new RuntimeException("Anthropic integration not implemented"));
    }
    
    private Flux<String> chatStreamWithAnthropic(List<ChatMessage> messages, String model) {
        return Flux.error(new RuntimeException("Anthropic integration not implemented"));
    }

    /**
     * Google相关方法（简化实现）
     */
    private Mono<String> completeWithGoogle(String prompt, String model) {
        return Mono.error(new RuntimeException("Google integration not implemented"));
    }
    
    private Flux<String> completeStreamWithGoogle(String prompt, String model) {
        return Flux.error(new RuntimeException("Google integration not implemented"));
    }
    
    private Mono<String> chatWithGoogle(List<ChatMessage> messages, String model) {
        return Mono.error(new RuntimeException("Google integration not implemented"));
    }
    
    private Flux<String> chatStreamWithGoogle(List<ChatMessage> messages, String model) {
        return Flux.error(new RuntimeException("Google integration not implemented"));
    }

    /**
     * 获取默认模型
     */
    private String getDefaultModel(AiProvider provider) {
        switch (provider) {
            case OPENAI:
                return affineConfig.getCopilot().getOpenai().getModel();
            case ANTHROPIC:
                return affineConfig.getCopilot().getAnthropic().getModel();
            case GOOGLE:
                return affineConfig.getCopilot().getGoogle().getModel();
            default:
                return "gpt-3.5-turbo";
        }
    }
    
    /**
     * AI会话数据内部类
     */
    private record SessionData(String key, String sessionKey, String sessionJson, AiSession session) {}
}