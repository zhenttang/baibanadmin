package com.yunke.backend.security.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

/**
 * 安全配置DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityConfigDto {
    
    /**
     * 是否启用安全策略
     */
    @NotNull(message = "安全策略启用状态不能为空")
    private Boolean enabled;
    
    /**
     * 登录安全配置
     */
    private LoginSecurityConfig loginSecurity;
    
    /**
     * IP访问控制配置
     */
    private IpAccessConfig ipAccess;
    
    /**
     * 密码策略配置
     */
    private PasswordPolicyConfig passwordPolicy;
    
    /**
     * 会话安全配置
     */
    private SessionSecurityConfig sessionSecurity;
    
    /**
     * API安全配置
     */
    private ApiSecurityConfig apiSecurity;
    
    /**
     * 安全监控配置
     */
    private SecurityMonitoringConfig monitoring;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginSecurityConfig {
        /**
         * 最大失败尝试次数
         */
        @Min(value = 1, message = "最大失败尝试次数不能小于1")
        @Max(value = 20, message = "最大失败尝试次数不能大于20")
        private Integer maxFailedAttempts;
        
        /**
         * 账户锁定时间（分钟）
         */
        @Min(value = 1, message = "锁定时间不能小于1分钟")
        private Integer lockoutDurationMinutes;
        
        /**
         * 是否启用验证码
         */
        private Boolean enableCaptcha;
        
        /**
         * 触发验证码的失败次数
         */
        private Integer captchaThreshold;
        
        /**
         * 是否启用双因素认证
         */
        private Boolean enableTwoFactor;
        
        /**
         * 登录IP限制
         */
        private Boolean enableIpRestriction;
        
        /**
         * 单点登录限制
         */
        private Boolean forceSingleSession;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IpAccessConfig {
        /**
         * 是否启用IP白名单
         */
        private Boolean enableWhitelist;
        
        /**
         * IP白名单
         */
        private List<String> whitelist;
        
        /**
         * 是否启用IP黑名单
         */
        private Boolean enableBlacklist;
        
        /**
         * IP黑名单
         */
        private List<String> blacklist;
        
        /**
         * 是否允许内网访问
         */
        private Boolean allowPrivateNetworks;
        
        /**
         * 地理位置限制
         */
        private List<String> allowedCountries;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordPolicyConfig {
        /**
         * 最小长度
         */
        @Min(value = 6, message = "密码最小长度不能小于6")
        private Integer minLength;
        
        /**
         * 最大长度
         */
        @Max(value = 128, message = "密码最大长度不能大于128")
        private Integer maxLength;
        
        /**
         * 是否要求大写字母
         */
        private Boolean requireUppercase;
        
        /**
         * 是否要求小写字母
         */
        private Boolean requireLowercase;
        
        /**
         * 是否要求数字
         */
        private Boolean requireNumbers;
        
        /**
         * 是否要求特殊字符
         */
        private Boolean requireSpecialChars;
        
        /**
         * 密码历史记录数量
         */
        private Integer passwordHistory;
        
        /**
         * 密码过期天数
         */
        private Integer expirationDays;
        
        /**
         * 禁用的常用密码
         */
        private List<String> bannedPasswords;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionSecurityConfig {
        /**
         * 会话超时时间（分钟）
         */
        @Min(value = 5, message = "会话超时时间不能小于5分钟")
        private Integer timeoutMinutes;
        
        /**
         * 是否启用会话固定保护
         */
        private Boolean enableSessionFixationProtection;
        
        /**
         * 是否启用并发会话控制
         */
        private Boolean enableConcurrencyControl;
        
        /**
         * 最大并发会话数
         */
        private Integer maxConcurrentSessions;
        
        /**
         * 是否启用安全Cookie
         */
        private Boolean enableSecureCookies;
        
        /**
         * Cookie SameSite策略
         */
        private String cookieSameSite;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiSecurityConfig {
        /**
         * API调用频率限制
         */
        private Integer rateLimitPerMinute;
        
        /**
         * 是否启用API Key认证
         */
        private Boolean enableApiKeyAuth;
        
        /**
         * 是否启用CORS保护
         */
        private Boolean enableCorsProtection;
        
        /**
         * 允许的跨域源
         */
        private List<String> allowedOrigins;
        
        /**
         * 是否启用CSRF保护
         */
        private Boolean enableCsrfProtection;
        
        /**
         * 是否启用请求签名验证
         */
        private Boolean enableRequestSigning;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityMonitoringConfig {
        /**
         * 是否启用安全事件监控
         */
        private Boolean enableEventMonitoring;
        
        /**
         * 是否启用异常登录检测
         */
        private Boolean enableAnomalyDetection;
        
        /**
         * 是否启用实时告警
         */
        private Boolean enableRealTimeAlerts;
        
        /**
         * 告警邮箱列表
         */
        private List<String> alertEmails;
        
        /**
         * 日志保留天数
         */
        private Integer logRetentionDays;
        
        /**
         * 是否记录成功登录
         */
        private Boolean logSuccessfulLogins;
        
        /**
         * 是否记录失败登录
         */
        private Boolean logFailedLogins;
        
        /**
         * 是否记录API调用
         */
        private Boolean logApiCalls;
    }
}