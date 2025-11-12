package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 邮件模板预览请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplatePreviewRequestDto {
    
    /**
     * 邮件主题模板
     */
    private String subject;
    
    /**
     * 邮件内容模板
     */
    private String content;
    
    /**
     * 纯文本内容模板
     */
    private String textContent;
    
    /**
     * 模板变量值
     */
    private Map<String, Object> variables;
    
    /**
     * 语言代码
     */
    private String language;
}