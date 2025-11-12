package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 邮件模板数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailTemplateDto {
    
    /**
     * 模板ID
     */
    private String id;
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String name;
    
    /**
     * 模板标识（用于代码中引用）
     */
    @NotBlank(message = "模板标识不能为空")
    private String identifier;
    
    /**
     * 模板分类
     */
    @NotNull(message = "模板分类不能为空")
    private String category;
    
    /**
     * 语言代码
     */
    @NotBlank(message = "语言代码不能为空")
    private String language;
    
    /**
     * 邮件主题模板
     */
    @NotBlank(message = "邮件主题不能为空")
    private String subject;
    
    /**
     * 邮件内容模板（HTML格式）
     */
    @NotBlank(message = "邮件内容不能为空")
    private String content;
    
    /**
     * 纯文本内容模板
     */
    private String textContent;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 模板版本
     */
    private Integer version;
    
    /**
     * 是否为默认模板
     */
    private Boolean isDefault;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 模板变量定义
     */
    private Map<String, Object> variables;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建者
     */
    private String createdBy;
    
    /**
     * 更新者
     */
    private String updatedBy;
}