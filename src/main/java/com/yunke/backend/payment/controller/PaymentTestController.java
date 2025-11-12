package com.yunke.backend.payment.controller;

import com.yunke.backend.payment.dto.PaymentRequest;
import com.yunke.backend.payment.dto.PaymentResult;
import com.yunke.backend.payment.domain.entity.PaymentStatus;
import com.yunke.backend.payment.service.AFFiNEPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 支付测试控制器
 */
@Slf4j
@Controller
@RequestMapping("/payment/test")
public class PaymentTestController {
    
    private final AFFiNEPaymentService paymentService;
    
    public PaymentTestController(@Qualifier("alipayPaymentService") AFFiNEPaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * 显示支付测试页面
     */
    @GetMapping
    public String showTestPage() {
        return "payment-test";
    }
    
    /**
     * 创建测试支付订单
     */
    @PostMapping("/create")
    @ResponseBody
    public Mono<ResponseEntity<PaymentResult>> createTestPayment(@RequestBody PaymentRequest request) {
        log.info("🧪 创建测试支付订单: {}", request);
        
        // 设置默认值
        if (request.getUserId() == null) {
            request.setUserId("test-user-" + System.currentTimeMillis());
        }
        if (request.getClientIp() == null) {
            request.setClientIp("127.0.0.1");
        }
        if (request.getReturnUrl() == null) {
            request.setReturnUrl("http://127.0.0.1:8080/payment/test/success");
        }
        
        return paymentService.createPayment(request)
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(e -> {
                    log.error("创建测试支付订单失败", e);
                    PaymentResult errorResult = PaymentResult.builder()
                            .success(false)
                            .message("创建支付订单失败: " + e.getMessage())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(errorResult));
                });
    }
    
    /**
     * 查询支付状态
     */
    @GetMapping("/status/{orderId}")
    @ResponseBody
    public Mono<ResponseEntity<PaymentStatus>> queryPaymentStatus(@PathVariable String orderId) {
        return paymentService.queryPaymentStatus(orderId)
                .map(status -> ResponseEntity.ok(status))
                .onErrorResume(e -> {
                    log.error("查询支付状态失败: orderId={}", orderId, e);
                    return Mono.just(ResponseEntity.badRequest().body(PaymentStatus.UNKNOWN));
                });
    }
    
    /**
     * 支付成功页面
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam(required = false) String orderId,
                               @RequestParam(required = false) String status,
                               Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("status", status);
        return "payment-success";
    }
}