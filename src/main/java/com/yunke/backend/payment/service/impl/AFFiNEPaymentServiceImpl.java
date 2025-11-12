package com.yunke.backend.payment.service.impl;

import com.yunke.backend.payment.client.JeepayApiClient;
import com.yunke.backend.infrastructure.config.JeepayProperties;
import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.dto.jeepay.*;
import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import com.yunke.backend.payment.exception.PaymentException;
import com.yunke.backend.payment.repository.AFFiNEPaymentOrderRepository;
import com.yunke.backend.payment.service.AFFiNEPaymentService;
import com.yunke.backend.payment.service.UserSubscriptionService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AFFiNE支付服务实现 - 替代假数据
 */
//@Service  // 暂时禁用，使用支付宝服务
@RequiredArgsConstructor
@Slf4j
public class AFFiNEPaymentServiceImpl implements AFFiNEPaymentService {
    
    private final JeepayApiClient jeepayApiClient;
    private final JeepayProperties jeepayProperties;
    private final AFFiNEPaymentOrderRepository paymentOrderRepository;
    private final UserSubscriptionService subscriptionService;
    
    /**
     * 创建支付订单 - 完全替代假数据实现
     */
    @Override
    @Transactional
    public Mono<PaymentResult> createPayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            log.info("🚀 创建真实支付订单: userId={}, amount={}, paymentMethod={}", 
                    request.getUserId(), request.getAmount(), request.getPaymentMethod());
            
            try {
                // 1. 创建AFFiNE支付订单记录
                AFFiNEPaymentOrder order = createAFFiNEOrder(request);
                
                // 2. 构建Jeepay统一下单请求
                UnifiedOrderRequest jeepayRequest = buildJeepayOrderRequest(request, order);
                
                // 3. 调用Jeepay统一下单接口
                ApiResponse<UnifiedOrderResponse> response = jeepayApiClient.unifiedOrder(jeepayRequest);
                
                if (!response.isSuccess()) {
                    log.error("❌ Jeepay下单失败: code={}, msg={}", response.getCode(), response.getMsg());
                    throw new PaymentException("支付下单失败: " + response.getMsg());
                }
                
                UnifiedOrderResponse orderResult = response.getData();
                
                // 4. 更新订单信息
                updateOrderWithJeepayResponse(order, orderResult);
                paymentOrderRepository.save(order);
                
                // 5. 构建返回结果
                PaymentResult result = buildPaymentResult(order, orderResult);
                
                log.info("✅ 真实支付订单创建成功: orderId={}, jeepayOrderId={}", 
                        order.getId(), orderResult.getPayOrderId());
                
                return result;
                
            } catch (PaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("💥 支付订单创建失败", e);
                throw new PaymentException("支付服务暂时不可用，请稍后重试");
            }
        });
    }
    
    /**
     * 查询支付状态 - 真实查询Jeepay状态
     */
    @Override
    public Mono<PaymentStatus> queryPaymentStatus(String orderId) {
        return Mono.fromCallable(() -> {
            try {
                // 1. 查找本地订单
                Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
                if (orderOpt.isEmpty()) {
                    log.warn("⚠️ 订单不存在: orderId={}", orderId);
                    return PaymentStatus.UNKNOWN;
                }
                
                AFFiNEPaymentOrder order = orderOpt.get();
                
                // 2. 如果订单已完成，直接返回状态
                if (order.getStatus() == PaymentStatus.SUCCESS || 
                    order.getStatus() == PaymentStatus.FAILED ||
                    order.getStatus() == PaymentStatus.CANCELLED) {
                    return order.getStatus();
                }
                
                // 3. 调用Jeepay查询接口
                PayOrderQueryRequest queryRequest = PayOrderQueryRequest.builder()
                    .mchOrderNo(order.getId())
                    .build();
                    
                ApiResponse<PayOrderQueryResponse> response = jeepayApiClient.payOrderQuery(queryRequest);
                
                if (!response.isSuccess()) {
                    log.error("❌ Jeepay订单查询失败: {}", response.getMsg());
                    return PaymentStatus.UNKNOWN;
                }
                
                PayOrderQueryResponse result = response.getData();
                PaymentStatus newStatus = PaymentStatus.fromJeepayState(result.getState());
                
                // 4. 更新本地订单状态
                if (order.getStatus() != newStatus) {
                    order.setStatus(newStatus);
                    if (newStatus == PaymentStatus.SUCCESS) {
                        order.setCompletedAt(LocalDateTime.now());
                    }
                    paymentOrderRepository.save(order);
                    
                    // 5. 处理支付成功的业务逻辑
                    if (newStatus == PaymentStatus.SUCCESS) {
                        handlePaymentSuccess(order);
                    }
                    
                    log.info("🔄 订单状态更新: orderId={}, status={}", orderId, newStatus);
                }
                
                return newStatus;
                
            } catch (Exception e) {
                log.error("💥 查询支付状态失败: orderId={}", orderId, e);
                return PaymentStatus.UNKNOWN;
            }
        });
    }
    
    /**
     * 处理支付回调 - 真实的Jeepay回调处理
     */
    @Override
    @Transactional
    public Mono<Void> handlePaymentCallback(String notifyData, String signature) {
        return Mono.fromRunnable(() -> {
            try {
                log.info("🔔 收到支付回调: {}", notifyData);
                
                // 1. 验证签名
                if (!jeepayApiClient.verifyNotifySignature(notifyData, signature)) {
                    log.error("❌ 支付回调签名验证失败");
                    throw new PaymentException("Invalid signature");
                }
                
                // 2. 解析回调数据
                JSONObject notifyObj = JSON.parseObject(notifyData);
                String mchOrderNo = notifyObj.getString("mchOrderNo");
                Integer state = notifyObj.getInteger("state");
                String payOrderId = notifyObj.getString("payOrderId");
                
                // 3. 查找对应的订单
                Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(mchOrderNo);
                if (orderOpt.isEmpty()) {
                    log.error("❌ 未找到对应订单: mchOrderNo={}", mchOrderNo);
                    return;
                }
                
                AFFiNEPaymentOrder order = orderOpt.get();
                PaymentStatus newStatus = PaymentStatus.fromJeepayState(state);
                
                // 4. 更新订单状态
                if (order.getStatus() != newStatus) {
                    PaymentStatus oldStatus = order.getStatus();
                    order.setStatus(newStatus);
                    order.setJeepayOrderNo(payOrderId);
                    
                    if (newStatus == PaymentStatus.SUCCESS) {
                        order.setCompletedAt(LocalDateTime.now());
                    }
                    
                    paymentOrderRepository.save(order);
                    
                    log.info("🔄 支付回调更新订单状态: orderId={}, {} -> {}", 
                            order.getId(), oldStatus, newStatus);
                    
                    // 5. 处理业务逻辑
                    if (newStatus == PaymentStatus.SUCCESS) {
                        handlePaymentSuccess(order);
                    }
                }
                
            } catch (PaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("💥 处理支付回调失败", e);
                throw new PaymentException("Failed to process payment callback");
            }
        });
    }
    
    /**
     * 取消支付订单
     */
    @Override
    @Transactional
    public Mono<Boolean> cancelPayment(String orderId, String userId) {
        return Mono.fromCallable(() -> {
            try {
                Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
                if (orderOpt.isEmpty()) {
                    return false;
                }
                
                AFFiNEPaymentOrder order = orderOpt.get();
                
                // 验证订单归属
                if (!order.getUserId().equals(userId)) {
                    log.warn("⚠️ 用户无权取消订单: orderId={}, userId={}", orderId, userId);
                    return false;
                }
                
                // 只能取消待支付或支付中的订单
                if (order.getStatus() != PaymentStatus.PENDING && 
                    order.getStatus() != PaymentStatus.PROCESSING) {
                    log.warn("⚠️ 订单状态不允许取消: orderId={}, status={}", orderId, order.getStatus());
                    return false;
                }
                
                // 调用Jeepay关闭订单接口
                CloseOrderRequest closeRequest = CloseOrderRequest.builder()
                    .mchOrderNo(orderId)
                    .build();
                    
                ApiResponse<Void> response = jeepayApiClient.closeOrder(closeRequest);
                
                if (response.isSuccess()) {
                    order.setStatus(PaymentStatus.CANCELLED);
                    paymentOrderRepository.save(order);
                    
                    log.info("✅ 订单取消成功: orderId={}", orderId);
                    return true;
                } else {
                    log.error("❌ Jeepay取消订单失败: {}", response.getMsg());
                    return false;
                }
                
            } catch (Exception e) {
                log.error("💥 取消支付订单失败: orderId={}", orderId, e);
                return false;
            }
        });
    }
    
    /**
     * 退款处理
     */
    @Override
    @Transactional
    public Mono<Boolean> refundPayment(String orderId, String userId, String reason) {
        return Mono.fromCallable(() -> {
            try {
                Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
                if (orderOpt.isEmpty()) {
                    return false;
                }
                
                AFFiNEPaymentOrder order = orderOpt.get();
                
                // 验证订单归属
                if (!order.getUserId().equals(userId)) {
                    log.warn("⚠️ 用户无权退款订单: orderId={}, userId={}", orderId, userId);
                    return false;
                }
                
                // 只能退款成功的订单
                if (order.getStatus() != PaymentStatus.SUCCESS) {
                    log.warn("⚠️ 订单状态不允许退款: orderId={}, status={}", orderId, order.getStatus());
                    return false;
                }
                
                // 调用Jeepay退款接口
                RefundOrderRequest refundRequest = RefundOrderRequest.builder()
                    .mchOrderNo(orderId)
                    .mchRefundNo(UUID.randomUUID().toString().replace("-", ""))
                    .refundAmount(order.getAmount())
                    .refundReason(reason)
                    .build();
                    
                ApiResponse<RefundOrderResponse> response = jeepayApiClient.refundOrder(refundRequest);
                
                if (response.isSuccess()) {
                    order.setStatus(PaymentStatus.REFUNDED);
                    paymentOrderRepository.save(order);
                    
                    // 处理退款后的业务逻辑（如取消订阅）
                    handlePaymentRefund(order);
                    
                    log.info("✅ 订单退款成功: orderId={}", orderId);
                    return true;
                } else {
                    log.error("❌ Jeepay退款失败: {}", response.getMsg());
                    return false;
                }
                
            } catch (Exception e) {
                log.error("💥 退款处理失败: orderId={}", orderId, e);
                return false;
            }
        });
    }
    
    /**
     * 获取用户支付订单列表
     */
    @Override
    public Mono<List<AFFiNEPaymentOrder>> getUserPaymentOrders(String userId, int page, int size) {
        return Mono.fromCallable(() -> {
            return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)
            ).getContent();
        });
    }
    
    /**
     * 验证订单归属
     */
    @Override
    public Mono<Boolean> isOrderOwnedByUser(String orderId, String userId) {
        return Mono.fromCallable(() -> {
            Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
            return orderOpt.map(order -> order.getUserId().equals(userId)).orElse(false);
        });
    }
    
    /**
     * 清理过期订单
     */
    @Override
    @Transactional
    public Mono<Integer> cleanupExpiredOrders() {
        return Mono.fromCallable(() -> {
            LocalDateTime expireTime = LocalDateTime.now();
            List<AFFiNEPaymentOrder> expiredOrders = paymentOrderRepository
                .findExpiredOrders(PaymentStatus.PENDING, expireTime);
                
            int count = 0;
            for (AFFiNEPaymentOrder order : expiredOrders) {
                order.setStatus(PaymentStatus.CANCELLED);
                paymentOrderRepository.save(order);
                count++;
            }
            
            if (count > 0) {
                log.info("🧹 清理过期订单: count={}", count);
            }
            
            return count;
        });
    }
    
    // ==================== 私有方法 ====================
    
    private AFFiNEPaymentOrder createAFFiNEOrder(PaymentRequest request) {
        return AFFiNEPaymentOrder.builder()
            .id(UUID.randomUUID().toString().replace("-", ""))
            .userId(request.getUserId())
            .workspaceId(request.getWorkspaceId())
            .planType(request.getPlanType())
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod())
            .subject(request.getSubject())
            .description(request.getDescription())
            .status(PaymentStatus.PENDING)
            .expireTime(LocalDateTime.now().plusMinutes(30)) // 30分钟过期
            .build();
    }
    
    private UnifiedOrderRequest buildJeepayOrderRequest(PaymentRequest request, AFFiNEPaymentOrder order) {
        return UnifiedOrderRequest.builder()
            .mchOrderNo(order.getId())
            .wayCode(mapPaymentMethod(request.getPaymentMethod()))
            .amount(request.getAmount())
            .currency("CNY")
            .subject(request.getSubject())
            .body(request.getDescription())
            .clientIp(request.getClientIp() != null ? request.getClientIp() : "127.0.0.1")
            .notifyUrl(jeepayProperties.getNotifyUrl())
            .returnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : jeepayProperties.getReturnUrl())
            .expiredTime(System.currentTimeMillis() + 30 * 60 * 1000L) // 30分钟过期
            .extParam(buildExtParam(request))
            .build();
    }
    
    private void updateOrderWithJeepayResponse(AFFiNEPaymentOrder order, UnifiedOrderResponse response) {
        order.setJeepayOrderNo(response.getPayOrderId());
        order.setStatus(PaymentStatus.fromJeepayState(response.getOrderState()));
        order.setPayDataType(response.getPayDataType());
        order.setPayData(response.getPayData());
        order.setPayUrl(response.getPayUrl());
        
        // 提取二维码URL
        if ("qrCode".equals(response.getPayDataType()) && response.getPayData() != null) {
            order.setQrCodeUrl(response.getPayData());
        }
    }
    
    private PaymentResult buildPaymentResult(AFFiNEPaymentOrder order, UnifiedOrderResponse response) {
        return PaymentResult.builder()
            .success(true)
            .orderId(order.getId())
            .jeepayOrderId(response.getPayOrderId())
            .payDataType(response.getPayDataType())
            .payData(response.getPayData())
            .payUrl(response.getPayUrl())
            .qrCodeUrl(order.getQrCodeUrl())
            .amount(order.getAmount())
            .expireTime(order.getExpireTime())
            .status(order.getStatus())
            .message("支付订单创建成功")
            .build();
    }
    
    private String mapPaymentMethod(String paymentMethod) {
        // 映射前端支付方式到Jeepay支付方式代码
        return switch (paymentMethod.toLowerCase()) {
            case "alipay" -> "alipay";
            case "wxpay" -> "wxpay";
            case "unionpay" -> "unionpay";
            default -> "alipay"; // 默认支付宝
        };
    }
    
    private String buildExtParam(PaymentRequest request) {
        JSONObject extParam = new JSONObject();
        extParam.put("userId", request.getUserId());
        extParam.put("planType", request.getPlanType());
        if (request.getWorkspaceId() != null) {
            extParam.put("workspaceId", request.getWorkspaceId());
        }
        return extParam.toJSONString();
    }
    
    /**
     * 处理支付成功的业务逻辑
     */
    private void handlePaymentSuccess(AFFiNEPaymentOrder order) {
        try {
            // 激活用户订阅
            subscriptionService.activateSubscription(
                order.getUserId(),
                order.getPlanType(),
                order.getAmount()
            );
            
            log.info("🎉 支付成功业务处理完成: orderId={}, userId={}, planType={}", 
                    order.getId(), order.getUserId(), order.getPlanType());
                    
        } catch (Exception e) {
            log.error("💥 支付成功业务处理失败: orderId={}", order.getId(), e);
        }
    }
    
    /**
     * 处理退款后的业务逻辑
     */
    private void handlePaymentRefund(AFFiNEPaymentOrder order) {
        try {
            // 取消用户订阅
            subscriptionService.cancelSubscription(order.getUserId(), "Payment refunded");
            
            log.info("🔄 退款业务处理完成: orderId={}, userId={}", order.getId(), order.getUserId());
            
        } catch (Exception e) {
            log.error("💥 退款业务处理失败: orderId={}", order.getId(), e);
        }
    }
}