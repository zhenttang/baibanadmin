package com.yunke.backend.system.service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Webhook服务接口
 */
public interface WebhookService {

    /**
     * 注册Webhook
     */
    Webhook registerWebhook(String url, List<String> events, String secret);

    /**
     * 删除Webhook
     */
    void deleteWebhook(String id);

    /**
     * 获取所有Webhook
     */
    List<Webhook> getAllWebhooks();

    /**
     * 触发Webhook事件
     */
    Mono<Void> triggerEvent(String event, Object payload);

    /**
     * 验证Webhook签名
     */
    boolean verifySignature(String payload, String signature, String secret);

    /**
     * Webhook记录
     */
    record Webhook(
            String id,
            String url,
            List<String> events,
            String secret,
            boolean active,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}

    /**
     * Webhook事件
     */
    record WebhookEvent(
            String id,
            String event,
            Object payload,
            java.time.Instant timestamp,
            Map<String, String> headers
    ) {}
}

