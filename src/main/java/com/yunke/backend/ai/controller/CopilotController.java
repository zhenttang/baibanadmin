package com.yunke.backend.ai.controller;

import com.yunke.backend.ai.dto.CreateChatSessionInput;
import com.yunke.backend.ai.dto.CopilotSessionDto;
import com.yunke.backend.ai.dto.CreateChatMessageInput;
import com.yunke.backend.ai.dto.CopilotMessageDto;
import com.yunke.backend.ai.dto.CopilotQuotaDto;
import com.yunke.backend.ai.domain.entity.CopilotQuota;
import com.yunke.backend.ai.domain.entity.CopilotSession;
import com.yunke.backend.ai.domain.entity.CopilotMessage;
import com.yunke.backend.ai.service.CopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

/**
 * Copilot REST API控制器
 * 对应Node.js版本的CopilotController
 */
@Slf4j
@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService copilotService;

    // ==================== 会话管理 ====================

    /**
     * 创建聊天会话
     * POST /api/copilot/sessions
     */
    @PostMapping("/sessions")
    public Mono<ResponseEntity<CopilotSessionDto>> createSession(
            @RequestBody CreateChatSessionInput input,
            Principal principal) {
        
        log.info("Received createSession request, principal: {}", principal);
        log.info("Request body: {}", input);
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.createSession(input, userId)
                .map(session -> ResponseEntity.ok(session))
                .onErrorResume(e -> {
                    log.error("Failed to create Copilot session", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取会话详情
     * GET /api/copilot/sessions/:sessionId
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<ResponseEntity<CopilotSessionDto>> getSession(
            @PathVariable String sessionId,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getSession(sessionId, userId)
                .map(session -> ResponseEntity.ok(session))
                .onErrorResume(e -> {
                    log.error("Failed to get Copilot session: {}", sessionId, e);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    /**
     * 获取用户会话列表
     * GET /api/copilot/sessions
     */
    @GetMapping("/sessions")
    public Mono<ResponseEntity<List<CopilotSessionDto>>> getUserSessions(
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getUserSessions(userId, limit)
                .map(sessions -> ResponseEntity.ok(sessions))
                .onErrorResume(e -> {
                    log.error("Failed to get user Copilot sessions", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取工作空间会话列表
     * GET /api/copilot/workspaces/:workspaceId/sessions
     */
    @GetMapping("/workspaces/{workspaceId}/sessions")
    public Mono<ResponseEntity<List<CopilotSessionDto>>> getWorkspaceSessions(
            @PathVariable String workspaceId,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getWorkspaceSessions(workspaceId, userId, limit)
                .map(sessions -> ResponseEntity.ok(sessions))
                .onErrorResume(e -> {
                    log.error("Failed to get workspace Copilot sessions: {}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 结束会话
     * POST /api/copilot/sessions/:sessionId/finish
     */
    @PostMapping("/sessions/{sessionId}/finish")
    public Mono<ResponseEntity<Boolean>> finishSession(
            @PathVariable String sessionId,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.finishSession(sessionId, userId)
                .map(success -> ResponseEntity.ok(success))
                .onErrorResume(e -> {
                    log.error("Failed to finish Copilot session: {}", sessionId, e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    /**
     * 删除会话
     * DELETE /api/copilot/sessions/:sessionId
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Mono<ResponseEntity<Boolean>> deleteSession(
            @PathVariable String sessionId,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.deleteSession(sessionId, userId)
                .map(success -> ResponseEntity.ok(success))
                .onErrorResume(e -> {
                    log.error("Failed to delete Copilot session: {}", sessionId, e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    /**
     * 清理用户所有会话
     * DELETE /api/copilot/sessions
     */
    @DeleteMapping("/sessions")
    public Mono<ResponseEntity<Integer>> cleanupUserSessions(Principal principal) {
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.cleanupUserSessions(userId)
                .map(count -> ResponseEntity.ok(count))
                .onErrorResume(e -> {
                    log.error("Failed to cleanup user Copilot sessions", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    // ==================== 调试端点 ====================
    
    /**
     * 测试端点 - 检查控制器是否可达
     */
    @PostMapping("/sessions/{sessionId}/test")
    public ResponseEntity<String> testEndpoint(
            @PathVariable String sessionId,
            @RequestBody(required = false) String rawBody,
            Principal principal) {
        
        log.info("=== 测试端点被调用 ===");
        log.info("Session ID: {}", sessionId);
        log.info("Principal: {}", principal);  
        log.info("Raw body: {}", rawBody);
        
        return ResponseEntity.ok("测试成功 - 控制器可达");
    }

    // ==================== 消息管理 ====================

    /**
     * 发送消息
     * POST /api/copilot/sessions/:sessionId/messages
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public Mono<ResponseEntity<CopilotMessageDto>> sendMessage(
            @PathVariable String sessionId,
            @RequestBody CreateChatMessageInput input,
            Principal principal) {
        
        log.info("=== CopilotController.sendMessage START ===");
        log.info("Session ID: {}", sessionId);
        log.info("Principal: {}", principal);
        log.info("Input object: {}", input);
        log.info("Input class: {}", input != null ? input.getClass().getName() : "null");
        
        try {
            input.setSessionId(sessionId);
            
            // 处理匿名用户的情况
            String userId = principal != null ? principal.getName() : "anonymous";
            log.info("Using userId: {}", userId);
            
            return copilotService.sendMessage(input, userId)
                    .map(message -> {
                        log.info("Message sent successfully: {}", message);
                        return ResponseEntity.ok(message);
                    })
                    .onErrorResume(e -> {
                        log.error("Failed to send Copilot message to session: {}", sessionId, e);
                        return Mono.just(ResponseEntity.badRequest().build());
                    });
        } catch (Exception e) {
            log.error("Exception in sendMessage controller: ", e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * 发送流式消息 - POST方法
     * POST /api/copilot/sessions/:sessionId/stream
     */
    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendStreamMessagePost(
            @PathVariable String sessionId,
            @RequestBody CreateChatMessageInput input,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        // 设置session ID和stream模式
        input.setSessionId(sessionId);
        input.setStream(true);
        
        log.info("=== 流式消息请求 (POST) ===");
        log.info("Session ID: {}, Content: {}, Stream: {}", sessionId, input.getContent(), input.getStream());
        
        return copilotService.sendStreamMessage(input, userId)
                .map(messageDto -> {
                    String content_chunk = messageDto.getContent();
                    log.debug("流式响应片段: '{}'", content_chunk);
                    return content_chunk;
                })
                .filter(chunk -> chunk != null && !chunk.trim().isEmpty()) // 过滤空内容
                .doOnNext(chunk -> log.debug("发送有效流式内容: '{}'", chunk))
                .doOnComplete(() -> {
                    log.info("=== 流式响应完成 ===");
                })
                .doOnError(e -> {
                    log.error("Failed to send streaming Copilot message to session: {}", sessionId, e);
                });
    }

    /**
     * 发送流式消息 - GET方法（保持兼容性）
     * GET /api/copilot/sessions/:sessionId/stream?content=xxx
     */
    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendStreamMessageGet(
            @PathVariable String sessionId,
            @RequestParam(value = "content", defaultValue = "Hello") String content,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        // 创建输入对象
        CreateChatMessageInput input = new CreateChatMessageInput();
        input.setSessionId(sessionId);
        input.setContent(content);
        input.setRole(CopilotMessage.MessageRole.USER);
        input.setStream(true);
        
        log.info("=== 流式消息请求 (GET) ===");
        log.info("Session ID: {}, Content: {}, Stream: {}", sessionId, content, input.getStream());
        
        return copilotService.sendStreamMessage(input, userId)
                .map(messageDto -> {
                    String content_chunk = messageDto.getContent();
                    log.debug("流式响应片段: '{}'", content_chunk);
                    return content_chunk;
                })
                .filter(chunk -> chunk != null && !chunk.trim().isEmpty()) // 过滤空内容
                .doOnNext(chunk -> log.debug("发送有效流式内容: '{}'", chunk))
                .doOnComplete(() -> {
                    log.info("=== 流式响应完成 ===");
                })
                .doOnError(e -> {
                    log.error("Failed to send streaming Copilot message to session: {}", sessionId, e);
                });
    }

    /**
     * 获取会话消息
     * GET /api/copilot/sessions/:sessionId/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Mono<ResponseEntity<List<CopilotMessageDto>>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getSessionMessages(sessionId, userId, limit, offset)
                .map(messages -> ResponseEntity.ok(messages))
                .onErrorResume(e -> {
                    log.error("Failed to get messages for session: {}", sessionId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 删除消息
     * DELETE /api/copilot/messages/:messageId
     */
    @DeleteMapping("/messages/{messageId}")
    public Mono<ResponseEntity<Boolean>> deleteMessage(
            @PathVariable String messageId,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.deleteMessage(messageId, userId)
                .map(success -> ResponseEntity.ok(success))
                .onErrorResume(e -> {
                    log.error("Failed to delete Copilot message: {}", messageId, e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    /**
     * 搜索消息
     * GET /api/copilot/sessions/:sessionId/messages/search
     */
    @GetMapping("/sessions/{sessionId}/messages/search")
    public Mono<ResponseEntity<List<CopilotMessageDto>>> searchMessages(
            @PathVariable String sessionId,
            @RequestParam("q") String query,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.searchMessages(sessionId, query, userId)
                .map(messages -> ResponseEntity.ok(messages))
                .onErrorResume(e -> {
                    log.error("Failed to search messages in session: {}", sessionId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    // ==================== 配额管理 ====================

    /**
     * 获取用户配额
     * GET /api/copilot/quota
     */
    @GetMapping("/quota")
    public Mono<ResponseEntity<List<CopilotQuotaDto>>> getUserQuotas(Principal principal) {
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getAllUserQuotas(userId)
                .map(quotas -> ResponseEntity.ok(quotas))
                .onErrorResume(e -> {
                    log.error("Failed to get user quotas", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取特定功能的用户配额
     * GET /api/copilot/quota/:feature
     */
    @GetMapping("/quota/{feature}")
    public Mono<ResponseEntity<CopilotQuotaDto>> getUserQuota(
            @PathVariable CopilotQuota.CopilotFeature feature,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getUserQuota(userId, feature)
                .map(quota -> ResponseEntity.ok(quota))
                .onErrorResume(e -> {
                    log.error("Failed to get user quota for feature: {}", feature, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取工作空间配额
     * GET /api/copilot/workspaces/:workspaceId/quota
     */
    @GetMapping("/workspaces/{workspaceId}/quota")
    public Mono<ResponseEntity<List<CopilotQuotaDto>>> getWorkspaceQuotas(
            @PathVariable String workspaceId,
            Principal principal) {
        
        return copilotService.getAllWorkspaceQuotas(workspaceId)
                .map(quotas -> ResponseEntity.ok(quotas))
                .onErrorResume(e -> {
                    log.error("Failed to get workspace quotas: {}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取特定功能的工作空间配额
     * GET /api/copilot/workspaces/:workspaceId/quota/:feature
     */
    @GetMapping("/workspaces/{workspaceId}/quota/{feature}")
    public Mono<ResponseEntity<CopilotQuotaDto>> getWorkspaceQuota(
            @PathVariable String workspaceId,
            @PathVariable CopilotQuota.CopilotFeature feature,
            Principal principal) {
        
        return copilotService.getWorkspaceQuota(workspaceId, feature)
                .map(quota -> ResponseEntity.ok(quota))
                .onErrorResume(e -> {
                    log.error("Failed to get workspace quota: {} for feature: {}", workspaceId, feature, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    // ==================== AI提供商管理 ====================

    /**
     * 获取可用的AI提供商
     * GET /api/copilot/providers
     */
    @GetMapping("/providers")
    public Mono<ResponseEntity<List<String>>> getAvailableProviders() {
        return copilotService.getAvailableProviders()
                .map(providers -> ResponseEntity.ok(providers))
                .onErrorResume(e -> {
                    log.error("Failed to get available providers", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取提供商支持的模型
     * GET /api/copilot/providers/:provider/models
     */
    @GetMapping("/providers/{provider}/models")
    public Mono<ResponseEntity<List<String>>> getProviderModels(
            @PathVariable CopilotSession.AIProvider provider) {
        
        return copilotService.getProviderModels(provider)
                .map(models -> ResponseEntity.ok(models))
                .onErrorResume(e -> {
                    log.error("Failed to get models for provider: {}", provider, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 验证提供商配置
     * POST /api/copilot/providers/:provider/validate
     */
    @PostMapping("/providers/{provider}/validate")
    public Mono<ResponseEntity<Boolean>> validateProviderConfig(
            @PathVariable CopilotSession.AIProvider provider) {
        
        return copilotService.validateProviderConfig(provider)
                .map(valid -> ResponseEntity.ok(valid))
                .onErrorResume(e -> {
                    log.error("Failed to validate provider config: {}", provider, e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    // ==================== 统计和监控 ====================

    /**
     * 获取用户使用统计
     * GET /api/copilot/stats/user
     */
    @GetMapping("/stats/user")
    public Mono<ResponseEntity<CopilotService.UsageStatsDto>> getUserUsageStats(Principal principal) {
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getUserUsageStats(userId)
                .map(stats -> ResponseEntity.ok(stats))
                .onErrorResume(e -> {
                    log.error("Failed to get user usage stats", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取工作空间使用统计
     * GET /api/copilot/stats/workspaces/:workspaceId
     */
    @GetMapping("/stats/workspaces/{workspaceId}")
    public Mono<ResponseEntity<CopilotService.UsageStatsDto>> getWorkspaceUsageStats(
            @PathVariable String workspaceId,
            Principal principal) {
        
        return copilotService.getWorkspaceUsageStats(workspaceId)
                .map(stats -> ResponseEntity.ok(stats))
                .onErrorResume(e -> {
                    log.error("Failed to get workspace usage stats: {}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取会话统计
     * GET /api/copilot/sessions/:sessionId/stats
     */
    @GetMapping("/sessions/{sessionId}/stats")
    public Mono<ResponseEntity<CopilotService.SessionStatsDto>> getSessionStats(
            @PathVariable String sessionId,
            Principal principal) {
        
        // 处理匿名用户的情况
        String userId = principal != null ? principal.getName() : "anonymous";
        
        return copilotService.getSessionStats(sessionId, userId)
                .map(stats -> ResponseEntity.ok(stats))
                .onErrorResume(e -> {
                    log.error("Failed to get session stats: {}", sessionId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
}