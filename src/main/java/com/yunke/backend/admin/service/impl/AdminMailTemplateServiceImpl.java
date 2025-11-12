package com.yunke.backend.admin.service.impl;

import com.yunke.backend.admin.service.AdminMailTemplateService;
import com.yunke.backend.notification.dto.*;
import com.yunke.backend.system.dto.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件模板管理服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMailTemplateServiceImpl implements AdminMailTemplateService {

    // 内存存储，实际应该使用数据库
    private final Map<String, MailTemplateDto> templates = new ConcurrentHashMap<>();
    private final Map<String, String> defaultTemplates = new ConcurrentHashMap<>();

    @Override
    public Mono<Page<MailTemplateDto>> getTemplates(TemplateQueryDto query) {
        log.info("获取邮件模板列表 - query: {}", query);

        List<MailTemplateDto> templateList = new ArrayList<>(templates.values());
        PageRequest pageRequest = PageRequest.of(0, 10); // 默认分页
        return Mono.just(new PageImpl<>(templateList, pageRequest, templateList.size()));
    }

    @Override
    public Mono<MailTemplateDto> getTemplate(String id) {
        log.info("获取邮件模板 - id: {}", id);

        MailTemplateDto template = templates.get(id);
        if (template == null) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + id));
        }
        return Mono.just(template);
    }

    @Override
    public Mono<MailTemplateDto> getTemplateByIdentifier(String identifier, String language) {
        log.info("根据标识获取邮件模板 - identifier: {}, language: {}", identifier, language);

        // 简单实现，实际应该根据identifier和language查询
        MailTemplateDto template = templates.values().stream()
                .filter(t -> identifier.equals(t.getName()))
                .findFirst()
                .orElse(null);

        if (template == null) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + identifier));
        }
        return Mono.just(template);
    }

    @Override
    public Mono<MailTemplateDto> createTemplate(MailTemplateDto template) {
        log.info("创建邮件模板 - name: {}", template.getName());

        String templateId = UUID.randomUUID().toString();
        template.setId(templateId);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        templates.put(templateId, template);
        return Mono.just(template);
    }

    @Override
    public Mono<MailTemplateDto> updateTemplate(String id, MailTemplateDto template) {
        log.info("更新邮件模板 - id: {}", id);

        if (!templates.containsKey(id)) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + id));
        }

        template.setId(id);
        template.setUpdatedAt(LocalDateTime.now());
        templates.put(id, template);

        return Mono.just(template);
    }

    @Override
    public Mono<Void> deleteTemplate(String id) {
        log.info("删除邮件模板 - id: {}", id);

        if (!templates.containsKey(id)) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + id));
        }

        templates.remove(id);
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteTemplates(List<String> ids) {
        log.info("批量删除邮件模板 - count: {}", ids.size());

        ids.forEach(templates::remove);
        return Mono.empty();
    }

    @Override
    public Mono<TemplatePreviewResultDto> previewTemplate(TemplatePreviewRequestDto request) {
        log.info("预览邮件模板 - request: {}", request);

        // 创建预览结果
        TemplatePreviewResultDto result = TemplatePreviewResultDto.builder()
                .renderedSubject("预览主题")
                .renderedContent("预览内容")
                .success(true)
                .renderTimeMs(100L)
                .build();

        return Mono.just(result);
    }

    @Override
    public Mono<MailerTestResultDto> sendTestTemplateEmail(String templateId, String toEmail,
                                                           Map<String, Object> variables) {
        log.info("发送测试邮件 - templateId: {}, toEmail: {}", templateId, toEmail);

        // 模拟测试结果
        MailerTestResultDto result = MailerTestResultDto.builder()
                .success(true)
                .message("测试邮件发送成功")
                .responseTimeMs(150L)
                .build();

        return Mono.just(result);
    }

    @Override
    public Flux<String> getCategories() {
        log.info("获取模板分类列表");

        return Flux.fromIterable(Arrays.asList(
            "WELCOME", "VERIFICATION", "PASSWORD_RESET",
            "NOTIFICATION", "INVITATION", "REMINDER"
        ));
    }

    @Override
    public Flux<String> getSupportedLanguages() {
        log.info("获取支持的语言列表");

        return Flux.fromIterable(Arrays.asList("zh-CN", "en-US", "ja-JP"));
    }

    @Override
    public Mono<MailTemplateDto> importTemplate(MailTemplateDto template) {
        log.info("导入模板 - name: {}", template.getName());

        String templateId = UUID.randomUUID().toString();
        template.setId(templateId);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        templates.put(templateId, template);
        return Mono.just(template);
    }

    @Override
    public Mono<String> exportTemplate(String id) {
        log.info("导出模板 - id: {}", id);

        MailTemplateDto template = templates.get(id);
        if (template == null) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + id));
        }

        // 简单返回JSON字符串，实际应该返回格式化的导出数据
        return Mono.just("{\"template\": \"exported\"}");
    }

    @Override
    public Mono<MailTemplateDto> copyTemplate(String id, String newName, String newLanguage) {
        log.info("复制模板 - id: {}, newName: {}, newLanguage: {}", id, newName, newLanguage);

        MailTemplateDto original = templates.get(id);
        if (original == null) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + id));
        }

        MailTemplateDto copy = new MailTemplateDto();
        copy.setId(UUID.randomUUID().toString());
        copy.setName(newName);
        copy.setSubject(original.getSubject());
        copy.setContent(original.getContent());
        copy.setEnabled(false);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());

        templates.put(copy.getId(), copy);
        return Mono.just(copy);
    }

    @Override
    public Mono<Void> setDefaultTemplate(String identifier, String language, String templateId) {
        log.info("设置默认模板 - identifier: {}, language: {}, templateId: {}", identifier, language, templateId);

        if (!templates.containsKey(templateId)) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + templateId));
        }

        String key = identifier + "_" + language;
        defaultTemplates.put(key, templateId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> toggleTemplate(String id, Boolean enabled) {
        log.info("切换模板状态 - id: {}, enabled: {}", id, enabled);

        MailTemplateDto template = templates.get(id);
        if (template == null) {
            return Mono.error(new IllegalArgumentException("模板不存在: " + id));
        }

        template.setEnabled(enabled);
        template.setUpdatedAt(LocalDateTime.now());

        return Mono.empty();
    }

    @Override
    public Mono<Map<String, Object>> getTemplateVariables(String category) {
        log.info("获取模板变量定义 - category: {}", category);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "用户名");
        variables.put("email", "邮箱地址");
        variables.put("verificationLink", "验证链接");
        variables.put("resetLink", "重置链接");

        return Mono.just(variables);
    }

}
