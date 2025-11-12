package com.yunke.backend.payment;

import com.yunke.backend.payment.dto.payment.CancelSubscriptionInput;
import com.yunke.backend.payment.dto.payment.CheckoutSessionDto;
import com.yunke.backend.payment.dto.payment.ConfirmPaymentIntentInput;
import com.yunke.backend.payment.dto.payment.CreateCheckoutSessionInput;
import com.yunke.backend.payment.dto.payment.CreateCustomerInput;
import com.yunke.backend.payment.dto.payment.CreateInvoiceInput;
import com.yunke.backend.payment.dto.payment.CreatePaymentIntentInput;
import com.yunke.backend.payment.dto.payment.CreateRefundInput;
import com.yunke.backend.payment.dto.payment.CreateSubscriptionInput;
import com.yunke.backend.payment.dto.payment.CustomerDto;
import com.yunke.backend.payment.dto.payment.CustomerPortalSessionDto;
import com.yunke.backend.payment.dto.payment.CreateCustomerPortalSessionInput;
import com.yunke.backend.payment.dto.payment.InvoiceDto;
import com.yunke.backend.payment.dto.payment.PaymentIntentDto;
import com.yunke.backend.payment.dto.payment.PriceDto;
import com.yunke.backend.payment.dto.payment.ProductDto;
import com.yunke.backend.payment.dto.payment.RefundDto;
import com.yunke.backend.payment.dto.SubscriptionDto;
import com.yunke.backend.payment.dto.payment.TaxCalculationDto;
import com.yunke.backend.payment.dto.payment.TaxCalculationInput;
import com.yunke.backend.payment.dto.payment.UpdateCustomerInput;
import com.yunke.backend.payment.dto.payment.UpdateSubscriptionInput;
import com.yunke.backend.payment.dto.payment.WebhookEventDto;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 支付提供商接口
 * 支持多个支付提供商的统一抽象
 */
public interface PaymentProvider {

    /**
     * 获取提供商类型
     */
    PaymentProviderType getProviderType();

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 检查提供商是否可用
     */
    Mono<Boolean> isAvailable();

    /**
     * 创建客户
     */
    Mono<CustomerDto> createCustomer(CreateCustomerInput input);

    /**
     * 获取客户信息
     */
    Mono<CustomerDto> getCustomer(String customerId);

    /**
     * 更新客户信息
     */
    Mono<CustomerDto> updateCustomer(String customerId, UpdateCustomerInput input);

    /**
     * 创建订阅
     */
    Mono<SubscriptionDto> createSubscription(CreateSubscriptionInput input);

    /**
     * 获取订阅信息
     */
    Mono<SubscriptionDto> getSubscription(String subscriptionId);

    /**
     * 更新订阅
     */
    Mono<SubscriptionDto> updateSubscription(String subscriptionId, UpdateSubscriptionInput input);

    /**
     * 取消订阅
     */
    Mono<SubscriptionDto> cancelSubscription(String subscriptionId, CancelSubscriptionInput input);

    /**
     * 创建支付意图
     */
    Mono<PaymentIntentDto> createPaymentIntent(CreatePaymentIntentInput input);

    /**
     * 确认支付意图
     */
    Mono<PaymentIntentDto> confirmPaymentIntent(String paymentIntentId, ConfirmPaymentIntentInput input);

    /**
     * 获取支付意图
     */
    Mono<PaymentIntentDto> getPaymentIntent(String paymentIntentId);

    /**
     * 创建结账会话
     */
    Mono<CheckoutSessionDto> createCheckoutSession(CreateCheckoutSessionInput input);

    /**
     * 获取结账会话
     */
    Mono<CheckoutSessionDto> getCheckoutSession(String sessionId);

    /**
     * 创建客户门户会话
     */
    Mono<CustomerPortalSessionDto> createCustomerPortalSession(CreateCustomerPortalSessionInput input);

    /**
     * 获取价格信息
     */
    Mono<List<PriceDto>> getPrices();

    /**
     * 获取产品信息
     */
    Mono<List<ProductDto>> getProducts();

    /**
     * 创建发票
     */
    Mono<InvoiceDto> createInvoice(CreateInvoiceInput input);

    /**
     * 发送发票
     */
    Mono<InvoiceDto> sendInvoice(String invoiceId);

    /**
     * 支付发票
     */
    Mono<InvoiceDto> payInvoice(String invoiceId);

    /**
     * 创建退款
     */
    Mono<RefundDto> createRefund(CreateRefundInput input);

    /**
     * 获取退款信息
     */
    Mono<RefundDto> getRefund(String refundId);

    /**
     * 验证Webhook签名
     */
    boolean verifyWebhookSignature(String payload, String signature, String secret);

    /**
     * 处理Webhook事件
     */
    Mono<WebhookEventDto> processWebhookEvent(String eventType, Map<String, Object> eventData);

    /**
     * 获取支持的支付方式
     */
    List<PaymentMethodType> getSupportedPaymentMethods();

    /**
     * 计算税费
     */
    Mono<TaxCalculationDto> calculateTax(TaxCalculationInput input);

    enum PaymentProviderType {
        STRIPE,
        PAYPAL,
        ALIPAY,
        WECHAT_PAY
    }

    enum PaymentMethodType {
        CARD,
        BANK_TRANSFER,
        DIGITAL_WALLET,
        CRYPTO
    }
}