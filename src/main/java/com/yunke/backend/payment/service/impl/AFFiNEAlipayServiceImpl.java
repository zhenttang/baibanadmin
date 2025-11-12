package com.yunke.backend.payment.service.impl;

import com.yunke.backend.infrastructure.config.AlipayProperties;
import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import com.yunke.backend.payment.exception.PaymentException;
import com.yunke.backend.payment.repository.AFFiNEPaymentOrderRepository;
import com.yunke.backend.payment.service.AFFiNEPaymentService;
import com.yunke.backend.payment.service.UserSubscriptionService;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.internal.util.AlipaySignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AFFiNE支付服务实现 - 集成支付宝官方SDK
 */
//@Service  // 通过PaymentServiceConfig配置Bean，不直接使用@Service
@RequiredArgsConstructor
@Slf4j
public class AFFiNEAlipayServiceImpl implements AFFiNEPaymentService {
    
    private final AlipayProperties alipayProperties;
    private final AFFiNEPaymentOrderRepository paymentOrderRepository;
    private final UserSubscriptionService subscriptionService;
    
    /**
     * 创建支付订单 - 支付宝统一收单线下交易预创建(生成二维码)
     */
    @Override
    @Transactional
    public Mono<PaymentResult> createPayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            log.info("🚀 创建支付宝支付订单: userId={}, amount={}, paymentMethod={}", 
                    request.getUserId(), request.getAmount(), request.getPaymentMethod());
            
            try {
                // 1. 创建AFFiNE支付订单记录
                AFFiNEPaymentOrder order = createAFFiNEOrder(request);
                paymentOrderRepository.save(order);
                
                // 2. 创建支付宝客户端
                AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(),
                    alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(),
                    alipayProperties.getFormat(),
                    alipayProperties.getCharset(),
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getSignType()
                );
                
                // 3. 构建支付宝预下单请求
                AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
                
                // 设置回调地址
                alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());
                alipayRequest.setReturnUrl(alipayProperties.getReturnUrl());
                
                // 构建请求参数
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", order.getId()); // 商户订单号
                bizContent.put("total_amount", formatAmount(request.getAmount())); // 订单总金额（元）
                bizContent.put("subject", request.getSubject()); // 订单标题
                bizContent.put("body", request.getDescription()); // 订单描述
                bizContent.put("store_id", "AFFiNE_STORE"); // 商户门店编号
                bizContent.put("timeout_express", "30m"); // 该笔订单允许的最晚付款时间
                
                alipayRequest.setBizContent(com.alibaba.fastjson2.JSON.toJSONString(bizContent));
                
                // 4. 执行请求
                AlipayTradePrecreateResponse response = alipayClient.execute(alipayRequest);
                
                if (!response.isSuccess()) {
                    log.error("❌ 支付宝预下单失败: code={}, msg={}, subCode={}, subMsg={}", 
                        response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                    throw new PaymentException("支付宝下单失败: " + response.getSubMsg());
                }
                
                // 5. 更新订单信息
                order.setJeepayOrderNo(response.getOutTradeNo()); // 商户订单号
                order.setStatus(PaymentStatus.PENDING);
                order.setPayDataType("qrCode");
                order.setPayData(response.getQrCode()); // 二维码内容
                order.setQrCodeUrl(response.getQrCode()); // 二维码URL
                paymentOrderRepository.save(order);
                
                // 6. 构建返回结果
                PaymentResult result = PaymentResult.builder()
                    .success(true)
                    .orderId(order.getId())
                    .jeepayOrderId(response.getOutTradeNo())
                    .payDataType("qrCode")
                    .payData(response.getQrCode())
                    .payUrl(generatePaymentPageUrl(order.getId()))
                    .qrCodeUrl(response.getQrCode())
                    .amount(order.getAmount())
                    .expireTime(order.getExpireTime())
                    .status(order.getStatus())
                    .message("支付宝订单创建成功")
                    .build();
                
                log.info("✅ 支付宝订单创建成功: orderId={}, outTradeNo={}, qrCode={}", 
                        order.getId(), response.getOutTradeNo(), response.getQrCode());
                
                return result;
                
            } catch (AlipayApiException e) {
                log.error("💥 支付宝API异常", e);
                throw new PaymentException("支付宝服务异常: " + e.getMessage());
            } catch (Exception e) {
                log.error("💥 支付订单创建失败", e);
                throw new PaymentException("支付服务暂时不可用，请稍后重试");
            }
        });
    }
    
    /**
     * 查询支付状态 - 支付宝统一收单线下交易查询
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
                
                // 3. 创建支付宝客户端查询订单
                AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(),
                    alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(),
                    alipayProperties.getFormat(),
                    alipayProperties.getCharset(),
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getSignType()
                );
                
                AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", orderId); // 商户订单号
                queryRequest.setBizContent(com.alibaba.fastjson2.JSON.toJSONString(bizContent));
                
                AlipayTradeQueryResponse response = alipayClient.execute(queryRequest);
                
                if (!response.isSuccess()) {
                    log.error("❌ 支付宝订单查询失败: {}", response.getSubMsg());
                    return PaymentStatus.UNKNOWN;
                }
                
                // 4. 更新本地订单状态
                PaymentStatus newStatus = mapAlipayTradeStatus(response.getTradeStatus());
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
                
            } catch (AlipayApiException e) {
                log.error("💥 支付宝查询API异常: orderId={}", orderId, e);
                return PaymentStatus.UNKNOWN;
            } catch (Exception e) {
                log.error("💥 查询支付状态失败: orderId={}", orderId, e);
                return PaymentStatus.UNKNOWN;
            }
        });
    }
    
    /**
     * 处理支付宝回调通知
     */
    @Override
    @Transactional
    public Mono<Void> handlePaymentCallback(String notifyData, String signature) {
        return Mono.fromRunnable(() -> {
            try {
                log.info("🔔 收到支付宝回调通知: {}", notifyData);
                
                // 1. 验证签名
                Map<String, String> params = parseNotifyParams(notifyData);
                boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, 
                    alipayProperties.getAlipayPublicKey(), 
                    alipayProperties.getCharset(), 
                    alipayProperties.getSignType()
                );
                
                if (!signVerified) {
                    log.error("❌ 支付宝回调签名验证失败");
                    throw new PaymentException("Invalid signature");
                }
                
                // 2. 解析回调参数
                String outTradeNo = params.get("out_trade_no"); // 商户订单号
                String tradeStatus = params.get("trade_status"); // 交易状态
                String tradeNo = params.get("trade_no"); // 支付宝交易号
                
                // 3. 查找对应的订单
                Optional<AFFiNEPaymentOrder> orderOpt = paymentOrderRepository.findById(outTradeNo);
                if (orderOpt.isEmpty()) {
                    log.error("❌ 未找到对应订单: outTradeNo={}", outTradeNo);
                    return;
                }
                
                AFFiNEPaymentOrder order = orderOpt.get();
                PaymentStatus newStatus = mapAlipayTradeStatus(tradeStatus);
                
                // 4. 更新订单状态
                if (order.getStatus() != newStatus) {
                    PaymentStatus oldStatus = order.getStatus();
                    order.setStatus(newStatus);
                    order.setJeepayOrderNo(tradeNo);
                    
                    if (newStatus == PaymentStatus.SUCCESS) {
                        order.setCompletedAt(LocalDateTime.now());
                    }
                    
                    paymentOrderRepository.save(order);
                    
                    log.info("🔄 支付宝回调更新订单状态: orderId={}, {} -> {}", 
                            order.getId(), oldStatus, newStatus);
                    
                    // 5. 处理业务逻辑
                    if (newStatus == PaymentStatus.SUCCESS) {
                        handlePaymentSuccess(order);
                    }
                }
                
            } catch (Exception e) {
                log.error("💥 处理支付宝回调失败", e);
                throw new PaymentException("Failed to process alipay callback");
            }
        });
    }
    
    /**
     * 退款处理 - 支付宝统一收单交易退款接口
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
                
                // 创建支付宝客户端
                AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(),
                    alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(),
                    alipayProperties.getFormat(),
                    alipayProperties.getCharset(),
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getSignType()
                );
                
                // 构建退款请求
                AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", orderId); // 商户订单号
                bizContent.put("refund_amount", formatAmount(order.getAmount())); // 退款金额
                bizContent.put("refund_reason", reason); // 退款原因
                bizContent.put("out_request_no", UUID.randomUUID().toString().replace("-", "")); // 退款请求号
                
                refundRequest.setBizContent(com.alibaba.fastjson2.JSON.toJSONString(bizContent));
                
                AlipayTradeRefundResponse response = alipayClient.execute(refundRequest);
                
                if (response.isSuccess()) {
                    order.setStatus(PaymentStatus.REFUNDED);
                    paymentOrderRepository.save(order);
                    
                    // 处理退款后的业务逻辑
                    handlePaymentRefund(order);
                    
                    log.info("✅ 支付宝退款成功: orderId={}", orderId);
                    return true;
                } else {
                    log.error("❌ 支付宝退款失败: {}", response.getSubMsg());
                    return false;
                }
                
            } catch (AlipayApiException e) {
                log.error("💥 支付宝退款API异常: orderId={}", orderId, e);
                return false;
            } catch (Exception e) {
                log.error("💥 退款处理失败: orderId={}", orderId, e);
                return false;
            }
        });
    }
    
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
        LocalDateTime now = LocalDateTime.now();
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
            .expireTime(now.plusMinutes(30)) // 30分钟过期
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
    
    /**
     * 格式化金额：分转元
     */
    private String formatAmount(Long amountInCents) {
        return new BigDecimal(amountInCents).divide(new BigDecimal(100)).toString();
    }
    
    /**
     * 生成支付页面URL
     */
    private String generatePaymentPageUrl(String orderId) {
        return "http://172.24.48.1:8080/payment/alipay/page/" + orderId;
    }
    
    /**
     * 映射支付宝交易状态到系统状态
     */
    private PaymentStatus mapAlipayTradeStatus(String tradeStatus) {
        return switch (tradeStatus) {
            case "WAIT_BUYER_PAY" -> PaymentStatus.PENDING;      // 交易创建，等待买家付款
            case "TRADE_SUCCESS" -> PaymentStatus.SUCCESS;       // 交易支付成功
            case "TRADE_FINISHED" -> PaymentStatus.SUCCESS;      // 交易结束，不可退款
            case "TRADE_CLOSED" -> PaymentStatus.CANCELLED;      // 未付款交易超时关闭
            default -> PaymentStatus.UNKNOWN;
        };
    }
    
    /**
     * 解析回调参数
     */
    private Map<String, String> parseNotifyParams(String notifyData) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = notifyData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
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