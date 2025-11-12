package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 邮件模板预览结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplatePreviewResultDto {
    
    /**
     * 渲染后的主题
     */
    private String renderedSubject;
    
    /**
     * 渲染后的HTML内容
     */
    private String renderedContent;
    
    /**
     * 渲染后的纯文本内容
     */
    private String renderedTextContent;
    
    /**
     * 渲染是否成功
     */
    private Boolean success;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 渲染耗时（毫秒）
     */
    private Long renderTimeMs;
    
    public static TemplatePreviewResultDto success(String subject, String content, String textContent, Long renderTime) {
        return new TemplatePreviewResultDto(subject, content, textContent, true, null, renderTime);
    }
    
    public static TemplatePreviewResultDto failure(String errorMessage) {
        return new TemplatePreviewResultDto(null, null, null, false, errorMessage, null);
    }
}