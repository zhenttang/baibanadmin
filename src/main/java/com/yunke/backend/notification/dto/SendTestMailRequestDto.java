package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * 发送测试邮件请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendTestMailRequestDto {
    
    /**
     * 收件人邮箱
     */
    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "收件人邮箱格式不正确")
    private String toEmail;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件内容
     */
    private String content;
    
    /**
     * 是否使用HTML格式
     */
    private Boolean html;
    
    /**
     * 是否使用当前配置进行测试（不保存配置）
     */
    private Boolean useCurrentConfig;
    
    /**
     * 临时配置（用于测试未保存的配置）
     */
    private MailerConfigDto tempConfig;
}