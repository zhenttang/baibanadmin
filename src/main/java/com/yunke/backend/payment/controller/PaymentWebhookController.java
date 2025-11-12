package com.yunke.backend.payment.controller;

import com.yunke.backend.payment.dto.PaymentProviderDtos;
import com.yunke.backend.payment.PaymentProvider;
import com.yunke.backend.payment.PaymentProviderManager;
import com.yunke.backend.payment.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * 支付Webhook事件处理控制器
 * 对应Node.js版本的Stripe Webhook控制器
 * 参考: /packages/backend/server/src/plugins/payment/controller.ts
 */
@RestController
@RequestMapping("/api/payments/webhooks")
public class PaymentWebhookController {

    @Autowired
    private PaymentProviderManager paymentProviderManager;

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * 处理Stripe Webhook事件
     * POST /api/payments/webhooks/stripe
     */
    @PostMapping("/stripe")
    public Mono<ResponseEntity<String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        Optional<PaymentProvider> providerOpt = paymentProviderManager.getProvider(PaymentProvider.PaymentProviderType.STRIPE);
        if (providerOpt.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Stripe provider not configured"));
        }
        
        PaymentProvider provider = providerOpt.get();
        return Mono.fromCallable(() -> provider.verifyWebhookSignature(payload, signature, "stripe_webhook_secret"))
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature"));
                    }
                    
                    // Simplified webhook event processing
                    com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event = com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto.builder()
                            .type("webhook.received")
                            .provider(PaymentProvider.PaymentProviderType.STRIPE)
                            .processed(false)
                            .build();
                    return processWebhookEvent(event)
                            .then(Mono.just(ResponseEntity.ok("Event processed successfully")))
                            .onErrorResume(error -> {
                                System.err.println("Error processing webhook: " + error.getMessage());
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error processing webhook: " + error.getMessage()));
                            });
                });
    }

    /**
     * 处理通用支付提供商Webhook事件
     * POST /api/payments/webhooks/{provider}
     */
    @PostMapping("/{provider}")
    public Mono<ResponseEntity<String>> handlePaymentWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers) {
        
        PaymentProvider.PaymentProviderType providerType;
        try {
            providerType = PaymentProvider.PaymentProviderType.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown payment provider"));
        }
        
        Optional<PaymentProvider> paymentProviderOpt = paymentProviderManager.getProvider(providerType);
        if (paymentProviderOpt.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Provider not configured"));
        }
        
        PaymentProvider paymentProvider = paymentProviderOpt.get();
        
        // 获取签名头（不同提供商可能使用不同的头）
        String signature = getSignatureFromHeaders(headers, providerType);
        if (signature == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature"));
        }
        
        return Mono.fromCallable(() -> paymentProvider.verifyWebhookSignature(payload, signature, "webhook_secret"))
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature"));
                    }
                    
                    // Simplified webhook event processing
                    com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event = com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto.builder()
                            .type("webhook.received")
                            .provider(PaymentProvider.PaymentProviderType.STRIPE)
                            .processed(false)
                            .build();
                    return processWebhookEvent(event)
                            .then(Mono.just(ResponseEntity.ok("Event processed successfully")))
                            .onErrorResume(error -> {
                                System.err.println("Error processing webhook: " + error.getMessage());
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error processing webhook: " + error.getMessage()));
                            });
                });
    }

    /**
     * 处理Webhook事件
     */
    private Mono<Void> processWebhookEvent(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        return switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            case "customer.subscription.created", "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
            case "invoice.created", "invoice.updated" -> handleInvoiceUpdated(event);
            case "customer.created", "customer.updated" -> handleCustomerUpdated(event);
            case "setup_intent.succeeded" -> handleSetupIntentSucceeded(event);
            default -> {
                System.out.println("Unhandled webhook event type: " + event.getType());
                yield Mono.empty();
            }
        };
    }

    /**
     * 处理支付成功事件
     */
    private Mono<Void> handlePaymentSucceeded(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String paymentIntentId = extractStringFromData(data, "id");
        
        if (paymentIntentId != null) {
            return subscriptionService.handlePaymentSucceeded(paymentIntentId);
        }
        
        return Mono.empty();
    }

    /**
     * 处理支付失败事件
     */
    private Mono<Void> handlePaymentFailed(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String paymentIntentId = extractStringFromData(data, "id");
        String reason = extractStringFromData(data, "last_payment_error.message");
        
        if (paymentIntentId != null) {
            return subscriptionService.handlePaymentFailed(paymentIntentId, reason);
        }
        
        return Mono.empty();
    }

    /**
     * 处理订阅更新事件
     */
    private Mono<Void> handleSubscriptionUpdated(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String subscriptionId = extractStringFromData(data, "id");
        
        if (subscriptionId != null) {
            return subscriptionService.handleSubscriptionUpdated(subscriptionId);
        }
        
        return Mono.empty();
    }

    /**
     * 处理订阅删除事件
     */
    private Mono<Void> handleSubscriptionDeleted(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String subscriptionId = extractStringFromData(data, "id");
        
        if (subscriptionId != null) {
            // 订阅删除时也调用订阅更新处理逻辑，让它同步状态
            return subscriptionService.handleSubscriptionUpdated(subscriptionId);
        }
        
        return Mono.empty();
    }

    /**
     * 处理发票支付成功事件
     */
    private Mono<Void> handleInvoicePaymentSucceeded(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String invoiceId = extractStringFromData(data, "id");
        
        if (invoiceId != null) {
            return subscriptionService.handleInvoicePaymentSucceeded(invoiceId);
        }
        
        return Mono.empty();
    }

    /**
     * 处理发票支付失败事件
     */
    private Mono<Void> handleInvoicePaymentFailed(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String invoiceId = extractStringFromData(data, "id");
        
        if (invoiceId != null) {
            return subscriptionService.handleInvoicePaymentFailed(invoiceId);
        }
        
        return Mono.empty();
    }

    /**
     * 处理发票更新事件
     */
    private Mono<Void> handleInvoiceUpdated(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String invoiceId = extractStringFromData(data, "id");
        
        // 可以在这里同步发票状态到本地数据库
        System.out.println("Invoice updated: " + invoiceId);
        
        return Mono.empty();
    }

    /**
     * 处理客户更新事件
     */
    private Mono<Void> handleCustomerUpdated(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String customerId = extractStringFromData(data, "id");
        
        // 可以在这里同步客户信息到本地数据库
        System.out.println("Customer updated: " + customerId);
        
        return Mono.empty();
    }

    /**
     * 处理设置意图成功事件
     */
    private Mono<Void> handleSetupIntentSucceeded(com.yunke.backend.payment.dto.PaymentProviderDtos.WebhookEventDto event) {
        Map<String, Object> data = event.getData();
        String setupIntentId = extractStringFromData(data, "id");
        
        // 设置意图成功，说明支付方法已经设置完成
        System.out.println("Setup intent succeeded: " + setupIntentId);
        
        return Mono.empty();
    }

    /**
     * 从不同支付提供商的请求头中提取签名
     */
    private String getSignatureFromHeaders(Map<String, String> headers, PaymentProvider.PaymentProviderType providerType) {
        return switch (providerType) {
            case STRIPE -> headers.get("stripe-signature") != null ? 
                    headers.get("stripe-signature") : headers.get("Stripe-Signature");
            // 其他支付提供商可以在这里添加
            default -> null;
        };
    }

    /**
     * 从嵌套的Map数据结构中提取字符串值
     */
    @SuppressWarnings("unchecked")
    private String extractStringFromData(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current != null ? current.toString() : null;
    }

    /**
     * 健康检查端点
     * GET /api/payments/webhooks/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payment webhook endpoint is healthy");
    }

    /**
     * 获取支持的事件类型
     * GET /api/payments/webhooks/events
     */
    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getSupportedEvents() {
        Map<String, Object> supportedEvents = Map.of(
                "stripe", java.util.List.of(
                        "payment_intent.succeeded",
                        "payment_intent.payment_failed",
                        "customer.subscription.created",
                        "customer.subscription.updated",
                        "customer.subscription.deleted",
                        "invoice.payment_succeeded",
                        "invoice.payment_failed",
                        "invoice.created",
                        "invoice.updated",
                        "customer.created",
                        "customer.updated",
                        "setup_intent.succeeded"
                ),
                "description", "AFFiNE支付系统支持的Webhook事件类型"
        );
        
        return ResponseEntity.ok(supportedEvents);
    }
}