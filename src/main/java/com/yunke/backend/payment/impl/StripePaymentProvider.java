package com.yunke.backend.payment.impl;

// 使用全限定类名导入，避免引用不明确问题
import com.yunke.backend.payment.dto.payment.CancelSubscriptionInput;
import com.yunke.backend.payment.dto.payment.CheckoutSessionDto;
import com.yunke.backend.payment.dto.payment.ConfirmPaymentIntentInput;
import com.yunke.backend.payment.dto.payment.CreateCheckoutSessionInput;
import com.yunke.backend.payment.dto.payment.CreateCustomerInput;
import com.yunke.backend.payment.dto.payment.CreateCustomerPortalSessionInput;
import com.yunke.backend.payment.dto.payment.CreateInvoiceInput;
import com.yunke.backend.payment.dto.payment.CreatePaymentIntentInput;
import com.yunke.backend.payment.dto.payment.CreateRefundInput;
import com.yunke.backend.payment.dto.payment.CreateSubscriptionInput;
import com.yunke.backend.payment.dto.payment.CustomerDto;
import com.yunke.backend.payment.dto.payment.CustomerPortalSessionDto;
import com.yunke.backend.payment.dto.payment.InvoiceDto;
import com.yunke.backend.payment.dto.payment.PaymentIntentDto;
import com.yunke.backend.payment.dto.payment.PriceDto;
import com.yunke.backend.payment.dto.payment.ProductDto;
import com.yunke.backend.payment.dto.payment.RefundDto;
import com.yunke.backend.payment.dto.SubscriptionDto;
import com.yunke.backend.payment.domain.entity.Subscription;
import com.yunke.backend.payment.dto.payment.TaxCalculationDto;
import com.yunke.backend.payment.dto.payment.TaxCalculationInput;
import com.yunke.backend.payment.dto.payment.UpdateCustomerInput;
import com.yunke.backend.payment.dto.payment.UpdateSubscriptionInput;

import com.yunke.backend.payment.dto.payment.WebhookEventDto;
import com.yunke.backend.payment.PaymentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
// import com.stripe.model.TaxCalculation;
// import com.stripe.model.TaxCalculationLineItem;
// import com.stripe.model.TaxCalculationParameters;
// import com.stripe.model.CustomerDetails;
import com.stripe.model.Address;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Stripe支付提供商实现
 * 对应Node.js版本的StripeProvider
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    // 自定义替代类，因为找不到原始Stripe类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxCalculation {
        private String id;
        private Long amount;
        private String currency;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxCalculationLineItem {
        private String id;
        private Long amount;
        private Double taxRate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxCalculationParameters {
        private String currency;
        private List<Object> lineItems;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetails {
        private String id;
        private String email;
        private Address address;
    }

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${payment.stripe.secret-key:}")
    private String secretKey;

    @Value("${payment.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${payment.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${payment.stripe.api-version:2023-10-16}")
    private String apiVersion;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl("https://api.stripe.com/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .defaultHeader("Stripe-Version", apiVersion)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();
        }
        return webClient;
    }

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public String getProviderName() {
        return "Stripe";
    }

    @Override
    public Mono<Boolean> isAvailable() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            return Mono.just(false);
        }

        return getWebClient()
                .get()
                .uri("/account")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> true)
                .onErrorReturn(false);
    }

    @Override
    public Mono<CustomerDto> createCustomer(CreateCustomerInput input) {
        log.debug("Creating Stripe customer: email={}", input.getEmail());

        String formData = buildFormData(Map.of(
                "email", input.getEmail(),
                "name", input.getName() != null ? input.getName() : "",
                "description", input.getDescription() != null ? input.getDescription() : ""
        ));

        return getWebClient()
                .post()
                .uri("/customers")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(this::parseCustomerResponse)
                .doOnSuccess(customer -> log.debug("Stripe customer created: {}", customer.getId()))
                .doOnError(error -> log.error("Failed to create Stripe customer", error));
    }

    @Override
    public Mono<CustomerDto> getCustomer(String customerId) {
        return getWebClient()
                .get()
                .uri("/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseCustomerResponse)
                .onErrorReturn(CustomerDto.builder().build());
    }

    @Override
    public Mono<CustomerDto> updateCustomer(String customerId, UpdateCustomerInput input) {
        String formData = buildFormData(Map.of(
                "email", input.getEmail() != null ? input.getEmail() : "",
                "name", input.getName() != null ? input.getName() : ""
        ));

        return getWebClient()
                .post()
                .uri("/customers/{id}", customerId)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseCustomerResponse);
    }

    @Override
    public Mono<SubscriptionDto> createSubscription(CreateSubscriptionInput input) {
        log.debug("Creating Stripe subscription: customer={}, priceId={}", 
                input.getCustomerId(), input.getPriceId());

        String formData = buildFormData(Map.of(
                "customer", input.getCustomerId(),
                "items[0][price]", input.getPriceId(),
                "payment_behavior", "default_incomplete",
                "payment_settings[save_default_payment_method]", "on_subscription",
                "expand[0]", "latest_invoice.payment_intent"
        ));

        return getWebClient()
                .post()
                .uri("/subscriptions")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(this::parseSubscriptionResponse)
                .doOnSuccess(sub -> log.debug("Stripe subscription created: {}", sub.getId()))
                .doOnError(error -> log.error("Failed to create Stripe subscription", error));
    }

    @Override
    public Mono<SubscriptionDto> getSubscription(String subscriptionId) {
        return getWebClient()
                .get()
                .uri("/subscriptions/{id}?expand[]=latest_invoice.payment_intent", subscriptionId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSubscriptionResponse);
    }

    @Override
    public Mono<SubscriptionDto> updateSubscription(String subscriptionId, UpdateSubscriptionInput input) {
        String formData = buildFormData(Map.of(
                "cancel_at_period_end", String.valueOf(input.getCancelAtPeriodEnd()),
                "proration_behavior", "create_prorations"
        ));

        return getWebClient()
                .post()
                .uri("/subscriptions/{id}", subscriptionId)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSubscriptionResponse);
    }

    @Override
    public Mono<SubscriptionDto> cancelSubscription(String subscriptionId, CancelSubscriptionInput input) {
        String formData = buildFormData(Map.of(
                "cancel_at_period_end", String.valueOf(input.getCancelAtPeriodEnd()),
                "cancellation_details[comment]", input.getReason() != null ? input.getReason() : ""
        ));

        return getWebClient()
                .delete()
                .uri("/subscriptions/{id}", subscriptionId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSubscriptionResponse);
    }

    @Override
    public Mono<PaymentIntentDto> createPaymentIntent(CreatePaymentIntentInput input) {
        String formData = buildFormData(Map.of(
                "amount", String.valueOf(input.getAmount().multiply(new java.math.BigDecimal("100")).intValue()),
                "currency", input.getCurrency(),
                "customer", input.getCustomerId() != null ? input.getCustomerId() : "",
                "description", input.getDescription() != null ? input.getDescription() : ""
        ));

        return getWebClient()
                .post()
                .uri("/payment_intents")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePaymentIntentResponse);
    }

    @Override
    public Mono<PaymentIntentDto> confirmPaymentIntent(String paymentIntentId, ConfirmPaymentIntentInput input) {
        String formData = buildFormData(Map.of(
                "payment_method", input.getPaymentMethodId() != null ? input.getPaymentMethodId() : "",
                "return_url", input.getReturnUrl() != null ? input.getReturnUrl() : ""
        ));

        return getWebClient()
                .post()
                .uri("/payment_intents/{id}/confirm", paymentIntentId)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePaymentIntentResponse);
    }

    @Override
    public Mono<PaymentIntentDto> getPaymentIntent(String paymentIntentId) {
        return getWebClient()
                .get()
                .uri("/payment_intents/{id}", paymentIntentId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePaymentIntentResponse);
    }

    @Override
    public Mono<CheckoutSessionDto> createCheckoutSession(CreateCheckoutSessionInput input) {
        String formData = buildFormData(Map.of(
                "customer", input.getCustomerId(),
                "mode", input.getMode(),
                "success_url", input.getSuccessUrl(),
                "cancel_url", input.getCancelUrl(),
                "line_items[0][price]", input.getPriceId(),
                "line_items[0][quantity]", "1"
        ));

        return getWebClient()
                .post()
                .uri("/checkout/sessions")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseCheckoutSessionResponse);
    }

    @Override
    public Mono<CheckoutSessionDto> getCheckoutSession(String sessionId) {
        return getWebClient()
                .get()
                .uri("/checkout/sessions/{id}", sessionId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseCheckoutSessionResponse);
    }

    @Override
    public Mono<CustomerPortalSessionDto> createCustomerPortalSession(CreateCustomerPortalSessionInput input) {
        String formData = buildFormData(Map.of(
                "customer", input.getCustomerId(),
                "return_url", input.getReturnUrl()
        ));

        return getWebClient()
                .post()
                .uri("/billing_portal/sessions")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseCustomerPortalSessionResponse);
    }

    @Override
    public Mono<List<PriceDto>> getPrices() {
        return getWebClient()
                .get()
                .uri("/prices?active=true&limit=100")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePricesResponse);
    }

    @Override
    public Mono<List<ProductDto>> getProducts() {
        return getWebClient()
                .get()
                .uri("/products?active=true&limit=100")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseProductsResponse);
    }

    @Override
    public Mono<InvoiceDto> createInvoice(CreateInvoiceInput input) {
        String formData = buildFormData(Map.of(
                "customer", input.getCustomerId(),
                "currency", input.getCurrency(),
                "description", input.getDescription() != null ? input.getDescription() : ""
        ));

        return getWebClient()
                .post()
                .uri("/invoices")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseInvoiceResponse);
    }

    @Override
    public Mono<InvoiceDto> sendInvoice(String invoiceId) {
        return getWebClient()
                .post()
                .uri("/invoices/{id}/send", invoiceId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseInvoiceResponse);
    }

    @Override
    public Mono<InvoiceDto> payInvoice(String invoiceId) {
        return getWebClient()
                .post()
                .uri("/invoices/{id}/pay", invoiceId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseInvoiceResponse);
    }

    @Override
    public Mono<RefundDto> createRefund(CreateRefundInput input) {
        String formData = buildFormData(Map.of(
                "payment_intent", input.getPaymentIntentId(),
                "amount", String.valueOf(input.getAmount().multiply(new java.math.BigDecimal("100")).intValue()),
                "reason", input.getReason() != null ? input.getReason() : "requested_by_customer"
        ));

        return getWebClient()
                .post()
                .uri("/refunds")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseRefundResponse);
    }

    @Override
    public Mono<RefundDto> getRefund(String refundId) {
        return getWebClient()
                .get()
                .uri("/refunds/{id}", refundId)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseRefundResponse);
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            String actualSecret = secret != null ? secret : webhookSecret;
            if (actualSecret == null || actualSecret.trim().isEmpty()) {
                log.warn("Webhook secret not configured");
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(actualSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            
            return computedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    @Override
    public Mono<WebhookEventDto> processWebhookEvent(String eventType, Map<String, Object> eventData) {
        log.debug("Processing Stripe webhook event: {}", eventType);
        
        return Mono.fromCallable(() -> {
            WebhookEventDto event = WebhookEventDto.builder()
                    .type(eventType)
                    .data(eventData)
                    .provider(getProviderType())
                    .processed(true)
                    .build();
            
            // 根据事件类型处理不同的逻辑
            switch (eventType) {
                case "invoice.payment_succeeded":
                case "invoice.payment_failed":
                case "customer.subscription.created":
                case "customer.subscription.updated":
                case "customer.subscription.deleted":
                    log.info("Processed Stripe webhook event: {}", eventType);
                    break;
                default:
                    log.debug("Unhandled Stripe webhook event: {}", eventType);
            }
            
            return event;
        });
    }

    @Override
    public List<PaymentMethodType> getSupportedPaymentMethods() {
        return List.of(
                PaymentMethodType.CARD,
                PaymentMethodType.BANK_TRANSFER,
                PaymentMethodType.DIGITAL_WALLET
        );
    }

    /**
     * 计算税费
     */
    @Override
    public Mono<TaxCalculationDto> calculateTax(TaxCalculationInput input) {
        // 简化实现，实际应该调用Stripe Tax API
        return Mono.just(TaxCalculationDto.builder()
                .amount(input.getAmount()) // 直接使用Long类型
                .taxAmount(0L) // 使用0L代替BigDecimal.ZERO
                .currency(input.getCurrency())
                .countryCode(input.getCountryCode())
                .build());
    }

    // 辅助方法
    private String buildFormData(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> entry.getKey() + "=" + java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
    }

    private CustomerDto parseCustomerResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            return CustomerDto.builder()
                    .id((String) data.get("id"))
                    .email((String) data.get("email"))
                    .name((String) data.get("name"))
                    .description((String) data.get("description"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse customer response", e);
            return CustomerDto.builder().build();
        }
    }

    private SubscriptionDto parseSubscriptionResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            String statusStr = (String) data.get("status");
            Subscription.SubscriptionStatus status = convertSubscriptionStatus(statusStr);
            
            return SubscriptionDto.builder()
                    .id((String) data.get("id"))
                    .stripeSubscriptionId((String) data.get("id"))
                    .status(status)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse subscription response", e);
            return SubscriptionDto.builder().build();
        }
    }

    /**
     * 将 Stripe 的状态字符串转换为 SubscriptionStatus 枚举
     */
    private Subscription.SubscriptionStatus convertSubscriptionStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return Subscription.SubscriptionStatus.INCOMPLETE;
        }
        
        // 将 Stripe 的状态值转换为枚举
        // Stripe 使用下划线命名，如 "incomplete", "incomplete_expired", "trialing", "active", "past_due", "canceled", "unpaid", "paused"
        try {
            // 先尝试直接匹配枚举名称（大写）
            String enumName = statusStr.toUpperCase().replace("-", "_");
            return Subscription.SubscriptionStatus.valueOf(enumName);
        } catch (IllegalArgumentException e) {
            // 如果直接匹配失败，尝试通过值匹配
            for (Subscription.SubscriptionStatus status : Subscription.SubscriptionStatus.values()) {
                if (status.getValue().equalsIgnoreCase(statusStr)) {
                    return status;
                }
            }
            // 默认返回 INCOMPLETE
            log.warn("Unknown subscription status: {}, defaulting to INCOMPLETE", statusStr);
            return Subscription.SubscriptionStatus.INCOMPLETE;
        }
    }

    private PaymentIntentDto parsePaymentIntentResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            return PaymentIntentDto.builder()
                    .id((String) data.get("id"))
                    .clientSecret((String) data.get("client_secret"))
                    .status((String) data.get("status"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse payment intent response", e);
            return PaymentIntentDto.builder().build();
        }
    }

    private CheckoutSessionDto parseCheckoutSessionResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            return CheckoutSessionDto.builder()
                    .id((String) data.get("id"))
                    .url((String) data.get("url"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse checkout session response", e);
            return CheckoutSessionDto.builder().build();
        }
    }

    private CustomerPortalSessionDto parseCustomerPortalSessionResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            return CustomerPortalSessionDto.builder()
                    .url((String) data.get("url"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse customer portal session response", e);
            return CustomerPortalSessionDto.builder().build();
        }
    }

    private List<PriceDto> parsePricesResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> prices = (List<Map<String, Object>>) data.get("data");
            return prices.stream()
                    .map(price -> PriceDto.builder()
                            .id((String) price.get("id"))
                            .currency((String) price.get("currency"))
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse prices response", e);
            return List.of();
        }
    }

    private List<ProductDto> parseProductsResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("data");
            return products.stream()
                    .map(product -> ProductDto.builder()
                            .id((String) product.get("id"))
                            .name((String) product.get("name"))
                            .description((String) product.get("description"))
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse products response", e);
            return List.of();
        }
    }

    private InvoiceDto parseInvoiceResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            return InvoiceDto.builder()
                    .id((String) data.get("id"))
                    .status((String) data.get("status"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse invoice response", e);
            return InvoiceDto.builder().build();
        }
    }

    private RefundDto parseRefundResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, Map.class);
            return RefundDto.builder()
                    .id((String) data.get("id"))
                    .status((String) data.get("status"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse refund response", e);
            return RefundDto.builder().build();
        }
    }
}