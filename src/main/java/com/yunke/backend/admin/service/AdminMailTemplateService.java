package com.yunke.backend.admin.service;

import com.yunke.backend.notification.dto.*;
import com.yunke.backend.system.dto.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 邮件模板管理服务接口
 */
public interface AdminMailTemplateService {
    
    /**
     * 分页查询邮件模板
     */
    Mono<Page<MailTemplateDto>> getTemplates(TemplateQueryDto query);
    
    /**
     * 根据ID获取邮件模板
     */
    Mono<MailTemplateDto> getTemplate(String id);
    
    /**
     * 根据标识和语言获取邮件模板
     */
    Mono<MailTemplateDto> getTemplateByIdentifier(String identifier, String language);
    
    /**
     * 创建邮件模板
     */
    Mono<MailTemplateDto> createTemplate(MailTemplateDto template);
    
    /**
     * 更新邮件模板
     */
    Mono<MailTemplateDto> updateTemplate(String id, MailTemplateDto template);
    
    /**
     * 删除邮件模板
     */
    Mono<Void> deleteTemplate(String id);
    
    /**
     * 批量删除邮件模板
     */
    Mono<Void> deleteTemplates(List<String> ids);
    
    /**
     * 预览邮件模板
     */
    Mono<TemplatePreviewResultDto> previewTemplate(TemplatePreviewRequestDto request);
    
    /**
     * 发送测试邮件
     */
    Mono<MailerTestResultDto> sendTestTemplateEmail(String templateId, String toEmail,
                                                    java.util.Map<String, Object> variables);
    
    /**
     * 获取模板分类列表
     */
    Flux<String> getCategories();
    
    /**
     * 获取支持的语言列表
     */
    Flux<String> getSupportedLanguages();
    
    /**
     * 导入模板
     */
    Mono<MailTemplateDto> importTemplate(MailTemplateDto template);
    
    /**
     * 导出模板
     */
    Mono<String> exportTemplate(String id);
    
    /**
     * 复制模板
     */
    Mono<MailTemplateDto> copyTemplate(String id, String newName, String newLanguage);
    
    /**
     * 设置默认模板
     */
    Mono<Void> setDefaultTemplate(String identifier, String language, String templateId);
    
    /**
     * 启用/禁用模板
     */
    Mono<Void> toggleTemplate(String id, Boolean enabled);
    
    /**
     * 获取模板变量定义
     */
    Mono<java.util.Map<String, Object>> getTemplateVariables(String category);
}