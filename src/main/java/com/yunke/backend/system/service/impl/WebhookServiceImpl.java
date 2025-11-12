package com.yunke.backend.system.service.impl;

import com.yunke.backend.system.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Webhook服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    // 内存中的Webhook缓存
    private final Map<String, Webhook> webhookCache = new ConcurrentHashMap<>();

    private static final String WEBHOOK_KEY_PREFIX = "webhook:";
    private static final String WEBHOOK_LIST_KEY = "webhooks:all";

    @Override
    public Webhook registerWebhook(String url, List<String> events, String secret) {
        log.info("Registering webhook: {} for events: {}", url, events);
        
        String id = UUID.randomUUID().toString();
        Webhook webhook = new Webhook(
                id,
                url,
                events,
                secret,
                true,
                Instant.now(),
                Instant.now()
        );
        
        try {
            // 保存到Redis
            String key = WEBHOOK_KEY_PREFIX + id;
            String webhookJson = objectMapper.writeValueAsString(webhook);
            redisTemplate.opsForValue().set(key, webhookJson, Duration.ofDays(365));
            
            // 添加到列表
            redisTemplate.opsForSet().add(WEBHOOK_LIST_KEY, id);
            
            // 更新缓存
            webhookCache.put(id, webhook);
            
            log.info("Webhook registered successfully: {}", id);
            return webhook;
        } catch (Exception e) {
            log.error("Failed to register webhook", e);
            throw new RuntimeException("Failed to register webhook", e);
        }
    }

    @Override
    public void deleteWebhook(String id) {
        log.info("Deleting webhook: {}", id);
        
        try {
            // 从Redis删除
            String key = WEBHOOK_KEY_PREFIX + id;
            redisTemplate.delete(key);
            redisTemplate.opsForSet().remove(WEBHOOK_LIST_KEY, id);
            
            // 从缓存删除
            webhookCache.remove(id);
            
            log.info("Webhook deleted successfully: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete webhook", e);
            throw new RuntimeException("Failed to delete webhook", e);
        }
    }

    @Override
    public List<Webhook> getAllWebhooks() {
        log.debug("Getting all webhooks");
        
        try {
            // 如果缓存为空，从Redis加载
            if (webhookCache.isEmpty()) {
                loadWebhooksFromRedis();
            }
            
            return new ArrayList<>(webhookCache.values());
        } catch (Exception e) {
            log.error("Failed to get all webhooks", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Mono<Void> triggerEvent(String event, Object payload) {
        log.debug("Triggering webhook event: {}", event);
        
        return Mono.fromCallable(() -> getAllWebhooks())
                .flatMapMany(Flux::fromIterable)
                .filter(webhook -> webhook.active() && webhook.events().contains(event))
                .flatMap(webhook -> sendWebhookEvent(webhook, event, payload))
                .then()
                .doOnSuccess(v -> log.debug("Webhook event triggered successfully: {}", event))
                .doOnError(e -> log.error("Failed to trigger webhook event: {}", event, e));
    }

    @Override
    public boolean verifySignature(String payload, String signature, String secret) {
        try {
            String expectedSignature = calculateSignature(payload, secret);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    /**
     * 发送Webhook事件
     */
    private Mono<Void> sendWebhookEvent(Webhook webhook, String event, Object payload) {
        return Mono.fromCallable(() -> {
            try {
                // 创建事件对象
                WebhookEvent webhookEvent = new WebhookEvent(
                        UUID.randomUUID().toString(),
                        event,
                        payload,
                        Instant.now(),
                        Map.of("User-Agent", "AFFiNE-Webhook/1.0")
                );
                
                // 序列化payload
                String payloadJson = objectMapper.writeValueAsString(webhookEvent);
                
                // 计算签名
                String signature = calculateSignature(payloadJson, webhook.secret());
                
                // 发送HTTP请求
                WebClient webClient = webClientBuilder.build();
                
                return webClient.post()
                        .uri(webhook.url())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("X-AFFiNE-Event", event)
                        .header("X-AFFiNE-Signature", signature)
                        .header("X-AFFiNE-Delivery", webhookEvent.id())
                        .bodyValue(payloadJson)
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(30))
                        .doOnSuccess(response -> log.debug("Webhook sent successfully: {} -> {}", 
                                webhook.url(), response.getStatusCode()))
                        .doOnError(error -> log.warn("Webhook failed: {} -> {}", 
                                webhook.url(), error.getMessage()))
                        .then();
            } catch (Exception e) {
                return Mono.error(e);
            }
        }).flatMap(mono -> (Mono<Void>) mono);
    }

    /**
     * 计算HMAC-SHA256签名
     */
    private String calculateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + Base64.getEncoder().encodeToString(signature);
    }

    /**
     * 从Redis加载Webhook
     */
    private void loadWebhooksFromRedis() {
        try {
            Set<Object> webhookIds = redisTemplate.opsForSet().members(WEBHOOK_LIST_KEY);
            if (webhookIds == null) {
                return;
            }
            
            for (Object webhookId : webhookIds) {
                String key = WEBHOOK_KEY_PREFIX + webhookId.toString();
                Object webhookJson = redisTemplate.opsForValue().get(key);
                
                if (webhookJson != null) {
                    Webhook webhook = objectMapper.readValue(webhookJson.toString(), Webhook.class);
                    webhookCache.put(webhook.id(), webhook);
                }
            }
            
            log.info("Loaded {} webhooks from Redis", webhookCache.size());
        } catch (Exception e) {
            log.error("Failed to load webhooks from Redis", e);
        }
    }
}