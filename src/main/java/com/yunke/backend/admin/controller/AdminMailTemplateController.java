package com.yunke.backend.admin.controller;

import com.yunke.backend.admin.service.AdminMailTemplateService;
import com.yunke.backend.notification.dto.*;
import com.yunke.backend.system.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 邮件模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/mailer/templates")
@RequiredArgsConstructor
@Tag(name = "邮件模板管理", description = "邮件模板的增删改查和管理")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminMailTemplateController {
    
    private final AdminMailTemplateService templateService;
    
    @GetMapping
    @Operation(summary = "分页查询邮件模板", description = "根据条件分页查询邮件模板列表")
    public Mono<ResponseEntity<Page<MailTemplateDto>>> getTemplates(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "模板分类") @RequestParam(required = false) String category,
            @Parameter(description = "语言代码") @RequestParam(required = false) String language,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "updatedAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDirection) {
        
        TemplateQueryDto query = new TemplateQueryDto();
        query.setPage(page);
        query.setSize(size);
        query.setKeyword(keyword);
        query.setCategory(category);
        query.setLanguage(language);
        query.setEnabled(enabled);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);
        
        return templateService.getTemplates(query)
                .map(ResponseEntity::ok)
                .doOnSuccess(templates -> log.info("查询邮件模板列表成功，页码: {}, 大小: {}", page, size))
                .onErrorResume(error -> {
                    log.error("查询邮件模板列表失败", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取邮件模板详情", description = "根据ID获取邮件模板详情")
    public Mono<ResponseEntity<MailTemplateDto>> getTemplate(
            @Parameter(description = "模板ID") @PathVariable String id) {
        return templateService.getTemplate(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnSuccess(template -> log.info("获取邮件模板详情成功: {}", id))
                .onErrorResume(error -> {
                    log.error("获取邮件模板详情失败: {}", id, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @GetMapping("/by-identifier/{identifier}")
    @Operation(summary = "根据标识获取模板", description = "根据模板标识和语言获取邮件模板")
    public Mono<ResponseEntity<MailTemplateDto>> getTemplateByIdentifier(
            @Parameter(description = "模板标识") @PathVariable String identifier,
            @Parameter(description = "语言代码") @RequestParam(defaultValue = "zh-CN") String language) {
        return templateService.getTemplateByIdentifier(identifier, language)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnSuccess(template -> log.info("根据标识获取邮件模板成功: {}, 语言: {}", identifier, language))
                .onErrorResume(error -> {
                    log.error("根据标识获取邮件模板失败: {}", identifier, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @PostMapping
    @Operation(summary = "创建邮件模板", description = "创建新的邮件模板")
    public Mono<ResponseEntity<MailTemplateDto>> createTemplate(
            @Valid @RequestBody MailTemplateDto template) {
        return templateService.createTemplate(template)
                .map(savedTemplate -> ResponseEntity.ok(savedTemplate))
                .doOnSuccess(savedTemplate -> log.info("创建邮件模板成功: {}", template.getName()))
                .onErrorResume(error -> {
                    log.error("创建邮件模板失败", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新邮件模板", description = "更新指定的邮件模板")
    public Mono<ResponseEntity<MailTemplateDto>> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable String id,
            @Valid @RequestBody MailTemplateDto template) {
        return templateService.updateTemplate(id, template)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnSuccess(updatedTemplate -> log.info("更新邮件模板成功: {}", id))
                .onErrorResume(error -> {
                    log.error("更新邮件模板失败: {}", id, error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除邮件模板", description = "删除指定的邮件模板")
    public Mono<ResponseEntity<Void>> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable String id) {
        return templateService.deleteTemplate(id)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> log.info("删除邮件模板成功: {}", id))
                .onErrorResume(error -> {
                    log.error("删除邮件模板失败: {}", id, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @DeleteMapping
    @Operation(summary = "批量删除邮件模板", description = "批量删除多个邮件模板")
    public Mono<ResponseEntity<Void>> deleteTemplates(
            @Parameter(description = "模板ID列表") @RequestBody List<String> ids) {
        return templateService.deleteTemplates(ids)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> log.info("批量删除邮件模板成功，数量: {}", ids.size()))
                .onErrorResume(error -> {
                    log.error("批量删除邮件模板失败", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @PostMapping("/preview")
    @Operation(summary = "预览邮件模板", description = "预览邮件模板渲染后的效果")
    public Mono<ResponseEntity<TemplatePreviewResultDto>> previewTemplate(
            @Valid @RequestBody TemplatePreviewRequestDto request) {
        return templateService.previewTemplate(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.info("邮件模板预览完成，成功: {}", result.getBody().getSuccess()))
                .onErrorResume(error -> {
                    log.error("邮件模板预览失败", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(TemplatePreviewResultDto.failure(error.getMessage())));
                });
    }
    
    @PostMapping("/{id}/send-test")
    @Operation(summary = "发送测试邮件", description = "使用指定模板发送测试邮件")
    public Mono<ResponseEntity<MailerTestResultDto>> sendTestTemplateEmail(
            @Parameter(description = "模板ID") @PathVariable String id,
            @Parameter(description = "收件人邮箱") @RequestParam String toEmail,
            @Parameter(description = "模板变量") @RequestBody(required = false) Map<String, Object> variables) {
        return templateService.sendTestTemplateEmail(id, toEmail, variables)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.info("发送测试模板邮件完成: {}, 成功: {}", id, result.getBody().getSuccess()))
                .onErrorResume(error -> {
                    log.error("发送测试模板邮件失败: {}", id, error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(MailerTestResultDto.failure("发送失败", error.getMessage())));
                });
    }
    
    @GetMapping("/categories")
    @Operation(summary = "获取模板分类", description = "获取所有可用的邮件模板分类")
    public Flux<String> getCategories() {
        return templateService.getCategories()
                .doOnComplete(() -> log.info("获取邮件模板分类列表成功"))
                .onErrorResume(error -> {
                    log.error("获取邮件模板分类列表失败", error);
                    return Flux.empty();
                });
    }
    
    @GetMapping("/languages")
    @Operation(summary = "获取支持的语言", description = "获取邮件模板支持的语言列表")
    public Flux<String> getSupportedLanguages() {
        return templateService.getSupportedLanguages()
                .doOnComplete(() -> log.info("获取支持的语言列表成功"))
                .onErrorResume(error -> {
                    log.error("获取支持的语言列表失败", error);
                    return Flux.empty();
                });
    }
    
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制邮件模板", description = "复制现有邮件模板")
    public Mono<ResponseEntity<MailTemplateDto>> copyTemplate(
            @Parameter(description = "源模板ID") @PathVariable String id,
            @Parameter(description = "新模板名称") @RequestParam String newName,
            @Parameter(description = "新模板语言") @RequestParam(required = false) String newLanguage) {
        return templateService.copyTemplate(id, newName, newLanguage)
                .map(ResponseEntity::ok)
                .doOnSuccess(copiedTemplate -> log.info("复制邮件模板成功: {} -> {}", id, newName))
                .onErrorResume(error -> {
                    log.error("复制邮件模板失败: {}", id, error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @PostMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用模板", description = "切换邮件模板的启用状态")
    public Mono<ResponseEntity<Void>> toggleTemplate(
            @Parameter(description = "模板ID") @PathVariable String id,
            @Parameter(description = "是否启用") @RequestParam Boolean enabled) {
        return templateService.toggleTemplate(id, enabled)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> log.info("切换邮件模板状态成功: {}, 启用: {}", id, enabled))
                .onErrorResume(error -> {
                    log.error("切换邮件模板状态失败: {}", id, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @PostMapping("/set-default")
    @Operation(summary = "设置默认模板", description = "设置指定模板为默认模板")
    public Mono<ResponseEntity<Void>> setDefaultTemplate(
            @Parameter(description = "模板标识") @RequestParam String identifier,
            @Parameter(description = "语言代码") @RequestParam String language,
            @Parameter(description = "模板ID") @RequestParam String templateId) {
        return templateService.setDefaultTemplate(identifier, language, templateId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> log.info("设置默认模板成功: {}, 语言: {}, 模板: {}", identifier, language, templateId))
                .onErrorResume(error -> {
                    log.error("设置默认模板失败", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    @GetMapping("/variables/{category}")
    @Operation(summary = "获取模板变量", description = "获取指定分类的模板变量定义")
    public Mono<ResponseEntity<Map<String, Object>>> getTemplateVariables(
            @Parameter(description = "模板分类") @PathVariable String category) {
        return templateService.getTemplateVariables(category)
                .map(ResponseEntity::ok)
                .doOnSuccess(variables -> log.info("获取模板变量成功: {}", category))
                .onErrorResume(error -> {
                    log.error("获取模板变量失败: {}", category, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}