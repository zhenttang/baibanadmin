package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 安全防护配置
 */
@Configuration
@ConfigurationProperties(prefix = "security.protection")
@Data
public class SecurityProtectionConfig {
    
    /**
     * DDoS防护配置
     */
    private DDoSConfig ddos = new DDoSConfig();
    
    /**
     * 登录保护配置
     */
    private LoginConfig login = new LoginConfig();
    
    /**
     * 爬虫防护配置
     */
    private BotConfig bot = new BotConfig();
    
    /**
     * 告警配置
     */
    private AlertConfig alert = new AlertConfig();
    
    @Data
    public static class DDoSConfig {
        /**
         * 是否启用DDoS防护
         */
        private boolean enabled = false;
        
        /**
         * 单IP每分钟最大请求数
         */
        private int maxRequestsPerMinute = 100;
        
        /**
         * 封禁时长（分钟）
         */
        private int blockDurationMinutes = 10;
        
        /**
         * 警告阈值（达到此值发送警告）
         */
        private int warningThreshold = 80;
    }
    
    @Data
    public static class LoginConfig {
        /**
         * 是否启用登录保护
         */
        private boolean enabled = false;
        
        /**
         * 最大失败尝试次数
         */
        private int maxFailedAttempts = 5;
        
        /**
         * 账户锁定时间（分钟）
         */
        private int lockoutMinutes = 15;
        
        /**
         * 触发验证码的失败次数
         */
        private int captchaThreshold = 3;
    }
    
    @Data
    public static class BotConfig {
        /**
         * 是否启用爬虫防护
         */
        private boolean enabled = false;
        
        /**
         * 单IP每分钟最大页面访问数
         */
        private int maxPagesPerMinute = 30;
        
        /**
         * 封禁时长（分钟）
         */
        private int blockDurationMinutes = 30;
    }
    
    @Data
    public static class AlertConfig {
        /**
         * 是否启用告警
         */
        private boolean enabled = false;
        
        /**
         * 管理员邮箱列表
         */
        private String[] adminEmails = new String[0];
        
        /**
         * 是否发送邮件告警
         */
        private boolean emailEnabled = false;
        
        /**
         * 是否记录日志
         */
        private boolean logEnabled = true;
    }
}

