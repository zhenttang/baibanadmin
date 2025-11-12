package com.yunke.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付提供商相关的DTO类集合
 */
public class PaymentProviderDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCustomerInput {
        private String email;
        private String name;
        private String description;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSubscriptionInput {
        private String customerId;
        private String priceId;
        private Integer quantity;
        private LocalDateTime trialEnd;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSubscriptionInput {
        private String priceId;
        private Integer quantity;
        private Boolean cancelAtPeriodEnd;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelSubscriptionInput {
        private Boolean cancelAtPeriodEnd;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePaymentIntentInput {
        private BigDecimal amount;
        private String currency;
        private String customerId;
        private String description;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmPaymentIntentInput {
        private String paymentMethodId;
        private String returnUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentIntentDto {
        private String id;
        private String clientSecret;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String customerId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCheckoutSessionInput {
        private String customerId;
        private String mode;
        private String successUrl;
        private String cancelUrl;
        private String priceId;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutSessionDto {
        private String id;
        private String url;
        private String customerId;
        private String mode;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCustomerPortalSessionInput {
        private String customerId;
        private String returnUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerPortalSessionDto {
        private String id;
        private String url;
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceDto {
        private String id;
        private String currency;
        private BigDecimal unitAmount;
        private String interval;
        private String productId;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {
        private String id;
        private String name;
        private String description;
        private Boolean active;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInvoiceInput {
        private String customerId;
        private String currency;
        private String description;
        private LocalDateTime dueDate;
        private List<InvoiceLineItem> lineItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLineItem {
        private String description;
        private BigDecimal amount;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceDto {
        private String id;
        private String customerId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime dueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRefundInput {
        private String paymentIntentId;
        private BigDecimal amount;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundDto {
        private String id;
        private String paymentIntentId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookEventDto {
        private String type;
        private Map<String, Object> data;
        private com.yunke.backend.payment.PaymentProvider.PaymentProviderType provider;
        private Boolean processed;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxCalculationInput {
        private BigDecimal amount;
        private String currency;
        private String country;
        private String state;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxCalculationDto {
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String taxRate;
    }
}