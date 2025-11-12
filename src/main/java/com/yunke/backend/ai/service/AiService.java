package com.yunke.backend.ai.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 */
public interface AiService {

    /**
     * 文本补全
     */
    Mono<String> complete(String prompt, AiProvider provider, String model);

    /**
     * 流式文本补全
     */
    Flux<String> completeStream(String prompt, AiProvider provider, String model);

    /**
     * 聊天对话
     */
    Mono<String> chat(List<ChatMessage> messages, AiProvider provider, String model);

    /**
     * 流式聊天对话
     */
    Flux<String> chatStream(List<ChatMessage> messages, AiProvider provider, String model);

    /**
     * 文档总结
     */
    Mono<String> summarizeDocument(String content, String language);

    /**
     * 文档翻译
     */
    Mono<String> translateDocument(String content, String sourceLanguage, String targetLanguage);

    /**
     * 代码生成
     */
    Mono<String> generateCode(String description, String language);

    /**
     * 智能搜索
     */
    Mono<List<SearchResult>> smartSearch(String query, String workspaceId, String userId);

    /**
     * 获取AI会话历史
     */
    Mono<List<AiSession>> getSessionHistory(String userId);

    /**
     * 创建AI会话
     */
    Mono<AiSession> createSession(String userId, String title, AiProvider provider);

    /**
     * 删除AI会话
     */
    Mono<Void> deleteSession(String sessionId);

    /**
     * AI提供者枚举
     */
    enum AiProvider {
        OPENAI,
        ANTHROPIC,
        GOOGLE
    }

    /**
     * 聊天消息
     */
    record ChatMessage(
            String role,
            String content,
            java.time.Instant timestamp
    ) {}

    /**
     * AI会话
     */
    record AiSession(
            String id,
            String userId,
            String title,
            AiProvider provider,
            String model,
            List<ChatMessage> messages,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}

    /**
     * 搜索结果
     */
    record SearchResult(
            String id,
            String title,
            String content,
            String type,
            double score,
            Map<String, Object> metadata
    ) {}
}