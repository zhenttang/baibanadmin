package com.yunke.backend.admin.controller;


import com.yunke.backend.admin.dto.AdminPaymentConfigDTO;
import com.yunke.backend.payment.dto.PaymentTestResultDTO;
import com.yunke.backend.payment.dto.PaymentProviderStatusDTO;
import com.yunke.backend.payment.dto.PaymentStatsDTO;
import com.yunke.backend.admin.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理员支付配置控制器
 * 提供支付系统的配置管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
@Tag(name = "管理员支付配置", description = "支付系统配置和管理API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @Operation(summary = "获取支付配置", description = "获取当前支付系统配置信息")
    @GetMapping("/config")
    public ResponseEntity<AdminPaymentConfigDTO> getPaymentConfig() {
        log.info("获取支付配置");
        AdminPaymentConfigDTO config = adminPaymentService.getPaymentConfig();
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "更新支付配置", description = "更新支付系统配置")
    @PutMapping("/config")
    public ResponseEntity<AdminPaymentConfigDTO> updatePaymentConfig(
            @Valid @RequestBody AdminPaymentConfigDTO configDTO) {
        log.info("更新支付配置: {}", configDTO);
        AdminPaymentConfigDTO updatedConfig = adminPaymentService.updatePaymentConfig(configDTO);
        return ResponseEntity.ok(updatedConfig);
    }

    @Operation(summary = "测试支付连接", description = "测试指定支付提供商的连接状态")
    @PostMapping("/test")
    public ResponseEntity<PaymentTestResultDTO> testPaymentConnection(
            @Parameter(description = "支付提供商类型", example = "STRIPE")
            @RequestParam String provider,
            @RequestBody(required = false) Map<String, Object> testConfig) {
        log.info("测试支付连接 - 提供商: {}", provider);
        PaymentTestResultDTO result = adminPaymentService.testPaymentConnection(provider, testConfig);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取支付提供商状态", description = "获取所有支付提供商的状态信息")
    @GetMapping("/providers/status")
    public ResponseEntity<List<PaymentProviderStatusDTO>> getProvidersStatus() {
        log.info("获取支付提供商状态");
        List<PaymentProviderStatusDTO> statuses = adminPaymentService.getProvidersStatus();
        return ResponseEntity.ok(statuses);
    }

    @Operation(summary = "启用支付提供商", description = "启用指定的支付提供商")
    @PostMapping("/providers/{provider}/enable")
    public ResponseEntity<Void> enableProvider(
            @Parameter(description = "支付提供商类型", example = "STRIPE")
            @PathVariable String provider) {
        log.info("启用支付提供商: {}", provider);
        adminPaymentService.enableProvider(provider);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "禁用支付提供商", description = "禁用指定的支付提供商")
    @PostMapping("/providers/{provider}/disable")
    public ResponseEntity<Void> disableProvider(
            @Parameter(description = "支付提供商类型", example = "STRIPE")
            @PathVariable String provider) {
        log.info("禁用支付提供商: {}", provider);
        adminPaymentService.disableProvider(provider);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取支付统计", description = "获取支付系统的统计数据")
    @GetMapping("/stats")
    public ResponseEntity<PaymentStatsDTO> getPaymentStats(
            @Parameter(description = "统计开始日期", example = "2024-01-01")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "统计结束日期", example = "2024-12-31")
            @RequestParam(required = false) String endDate) {
        log.info("获取支付统计 - 开始日期: {}, 结束日期: {}", startDate, endDate);
        PaymentStatsDTO stats = adminPaymentService.getPaymentStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "重新加载支付配置", description = "重新加载支付配置，使配置更改生效")
    @PostMapping("/reload")
    public ResponseEntity<Void> reloadPaymentConfig() {
        log.info("重新加载支付配置");
        adminPaymentService.reloadPaymentConfig();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "验证Webhook配置", description = "验证Webhook端点的配置和可访问性")
    @PostMapping("/webhook/verify")
    public ResponseEntity<Map<String, Object>> verifyWebhookConfig() {
        log.info("验证Webhook配置");
        Map<String, Object> result = adminPaymentService.verifyWebhookConfig();
        return ResponseEntity.ok(result);
    }
}