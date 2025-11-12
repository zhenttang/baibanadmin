package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置类
 * 对应Node.js版本的邮件配置
 * 参考: /packages/backend/server/src/core/mail/config.ts
 */
@Configuration
@ConfigurationProperties(prefix = "affine.mail")
@Data
public class MailConfig {

    /**
     * SMTP服务器主机
     */
    private String host = "localhost";

    /**
     * SMTP服务器端口
     */
    private Integer port = 465;

    /**
     * SMTP用户名
     */
    private String username;

    /**
     * SMTP密码
     */
    private String password;

    /**
     * 发件人邮箱
     */
    private String sender = "noreply@affine.pro";

    /**
     * 发件人名称
     */
    private String senderName = "AFFiNE";

    /**
     * 是否忽略TLS
     */
    private Boolean ignoreTls = false;

    /**
     * 是否启用SSL
     */
    private Boolean enableSsl = true;

    /**
     * 是否启用STARTTLS
     */
    private Boolean enableStartTls = true;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 10000;

    /**
     * 是否启用邮件发送
     */
    private Boolean enabled = true;

    /**
     * 是否启用邮件队列
     */
    private Boolean queueEnabled = true;

    /**
     * 队列重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private Integer retryDelay = 5000;

    /**
     * 是否为测试模式
     */
    private Boolean testMode = false;

    /**
     * 测试模式下的邮件接收地址
     */
    private String testEmail;

    /**
     * 邮件模板基础URL
     */
    private String templateBaseUrl = "https://affine.pro";

    /**
     * 应用基础URL
     */
    private String baseUrl = "https://app.affine.pro";

    /**
     * 邮件模板资源路径
     */
    private String templatePath = "classpath:/mail-templates/";

    /**
     * 获取发件人地址
     */
    public String getFrom() {
        return sender;
    }

    /**
     * 检查配置是否有效
     */
    public boolean isConfigValid() {
        return enabled && host != null && !host.isEmpty() && 
               username != null && !username.isEmpty() && 
               password != null && !password.isEmpty();
    }

    /**
     * 获取完整的发件人地址
     */
    public String getFullSender() {
        if (senderName != null && !senderName.isEmpty()) {
            return String.format("%s <%s>", senderName, sender);
        }
        return sender;
    }

    /**
     * 获取SMTP协议
     */
    public String getProtocol() {
        return enableSsl ? "smtps" : "smtp";
    }
}