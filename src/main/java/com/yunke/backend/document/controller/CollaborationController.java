package com.yunke.backend.document.controller;

import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.document.service.CollaborationService;
import com.yunke.backend.workspace.service.WorkspaceDocService;
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
import java.util.UUID;

/**
 * 协作控制器
 * 处理实时协作相关的HTTP和WebSocket请求
 */
@RestController
@RequestMapping("/api/collaboration")
@RequiredArgsConstructor
@Slf4j
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final WorkspaceDocService docService;

    /**
     * 加入文档协作
     */
    @PostMapping("/docs/{docId}/join")
    public Mono<ResponseEntity<Map<String, Object>>> joinDocument(
            @PathVariable String docId,
            @RequestBody JoinDocumentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        return collaborationService.joinDocument(docId, userId, request.sessionId())
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Joined document collaboration successfully");
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to join document")));
    }

    /**
     * 离开文档协作
     */
    @PostMapping("/docs/{docId}/leave")
    public Mono<ResponseEntity<Map<String, Object>>> leaveDocument(
            @PathVariable String docId,
            @RequestBody LeaveDocumentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        return collaborationService.leaveDocument(docId, userId, request.sessionId())
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Left document collaboration successfully");
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to leave document")));
    }

    /**
     * 获取活跃协作者
     */
    @GetMapping("/docs/{docId}/collaborators")
    public Mono<ResponseEntity<Map<String, Object>>> getActiveCollaborators(
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
        }
        
        return collaborationService.getActiveCollaborators(docId)
                .map(collaborators -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("collaborators", collaborators);
                    response.put("count", collaborators.size());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get collaborators")));
    }

    /**
     * 发送操作
     */
    @PostMapping("/docs/{docId}/operations")
    public Mono<ResponseEntity<Map<String, Object>>> sendOperation(
            @PathVariable String docId,
            @RequestBody SendOperationRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 创建操作对象
        CollaborationService.Operation operation = new CollaborationService.Operation(
                UUID.randomUUID().toString(),
                request.type(),
                docId,
                userId,
                request.data(),
                Instant.now()
        );
        
        return collaborationService.handleOperation(docId, userId, operation)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("operationId", operation.id());
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to send operation")));
    }

    /**
     * 获取操作流（Server-Sent Events）
     */
    @GetMapping(value = "/docs/{docId}/operations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CollaborationService.Operation> getOperationStream(
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Flux.error(new IllegalArgumentException("Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        return collaborationService.getOperationStream(docId, userId);
    }

    /**
     * 获取文档状态
     */
    @GetMapping("/docs/{docId}/state")
    public Mono<ResponseEntity<Map<String, Object>>> getDocumentState(
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
        }
        
        return collaborationService.getDocumentState(docId)
                .map(state -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("state", state);
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get document state")));
    }

    /**
     * 保存文档快照
     */
    @PostMapping("/docs/{docId}/snapshot")
    public Mono<ResponseEntity<Map<String, Object>>> saveSnapshot(
            @PathVariable String docId,
            @RequestBody SaveSnapshotRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档编辑权限
        if (!docService.hasDocEditPermission(docId, userId)) {
            return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
        }
        
        return collaborationService.saveSnapshot(docId, request.content())
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Snapshot saved successfully");
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to save snapshot")));
    }

    /**
     * 获取文档历史记录
     */
    @GetMapping("/docs/{docId}/history")
    public Mono<ResponseEntity<Map<String, Object>>> getDocumentHistory(
            @PathVariable String docId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
        }
        
        return collaborationService.getDocumentHistory(docId, limit)
                .map(history -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("history", history);
                    response.put("count", history.size());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get document history")));
    }

    // 请求数据类
    public record JoinDocumentRequest(String sessionId) {}
    public record LeaveDocumentRequest(String sessionId) {}
    public record SendOperationRequest(String type, Map<String, Object> data) {}
    public record SaveSnapshotRequest(String content) {}
}