package com.yunke.backend.payment.service.impl;

import com.yunke.backend.infrastructure.config.JeepaySDKProperties;
import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import com.yunke.backend.payment.exception.PaymentException;
import com.yunke.backend.payment.repository.AFFiNEPaymentOrderRepository;
import com.yunke.backend.payment.service.AFFiNEPaymentService;
import com.yunke.backend.payment.service.UserSubscriptionService;
import com.jeequan.jeepay.JeepayClient;
import com.jeequan.jeepay.exception.JeepayException;
import com.jeequan.jeepay.model.PayOrderCreateReqModel;
import com.jeequan.jeepay.model.PayOrderQueryReqModel;
import com.jeequan.jeepay.model.RefundOrderCreateReqModel;
import com.jeequan.jeepay.request.PayOrderCreateRequest;
import com.jeequan.jeepay.request.PayOrderQueryRequest;
import com.jeequan.jeepay.request.RefundOrderCreateRequest;
import com.jeequan.jeepay.response.PayOrderCreateResponse;
import com.jeequan.jeepay.response.PayOrderQueryResponse;
import com.jeequan.jeepay.response.RefundOrderCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AFFiNE支付服务实现 - 使用Jeepay官方SDK
 */
//@Service  // 暂时禁用，使用支付宝服务
@RequiredArgsConstructor
@Slf4j
public class AFFiNEPaymentSDKServiceImpl implements AFFiNEPaymentService {
    
    private final JeepaySDKProperties jeepaySDKProperties;
    private final AFFiNEPaymentOrderRepository paymentOrderRepository;
    private final UserSubscriptionService subscriptionService;
    
    /**
     * 创建支付订单 - 使用官方SDK
     */
    @Override
    @Transactional
    public Mono<PaymentResult> createPayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            log.info("🚀 使用Jeepay官方SDK创建支付订单: userId={}, amount={}, paymentMethod={}", 
                    request.getUserId(), request.getAmount(), request.getPaymentMethod());
            
            try {
                // 1. 创建AFFiNE支付订单记录
                AFFiNEPaymentOrder order = createAFFiNEOrder(request);
                paymentOrderRepository.save(order);
                
                // 2. 创建Jeepay客户端 - 使用官方SDK
                JeepayClient jeepayClient = JeepayClient.getInstance(
                    jeepaySDKProperties.getAppId(), 
                    jeepaySDKProperties.getApiKey(),
                    jeepaySDKProperties.getApiUrl()
                );
                
                // 3. 构建支付请求 - 参考官方示例
                PayOrderCreateRequest payRequest = new PayOrderCreateRequest();
                PayOrderCreateReqModel model = new PayOrderCreateReqModel();
                
                model.setMchNo(jeepaySDKProperties.getMchNo());
                model.setAppId(jeepayClient.getAppId());
                model.setMchOrderNo(order.getId());
                model.setWayCode(mapPaymentMethod(request.getPaymentMethod()));
                model.setAmount(request.getAmount());
                model.setCurrency("CNY");
                model.setSubject(request.getSubject());
                model.setBody(request.getDescription());
                model.setClientIp(request.getClientIp() != null ? request.getClientIp() : "127.0.0.1");
                model.setNotifyUrl(jeepaySDKProperties.getNotifyUrl());
                model.setReturnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : jeepaySDKProperties.getReturnUrl());
                model.setChannelExtra(buildChannelExtra(request.getPaymentMethod()));
                model.setExtParam(buildExtParam(request));
                
                payRequest.setBizModel(model);
                
                // 4. 发起统一下单
                PayOrderCreateResponse response = jeepayClient.execute(payRequest);
                
                // 5. 验证返回数据签名
                if (!response.checkSign(jeepaySDKProperties.getApiKey())) {
                    log.error("❌ Jeepay SDK签名验证失败");
                    throw new PaymentException("支付签名验证失败");
                }
                
                // 6. 判断下单是否成功
                if (!response.isSuccess(jeepaySDKProperties.getApiKey())) {
                    log.error("❌ Jeepay SDK下单失败: errCode={}, errMsg={}", 
                        response.get().getErrCode(), response.get().getErrMsg());
                    throw new PaymentException("支付下单失败: " + response.get().getErrMsg());
                }
                
                // 7. 更新订单信息
                updateOrderWithSDKResponse(order, response);
                paymentOrderRepository.save(order);
                
                // 8. 构建返回结果
                PaymentResult result = buildPaymentResult(order, response);
                
                log.info("✅ Jeepay SDK支付订单创建成功: orderId={}, payOrderId={}", 
                        order.getId(), response.get().getPayOrderId());
                
                return result;
                
            } catch (JeepayException e) {
                log.error("💥 Jeepay SDK异常", e);
                throw new PaymentException("支付服务异常: " + e.getMessage());
            } catch (Exception e) {
                log.error("💥 支付订单创建失败", e);
                throw new PaymentException("支付服务暂时不可用，请稍后重试");
            }
        });
    }
    
    /**
     * 查询支付状态 - 使用官方SDK
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
                
                // 3. 创建Jeepay客户端查询订单
                JeepayClient jeepayClient = JeepayClient.getInstance(
                    jeepaySDKProperties.getAppId(), 
                    jeepaySDKProperties.getApiKey(),
                    jeepaySDKProperties.getApiUrl()
                );
                
                PayOrderQueryRequest queryRequest = new PayOrderQueryRequest();
                PayOrderQueryReqModel model = new PayOrderQueryReqModel();
                model.setMchNo(jeepaySDKProperties.getMchNo());
                model.setAppId(jeepayClient.getAppId());
                model.setMchOrderNo(orderId);
                queryRequest.setBizModel(model);
                
                PayOrderQueryResponse response = jeepayClient.execute(queryRequest);
                
                // 4. 验证签名
                if (!response.checkSign(jeepaySDKProperties.getApiKey())) {
                    log.error("❌ Jeepay SDK查询签名验证失败");
                    return PaymentStatus.UNKNOWN;
                }
                
                if (!response.isSuccess(jeepaySDKProperties.getApiKey())) {
                    log.error("❌ Jeepay SDK订单查询失败: {}", response.getMsg());
                    return PaymentStatus.UNKNOWN;
                }
                
                PaymentStatus newStatus = PaymentStatus.fromJeepayState(response.get().getState());
                
                // 5. 更新本地订单状态
                if (order.getStatus() != newStatus) {
                    order.setStatus(newStatus);
                    if (newStatus == PaymentStatus.SUCCESS) {
                        order.setCompletedAt(LocalDateTime.now());
                    }
                    paymentOrderRepository.save(order);
                    
                    // 6. 处理支付成功的业务逻辑
                    if (newStatus == PaymentStatus.SUCCESS) {
                        handlePaymentSuccess(order);
                    }
                    
                    log.info("🔄 订单状态更新: orderId={}, status={}", orderId, newStatus);
                }
                
                return newStatus;
                
            } catch (JeepayException e) {
                log.error("💥 Jeepay SDK查询异常: orderId={}", orderId, e);
                return PaymentStatus.UNKNOWN;
            } catch (Exception e) {
                log.error("💥 查询支付状态失败: orderId={}", orderId, e);
                return PaymentStatus.UNKNOWN;
            }
        });
    }
    
    /**
     * 处理支付回调 - 简化实现
     */
    @Override
    @Transactional
    public Mono<Void> handlePaymentCallback(String notifyData, String signature) {
        return Mono.fromRunnable(() -> {
            try {
                log.info("🔔 收到支付回调: {}", notifyData);
                
                // TODO: 实现真实的回调处理逻辑
                // 1. 验证签名
                // 2. 解析回调数据
                // 3. 更新订单状态
                // 4. 处理业务逻辑
                
                log.info("✅ 支付回调处理完成");
                
            } catch (Exception e) {
                log.error("💥 处理支付回调失败", e);
                throw new PaymentException("Failed to process payment callback");
            }
        });
    }
    
    /**
     * 退款处理 - 使用官方SDK
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
                
                // 创建Jeepay客户端
                JeepayClient jeepayClient = JeepayClient.getInstance(
                    jeepaySDKProperties.getAppId(), 
                    jeepaySDKProperties.getApiKey(),
                    jeepaySDKProperties.getApiUrl()
                );
                
                // 构建退款请求 - 参考官方示例
                RefundOrderCreateRequest refundRequest = new RefundOrderCreateRequest();
                RefundOrderCreateReqModel model = new RefundOrderCreateReqModel();
                
                model.setMchNo(jeepaySDKProperties.getMchNo());
                model.setAppId(jeepaySDKProperties.getAppId());
                model.setMchOrderNo(orderId);
                model.setMchRefundNo("mrf" + new Date().getTime());
                model.setRefundAmount(order.getAmount());
                model.setCurrency("CNY");
                model.setClientIp("127.0.0.1");
                model.setRefundReason(reason);
                model.setNotifyUrl(jeepaySDKProperties.getNotifyUrl());
                model.setChannelExtra("");
                model.setExtParam("");
                
                refundRequest.setBizModel(model);
                
                RefundOrderCreateResponse response = jeepayClient.execute(refundRequest);
                
                // 验证返回数据签名
                if (!response.checkSign(jeepaySDKProperties.getApiKey())) {
                    log.error("❌ Jeepay SDK退款签名验证失败");
                    return false;
                }
                
                if (response.isSuccess(jeepaySDKProperties.getApiKey())) {
                    order.setStatus(PaymentStatus.REFUNDED);
                    paymentOrderRepository.save(order);
                    
                    // 处理退款后的业务逻辑
                    handlePaymentRefund(order);
                    
                    log.info("✅ Jeepay SDK退款成功: orderId={}, refundOrderId={}", 
                        orderId, response.get().getRefundOrderId());
                    return true;
                } else {
                    log.error("❌ Jeepay SDK退款失败: errCode={}, errMsg={}", 
                        response.get().getErrCode(), response.get().getErrMsg());
                    return false;
                }
                
            } catch (JeepayException e) {
                log.error("💥 Jeepay SDK退款异常: orderId={}", orderId, e);
                return false;
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
                
                // 只能取消待支付或支付中的订单
                if (order.getStatus() != PaymentStatus.PENDING && 
                    order.getStatus() != PaymentStatus.PROCESSING) {
                    log.warn("⚠️ 订单状态不允许取消: orderId={}, status={}", orderId, order.getStatus());
                    return false;
                }
                
                // 直接更新本地状态为取消
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
    
    private void updateOrderWithSDKResponse(AFFiNEPaymentOrder order, PayOrderCreateResponse response) {
        order.setJeepayOrderNo(response.get().getPayOrderId());
        order.setStatus(PaymentStatus.fromJeepayState(response.get().getOrderState()));
        order.setPayDataType(response.get().getPayDataType());
        order.setPayData(response.get().getPayData());
        // 注意：Jeepay SDK的PayOrderCreateResModel没有getPayUrl()方法
        // order.setPayUrl(generatePaymentPageUrl(order.getId()));
        
        // 提取二维码URL
        if ("qrCode".equals(response.get().getPayDataType()) && response.get().getPayData() != null) {
            order.setQrCodeUrl(response.get().getPayData());
        }
    }
    
    private PaymentResult buildPaymentResult(AFFiNEPaymentOrder order, PayOrderCreateResponse response) {
        return PaymentResult.builder()
            .success(true)
            .orderId(order.getId())
            .jeepayOrderId(response.get().getPayOrderId())
            .payDataType(response.get().getPayDataType())
            .payData(response.get().getPayData())
            .payUrl(generatePaymentPageUrl(order.getId())) // 生成自定义支付页面URL
            .qrCodeUrl(order.getQrCodeUrl())
            .amount(order.getAmount())
            .expireTime(order.getExpireTime())
            .status(order.getStatus())
            .message("支付订单创建成功")
            .build();
    }
    
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
    
    private String mapPaymentMethod(String paymentMethod) {
        // 映射前端支付方式到Jeepay支付方式代码
        return switch (paymentMethod.toLowerCase()) {
            case "alipay" -> "ALI_QR";        // 支付宝二维码
            case "wxpay" -> "WX_NATIVE";      // 微信Native支付
            case "unionpay" -> "UP_QR";       // 银联二维码
            default -> "ALI_QR";             // 默认支付宝
        };
    }
    
    private String buildChannelExtra(String paymentMethod) {
        // 根据支付方式构建渠道扩展参数
        return switch (paymentMethod.toLowerCase()) {
            case "wxpay" -> "{\"payDataType\":\"codeImgUrl\"}";
            case "alipay" -> "{\"payDataType\":\"codeImgUrl\"}";
            default -> "";
        };
    }
    
    private String buildExtParam(PaymentRequest request) {
        return String.format("{\"userId\":\"%s\",\"planType\":\"%s\"}", 
            request.getUserId(), request.getPlanType());
    }
    
    /**
     * 生成支付页面URL
     */
    private String generatePaymentPageUrl(String orderId) {
        return "http://172.24.48.1:8080/payment/jeepay/page/" + orderId;
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