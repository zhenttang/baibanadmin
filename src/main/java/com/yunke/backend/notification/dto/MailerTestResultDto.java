package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 邮件测试结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailerTestResultDto {
    
    /**
     * 测试是否成功
     */
    private Boolean success;
    
    /**
     * 测试消息
     */
    private String message;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;
    
    /**
     * 错误详情
     */
    private String errorDetails;
    
    /**
     * 连接信息
     */
    private String connectionInfo;
    
    /**
     * 测试步骤结果
     */
    private TestStepsDto steps;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStepsDto {
        /**
         * 连接测试是否成功
         */
        private Boolean connection;
        
        /**
         * 认证测试是否成功
         */
        private Boolean authentication;
        
        /**
         * 发送测试是否成功
         */
        private Boolean sending;
        
        /**
         * 各步骤的错误消息
         */
        private String connectionError;
        private String authenticationError;
        private String sendingError;
    }
    
    public static MailerTestResultDto success(String message, Long responseTime) {
        return new MailerTestResultDto(true, message, responseTime, null, null, null);
    }
    
    public static MailerTestResultDto failure(String message, String errorDetails) {
        return new MailerTestResultDto(false, message, null, errorDetails, null, null);
    }
}