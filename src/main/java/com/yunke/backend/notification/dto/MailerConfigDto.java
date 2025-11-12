package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;

/**
 * 邮件配置数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailerConfigDto {
    
    /**
     * 是否启用邮件服务
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
    
    /**
     * SMTP服务器主机
     */
    @NotBlank(message = "SMTP主机不能为空")
    private String host;
    
    /**
     * SMTP服务器端口
     */
    @NotNull(message = "SMTP端口不能为空")
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号必须小于65535")
    private Integer port;
    
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
    @NotBlank(message = "发件人邮箱不能为空")
    @Email(message = "发件人邮箱格式不正确")
    private String sender;
    
    /**
     * 发件人名称
     */
    private String senderName;
    
    /**
     * 是否启用SSL
     */
    private Boolean ssl;
    
    /**
     * 是否启用STARTTLS
     */
    private Boolean startTls;
    
    /**
     * 是否忽略TLS证书验证
     */
    private Boolean ignoreTls;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout;
    
    /**
     * 是否开启调试模式
     */
    private Boolean debug;
    
    /**
     * 邮件服务提供商类型
     */
    private String provider;
    
    /**
     * 最大队列大小
     */
    private Integer maxQueueSize;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetries;
    
    /**
     * 重试间隔（分钟）
     */
    private Integer retryInterval;
    
    /**
     * 是否启用队列
     */
    private Boolean queueEnabled;
}