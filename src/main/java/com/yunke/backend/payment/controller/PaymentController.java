package com.yunke.backend.payment.controller;

import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.domain.entity.AFFiNEPaymentOrder;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import com.yunke.backend.payment.service.AFFiNEPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AFFiNE支付控制器 - 替代假数据实现
 */
@Slf4j
@RestController
@RequestMapping("/api/community/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final AFFiNEPaymentService paymentService;
    
    /**
     * 创建支付订单 - 真实Jeepay集成
     */
    @PostMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> createPaymentOrder(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") String userId,
            HttpServletRequest httpRequest) {
        
        log.info("🚀 创建真实支付订单: userId={}, amount={}, paymentMethod={}", 
                userId, request.getAmount(), request.getPaymentMethod());
        
        // 设置用户ID和客户端IP
        request.setUserId(userId);
        if (request.getClientIp() == null) {
            request.setClientIp(getClientIp(httpRequest));
        }
        
        return paymentService.createPayment(request)
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", result.getSuccess());
                    response.put("message", result.getMessage());
                    response.put("data", result);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ 创建支付订单失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }
    
    /**
     * Jeepay支付回调
     */
    @PostMapping("/callback")
    public Mono<ResponseEntity<String>> paymentCallback(
            @RequestBody String notifyData,
            @RequestParam(required = false) String sign) {
        
        log.info("🔔 收到Jeepay支付回调: data={}", notifyData);
        
        return paymentService.handlePaymentCallback(notifyData, sign)
                .map(result -> ResponseEntity.ok("success"))
                .onErrorResume(e -> {
                    log.error("❌ 处理支付回调失败", e);
                    return Mono.just(ResponseEntity.badRequest().body("fail"));
                });
    }
    
    /**
     * 查询支付状态
     */
    @GetMapping("/status/{orderId}")
    public Mono<ResponseEntity<Map<String, Object>>> queryPaymentStatus(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("🔍 查询支付状态: userId={}, orderId={}", userId, orderId);
        
        return paymentService.isOrderOwnedByUser(orderId, userId)
                .flatMap(isOwned -> {
                    if (!isOwned) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "订单不存在或无权限访问");
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                    
                    return paymentService.queryPaymentStatus(orderId)
                            .map(status -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("status", status);
                                return ResponseEntity.ok(response);
                            });
                })
                .onErrorResume(e -> {
                    log.error("❌ 查询支付状态失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }
    
    /**
     * 获取用户支付订单列表
     */
    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> getUserPaymentOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("📋 获取用户支付订单: userId={}, page={}, size={}", userId, page, size);
        
        return paymentService.getUserPaymentOrders(userId, page, size)
                .map(orders -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of(
                        "items", orders,
                        "page", page,
                        "size", size,
                        "total", orders.size()
                    ));
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ 获取用户支付订单失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }
    
    /**
     * 取消支付订单
     */
    @PostMapping("/cancel/{orderId}")
    public Mono<ResponseEntity<Map<String, Object>>> cancelPayment(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("❌ 取消支付订单: userId={}, orderId={}", userId, orderId);
        
        return paymentService.cancelPayment(orderId, userId)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ? "订单取消成功" : "订单取消失败");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ 取消支付订单失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }
    
    /**
     * 申请退款
     */
    @PostMapping("/refund/{orderId}")
    public Mono<ResponseEntity<Map<String, Object>>> requestRefund(
            @PathVariable String orderId,
            @RequestParam(required = false, defaultValue = "用户申请退款") String reason,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("💰 申请退款: userId={}, orderId={}, reason={}", userId, orderId, reason);
        
        return paymentService.refundPayment(orderId, userId, reason)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ? "退款申请已提交" : "退款申请失败");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ 申请退款失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }
    
    /**
     * 清理过期订单（管理员接口）
     */
    @PostMapping("/admin/cleanup-expired")
    public Mono<ResponseEntity<Map<String, Object>>> cleanupExpiredOrders() {
        
        log.info("🧹 清理过期订单");
        
        return paymentService.cleanupExpiredOrders()
                .map(count -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "过期订单清理完成");
                    response.put("cleanedCount", count);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ 清理过期订单失败", e);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(response));
                });
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}