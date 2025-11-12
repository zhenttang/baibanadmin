package com.yunke.backend.ai.controller;

import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI控制器
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiService aiService;

    /**
     * 文本补全
     */
    @PostMapping("/complete")
    public Mono<ResponseEntity<Map<String, Object>>> complete(
            @RequestBody CompleteRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return aiService.complete(request.prompt(), request.provider(), request.model())
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("result", result);
                    response.put("provider", request.provider());
                    response.put("model", request.model());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "AI completion failed")));
    }

    /**
     * 流式文本补全
     */
    @PostMapping(value = "/complete/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> completeStream(
            @RequestBody CompleteRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Flux.error(new RuntimeException("Unauthorized"));
        }
        
        return aiService.completeStream(request.prompt(), request.provider(), request.model())
                .map(chunk -> "data: " + chunk + "\n\n")
                .onErrorResume(error -> Flux.just("data: [ERROR] " + error.getMessage() + "\n\n"));
    }

    /**
     * 聊天对话
     */
    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, Object>>> chat(
            @RequestBody ChatRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return aiService.chat(request.messages(), request.provider(), request.model())
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("result", result);
                    response.put("provider", request.provider());
                    response.put("model", request.model());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "AI chat failed")));
    }

    /**
     * 流式聊天对话
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestBody ChatRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Flux.error(new RuntimeException("Unauthorized"));
        }
        
        return aiService.chatStream(request.messages(), request.provider(), request.model())
                .map(chunk -> "data: " + chunk + "\n\n")
                .onErrorResume(error -> Flux.just("data: [ERROR] " + error.getMessage() + "\n\n"));
    }

    /**
     * 文档总结
     */
    @PostMapping("/summarize")
    public Mono<ResponseEntity<Map<String, Object>>> summarizeDocument(
            @RequestBody SummarizeRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return aiService.summarizeDocument(request.content(), request.language())
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("summary", result);
                    response.put("language", request.language());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Document summarization failed")));
    }

    /**
     * 文档翻译
     */
    @PostMapping("/translate")
    public Mono<ResponseEntity<Map<String, Object>>> translateDocument(
            @RequestBody TranslateRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return aiService.translateDocument(request.content(), request.sourceLanguage(), request.targetLanguage())
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("translation", result);
                    response.put("sourceLanguage", request.sourceLanguage());
                    response.put("targetLanguage", request.targetLanguage());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Document translation failed")));
    }

    /**
     * 代码生成
     */
    @PostMapping("/generate-code")
    public Mono<ResponseEntity<Map<String, Object>>> generateCode(
            @RequestBody GenerateCodeRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return aiService.generateCode(request.description(), request.language())
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("code", result);
                    response.put("language", request.language());
                    response.put("description", request.description());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Code generation failed")));
    }

    /**
     * 智能搜索
     */
    @PostMapping("/search")
    public Mono<ResponseEntity<Map<String, Object>>> smartSearch(
            @RequestBody SmartSearchRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        return aiService.smartSearch(request.query(), request.workspaceId(), userId)
                .map(results -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("results", results);
                    response.put("count", results.size());
                    response.put("query", request.query());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Smart search failed")));
    }

    /**
     * 获取AI会话历史
     */
    @GetMapping("/sessions")
    public Mono<ResponseEntity<Map<String, Object>>> getSessionHistory(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        return aiService.getSessionHistory(userId)
                .map(sessions -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("sessions", sessions);
                    response.put("count", sessions.size());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get session history")));
    }

    /**
     * 创建AI会话
     */
    @PostMapping("/sessions")
    public Mono<ResponseEntity<Map<String, Object>>> createSession(
            @RequestBody CreateSessionRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        return aiService.createSession(userId, request.title(), request.provider())
                .map(session -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("session", session);
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to create session")));
    }

    /**
     * 删除AI会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return aiService.deleteSession(sessionId)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Session deleted successfully");
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to delete session")));
    }

    // 请求数据类
    public record CompleteRequest(String prompt, AiService.AiProvider provider, String model) {}
    
    public record ChatRequest(List<AiService.ChatMessage> messages, AiService.AiProvider provider, String model) {}
    
    public record SummarizeRequest(String content, String language) {}
    
    public record TranslateRequest(String content, String sourceLanguage, String targetLanguage) {}
    
    public record GenerateCodeRequest(String description, String language) {}
    
    public record SmartSearchRequest(String query, String workspaceId) {}
    
    public record CreateSessionRequest(String title, AiService.AiProvider provider) {}
}