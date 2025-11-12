package com.yunke.backend.community.controller;


import com.yunke.backend.community.dto.ShareToCommunityRequest;
import com.yunke.backend.community.enums.CommunityPermission;
import com.yunke.backend.document.controller.WorkspaceDocController;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 社区功能控制器
 * 提供文档分享到社区、获取社区文档列表等API
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {
    
    private final CommunityService communityService;
    private final WorkspaceDocController workspaceDocController;
    
    /**
     * 获取当前认证用户ID
     */
    private Mono<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.empty();
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return Mono.just(userDetails.getUserId());
    }
    
    /**
     * 分享文档到社区
     * POST /api/community/workspaces/{workspaceId}/docs/{docId}/share
     */
    @PostMapping("/workspaces/{workspaceId}/docs/{docId}/share")
    public Mono<ResponseEntity<Map<String, Object>>> shareDocToCommunity(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody ShareToCommunityRequest request) {
        
        log.info("分享文档到社区API调用: workspaceId={}, docId={}, permission={}", 
                workspaceId, docId, request.permission());
        
        return getCurrentUserId()
                .flatMap(userId -> communityService.shareDocToCommunity(
                        docId, workspaceId, userId, 
                        request.permission(), request.title(), request.description()))
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", "文档已成功分享到社区");
                    response.put("docId", docId);
                    response.put("permission", request.permission());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("分享文档到社区失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> 
                    ResponseEntity.status(401).body(Map.of("error", "未认证用户"))));
    }
    
    /**
     * 获取社区文档列表
     * GET /api/community/workspaces/{workspaceId}/docs
     */
    @GetMapping("/workspaces/{workspaceId}/docs")
    public Mono<ResponseEntity<Map<String, Object>>> getCommunityDocs(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        
        log.info("获取社区文档列表: workspaceId={}, page={}, size={}, search={}", 
                workspaceId, page, size, search);
        
        return getCurrentUserId()
                .flatMap(userId -> communityService.getCommunityDocs(workspaceId, userId, page, size, search))
                .map(docsPage -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("docs", docsPage.getContent());
                    response.put("page", page);
                    response.put("size", size);
                    response.put("total", docsPage.getTotalElements());
                    response.put("totalPages", docsPage.getTotalPages());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("获取社区文档列表失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> 
                    ResponseEntity.status(401).body(Map.of("error", "未认证用户"))));
    }
    
    /**
     * 取消文档在社区的分享
     * DELETE /api/community/workspaces/{workspaceId}/docs/{docId}/share
     */
    @DeleteMapping("/workspaces/{workspaceId}/docs/{docId}/share")
    public Mono<ResponseEntity<Map<String, Object>>> unshareDocFromCommunity(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.info("取消文档社区分享: workspaceId={}, docId={}", workspaceId, docId);
        
        return getCurrentUserId()
                .flatMap(userId -> communityService.unshareDocFromCommunity(docId, workspaceId, userId))
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", "已取消文档在社区的分享");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("取消社区分享失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> 
                    ResponseEntity.status(401).body(Map.of("error", "未认证用户"))));
    }
    
    /**
     * 更新文档社区权限
     * PUT /api/community/workspaces/{workspaceId}/docs/{docId}/permission
     */
    @PutMapping("/workspaces/{workspaceId}/docs/{docId}/permission")
    public Mono<ResponseEntity<Map<String, Object>>> updateCommunityPermission(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody Map<String, String> request) {
        
        CommunityPermission permission;
        try {
            permission = CommunityPermission.valueOf(request.get("permission"));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "无效的权限值");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
        
        log.info("更新文档社区权限: workspaceId={}, docId={}, permission={}", 
                workspaceId, docId, permission);
        
        return getCurrentUserId()
                .flatMap(userId -> communityService.updateCommunityPermission(docId, workspaceId, userId, permission))
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", "社区权限已更新");
                    response.put("permission", permission);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("更新社区权限失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> 
                    ResponseEntity.status(401).body(Map.of("error", "未认证用户"))));
    }
    
    /**
     * 增加文档浏览次数
     * POST /api/community/workspaces/{workspaceId}/docs/{docId}/view
     */
    @PostMapping("/workspaces/{workspaceId}/docs/{docId}/view")
    public Mono<ResponseEntity<Map<String, Object>>> incrementViewCount(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.info("增加文档浏览次数: workspaceId={}, docId={}", workspaceId, docId);
        
        return communityService.incrementViewCount(docId, workspaceId)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", "浏览次数已更新");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("增加浏览次数失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }
    
    /**
     * 检查用户是否可以查看社区文档
     * GET /api/community/workspaces/{workspaceId}/docs/{docId}/access
     */
    @GetMapping("/workspaces/{workspaceId}/docs/{docId}/access")
    public Mono<ResponseEntity<Map<String, Object>>> checkDocAccess(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.info("检查文档访问权限: workspaceId={}, docId={}", workspaceId, docId);
        
        return getCurrentUserId()
                .flatMap(userId -> communityService.canUserViewCommunityDoc(docId, workspaceId, userId))
                .map(canView -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("canView", canView);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("检查文档访问权限失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> 
                    ResponseEntity.status(401).body(Map.of("error", "未认证用户"))));
    }
    
    /**
     * 获取社区文档内容
     * GET /api/community/workspaces/{workspaceId}/docs/{docId}
     * 
     * 这个端点通过调用 WorkspaceDocController 来获取实际的文档内容
     */
    @GetMapping("/workspaces/{workspaceId}/docs/{docId}")
    public ResponseEntity<byte[]> getCommunityDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "X-User-ID", required = false) String userIdFromHeader,
            @RequestHeader(value = "X-State-Vector", required = false) String stateVectorHeader,
            @RequestParam(value = "userId", required = false) String userIdFromParam,
            @RequestParam(value = "stateVector", required = false) String stateVectorParam,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("🔄 [COMMUNITY-FORWARD] 社区文档请求: workspaceId={}, docId={}", workspaceId, docId);
        
        // 检查用户是否有权限访问社区文档
        String userId = extractUserId(authentication, userIdFromHeader, userIdFromParam, request);
        
        try {
            // 检查是否是社区文档访问权限
            Boolean canView = communityService.canUserViewCommunityDoc(docId, workspaceId, userId).block();
            
            if (Boolean.FALSE.equals(canView)) {
                log.warn("🚫 [COMMUNITY-FORWARD] 用户无权访问社区文档: userId={}, docId={}", userId, docId);
                String errorJson = "{\"success\":false,\"error\":\"您没有权限访问此社区文档\"}";
                return ResponseEntity.status(403)
                        .header("Content-Type", "application/json")
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            // 权限验证通过，直接调用 WorkspaceDocController 的 getDoc 方法
            log.info("✅ [COMMUNITY-FORWARD] 权限验证通过，调用 WorkspaceDocController.getDoc()");
            
//            return workspaceDocController.getDoc(
//                    workspaceId,
//                    docId,
//                    acceptHeader,
//                    userIdFromHeader,
//                    stateVectorHeader,
//                    userIdFromParam,
//                    stateVectorParam,
//                    authentication,
//                    request
//            );
            return null;
            
        } catch (Exception e) {
            log.error("🚫 [COMMUNITY-FORWARD] 处理社区文档请求失败: docId={}", docId, e);
            String errorJson = String.format("{\"success\":false,\"error\":\"获取文档失败: %s\"}", 
                    e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 辅助方法：从各种来源提取用户ID
     */
    private String extractUserId(Authentication authentication, String userIdFromHeader, 
                                 String userIdFromParam, jakarta.servlet.http.HttpServletRequest request) {
        // 从Authentication获取
        if (authentication != null && authentication.getPrincipal() instanceof AffineUserDetails) {
            return ((AffineUserDetails) authentication.getPrincipal()).getUserId();
        }
        
        // 从请求头获取
        if (userIdFromHeader != null && !userIdFromHeader.isEmpty()) {
            return userIdFromHeader;
        }
        
        // 从查询参数获取
        if (userIdFromParam != null && !userIdFromParam.isEmpty()) {
            return userIdFromParam;
        }
        
        // 从Cookie获取
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("affine_user".equals(cookie.getName()) && cookie.getValue() != null) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}