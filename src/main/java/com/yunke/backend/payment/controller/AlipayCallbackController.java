package com.yunke.backend.payment.controller;

import com.yunke.backend.payment.service.AFFiNEPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/community/payments/alipay")
public class AlipayCallbackController {
    
    private final AFFiNEPaymentService paymentService;
    
    public AlipayCallbackController(@Qualifier("alipayPaymentService") AFFiNEPaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * 支付宝异步通知接口
     */
    @PostMapping("/notify")
    public Mono<ResponseEntity<String>> handleAlipayNotify(HttpServletRequest request) {
        
        log.info("🔔 收到支付宝异步通知");
        
        // 获取支付宝的通知参数
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        
        // 构建参数字符串（用于签名验证）
        StringBuilder notifyData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (notifyData.length() > 0) {
                notifyData.append("&");
            }
            notifyData.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        String signature = params.get("sign");
        
        return paymentService.handlePaymentCallback(notifyData.toString(), signature)
                .map(result -> {
                    log.info("✅ 支付宝回调处理成功");
                    return ResponseEntity.ok("success");
                })
                .onErrorResume(e -> {
                    log.error("❌ 支付宝回调处理失败", e);
                    return Mono.just(ResponseEntity.badRequest().body("fail"));
                });
    }
    
    /**
     * 支付宝同步跳转接口
     */
    @GetMapping("/return")
    public Mono<ResponseEntity<String>> handleAlipayReturn(
            @RequestParam String out_trade_no,
            @RequestParam String trade_no,
            @RequestParam(required = false) String trade_status) {
        
        log.info("🔄 支付宝同步跳转: out_trade_no={}, trade_no={}, trade_status={}", 
                out_trade_no, trade_no, trade_status);
        
        // 同步跳转一般用于页面展示，不做业务处理
        String redirectUrl = String.format("http://172.24.48.1:3000/payment/success?orderId=%s&status=%s", 
                out_trade_no, trade_status != null ? trade_status : "unknown");
        
        String html = String.format("""
            <html>
            <head><title>支付结果</title></head>
            <body>
                <script>
                    window.location.href = '%s';
                </script>
                <p>支付完成，正在跳转...</p>
                <a href='%s'>如果没有自动跳转，请点击这里</a>
            </body>
            </html>
            """, redirectUrl, redirectUrl);
        
        return Mono.just(ResponseEntity.ok()
                .header("Content-Type", "text/html;charset=UTF-8")
                .body(html));
    }
}