package com.yunke.backend.payment.service.impl;

import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import com.yunke.backend.payment.exception.PaymentException;
import com.yunke.backend.payment.repository.AFFiNEPaymentOrderRepository;
import com.yunke.backend.payment.service.AFFiNEPaymentService;
import com.yunke.backend.payment.service.UserSubscriptionService;
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

/**
 * AFFiNE支付服务实现 - 轻量级方案：直接对接官方支付SDK
 * 无需部署额外的支付网关服务
 */
//@Service  // 暂时禁用，使用支付宝服务
@RequiredArgsConstructor
@Slf4j
public class AFFiNEDirectPaymentServiceImpl implements AFFiNEPaymentService {
    
    private final AFFiNEPaymentOrderRepository paymentOrderRepository;
    private final UserSubscriptionService subscriptionService;
    
    /**
     * 创建支付订单 - 直接生成支付二维码URL
     */
    @Override
    @Transactional
    public Mono<PaymentResult> createPayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            log.info("🚀 创建轻量级支付订单: userId={}, amount={}, paymentMethod={}", 
                    request.getUserId(), request.getAmount(), request.getPaymentMethod());
            
            try {
                // 1. 创建AFFiNE支付订单记录
                AFFiNEPaymentOrder order = createAFFiNEOrder(request);
                paymentOrderRepository.save(order);
                
                // 2. 根据支付方式生成支付URL
                String payUrl = generatePaymentUrl(order, request.getPaymentMethod());
                
                // 3. 更新订单信息
                order.setPayUrl(payUrl);
                order.setQrCodeUrl(payUrl);
                order.setPayDataType("qrCode");
                order.setPayData(payUrl);
                paymentOrderRepository.save(order);
                
                // 4. 构建返回结果
                PaymentResult result = PaymentResult.builder()
                    .success(true)
                    .orderId(order.getId())
                    .jeepayOrderId(order.getId()) // 使用自己的订单ID
                    .payDataType("qrCode")
                    .payData(payUrl)
                    .payUrl(payUrl)
                    .qrCodeUrl(payUrl)
                    .amount(order.getAmount())
                    .expireTime(order.getExpireTime())
                    .status(order.getStatus())
                    .message("支付订单创建成功")
                    .build();
                
                log.info("✅ 轻量级支付订单创建成功: orderId={}, payUrl={}", 
                        order.getId(), payUrl);
                
                return result;
                
            } catch (Exception e) {
                log.error("💥 支付订单创建失败", e);
                throw new PaymentException("支付服务暂时不可用，请稍后重试");
            }
        });
    }
    
    /**
     * 模拟支付成功（开发测试用）
     */
    @Override
    @Transactional
    public Mono<Void> handlePaymentCallback(String notifyData, String signature) {
        return Mono.fromRunnable(() -> {
            try {
                log.info("🔔 处理支付回调（模拟）: {}", notifyData);
                
                // 简单的模拟逻辑：所有回调都当作支付成功
                // 实际项目中，这里应该验证支付宝/微信的回调签名
                
                // 解析订单ID（这里简化处理）
                String orderId = extractOrderIdFromCallback(notifyData);
                if (orderId != null) {
                    Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
                    if (orderOpt.isPresent()) {
                        AFFiNEPaymentOrder order = orderOpt.get();
                        order.setStatus(PaymentStatus.SUCCESS);
                        order.setCompletedAt(LocalDateTime.now());
                        paymentOrderRepository.save(order);
                        
                        // 处理业务逻辑
                        handlePaymentSuccess(order);
                        
                        log.info("✅ 支付回调处理完成: orderId={}", orderId);
                    }
                }
                
            } catch (Exception e) {
                log.error("💥 处理支付回调失败", e);
                throw new PaymentException("Failed to process payment callback");
            }
        });
    }
    
    /**
     * 查询支付状态 - 简化实现
     */
    @Override
    public Mono<PaymentStatus> queryPaymentStatus(String orderId) {
        return Mono.fromCallable(() -> {
            try {
                Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
                if (orderOpt.isEmpty()) {
                    log.warn("⚠️ 订单不存在: orderId={}", orderId);
                    return PaymentStatus.UNKNOWN;
                }
                
                AFFiNEPaymentOrder order = orderOpt.get();
                
                // 检查订单是否过期
                if (order.getExpireTime() != null && 
                    LocalDateTime.now().isAfter(order.getExpireTime()) &&
                    order.getStatus() == PaymentStatus.PENDING) {
                    order.setStatus(PaymentStatus.CANCELLED);
                    paymentOrderRepository.save(order);
                }
                
                return order.getStatus();
                
            } catch (Exception e) {
                log.error("💥 查询支付状态失败: orderId={}", orderId, e);
                return PaymentStatus.UNKNOWN;
            }
        });
    }
    
    /**
     * 退款处理 - 简化实现
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
                
                // 模拟退款成功
                order.setStatus(PaymentStatus.REFUNDED);
                paymentOrderRepository.save(order);
                
                // 处理退款后的业务逻辑
                handlePaymentRefund(order);
                
                log.info("✅ 退款成功: orderId={}", orderId);
                return true;
                
            } catch (Exception e) {
                log.error("💥 退款处理失败: orderId={}", orderId, e);
                return false;
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
                
                // 只能取消待支付的订单
                if (order.getStatus() != PaymentStatus.PENDING) {
                    log.warn("⚠️ 订单状态不允许取消: orderId={}, status={}", orderId, order.getStatus());
                    return false;
                }
                
                order.setStatus(PaymentStatus.CANCELLED);
                paymentOrderRepository.save(order);
                
                log.info("✅ 订单取消成功: orderId={}", orderId);
                return true;
                
            } catch (Exception e) {
                log.error("💥 取消支付订单失败: orderId={}", orderId, e);
                return false;
            }
        });
    }
    
    // ========== 以下方法保持不变 ==========
    
    @Override
    public Mono<List<AFFiNEPaymentOrder>> getUserPaymentOrders(String userId, int page, int size) {
        return Mono.fromCallable(() -> {
            return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)
            ).getContent();
        });
    }
    
    @Override
    public Mono<Boolean> isOrderOwnedByUser(String orderId, String userId) {
        return Mono.fromCallable(() -> {
            Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(orderId);
            return orderOpt.map(order -> order.getUserId().equals(userId)).orElse(false);
        });
    }
    
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
    
    /**
     * 生成支付URL - 轻量级实现
     */
    private String generatePaymentUrl(AFFiNEPaymentOrder order, String paymentMethod) {
        String baseUrl = "http://172.24.48.1:8080";
        
        return switch (paymentMethod.toLowerCase()) {
            case "alipay" -> baseUrl + "/payment/alipay/qr/" + order.getId();
            case "wxpay" -> baseUrl + "/payment/wxpay/qr/" + order.getId();
            case "unionpay" -> baseUrl + "/payment/unionpay/qr/" + order.getId();
            default -> baseUrl + "/payment/mock/qr/" + order.getId();
        };
    }
    
    /**
     * 从回调数据中提取订单ID
     */
    private String extractOrderIdFromCallback(String notifyData) {
        // 简化实现：假设回调数据包含订单ID
        // 实际项目中需要根据支付宝/微信的回调格式解析
        if (notifyData != null && notifyData.contains("orderId=")) {
            int start = notifyData.indexOf("orderId=") + 8;
            int end = notifyData.indexOf("&", start);
            if (end == -1) end = notifyData.length();
            return notifyData.substring(start, end);
        }
        return null;
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