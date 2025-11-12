package com.yunke.backend.admin.controller;

import com.yunke.backend.admin.service.AdminMailerService;
import com.yunke.backend.common.dto.ValidationResultDto;
import com.yunke.backend.notification.dto.*;
import com.yunke.backend.system.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

/**
 * 邮件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/mailer")
@RequiredArgsConstructor
@Tag(name = "邮件管理", description = "邮件服务配置和管理")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminMailerController {
    
    private final AdminMailerService adminMailerService;
    
    @GetMapping("/config")
    @Operation(summary = "获取邮件配置", description = "获取当前的邮件服务配置")
    public Mono<ResponseEntity<MailerConfigDto>> getMailerConfig() {
        return adminMailerService.getMailerConfig()
                .map(ResponseEntity::ok)
                .doOnSuccess(config -> log.info("获取邮件配置成功"))
                .onErrorResume(error -> {
                    log.error("获取邮件配置失败", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @PutMapping("/config")
    @Operation(summary = "更新邮件配置", description = "更新邮件服务配置")
    public Mono<ResponseEntity<MailerConfigDto>> updateMailerConfig(
            @Valid @RequestBody MailerConfigDto config) {
        return adminMailerService.updateMailerConfig(config)
                .map(ResponseEntity::ok)
                .doOnSuccess(savedConfig -> log.info("邮件配置更新成功"))
                .onErrorResume(error -> {
                    log.error("更新邮件配置失败", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @PostMapping("/test-connection")
    @Operation(summary = "测试邮件连接", description = "测试邮件服务器连接配置")
    public Mono<ResponseEntity<MailerTestResultDto>> testConnection(
            @Valid @RequestBody MailerConfigDto config) {
        return adminMailerService.testConnection(config)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.info("邮件连接测试完成，结果: {}", result.getBody() != null ? result.getBody().getSuccess() : "unknown"))
                .onErrorResume(error -> {
                    log.error("邮件连接测试失败", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(MailerTestResultDto.failure("测试失败", error.getMessage())));
                });
    }
    
    @PostMapping("/send-test")
    @Operation(summary = "发送测试邮件", description = "发送测试邮件验证配置")
    public Mono<ResponseEntity<MailerTestResultDto>> sendTestMail(
            @Valid @RequestBody SendTestMailRequestDto request) {
        return adminMailerService.sendTestMail(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.info("测试邮件发送完成，结果: {}", result.getBody() != null ? result.getBody().getSuccess() : "unknown"))
                .onErrorResume(error -> {
                    log.error("发送测试邮件失败", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(MailerTestResultDto.failure("发送失败", error.getMessage())));
                });
    }
    
    @GetMapping("/providers")
    @Operation(summary = "获取支持的邮件提供商", description = "获取所有支持的邮件服务提供商列表")
    public Flux<MailerProviderDto> getSupportedProviders() {
        return adminMailerService.getSupportedProviders()
                .doOnComplete(() -> log.info("获取邮件提供商列表成功"))
                .onErrorResume(error -> {
                    log.error("获取邮件提供商列表失败", error);
                    return Flux.empty();
                });
    }
    
    @GetMapping("/providers/{provider}")
    @Operation(summary = "获取提供商预设配置", description = "获取指定邮件提供商的预设配置")
    public Mono<ResponseEntity<MailerProviderDto>> getProviderPreset(
            @Parameter(description = "提供商标识") @PathVariable String provider) {
        return adminMailerService.getProviderPreset(provider)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnSuccess(preset -> log.info("获取提供商 {} 预设配置成功", provider))
                .onErrorResume(error -> {
                    log.error("获取提供商预设配置失败: {}", provider, error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @PostMapping("/validate")
    @Operation(summary = "验证邮件配置", description = "验证邮件配置的有效性")
    public Mono<ResponseEntity<ValidationResultDto>> validateConfig(
            @Valid @RequestBody MailerConfigDto config) {
        return adminMailerService.validateConfig(config)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.info("邮件配置验证完成，结果: {}", result.getBody() != null ? result.getBody().getValid() : "unknown"))
                .onErrorResume(error -> {
                    log.error("邮件配置验证失败", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ValidationResultDto.failure(error.getMessage())));
                });
    }
    
    @GetMapping("/stats")
    @Operation(summary = "获取邮件统计", description = "获取邮件服务的统计信息")
    public Mono<ResponseEntity<MailerStatsDto>> getMailerStats() {
        return adminMailerService.getMailerStats()
                .map(ResponseEntity::ok)
                .doOnSuccess(stats -> log.info("获取邮件统计信息成功"))
                .onErrorResume(error -> {
                    log.error("获取邮件统计信息失败", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @PostMapping("/reload")
    @Operation(summary = "重新加载配置", description = "重新加载邮件配置")
    public Mono<ResponseEntity<Void>> reloadConfig() {
        return adminMailerService.reloadConfig()
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> log.info("邮件配置重新加载成功"))
                .onErrorResume(error -> {
                    log.error("重新加载邮件配置失败", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}