package com.yunke.backend.admin.service;

import com.yunke.backend.common.dto.ValidationResultDto;
import com.yunke.backend.notification.dto.*;
import com.yunke.backend.system.dto.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.List;

/**
 * 邮件管理服务接口
 */
public interface AdminMailerService {
    
    /**
     * 获取邮件配置
     */
    Mono<MailerConfigDto> getMailerConfig();
    
    /**
     * 更新邮件配置
     */
    Mono<MailerConfigDto> updateMailerConfig(MailerConfigDto config);
    
    /**
     * 测试邮件配置连接
     */
    Mono<MailerTestResultDto> testConnection(MailerConfigDto config);
    
    /**
     * 发送测试邮件
     */
    Mono<MailerTestResultDto> sendTestMail(SendTestMailRequestDto request);
    
    /**
     * 获取支持的邮件提供商列表
     */
    Flux<MailerProviderDto> getSupportedProviders();
    
    /**
     * 获取指定提供商的预设配置
     */
    Mono<MailerProviderDto> getProviderPreset(String provider);
    
    /**
     * 验证邮件配置
     */
    Mono<ValidationResultDto> validateConfig(MailerConfigDto config);
    
    /**
     * 获取邮件统计信息
     */
    Mono<MailerStatsDto> getMailerStats();
    
    /**
     * 重新加载邮件配置
     */
    Mono<Void> reloadConfig();
}