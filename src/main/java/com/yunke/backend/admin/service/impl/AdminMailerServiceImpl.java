package com.yunke.backend.admin.service.impl;

import com.yunke.backend.system.dto.*;
import com.yunke.backend.admin.service.AdminMailerService;
import com.yunke.backend.notification.dto.*;
import com.yunke.backend.common.dto.ValidationResultDto;
import com.yunke.backend.system.service.ConfigService;
import com.yunke.backend.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.*;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

/**
 * 邮件管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMailerServiceImpl implements AdminMailerService {
    
    private final ConfigService configService;
    private final MailService mailService;
    
    private static final String CONFIG_MODULE = "mailer";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    
    @Override
    public Mono<MailerConfigDto> getMailerConfig() {
        try {
            Map<String, Object> config = configService.getModuleConfig(CONFIG_MODULE);
            return Mono.just(convertToDto(config));
        } catch (Exception e) {
            log.error("获取邮件配置失败", e);
            return Mono.just(new MailerConfigDto());
        }
    }
    
    @Override
    public Mono<MailerConfigDto> updateMailerConfig(MailerConfigDto config) {
        return validateConfig(config)
                .flatMap(validation -> {
                    if (!validation.getValid()) {
                        return Mono.error(new IllegalArgumentException(validation.getMessage()));
                    }

                    try {
                        Map<String, Object> configMap = convertToConfigMap(config);
                        configService.updateModuleConfig(CONFIG_MODULE, configMap);
                        return reloadConfig().thenReturn(config);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .doOnSuccess(savedConfig -> log.info("邮件配置已更新"))
                .doOnError(error -> log.error("更新邮件配置失败", error));
    }
    
    @Override
    public Mono<MailerTestResultDto> testConnection(MailerConfigDto config) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            MailerTestResultDto.TestStepsDto steps = new MailerTestResultDto.TestStepsDto();
            
            try {
                // 1. 验证配置
                ValidationResultDto validation = validateConfigSync(config);
                if (!validation.getValid()) {
                    return MailerTestResultDto.failure("配置验证失败", validation.getMessage());
                }
                
                // 2. 创建邮件发送器
                JavaMailSenderImpl mailSender = createMailSender(config);
                
                // 3. 测试连接
                try {
                    Session session = mailSender.getSession();
                    Transport transport = session.getTransport("smtp");
                    transport.connect();
                    transport.close();
                    steps.setConnection(true);
                } catch (Exception e) {
                    steps.setConnection(false);
                    steps.setConnectionError("连接失败: " + e.getMessage());
                    throw e;
                }
                
                // 4. 测试认证
                if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                    try {
                        Session session = mailSender.getSession();
                        Transport transport = session.getTransport("smtp");
                        transport.connect(config.getHost(), config.getPort(), 
                                        config.getUsername(), config.getPassword());
                        transport.close();
                        steps.setAuthentication(true);
                    } catch (Exception e) {
                        steps.setAuthentication(false);
                        steps.setAuthenticationError("认证失败: " + e.getMessage());
                        throw e;
                    }
                }
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                MailerTestResultDto result = MailerTestResultDto.success(
                    "连接测试成功", responseTime);
                result.setSteps(steps);
                result.setConnectionInfo(String.format("连接到 %s:%d", config.getHost(), config.getPort()));
                
                return result;
                
            } catch (Exception e) {
                log.error("邮件连接测试失败", e);
                MailerTestResultDto result = MailerTestResultDto.failure(
                    "连接测试失败", e.getMessage());
                result.setSteps(steps);
                return result;
            }
        });
    }
    
    @Override
    public Mono<MailerTestResultDto> sendTestMail(SendTestMailRequestDto request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MailerConfigDto config = request.getUseCurrentConfig() ? 
                    getCurrentConfigSync() : request.getTempConfig();
                
                if (config == null) {
                    return MailerTestResultDto.failure("配置不存在", "没有找到邮件配置");
                }
                
                // 验证配置
                ValidationResultDto validation = validateConfigSync(config);
                if (!validation.getValid()) {
                    return MailerTestResultDto.failure("配置验证失败", validation.getMessage());
                }
                
                // 创建邮件发送器
                JavaMailSenderImpl mailSender = createMailSender(config);
                
                // 创建邮件消息
                MimeMessage message = mailSender.createMimeMessage();
                message.setFrom(new InternetAddress(config.getSender(), config.getSenderName()));
                message.setRecipients(MimeMessage.RecipientType.TO, 
                    InternetAddress.parse(request.getToEmail()));
                
                String subject = request.getSubject() != null ? request.getSubject() : 
                    "AFFiNE 邮件服务测试";
                message.setSubject(subject);
                
                String content = request.getContent() != null ? request.getContent() : 
                    "这是一封来自 AFFiNE 的测试邮件。如果您收到这封邮件，说明邮件服务配置正确。";
                
                if (Boolean.TRUE.equals(request.getHtml())) {
                    message.setContent(content, "text/html; charset=utf-8");
                } else {
                    message.setText(content, "utf-8");
                }
                
                message.setSentDate(new Date());
                
                // 发送邮件
                mailSender.send(message);
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                return MailerTestResultDto.success(
                    String.format("测试邮件已成功发送到 %s", request.getToEmail()), 
                    responseTime);
                
            } catch (Exception e) {
                log.error("发送测试邮件失败", e);
                return MailerTestResultDto.failure("发送失败", e.getMessage());
            }
        });
    }
    
    @Override
    public Flux<MailerProviderDto> getSupportedProviders() {
        return Flux.fromIterable(createSupportedProviders());
    }
    
    @Override
    public Mono<MailerProviderDto> getProviderPreset(String provider) {
        return getSupportedProviders()
                .filter(p -> p.getProvider().equals(provider))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("不支持的邮件提供商: " + provider)));
    }
    
    @Override
    public Mono<ValidationResultDto> validateConfig(MailerConfigDto config) {
        return Mono.fromCallable(() -> validateConfigSync(config));
    }
    
    @Override
    public Mono<MailerStatsDto> getMailerStats() {
        return Mono.fromCallable(() -> {
            // TODO: 从实际的邮件统计服务获取数据
            MailerStatsDto stats = new MailerStatsDto();
            stats.setTotalSent(0L);
            stats.setSuccessCount(0L);
            stats.setFailureCount(0L);
            stats.setQueueSize(0L);
            stats.setTodaySent(0L);
            stats.setWeekSent(0L);
            stats.setMonthSent(0L);
            stats.setAvgResponseTime(0.0);
            stats.setSuccessRate(100.0);
            stats.setConfigStatus("已配置");
            stats.setServiceStatus("运行中");
            stats.setLastSentTime(LocalDateTime.now());
            return stats;
        });
    }
    
    @Override
    public Mono<Void> reloadConfig() {
        return Mono.fromRunnable(() -> {
            // TODO: 重新加载邮件配置，重新初始化邮件发送器
            log.info("邮件配置已重新加载");
        });
    }
    
    // 私有辅助方法
    
    private MailerConfigDto convertToDto(Map<String, Object> configMap) {
        MailerConfigDto dto = new MailerConfigDto();
        dto.setEnabled(getBoolean(configMap, "enabled", true));
        dto.setHost(getString(configMap, "host", ""));
        dto.setPort(getInteger(configMap, "port", 587));
        dto.setUsername(getString(configMap, "username", ""));
        dto.setPassword(getString(configMap, "password", ""));
        dto.setSender(getString(configMap, "sender", ""));
        dto.setSenderName(getString(configMap, "senderName", "AFFiNE"));
        dto.setSsl(getBoolean(configMap, "ssl", false));
        dto.setStartTls(getBoolean(configMap, "startTls", true));
        dto.setIgnoreTls(getBoolean(configMap, "ignoreTls", false));
        dto.setConnectionTimeout(getInteger(configMap, "connectionTimeout", 30000));
        dto.setReadTimeout(getInteger(configMap, "readTimeout", 30000));
        dto.setDebug(getBoolean(configMap, "debug", false));
        dto.setProvider(getString(configMap, "provider", "custom"));
        dto.setMaxQueueSize(getInteger(configMap, "maxQueueSize", 1000));
        dto.setMaxRetries(getInteger(configMap, "maxRetries", 3));
        dto.setRetryInterval(getInteger(configMap, "retryInterval", 5));
        dto.setQueueEnabled(getBoolean(configMap, "queueEnabled", true));
        return dto;
    }
    
    private Map<String, Object> convertToConfigMap(MailerConfigDto dto) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", dto.getEnabled());
        configMap.put("host", dto.getHost());
        configMap.put("port", dto.getPort());
        configMap.put("username", dto.getUsername());
        configMap.put("password", dto.getPassword());
        configMap.put("sender", dto.getSender());
        configMap.put("senderName", dto.getSenderName());
        configMap.put("ssl", dto.getSsl());
        configMap.put("startTls", dto.getStartTls());
        configMap.put("ignoreTls", dto.getIgnoreTls());
        configMap.put("connectionTimeout", dto.getConnectionTimeout());
        configMap.put("readTimeout", dto.getReadTimeout());
        configMap.put("debug", dto.getDebug());
        configMap.put("provider", dto.getProvider());
        configMap.put("maxQueueSize", dto.getMaxQueueSize());
        configMap.put("maxRetries", dto.getMaxRetries());
        configMap.put("retryInterval", dto.getRetryInterval());
        configMap.put("queueEnabled", dto.getQueueEnabled());
        return configMap;
    }
    
    private ValidationResultDto validateConfigSync(MailerConfigDto config) {
        List<ValidationResultDto.FieldErrorDto> errors = new ArrayList<>();
        
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            errors.add(new ValidationResultDto.FieldErrorDto("host", "SMTP主机不能为空", "REQUIRED"));
        }
        
        if (config.getPort() == null || config.getPort() < 1 || config.getPort() > 65535) {
            errors.add(new ValidationResultDto.FieldErrorDto("port", "端口号必须在1-65535之间", "INVALID_RANGE"));
        }
        
        if (config.getSender() == null || config.getSender().trim().isEmpty()) {
            errors.add(new ValidationResultDto.FieldErrorDto("sender", "发件人邮箱不能为空", "REQUIRED"));
        } else if (!EMAIL_PATTERN.matcher(config.getSender()).matches()) {
            errors.add(new ValidationResultDto.FieldErrorDto("sender", "发件人邮箱格式不正确", "INVALID_FORMAT"));
        }
        
        if (errors.isEmpty()) {
            return ValidationResultDto.success();
        } else {
            ValidationResultDto result = ValidationResultDto.failure("配置验证失败");
            result.setFieldErrors(errors);
            return result;
        }
    }
    
    private JavaMailSenderImpl createMailSender(MailerConfigDto config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", config.getUsername() != null && !config.getUsername().isEmpty());
        props.put("mail.smtp.starttls.enable", config.getStartTls());
        props.put("mail.smtp.ssl.enable", config.getSsl());
        props.put("mail.smtp.connectiontimeout", config.getConnectionTimeout());
        props.put("mail.smtp.timeout", config.getReadTimeout());
        props.put("mail.debug", config.getDebug());
        
        if (config.getIgnoreTls()) {
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.ssl.checkserveridentity", false);
        }
        
        return mailSender;
    }
    
    private MailerConfigDto getCurrentConfigSync() {
        try {
            return getMailerConfig().block();
        } catch (Exception e) {
            log.error("获取当前邮件配置失败", e);
            return null;
        }
    }
    
    private List<MailerProviderDto> createSupportedProviders() {
        List<MailerProviderDto> providers = new ArrayList<>();
        
        // Gmail
        providers.add(createGmailProvider());
        
        // Outlook
        providers.add(createOutlookProvider());
        
        // SendGrid
        providers.add(createSendGridProvider());
        
        // Mailgun
        providers.add(createMailgunProvider());
        
        // 自定义SMTP
        providers.add(createCustomProvider());
        
        return providers;
    }
    
    private MailerProviderDto createGmailProvider() {
        MailerProviderDto provider = new MailerProviderDto();
        provider.setProvider("gmail");
        provider.setName("Gmail");
        provider.setDescription("Google Gmail SMTP服务");
        provider.setIcon("/icons/gmail.png");
        provider.setSupported(true);
        provider.setRecommended(true);
        
        Map<String, Object> preset = new HashMap<>();
        preset.put("host", "smtp.gmail.com");
        preset.put("port", 587);
        preset.put("ssl", false);
        preset.put("startTls", true);
        provider.setPresetConfig(preset);
        
        provider.setInstructions("需要使用应用专用密码，不能使用普通登录密码");
        
        return provider;
    }
    
    private MailerProviderDto createOutlookProvider() {
        MailerProviderDto provider = new MailerProviderDto();
        provider.setProvider("outlook");
        provider.setName("Outlook");
        provider.setDescription("Microsoft Outlook SMTP服务");
        provider.setIcon("/icons/outlook.png");
        provider.setSupported(true);
        provider.setRecommended(true);
        
        Map<String, Object> preset = new HashMap<>();
        preset.put("host", "smtp-mail.outlook.com");
        preset.put("port", 587);
        preset.put("ssl", false);
        preset.put("startTls", true);
        provider.setPresetConfig(preset);
        
        return provider;
    }
    
    private MailerProviderDto createSendGridProvider() {
        MailerProviderDto provider = new MailerProviderDto();
        provider.setProvider("sendgrid");
        provider.setName("SendGrid");
        provider.setDescription("SendGrid 邮件发送服务");
        provider.setIcon("/icons/sendgrid.png");
        provider.setSupported(true);
        provider.setRecommended(true);
        
        Map<String, Object> preset = new HashMap<>();
        preset.put("host", "smtp.sendgrid.net");
        preset.put("port", 587);
        preset.put("username", "apikey");
        preset.put("ssl", false);
        preset.put("startTls", true);
        provider.setPresetConfig(preset);
        
        provider.setInstructions("用户名固定为 'apikey'，密码使用 SendGrid API Key");
        
        return provider;
    }
    
    private MailerProviderDto createMailgunProvider() {
        MailerProviderDto provider = new MailerProviderDto();
        provider.setProvider("mailgun");
        provider.setName("Mailgun");
        provider.setDescription("Mailgun 邮件发送服务");
        provider.setIcon("/icons/mailgun.png");
        provider.setSupported(true);
        provider.setRecommended(false);
        
        Map<String, Object> preset = new HashMap<>();
        preset.put("host", "smtp.mailgun.org");
        preset.put("port", 587);
        preset.put("ssl", false);
        preset.put("startTls", true);
        provider.setPresetConfig(preset);
        
        return provider;
    }
    
    private MailerProviderDto createCustomProvider() {
        MailerProviderDto provider = new MailerProviderDto();
        provider.setProvider("custom");
        provider.setName("自定义SMTP");
        provider.setDescription("自定义SMTP服务器配置");
        provider.setIcon("/icons/email.png");
        provider.setSupported(true);
        provider.setRecommended(false);
        
        Map<String, Object> preset = new HashMap<>();
        preset.put("port", 587);
        preset.put("ssl", false);
        preset.put("startTls", true);
        provider.setPresetConfig(preset);
        
        return provider;
    }
    
    // 配置解析辅助方法
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private Boolean getBoolean(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}