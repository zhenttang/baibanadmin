package com.yunke.backend.document.controller;

import com.yunke.backend.document.dto.DocDefaultRoleDto;
import com.yunke.backend.document.dto.DocRecord;
import com.yunke.backend.document.dto.DocRoleGrantRequest;
import com.yunke.backend.document.dto.DocRolePageDto;
import com.yunke.backend.document.dto.DocRoleUpdateRequest;
import com.yunke.backend.document.service.DocRoleService;
import com.yunke.backend.document.service.RootDocumentService;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import com.yunke.backend.storage.impl.WorkspaceDocStorageAdapter;
import com.yunke.backend.storage.service.PgWorkspaceDocStorageAdapter;
import com.yunke.backend.system.domain.entity.Snapshot;
import com.yunke.backend.user.domain.entity.UserSnapshot;
import com.yunke.backend.infrastructure.util.WorkspaceIdConverter;
import com.yunke.backend.workspace.domain.entity.Workspace;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.document.repository.DocRecordRepository;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.workspace.service.UserspaceSyncService;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.workspace.service.WorkspaceService;

import com.yunke.backend.workspace.service.WorkspaceIdMappingService;


import com.yunke.backend.system.service.DatabaseSyncService;
import com.yunke.backend.document.util.DocID;
import com.yunke.backend.infrastructure.util.IdConverter;
import com.yunke.backend.document.util.YjsUtils;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationContext;
import com.yunke.backend.document.service.DocReader;
import com.yunke.backend.document.dto.DocPermissionInfo;
import com.yunke.backend.document.service.impl.WorkspaceDocServiceImpl;

/**
 * 工作空间文档控制器
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class WorkspaceDocController {

    private final WorkspaceDocService docService;
    private final WorkspaceService workService;
    private final PermissionService permissionService;
    private final ApplicationContext applicationContext;
    private final WorkspaceDocStorageAdapter storageAdapter;
    private final DatabaseSyncService databaseSyncService;
    private final IdConverter idConverter;
    private final WorkspaceIdMappingService workspaceIdMappingService;
    private final WorkspaceIdConverter workspaceIdConverter;
    private final UserspaceSyncService userspaceSyncService;
    private final PgWorkspaceDocStorageAdapter pgWorkspaceDocStorageAdapter;
    private final WorkspaceDocRepository workspaceDocRepository;
    private final SnapshotRepository snapshotRepository;
    private final DocRecordRepository docRecordRepository;
    private final DocBinaryStorageService binaryStorageService;
    private final YjsUtils yjsUtils;
    private final DocRoleService docRoleService;

    // ===== Doc roles & permissions (bitmask) =====
    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}/roles")
    public ResponseEntity<?> getDocRoles(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestParam(name = "first", defaultValue = "8") int first,
            @RequestParam(name = "after", required = false) String after,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        DocRolePageDto dto = docRoleService.listDocRoles(workspaceId, docId, first, after);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/workspaces/{workspaceId}/docs/{docId}/roles/grant")
    public ResponseEntity<Map<String, Object>> grantDocRoles(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody DocRoleGrantRequest request,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            docRoleService.grantDocRoles(workspaceId, docId, request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/api/workspaces/{workspaceId}/docs/{docId}/roles/{userId}")
    public ResponseEntity<Map<String, Object>> updateDocRole(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @PathVariable String userId,
            @RequestBody DocRoleUpdateRequest request,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            docRoleService.updateDocRole(workspaceId, docId, userId, request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/docs/{docId}/roles/{userId}")
    public ResponseEntity<Map<String, Object>> deleteDocRole(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @PathVariable String userId,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        docRoleService.deleteDocRole(workspaceId, docId, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}/default-role")
    public ResponseEntity<?> getDefaultRole(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            DocDefaultRoleDto dto = docRoleService.getDefaultRole(workspaceId, docId);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/api/workspaces/{workspaceId}/docs/{docId}/default-role")
    public ResponseEntity<Map<String, Object>> updateDefaultRole(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody DocRoleUpdateRequest request,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            DocDefaultRoleDto dto = docRoleService.updateDefaultRole(workspaceId, docId, request);
            Map<String, Object> payload = new HashMap<>();
            payload.put("success", true);
            payload.put("defaultRole", dto);
            return ResponseEntity.ok(payload);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * 获取文档所属的工作空间ID
     */
    @GetMapping("/api/docs/{docId}/workspace")
    public ResponseEntity<Map<String, Object>> getWorkspaceByDocId(
            @PathVariable String docId,
            @RequestHeader(value = "X-User-ID", required = false) String userIdFromHeader,
            @RequestParam(value = "userId", required = false) String userIdFromParam,
            Authentication authentication,
            HttpServletRequest request) {
        
        log.info("获取文档所属工作空间: docId={}", docId);
        
        // 获取用户ID，类似于getDoc方法
        String userId = extractUserId(authentication, userIdFromHeader, userIdFromParam, request);
        
        // 🔄 使用ID转换器转换文档ID
        String convertedDocId = workspaceIdConverter.convertDocId(docId);
        
        // 查找文档所属的工作空间ID
        Optional<String> workspaceId = docService.findWorkspaceIdByDocId(convertedDocId);
        
        if (workspaceId.isEmpty()) {
            log.warn("找不到文档: docId={}, convertedDocId={}", docId, convertedDocId);
            // 🚨 不再返回默认工作空间，这是虚假数据！
            // 返回404让前端正确处理
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Document not found");
            response.put("docId", docId);
            return ResponseEntity.status(404).body(response);
        }
        
        // 如果有用户ID，检查用户是否有权访问该文档
        // TODO 文档权限校验先注释
//        if (userId != null && !permissionService.hasWorkspaceAccess(userId, workspaceId.get())) {
//            log.warn("用户无权访问此文档: userId={}, docId={}", userId, docId);
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("success", false);
//            errorResponse.put("error", "Access denied");
//            return ResponseEntity.status(403).body(errorResponse);
//        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("workspaceId", workspaceId.get());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 辅助方法：从各种来源提取用户ID
     */
    private String extractUserId(Authentication authentication, String userIdFromHeader, String userIdFromParam, HttpServletRequest request) {
        String userId = null;
        
        // 从Authentication获取
        if (authentication != null && authentication.getPrincipal() instanceof AffineUserDetails) {
            userId = ((AffineUserDetails) authentication.getPrincipal()).getUserId();
            log.debug("从Authentication获取用户ID: {}", userId);
            return userId;
        }
        
        // 从SecurityContextHolder获取
        Authentication securityAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (securityAuth != null && securityAuth.isAuthenticated() && securityAuth.getPrincipal() instanceof AffineUserDetails) {
            userId = ((AffineUserDetails) securityAuth.getPrincipal()).getUserId();
            log.debug("从SecurityContextHolder获取用户ID: {}", userId);
            return userId;
        }
        
        // 从请求头获取
        if (userIdFromHeader != null && !userIdFromHeader.isEmpty()) {
            log.debug("从X-User-ID头获取用户ID: {}", userIdFromHeader);
            return userIdFromHeader;
        }
        
        // 从查询参数获取
        if (userIdFromParam != null && !userIdFromParam.isEmpty()) {
            log.debug("从查询参数获取用户ID: {}", userIdFromParam);
            return userIdFromParam;
        }
        
        // 从Cookie获取
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("affine_user".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    log.debug("从Cookie获取用户ID: {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        
        log.debug("无法获取用户ID");
        return null;
    }

    /**
     * 获取文档创建者/更新者信息（用于迁移）
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/created-updated")
    public ResponseEntity<Map<String, Object>> getDocsCreatedUpdated(
            @PathVariable String workspaceId,
            @RequestParam(name = "first", defaultValue = "100") int first,
            @RequestParam(name = "after", required = false) String after,
            Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        String convertedWorkspaceId = workspaceIdConverter.convertWorkspaceId(workspaceId);

        if (!permissionService.hasWorkspaceAccess(userId, convertedWorkspaceId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        int pageSize = Math.max(1, Math.min(first, 500));

        List<WorkspaceDoc> allDocs = workspaceDocRepository.findByWorkspaceId(convertedWorkspaceId);
        allDocs.sort((a, b) -> {
            LocalDateTime aTime = a.getUpdatedAt();
            LocalDateTime bTime = b.getUpdatedAt();
            if (aTime == null && bTime == null) {
                return 0;
            }
            if (aTime == null) {
                return 1;
            }
            if (bTime == null) {
                return -1;
            }
            return bTime.compareTo(aTime);
        });

        int startIndex = 0;
        if (after != null && !after.isBlank()) {
            for (int i = 0; i < allDocs.size(); i++) {
                if (after.equals(allDocs.get(i).getDocId())) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        int endIndex = Math.min(startIndex + pageSize, allDocs.size());
        List<WorkspaceDoc> slice = allDocs.subList(startIndex, endIndex);
        boolean hasNextPage = endIndex < allDocs.size();
        String endCursor = slice.isEmpty() ? null : slice.get(slice.size() - 1).getDocId();

        List<Map<String, Object>> edges = new ArrayList<>();
        for (WorkspaceDoc doc : slice) {
            String docId = doc.getDocId();

            var snapshotOpt = snapshotRepository.findByWorkspaceIdAndId(convertedWorkspaceId, docId);
            String creatorId = snapshotOpt.map(Snapshot::getCreatedBy).orElse(null);
            String lastUpdaterId = snapshotOpt.map(Snapshot::getUpdatedBy)
                    .filter(id -> id != null && !id.isBlank())
                    .orElseGet(() -> docRecordRepository
                            .findBySpaceIdAndDocId(convertedWorkspaceId, docId)
                            .map(com.yunke.backend.document.domain.entity.DocRecord::getEditorId)
                            .orElse(null));

            LocalDateTime updatedAt = snapshotOpt.map(Snapshot::getUpdatedAt).orElse(doc.getUpdatedAt());
            LocalDateTime createdAt = doc.getCreatedAt();

            Map<String, Object> node = new HashMap<>();
            node.put("id", docId);
            if (creatorId != null && !creatorId.isBlank()) {
                node.put("creatorId", creatorId);
            }
            if (lastUpdaterId != null && !lastUpdaterId.isBlank()) {
                node.put("lastUpdaterId", lastUpdaterId);
            }
            if (createdAt != null) {
                node.put("createdAt", createdAt.atZone(ZoneId.systemDefault()).toInstant().toString());
            }
            if (updatedAt != null) {
                node.put("updatedAt", updatedAt.atZone(ZoneId.systemDefault()).toInstant().toString());
            }

            Map<String, Object> edge = new HashMap<>();
            edge.put("node", node);
            edges.add(edge);
        }

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("hasNextPage", hasNextPage);
        pageInfo.put("endCursor", endCursor);

        Map<String, Object> docsPayload = new HashMap<>();
        docsPayload.put("edges", edges);
        docsPayload.put("totalCount", allDocs.size());
        docsPayload.put("pageInfo", pageInfo);

        Map<String, Object> workspacePayload = new HashMap<>();
        workspacePayload.put("docs", docsPayload);

        Map<String, Object> response = new HashMap<>();
        response.put("workspace", workspacePayload);

        return ResponseEntity.ok(response);
    }

    /**
     * 创建文档
     * POST /api/workspaces/{workspaceId}/docs
     */
    @PostMapping("/api/workspaces/{workspaceId}/docs")
    public ResponseEntity<Map<String, Object>> createDoc(
            @PathVariable String workspaceId,
            @RequestBody CreateDocRequest request,
            Authentication authentication) {
        
        log.info("📝📝📝 [DOC-CREATE-API] ========== 创建文档请求开始 ==========");
        log.info("📝📝📝 [DOC-CREATE-API] 请求路径: POST /api/workspaces/{}/docs", workspaceId);
        log.info("📝📝📝 [DOC-CREATE-API] 路径参数: workspaceId='{}'", workspaceId);
        log.info("📝📝📝 [DOC-CREATE-API] 请求体: title='{}', docId='{}'", 
                request != null ? request.title() : "null", 
                request != null ? request.docId() : "null");
        log.info("📝📝📝 [DOC-CREATE-API] 认证信息: authentication={}", 
                authentication != null ? authentication.getClass().getSimpleName() : "null");
        
        // 1. 认证检查
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            log.error("📝📝📝 [DOC-CREATE-API] ❌ 认证失败: authentication={}", authentication);
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        log.info("📝📝📝 [DOC-CREATE-API] ✅ 认证通过: userId='{}'", userId);
        
        // 2. 请求体验证
        if (request == null) {
            log.error("📝📝📝 [DOC-CREATE-API] ❌ 请求体为空");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body is required"));
        }
        
        // 3. 工作空间访问权限检查
        log.info("📝📝📝 [DOC-CREATE-API] 检查工作空间访问权限: userId='{}', workspaceId='{}'", userId, workspaceId);
        boolean hasAccess = permissionService.hasWorkspaceAccess(userId, workspaceId);
        if (!hasAccess) {
            log.error("📝📝📝 [DOC-CREATE-API] ❌ 权限检查失败: userId='{}', workspaceId='{}'", userId, workspaceId);
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied",
                    "userId", userId,
                    "workspaceId", workspaceId,
                    "message", "You do not have permission to create documents in this workspace"
            ));
        }
        log.info("📝📝📝 [DOC-CREATE-API] ✅ 权限检查通过");
        
        // 4. 调用服务创建文档
        try {
            log.info("📝📝📝 [DOC-CREATE-API] 调用 docService.createDoc: workspaceId='{}', userId='{}', title='{}', docId='{}'", 
                    workspaceId, userId, request.title(), request.docId());
            
            WorkspaceDoc doc = docService.createDoc(
                workspaceId,
                userId,
                request.title(),
                request.docId()
            );
            
            log.info("📝📝📝 [DOC-CREATE-API] ✅ 文档创建成功: docId='{}', title='{}'", 
                    doc.getDocId(), doc.getTitle());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("doc", doc);
            response.put("docId", doc.getDocId());
            response.put("workspaceId", doc.getWorkspaceId());
            response.put("title", doc.getTitle());
            
            log.info("📝📝📝 [DOC-CREATE-API] ========== 创建文档请求成功 ==========");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("📝📝📝 [DOC-CREATE-API] ❌ 参数错误: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid request",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("📝📝📝 [DOC-CREATE-API] ❌ 创建文档失败: workspaceId='{}', userId='{}', error={}", 
                    workspaceId, userId, e.getMessage(), e);
            log.error("📝📝📝 [DOC-CREATE-API] 异常堆栈:", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to create document",
                    "message", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * 获取工作空间文档列表
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs")
    public ResponseEntity<Map<String, Object>> getWorkspaceDocs(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        log.info("🗂️🗂️🗂️ [CRITICAL-DEBUG] 获取工作空间文档列表API被调用!!!");
        log.info("  📋 原始请求参数: workspaceId='{}', page={}, size={}, sortBy={}, sortDir={}", 
                workspaceId, page, size, sortBy, sortDir);
        
        // 🔄 [关键修改] 使用统一ID转换
        String convertedWorkspaceId = workspaceIdConverter.convertWorkspaceId(workspaceId);
        log.info("  🔄 [ID-CONVERT] 工作空间ID转换: '{}' -> '{}'", workspaceId, convertedWorkspaceId);
        
        log.info("  🔐 认证状态: authentication={}", authentication != null ? "存在" : "null");
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            log.error("❌ [文档列表] 认证失败: workspaceId={}", workspaceId);
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        log.info("  👤 用户信息: userId='{}', 转换后workspaceId='{}'", userId, convertedWorkspaceId);
        
        // 检查工作空间访问权限（使用转换后的ID）
        log.info("  🔐 开始检查工作空间访问权限...");
        if (!permissionService.hasWorkspaceAccess(userId, convertedWorkspaceId)) {
            log.error("❌ [文档列表] 权限检查失败: userId='{}', convertedWorkspaceId='{}'", userId, convertedWorkspaceId);
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        log.info("  ✅ 权限检查通过");
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        log.info("  📊 开始查询文档: convertedWorkspaceId='{}', pageable={}", convertedWorkspaceId, pageable);
        Page<WorkspaceDoc> docs = docService.getWorkspaceDocs(convertedWorkspaceId, pageable);
        log.info("  📊 查询结果: 找到{}个文档, 总页数={}, 当前页={}", 
                docs.getTotalElements(), docs.getTotalPages(), docs.getNumber());
        
        // 打印前几个文档的详情
        if (docs.hasContent()) {
            log.info("  📝 文档列表详情:");
            docs.getContent().stream().limit(5).forEach(doc -> {
                log.info("    - docId='{}', title='{}', updatedAt={}", 
                        doc.getId(), doc.getTitle(), doc.getUpdatedAt());
            });
        } else {
            log.warn("  ⚠️ 工作空间中没有找到任何文档!");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("docs", docs.getContent());
        response.put("totalElements", docs.getTotalElements());
        response.put("totalPages", docs.getTotalPages());
        response.put("currentPage", docs.getNumber());
        response.put("size", docs.getSize());
        
        log.info("  📤 准备返回响应: 文档数={}, 总数={}, 页数={}", 
                docs.getSize(), docs.getTotalElements(), docs.getTotalPages());
        log.info("🗂️🗂️🗂️ [CRITICAL-DEBUG] 文档列表API处理完成，返回HTTP 200");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取文档详情 - AFFiNE兼容版本，支持完整的ID解析和数据库同步
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}")
    public ResponseEntity<?> getDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "X-User-ID", required = false) String userIdFromHeader,
            @RequestHeader(value = "X-State-Vector", required = false) String stateVectorHeader,
            @RequestParam(value = "userId", required = false) String userIdFromParam,
            @RequestParam(value = "stateVector", required = false) String stateVectorParam,
            Authentication authentication,
            HttpServletRequest request) {
//        if (shouldReturnJson(acceptHeader)) {
//            return getDocMeta(
//                    workspaceId,
//                    docId,
//                    acceptHeader,
//                    userIdFromHeader,
//                    userIdFromParam,
//                    authentication,
//                    request
//            );
//        }

        log.info("🎯🎯🎯 [AFFINE-COMPAT] ========== 文档请求开始 ==========");
        log.info("🎯🎯🎯 [AFFINE-COMPAT] 请求参数: workspaceId='{}', docId='{}'", workspaceId, docId);
        log.info("🎯🎯🎯 [AFFINE-COMPAT] 请求头: Accept='{}', X-User-ID='{}', X-State-Vector='{}'", 
                acceptHeader, userIdFromHeader, stateVectorHeader);
        log.info("🎯🎯🎯 [AFFINE-COMPAT] 请求参数: userId='{}', stateVector='{}'", 
                userIdFromParam, stateVectorParam);
        log.info("🎯🎯🎯 [AFFINE-COMPAT] 认证信息: authentication={}", 
                authentication != null ? authentication.getClass().getSimpleName() : "null");
        
        // 🔄 [关键修改] 使用统一的ID转换工具
        String convertedWorkspaceId = workspaceIdConverter.convertWorkspaceId(workspaceId);
        String convertedDocId = workspaceIdConverter.convertDocId(docId);
        
        // 自动发现并创建ID映射关系
        workspaceIdConverter.autoDiscoverMapping(workspaceId, convertedWorkspaceId);
        workspaceIdConverter.autoDiscoverMapping(docId, convertedDocId);
        
        log.info("🔄 [ID-CONVERT] ID转换结果: workspace '{}'->'{}', doc '{}'->'{}' ", 
                workspaceId, convertedWorkspaceId, docId, convertedDocId);
        
        // 🎯 [AFFINE-COMPAT] 使用新的DocID解析机制
        DocID workspaceDocId = DocID.parse(convertedWorkspaceId);
        DocID documentDocId = DocID.parse(convertedDocId, extractRealWorkspaceId(convertedWorkspaceId, authentication));
        
        log.info("🎯🎯🎯 [AFFINE-COMPAT] AFFiNE兼容的文档请求解析:");
        log.info("  📋 原始参数: workspaceId='{}', docId='{}'", workspaceId, docId);
        log.info("  🔄 转换后参数: workspaceId='{}', docId='{}'", convertedWorkspaceId, convertedDocId);
        log.info("  🔍 工作空间解析: variant={}, guid={}", 
                workspaceDocId != null ? workspaceDocId.getVariant() : "PARSE_FAILED", 
                workspaceDocId != null ? workspaceDocId.getGuid() : "N/A");
        log.info("  🔍 文档解析: variant={}, guid={}", 
                documentDocId != null ? documentDocId.getVariant() : "PARSE_FAILED", 
                documentDocId != null ? documentDocId.getGuid() : "N/A");
        
        // 确定真实的工作空间ID和处理策略
        String realWorkspaceId = determineRealWorkspaceId(convertedWorkspaceId, documentDocId, authentication);
        ProcessingStrategy strategy = determineProcessingStrategy(workspaceDocId, documentDocId);
        
        log.info("  🎯 处理策略: {}, 真实workspaceId='{}'", strategy, realWorkspaceId);
        
        // 获取用户ID
        String userId = extractUserId(authentication, userIdFromHeader, userIdFromParam, request);
        log.info("🎯🎯🎯 [AFFINE-COMPAT] 提取的用户ID: userId='{}'", userId);
        
        if (userId == null || userId.isEmpty()) {
            log.warn("🎯🎯🎯 [AFFINE-COMPAT] ⚠️ 用户ID为空，可能导致权限检查失败");
        }
        
        // 特殊处理 editorSetting 请求
        if ("editorSetting".equals(docId)) {
            log.info("🎯🎯🎯 [AFFINE-COMPAT] 检测到 editorSetting 请求，使用特殊处理");
            return handleEditorSettingRequest(workspaceId, userId);
        }
        
        // 根据策略处理请求
        try {
            return switch (strategy) {
                case DATABASE_SYNC -> handleDatabaseSyncRequest(realWorkspaceId, documentDocId.getCollectionName(), 
                        stateVectorHeader, stateVectorParam, userId);
                case ROOT_DOCUMENT -> handleRootDocumentRequest(realWorkspaceId, documentDocId.getGuid(), 
                        stateVectorHeader, stateVectorParam, userId);
                case REGULAR_DOCUMENT -> handleRegularDocumentRequest(realWorkspaceId, documentDocId.getGuid(), 
                        stateVectorHeader, stateVectorParam, userId);
                case USER_DATA -> handleUserDataRequest(realWorkspaceId, documentDocId.getSub(), 
                        stateVectorHeader, stateVectorParam, userId);
                default -> handleUnsupportedRequest(workspaceId, docId);
            };
            
        } catch (Exception e) {
            log.error("🎯 [AFFINE-COMPAT] 文档请求处理失败: workspaceId={}, docId={}", workspaceId, docId, e);
            return ResponseEntity.status(500)
                    .header("X-Error", "Internal server error")
                    .body(("文档处理失败: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * 处理策略枚举
     */
    private enum ProcessingStrategy {
        DATABASE_SYNC,    // 数据库同步请求
        ROOT_DOCUMENT,    // 根文档请求
        REGULAR_DOCUMENT, // 常规文档请求
        USER_DATA,        // 用户数据请求
        UNSUPPORTED       // 不支持的请求
    }
    
    /**
     * 确定处理策略
     */
    private ProcessingStrategy determineProcessingStrategy(DocID workspaceDocId, DocID documentDocId) {
        // 数据库同步请求：workspaceId和docId都是db$格式
        if (workspaceDocId != null && workspaceDocId.isDatabaseSync() && 
            documentDocId != null && documentDocId.isDatabaseSync()) {
            return ProcessingStrategy.DATABASE_SYNC;
        }
        
        // 用户数据请求
        if (documentDocId != null && documentDocId.isUserData()) {
            return ProcessingStrategy.USER_DATA;
        }
        
        // 根文档请求：docId等于workspaceId
        if (documentDocId != null && documentDocId.isWorkspace()) {
            return ProcessingStrategy.ROOT_DOCUMENT;
        }
        
        // 常规文档请求
        if (documentDocId != null && !documentDocId.isDatabaseSync() && !documentDocId.isUserData()) {
            return ProcessingStrategy.REGULAR_DOCUMENT;
        }
        
        return ProcessingStrategy.UNSUPPORTED;
    }
    
    /**
     * 确定真实的工作空间ID - 增强版支持ID映射转换
     */
    private String determineRealWorkspaceId(String originalWorkspaceId, DocID documentDocId, Authentication authentication) {
        log.debug("🔄 [ID-MAPPING] 确定真实工作空间ID: originalWorkspaceId='{}'", originalWorkspaceId);
        
        // 1. 如果原始workspaceId是数据库同步格式，需要从认证上下文获取真实的workspaceId
        DocID workspaceDocId = DocID.parse(originalWorkspaceId);
        if (workspaceDocId != null && workspaceDocId.isDatabaseSync()) {
            String contextWorkspaceId = extractRealWorkspaceIdFromContext(authentication);
            log.debug("🔄 [ID-MAPPING] 数据库同步格式，从上下文获取: '{}'", contextWorkspaceId);
            return contextWorkspaceId;
        }
        
        // 2. 尝试通过ID映射服务转换工作空间ID
        String realWorkspaceId = workspaceIdMappingService.getRealWorkspaceId(originalWorkspaceId);
        if (!realWorkspaceId.equals(originalWorkspaceId)) {
            log.info("🔄 [ID-MAPPING] 工作空间ID转换成功: '{}' -> '{}'", originalWorkspaceId, realWorkspaceId);
            return realWorkspaceId;
        }
        
        // 3. 对于常规请求，直接使用原始workspaceId
        log.debug("🔄 [ID-MAPPING] 使用原始工作空间ID: '{}'", originalWorkspaceId);
        return originalWorkspaceId;
    }
    
    /**
     * 从认证上下文提取真实的工作空间ID
     * TODO: 实现真正的工作空间ID提取逻辑
     */
    private String extractRealWorkspaceId(String workspaceId, Authentication authentication) {
        DocID docId = DocID.parse(workspaceId);
        if (docId != null && docId.isDatabaseSync()) {
            // 这里应该从用户当前上下文、会话或其他机制中获取真实的工作空间ID
            // 作为临时解决方案，我们可以尝试从用户的默认工作空间或最近访问的工作空间中获取
            return extractRealWorkspaceIdFromContext(authentication);
        }
        return workspaceId;
    }
    
    /**
     * 从认证上下文提取真实工作空间ID的辅助方法
     */
    private String extractRealWorkspaceIdFromContext(Authentication authentication) {
        // 1. 从用户认证信息中提取
        if (authentication != null && authentication.getPrincipal() instanceof AffineUserDetails) {
            AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserId();
            
            // 2. 查询用户的默认工作空间或最近访问的工作空间
            try {
                // 从用户服务获取默认工作空间
                Optional<String> defaultWorkspace = workService.getUserDefaultWorkspace(userId);
                if (defaultWorkspace.isPresent()) {
                    log.debug("🔍 [WORKSPACE-EXTRACT] 从用户默认工作空间获取: {}", defaultWorkspace.get());
                    return defaultWorkspace.get();
                }
                
                // 如果没有默认工作空间，获取用户有权限的第一个工作空间
                List<Workspace> userWorkspaces = workService.getUserWorkspaces(userId);
                if (!userWorkspaces.isEmpty()) {
                    String firstWorkspace = userWorkspaces.get(0).getId();
                    log.debug("🔍 [WORKSPACE-EXTRACT] 从用户工作空间列表获取第一个: {}", firstWorkspace);
                    return firstWorkspace;
                }
                
            } catch (Exception e) {
                log.warn("⚠️ [WORKSPACE-EXTRACT] 无法从用户服务获取工作空间: {}", e.getMessage());
            }
        }
        
        // 3. 临时实现：使用测试工作空间ID
        String testWorkspaceId = "d8da6c13-114e-4709-bb26-268bf8565f52";
        log.warn("⚠️ [TEMP] 使用临时工作空间ID: {}", testWorkspaceId);
        return testWorkspaceId;
    }
    
    /**
     * 处理数据库同步请求
     */
    private ResponseEntity<byte[]> handleDatabaseSyncRequest(String realWorkspaceId, String collectionName, 
                                                           String stateVectorHeader, String stateVectorParam, String userId) {
        log.info("🗄️🗄️🗄️ [DB-SYNC] 处理数据库同步请求: workspaceId='{}', collection='{}', userId='{}'", 
                realWorkspaceId, collectionName, userId);
        
        try {
            // 验证集合名称是否受支持
            if (!databaseSyncService.isCollectionSupported(collectionName)) {
                log.warn("🗄️ [DB-SYNC] 不支持的集合: {}", collectionName);
                String errorJson = String.format(
                    "{\"error\":\"Unsupported collection\",\"collection\":\"%s\",\"supported\":[%s]}", 
                    collectionName, 
                    String.join(",", java.util.Arrays.stream(databaseSyncService.getSupportedCollections())
                            .map(s -> "\"" + s + "\"")
                            .toArray(String[]::new))
                );
                
                return ResponseEntity.status(404)
                        .header("X-Error", "Unsupported collection")
                        .header("X-Collection", collectionName)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            // 检查是否为差异请求
            String stateVector = stateVectorHeader != null ? stateVectorHeader : stateVectorParam;
            if (stateVector != null && !stateVector.isEmpty()) {
                try {
                    byte[] stateVectorBytes = java.util.Base64.getDecoder().decode(stateVector);
                    ResponseEntity<byte[]> diffResponse = databaseSyncService.handleDatabaseSyncDiff(
                            realWorkspaceId, collectionName, stateVectorBytes, userId).block();
                    
                    if (diffResponse != null) {
                        log.info("🗄️ [DB-SYNC] 差异同步成功: collection='{}', status={}", 
                                collectionName, diffResponse.getStatusCode());
                        return diffResponse;
                    }
                } catch (Exception e) {
                    log.error("🗄️ [DB-SYNC] 差异请求处理失败，回退到完整同步: {}", e.getMessage());
                }
            }
            
            // 完整同步请求
            ResponseEntity<byte[]> fullResponse = databaseSyncService.handleDatabaseSync(
                    realWorkspaceId, collectionName, userId).block();
            
            if (fullResponse != null) {
                log.info("🗄️ [DB-SYNC] 完整同步成功: collection='{}', status={}, size={} 字节", 
                        collectionName, fullResponse.getStatusCode(), 
                        fullResponse.getBody() != null ? fullResponse.getBody().length : 0);
                return fullResponse;
            } else {
                log.error("🗄️ [DB-SYNC] 同步服务返回null响应");
                throw new RuntimeException("Database sync service returned null response");
            }
            
        } catch (Exception e) {
            log.error("🗄️ [DB-SYNC] 数据库同步请求处理失败: collection='{}', error={}", 
                    collectionName, e.getMessage(), e);
            
            String errorJson = "{\"error\":\"Database sync failed\",\"collection\":\"" + collectionName + "\"}";
            return ResponseEntity.status(500)
                    .header("X-Error", "Database sync failed")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 处理根文档请求
     */
    private ResponseEntity<byte[]> handleRootDocumentRequest(String workspaceId, String docId, 
                                                           String stateVectorHeader, String stateVectorParam, String userId) {
        log.info("🏠🏠🏠 [ROOT-DOC] 处理根文档请求: workspaceId='{}', docId='{}', userId='{}'", 
                workspaceId, docId, userId);
        
        // 使用原有的根文档处理逻辑
        return handleFullDocRequest(workspaceId, docId, userId);
    }
    
    /**
     * 处理常规文档请求
     */
    private ResponseEntity<byte[]> handleRegularDocumentRequest(String workspaceId, String docId, 
                                                              String stateVectorHeader, String stateVectorParam, String userId) {
        log.info("📄📄📄 [DOC] 处理常规文档请求: workspaceId='{}', docId='{}', userId='{}'", 
                workspaceId, docId, userId);
        
        // 使用原有的文档处理逻辑
        return handleFullDocRequest(workspaceId, docId, userId);
    }
    
    /**
     * 处理用户数据请求 - 修正为Userspace处理逻辑
     */
    private ResponseEntity<byte[]> handleUserDataRequest(String workspaceId, String userDataId, 
                                                       String stateVectorHeader, String stateVectorParam, String userId) {
        log.info("👤👤👤 [USER-DATA] 处理用户数据请求: workspaceId='{}', userDataId='{}', userId='{}'", 
                workspaceId, userDataId, userId);
        
        try {
            // 🎯 根据AFFiNE架构，用户数据请求应该在用户空间(Userspace)中处理
            // workspaceId格式: userdata$userId$dataType 或 userdata$userId$workspaceId$dataType
            
            // 解析用户数据ID以获取用户ID和集合名称
            IdConverter.UserDataInfo userDataInfo = idConverter.extractUserDataInfo("userdata$" + userDataId);
            String targetUserId = userDataInfo.userId;
            String collectionName = userDataInfo.collectionName;
            
            log.info("👤 [USER-DATA] 解析结果: targetUserId='{}', collection='{}'", targetUserId, collectionName);
            
            // 🎯 特殊处理：__local__ 表示当前登录用户
            if ("__local__".equals(targetUserId)) {
                targetUserId = userId; // 将 __local__ 映射为当前用户ID
                log.info("👤 [USER-DATA] __local__ 映射为当前用户: userId='{}'", userId);
            }
            
            // 🔐 Userspace权限检查：用户只能访问自己的数据
            if (!targetUserId.equals(userId)) {
                log.warn("👤 [USER-DATA] Userspace权限拒绝: 用户 '{}' 试图访问用户 '{}' 的数据", userId, targetUserId);
                return ResponseEntity.status(403)
                        .header("X-Error", "Access denied to user data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Access denied\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            // 🎯 关键修正：在Userspace中，spaceId就是userId
            String userSpaceId = targetUserId;
            String fullUserDataDocId = "userdata$" + targetUserId + "$" + collectionName;
            
            log.info("👤 [USER-DATA] Userspace查询参数: spaceId='{}', docId='{}'", userSpaceId, fullUserDataDocId);
            
            // 检查是否为差异请求
            String stateVector = stateVectorHeader != null ? stateVectorHeader : stateVectorParam;
            if (stateVector != null && !stateVector.isEmpty()) {
                try {
                    byte[] stateVectorBytes = java.util.Base64.getDecoder().decode(stateVector);
                    // TODO: 实现用户数据的差异同步
                    log.debug("👤 [USER-DATA] 用户数据差异请求，暂时返回无变化");
                    
                    return ResponseEntity.noContent()
                            .header("X-Doc-No-Changes", "true")
                            .header("X-User-Data-Id", userDataId)
                            .build();
                } catch (Exception e) {
                    log.warn("👤 [USER-DATA] 差异请求处理失败，回退到完整请求: {}", e.getMessage());
                }
            }
            
            // 🎯 使用正确的Userspace查询参数获取用户数据
            Optional<byte[]> userData = getUserDataFromUserspace(targetUserId, collectionName, userSpaceId, fullUserDataDocId);
            
            if (userData.isPresent()) {
                log.info("👤 [USER-DATA] 用户数据请求成功: collection='{}', size={} 字节", 
                        collectionName, userData.get().length);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("X-Doc-Type", "user-data")
                        .header("X-User-Data-Id", userDataId)
                        .header("X-Collection", collectionName)
                        .header("X-Space-Type", "userspace")
                        .header("Cache-Control", "private, max-age=60")
                        .body(userData.get());
            } else {
                // 用户数据不存在，返回404
                log.info("👤 [USER-DATA] 用户数据不存在: collection='{}', userId='{}'", collectionName, targetUserId);
                String errorJson = String.format(
                    "{\"error\":\"User data not found\",\"collection\":\"%s\",\"userId\":\"%s\",\"message\":\"User %s data does not exist. It may not have been created yet.\"}", 
                    collectionName, targetUserId, collectionName
                );
                return ResponseEntity.status(404)
                        .header("X-Error", "User data not found")
                        .header("X-Collection", collectionName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
                    
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 用户数据请求失败: userDataId='{}', error={}", userDataId, e.getMessage(), e);
            
            String errorJson = String.format(
                "{\"error\":\"User data request failed\",\"userDataId\":\"%s\",\"message\":\"%s\"}", 
                userDataId, e.getMessage()
            );
            return ResponseEntity.status(500)
                    .header("X-Error", "User data request failed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 处理editorSetting特殊文档请求
     */
    private ResponseEntity<byte[]> handleEditorSettingRequest(String workspaceId, String userId) {
        log.info("⚙️ [EDITOR-SETTING] 处理编辑器设置请求: workspaceId='{}', userId='{}'", workspaceId, userId);
        
        try {
            // 检查是否有用户ID
            if (userId == null || userId.isEmpty()) {
                log.warn("⚙️ [EDITOR-SETTING] 用户未登录，无法获取编辑器设置");
                String errorJson = "{\"error\":\"User not authenticated\",\"message\":\"Please login to access editor settings\"}";
                return ResponseEntity.status(401)
                        .header("X-Error", "User not authenticated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            // 尝试从用户空间获取编辑器设置
            String editorSettingDocId = "userdata$" + userId + "$editorSetting";
            Optional<UserSnapshot> userSnapshot = 
                userspaceSyncService.getUserDoc(userId, editorSettingDocId);
            
            if (userSnapshot.isPresent()) {
                byte[] settingsData = userSnapshot.get().getBlob();
                log.info("✅ [EDITOR-SETTING] 编辑器设置已存在: userId='{}', size={} 字节", userId, settingsData.length);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("X-Doc-Type", "editor-setting")
                        .header("X-User-Id", userId)
                        .header("Cache-Control", "private, max-age=300")
                        .body(settingsData);
            } else {
                // 编辑器设置不存在，返回404让前端知道需要创建
                log.info("❌ [EDITOR-SETTING] 编辑器设置不存在: userId='{}'", userId);
                String errorJson = String.format(
                    "{\"error\":\"Editor settings not found\",\"userId\":\"%s\",\"message\":\"Editor settings have not been created yet. The client should create default settings.\"}", 
                    userId
                );
                return ResponseEntity.status(404)
                        .header("X-Error", "Editor settings not found")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("⚙️ [EDITOR-SETTING] 获取编辑器设置失败: userId='{}', error={}", userId, e.getMessage(), e);
            
            String errorJson = String.format(
                "{\"error\":\"Failed to get editor settings\",\"message\":\"%s\"}", 
                e.getMessage()
            );
            return ResponseEntity.status(500)
                    .header("X-Error", "Failed to get editor settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 从用户空间获取用户数据 - 完全按照AFFiNE Userspace架构重写
     */
    private Optional<byte[]> getUserDataFromUserspace(String targetUserId, String collectionName, String userSpaceId, String fullUserDataDocId) {
        log.info("👤🎯 [USERSPACE-COMPAT] 开始按照AFFiNE架构从用户空间获取数据: userId='{}', collection='{}', spaceId='{}', docId='{}'", 
                targetUserId, collectionName, userSpaceId, fullUserDataDocId);
        
        try {
            // 🎯 关键：按照AFFiNE架构，使用UserspaceSyncService获取用户文档
            // 在AFFiNE的Userspace中，spaceId就是userId，docId是完整的用户数据文档ID
            Optional<UserSnapshot> userSnapshot = 
                userspaceSyncService.getUserDoc(targetUserId, fullUserDataDocId);
            
            if (userSnapshot.isPresent()) {
                byte[] userData = userSnapshot.get().getBlob();
                log.info("✅👤🎯 [USERSPACE-COMPAT] 用户数据已存在: userId='{}', collection='{}', size={} 字节", 
                        targetUserId, collectionName, userData.length);
                return Optional.of(userData);
            } else {
                log.info("❌👤🎯 [USERSPACE-COMPAT] 用户数据不存在: userId='{}', collection='{}'", targetUserId, collectionName);
                // 🎯 按照AFFiNE架构，数据不存在时返回空Optional，让调用方决定如何处理
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("👤🎯 [USERSPACE-COMPAT] 获取用户空间数据失败: userId='{}', collection='{}', error={}", 
                    targetUserId, collectionName, e.getMessage(), e);
            // 异常情况下返回空，让调用方处理
            return Optional.empty();
        }
    }
    
    /**
     * 获取用户偏好设置数据 - 保留兼容性，但推荐使用Userspace方法
     */
    private byte[] createUserPreferencesData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户偏好设置: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户偏好设置
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$preferences").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户偏好设置数据已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户偏好设置不存在: userId='{}'", userId);
                // 🎯 不存在时抛出异常，让上层返回404
                throw new ResourceNotFoundException("UserPreferences", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户偏好设置失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user preferences data", e);
        }
    }
    
    /**
     * 获取用户收藏数据（复数形式）
     */
    private byte[] createUserFavoritesData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户收藏数据: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户收藏数据
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$favorites").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户收藏数据已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户收藏数据不存在: userId='{}'", userId);
                throw new ResourceNotFoundException("UserFavorites", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户收藏数据失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user favorites data", e);
        }
    }
    
    /**
     * 获取用户收藏数据（单数形式）
     */
    private byte[] createUserFavoriteData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户收藏数据(单数): userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户收藏数据（单数形式）
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$favorite").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户收藏数据(单数)已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户收藏数据(单数)不存在: userId='{}'", userId);
                throw new ResourceNotFoundException("UserFavorite", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户收藏数据(单数)失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user favorite data", e);
        }
    }
    
    /**
     * 获取用户最近访问数据
     */
    private byte[] createUserRecentData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户最近访问数据: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户最近访问数据
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$recent").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户最近访问数据已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户最近访问数据不存在: userId='{}'", userId);
                throw new ResourceNotFoundException("UserRecentData", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户最近访问数据失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user recent data", e);
        }
    }
    
    /**
     * 获取用户设置数据
     */
    private byte[] createUserSettingsData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户设置数据: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户设置数据
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$settings").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户设置数据已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户设置数据不存在: userId='{}'", userId);
                throw new ResourceNotFoundException("UserSettings", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户设置数据失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user settings data", e);
        }
    }
    
    /**
     * 获取用户文档集成引用数据
     */
    private byte[] createUserDocIntegrationRefData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户文档集成引用数据: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户文档集成引用数据
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$docIntegrationRef").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户文档集成引用数据已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户文档集成引用数据不存在: userId='{}'", userId);
                throw new ResourceNotFoundException("UserDocIntegrationRef", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户文档集成引用数据失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user docIntegrationRef data", e);
        }
    }
    
    /**
     * 获取用户书签数据
     */
    private byte[] createUserBookmarksData(String userId, String workspaceId) {
        log.debug("👤 [USER-DATA] 获取用户书签数据: userId='{}', workspaceId='{}'", userId, workspaceId);
        
        try {
            // 🎯 从数据库查询现有的用户书签数据
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "userdata$" + userId + "$bookmarks").block();
            
            if (docRecord.isPresent()) {
                log.info("👤 [USER-DATA] 用户书签数据已存在: userId='{}', size={} 字节", userId, docRecord.get().getBlob().length);
                return docRecord.get().getBlob();
            } else {
                log.info("👤 [USER-DATA] 用户书签数据不存在: userId='{}'", userId);
                throw new ResourceNotFoundException("UserBookmarks", userId);
            }
        } catch (Exception e) {
            log.error("👤 [USER-DATA] 获取用户书签数据失败: userId='{}', error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user bookmarks data", e);
        }
    }
    
    /**
     * 创建用户数据（根据集合类型）
     */
    private byte[] createEmptyUserData(String collectionName) {
        log.warn("👤 [USER-DATA] 不支持的用户数据集合: collection='{}'", collectionName);
        // 🎯 不支持的集合类型，抛出异常
        throw new UnsupportedOperationException("不支持的用户数据集合: " + collectionName);
    }
    
    /**
     * 处理不支持的请求
     */
    private ResponseEntity<byte[]> handleUnsupportedRequest(String workspaceId, String docId) {
        log.warn("❌ [UNSUPPORTED] 不支持的文档请求格式: workspaceId='{}', docId='{}'", workspaceId, docId);
        
        String errorJson = String.format(
            "{\"error\":\"Unsupported document request format\",\"workspaceId\":\"%s\",\"docId\":\"%s\"}", 
            workspaceId, docId);
        
        return ResponseEntity.status(400)
                .header("X-Error", "Unsupported format")
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    /**
     * 处理完整文档请求 - AFFiNE兼容版本，使用PgWorkspaceDocStorageAdapter
     */
    private ResponseEntity<byte[]> handleFullDocRequest(String workspaceId, String docId, String userId) {
        boolean isRootDoc = docId.equals(workspaceId);
        boolean isDbSyncDoc = docId.startsWith("db$");
        String logPrefix = isRootDoc ? "🏠🎯 [ROOT-DOC-AFFINE]" : (isDbSyncDoc ? "🗄️🎯 [DB-SYNC-AFFINE]" : "📄🎯 [DOC-AFFINE]");
        
        log.info("{} AFFiNE兼容的文档请求: workspaceId='{}', docId='{}', userId='{}', isDbSync={}", 
                logPrefix, workspaceId, docId, userId, isDbSyncDoc);
        
        // 🗄️ 处理数据库同步请求
        if (isDbSyncDoc) {
            return handleDbSyncRequest(workspaceId, docId, userId);
        }
        
        // 🔒 权限检查：对于非根文档，需要检查访问权限
        if (!isRootDoc) {
            log.info("🔐 [PERMISSION-CHECK] 开始权限检查: docId='{}', userId='{}', workspaceId='{}'", 
                    docId, userId, workspaceId);
            
            // ✅ 使用带 workspaceId 的权限检查方法（更精确，支持文档不存在时的工作空间权限检查）
            boolean hasAccess;
            if (docService instanceof WorkspaceDocServiceImpl) {
                hasAccess = ((WorkspaceDocServiceImpl) docService).hasDocAccess(workspaceId, docId, userId);
            } else {
                // 降级到原有方法
                hasAccess = docService.hasDocAccess(docId, userId);
            }
            
            log.info("🔐 [PERMISSION-CHECK] 权限检查结果: docId='{}', userId='{}', workspaceId='{}', hasAccess={}", 
                    docId, userId, workspaceId, hasAccess);
            
            if (!hasAccess) {
                log.error("🚫 [DOC-ACCESS] ❌ 用户无权访问文档: docId='{}', userId='{}', workspaceId='{}'", 
                        docId, userId, workspaceId);
                log.error("🚫 [DOC-ACCESS] 可能的原因:");
                log.error("  1. 文档不存在 (docId='{}')", docId);
                log.error("  2. 用户没有工作空间访问权限 (userId='{}', workspaceId='{}')", userId, workspaceId);
                log.error("  3. 文档不是公开文档且用户无权限");
                
                String errorJson = String.format(
                    "{\"error\":\"Access denied\",\"docId\":\"%s\",\"userId\":\"%s\",\"workspaceId\":\"%s\",\"message\":\"You do not have permission to access this document.\"}", 
                    docId, userId, workspaceId
                );
                return ResponseEntity.status(403)
                        .header("X-Error", "Access denied")
                        .header("X-Doc-Id", docId)
                        .header("X-User-Id", userId != null ? userId : "null")
                        .header("X-Workspace-Id", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            log.info("🔐 [PERMISSION-CHECK] ✅ 权限检查通过，继续处理文档请求");
        } else {
            log.info("🔐 [PERMISSION-CHECK] 跳过权限检查（根文档）: docId='{}'", docId);
        }
        
        if (isRootDoc) {
            log.info("🏠🎯 [ROOT-DOC-AFFINE] 正在获取根文档数据，使用AFFiNE兼容架构!");
            log.info("  📊 如果此请求失败或返回空数据，前端会永远卡在同步状态");
            log.info("  🔍 开始调用 AFFiNE兼容的文档存储适配器...");
        }
        
        // 🎯 使用AFFiNE兼容的文档存储适配器获取文档
        Optional<Snapshot> snapshotOpt = pgWorkspaceDocStorageAdapter.getDoc(workspaceId, docId);
        
        if (snapshotOpt.isEmpty()) {
            if (isRootDoc) {
                log.warn("🚨🎯 [ROOT-DOC-AFFINE] 根文档不存在! 返回空文档让前端初始化");
            } else {
                log.info("🎯 [DOC-READ-AFFINE] 常规文档不存在: docId='{}', 返回空YJS文档让前端创建", docId);
            }
            
            // 🎯 按照AFFiNE架构：文档不存在时，对于yjs文档返回空的yjs文档而不是JSON错误
            // 这样前端yjs可以正确解析，并知道需要创建新文档
            // ✅ 修复：常规文档不存在时也返回空YJS文档，而不是404错误
            log.info("🎯 [DOC-READ-AFFINE] 文档不存在，返回空的yjs文档让前端创建: docId='{}', isRootDoc={}", docId, isRootDoc);
            
            // 创建一个空的Y.Doc并编码为二进制
            // ✅ 使用 yjs-service 创建标准的空 Y.js 文档
            byte[] emptyYjsDoc = yjsUtils.createEmptyYjsDoc();
            log.info("🎯 [DOC-READ-AFFINE] ✅ 创建空YJS文档成功: 大小={}字节", emptyYjsDoc.length);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("X-Doc-Empty", "true")
                    .header("X-Doc-Type", isRootDoc ? "root" : "regular")
                    .header("X-Doc-Id", docId)
                    .header("X-Workspace-Id", workspaceId)
                    .body(emptyYjsDoc);
        }
        
        Snapshot snapshot = snapshotOpt.get();
        byte[] snapshotBytes = resolveSnapshotBytes(snapshot);
        
        if (isRootDoc) {
            log.info("🎉🎯 [ROOT-DOC-AFFINE] 成功获取根文档数据!");
            log.info("  📊 根文档大小: {} 字节", snapshotBytes.length);
            log.info("  ⏰ 根文档创建时间: {}", snapshot.getCreatedAt());
            log.info("  👤 根文档创建者: {}", snapshot.getCreatedBy());
            log.info("  ✅ 前端将收到根文档数据，应该能正常完成同步");
        }
        
        // 记录文档访问
        try {
            docService.recordDocAccess(docId, userId);
        } catch (Exception e) {
            log.warn("记录文档访问失败: docId={}, userId={}", docId, userId, e);
        }
        
        log.info("🎯 [DOC-READ-AFFINE] 成功获取AFFiNE文档: docId={}, size={}, createdAt={}", 
                docId, snapshotBytes.length, snapshot.getCreatedAt());
        
        // 获取文档权限和模式信息
        DocPermissionInfo permissionInfo = getDocPermissionInfo(workspaceId, docId, userId);
        
        ResponseEntity<byte[]> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                // 必需的权限和模式头部
                .header("publish-mode", permissionInfo.getPublishMode())
                .header("permission-mode", permissionInfo.getPermissionMode())
                // AFFiNE兼容的元数据头部
                .header("X-Doc-Created-At", snapshot.getCreatedAt().toString())
                .header("X-Doc-Updated-At", snapshot.getUpdatedAt().toString())
                .header("X-Doc-Created-By", snapshot.getCreatedBy())
                .header("X-Doc-Updated-By", snapshot.getUpdatedBy())
                .header("X-Doc-Size", String.valueOf(snapshotBytes.length))
                .header("X-Doc-Seq", String.valueOf(snapshot.getSeq()))
                .header("Cache-Control", "public, max-age=60")
                .header("Last-Modified", formatHttpDate(snapshot.getUpdatedAt()))
                .body(snapshotBytes);
        
        if (isRootDoc) {
            log.info("🏠🎯 [ROOT-DOC-AFFINE] 根文档响应已生成，即将发送给前端!");
            log.info("  📤 响应状态: HTTP 200 OK");
            log.info("  📊 响应数据大小: {} 字节", snapshotBytes.length);
            log.info("  🎯 前端收到后应该将 syncing 设为 false, ready 设为 true");
            log.info("  ✅ 如果前端仍卡住，请检查网络连接和前端错误日志");
        }
        
        return response;
    }
    
    /**
     * 处理文档差异请求
     */
    private ResponseEntity<byte[]> handleDocDiffRequest(String workspaceId, String docId, String stateVector, String userId) {
        boolean isRootDoc = docId.equals(workspaceId);
        String logPrefix = isRootDoc ? "🏠🏠🏠 [ROOT-DOC-DIFF]" : "📄📄📄 [DOC-DIFF]";
        
        log.info("{} 处理文档差异请求: workspaceId='{}', docId='{}', userId='{}'", 
                logPrefix, workspaceId, docId, userId);
        
        if (isRootDoc) {
            log.info("🏠🏠🏠 [ROOT-DOC-DIFF] 根文档差异同步请求");
            log.info("  🔄 前端提供的状态向量长度: {}", stateVector != null ? stateVector.length() : 0);
        }
        
        try {
            // 解码状态向量
            byte[] stateVectorBytes = java.util.Base64.getDecoder().decode(stateVector);
            
            // 获取差异数据
            byte[] diffData = getDocReader().getDocDiff(workspaceId, docId, stateVectorBytes).block();
            
            if (diffData == null || diffData.length == 0) {
                if (isRootDoc) {
                    log.info("🏠🏠🏠 [ROOT-DOC-DIFF] 根文档无差异更新，前端已同步");
                }
                log.debug("【文档加载API】无差异更新: docId={}", docId);
                return ResponseEntity.noContent()
                        .header("X-Doc-No-Changes", "true")
                        .build();
            }
            
            if (isRootDoc) {
                log.info("🏠🏠🏠 [ROOT-DOC-DIFF] 根文档差异数据: {} 字节", diffData.length);
                log.info("  🔄 前端将收到根文档增量更新");
            }
            
            log.info("【文档加载API】返回差异更新: docId={}, diffSize={}", docId, diffData.length);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("X-Doc-Diff", "true")
                    .header("X-Doc-Size", String.valueOf(diffData.length))
                    .header("Cache-Control", "no-cache") // 差异数据不缓存
                    .body(diffData);
                    
        } catch (Exception e) {
            if (isRootDoc) {
                log.error("🚨🚨🚨 [ROOT-DOC-DIFF] 根文档差异请求失败，回退到完整请求: {}", e.getMessage());
                log.error("  🔄 这可能会影响根文档同步性能，但不会阻塞同步");
            }
            log.error("【文档加载API】处理差异请求失败: docId={}, stateVector={}", docId, stateVector, e);
            
            // 差异请求失败，回退到完整文档请求
            log.info("【文档加载API】差异请求失败，回退到完整文档请求: docId={}", docId);
            return handleFullDocRequest(workspaceId, docId, userId);
        }
    }
    
    /**
     * 处理数据库同步请求
     * 前端会请求 db$collectionName 格式的文档来同步本地数据库集合
     */
    private ResponseEntity<byte[]> handleDbSyncRequest(String workspaceId, String docId, String userId) {
        String collectionName = docId.substring(3); // 去掉 "db$" 前缀
        
        log.info("🗄️🗄️🗄️ [DB-SYNC] 处理数据库同步请求: workspaceId='{}', collection='{}', userId='{}'", 
                workspaceId, collectionName, userId);
        
        try {
            // 针对不同的集合返回不同的处理
            switch (collectionName) {
                case "docCustomPropertyInfo":
                    return handleDocCustomPropertyInfoSync(workspaceId, userId);
                case "pinnedCollections":
                    return handlePinnedCollectionsSync(workspaceId, userId);
                default:
                    log.warn("🗄️ [DB-SYNC] 未支持的数据库集合同步: collection='{}'", collectionName);
                    return handleUnsupportedDbSync(workspaceId, collectionName, userId);
            }
            
        } catch (Exception e) {
            log.error("🗄️ [DB-SYNC] 数据库同步请求处理失败: collection='{}', error={}", 
                    collectionName, e.getMessage(), e);
            
            String errorJson = "{\"error\":\"Database sync failed\",\"collection\":\"" + collectionName + "\"}";
            return ResponseEntity.status(500)
                    .header("X-Error", "Database sync failed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 处理文档自定义属性信息同步
     */
    private ResponseEntity<byte[]> handleDocCustomPropertyInfoSync(String workspaceId, String userId) {
        log.info("🗄️ [DB-SYNC] 处理docCustomPropertyInfo同步: workspaceId='{}', userId='{}'", 
                workspaceId, userId);
        
        try {
            // 🎯 从数据库查询现有的文档自定义属性配置
            // 如果不存在，返回404让前端知道需要初始化
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "db$docCustomPropertyInfo").block();
            
            if (docRecord.isPresent()) {
                log.info("🗄️ [DB-SYNC] docCustomPropertyInfo数据已存在: workspaceId='{}', size={} 字节", 
                        workspaceId, docRecord.get().getBlob().length);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("X-Doc-Type", "db-sync")
                        .header("X-Collection", "docCustomPropertyInfo")
                        .header("X-Doc-Size", String.valueOf(docRecord.get().getBlob().length))
                        .header("X-Doc-Timestamp", String.valueOf(docRecord.get().getTimestamp()))
                        .header("Cache-Control", "public, max-age=60")
                        .body(docRecord.get().getBlob());
            } else {
                // 🎯 正确的做法：配置不存在时返回404，让前端知道需要初始化
                log.info("🗄️ [DB-SYNC] docCustomPropertyInfo不存在，返回404: workspaceId='{}'", workspaceId);
                String errorJson = "{\"error\":\"Collection not initialized\",\"collection\":\"docCustomPropertyInfo\",\"workspaceId\":\"" + workspaceId + "\"}";
                return ResponseEntity.status(404)
                        .header("X-Error", "Collection not found")
                        .header("X-Collection", "docCustomPropertyInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
                    
        } catch (Exception e) {
            log.error("🗄️ [DB-SYNC] docCustomPropertyInfo同步失败: workspaceId='{}', error={}", 
                    workspaceId, e.getMessage(), e);
            String errorJson = "{\"error\":\"Failed to sync docCustomPropertyInfo\",\"workspaceId\":\"" + workspaceId + "\"}";
            return ResponseEntity.status(500)
                    .header("X-Error", "Sync failed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 处理置顶集合同步
     */
    private ResponseEntity<byte[]> handlePinnedCollectionsSync(String workspaceId, String userId) {
        log.info("🗄️ [DB-SYNC] 处理pinnedCollections同步: workspaceId='{}', userId='{}'", 
                workspaceId, userId);
        
        try {
            // 🎯 从数据库查询现有的置顶集合配置
            // 如果不存在，返回404让前端知道需要初始化
            Optional<DocRecord> docRecord = getDocReader().getDoc(workspaceId, "db$pinnedCollections").block();
            
            if (docRecord.isPresent()) {
                log.info("🗄️ [DB-SYNC] pinnedCollections数据已存在: workspaceId='{}', size={} 字节", 
                        workspaceId, docRecord.get().getBlob().length);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("X-Doc-Type", "db-sync")
                        .header("X-Collection", "pinnedCollections")
                        .header("X-Doc-Size", String.valueOf(docRecord.get().getBlob().length))
                        .header("X-Doc-Timestamp", String.valueOf(docRecord.get().getTimestamp()))
                        .header("Cache-Control", "public, max-age=60")
                        .body(docRecord.get().getBlob());
            } else {
                // 🎯 正确的做法：配置不存在时返回404，让前端知道需要初始化
                log.info("🗄️ [DB-SYNC] pinnedCollections不存在，返回404: workspaceId='{}', userId='{}'", workspaceId, userId);
                String errorJson = "{\"error\":\"Collection not initialized\",\"collection\":\"pinnedCollections\",\"workspaceId\":\"" + workspaceId + "\"}";
                return ResponseEntity.status(404)
                        .header("X-Error", "Collection not found")
                        .header("X-Collection", "pinnedCollections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
                    
        } catch (Exception e) {
            log.error("🗄️ [DB-SYNC] pinnedCollections同步失败: workspaceId='{}', error={}", 
                    workspaceId, e.getMessage(), e);
            String errorJson = "{\"error\":\"Failed to sync pinnedCollections\",\"workspaceId\":\"" + workspaceId + "\"}";
            return ResponseEntity.status(500)
                    .header("X-Error", "Sync failed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 处理不支持的数据库同步请求
     */
    private ResponseEntity<byte[]> handleUnsupportedDbSync(String workspaceId, String collectionName, String userId) {
        log.warn("🗄️ [DB-SYNC] 不支持的集合同步请求: workspaceId='{}', collection='{}', userId='{}'", 
                workspaceId, collectionName, userId);
        
        // 返回404，让前端知道该集合不支持同步
        String errorJson = "{\"error\":\"Unsupported collection\",\"collection\":\"" + collectionName + "\"}";
        return ResponseEntity.status(404)
                .header("X-Error", "Unsupported collection")
                .header("X-Collection", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    /**
     * 获取文档读取器
     */
    private DocReader getDocReader() {
        // 这里可以根据配置选择不同的读取器
        // 如果配置了RPC服务，优先使用RPC读取器
        try {
            return (DocReader) applicationContext.getBean("rpcDocReader");
        } catch (Exception e) {
            log.debug("RPC文档读取器不可用，使用数据库读取器");
            return (DocReader) applicationContext.getBean("databaseDocReader");
        }
    }
    
    /**
     * 格式化HTTP日期
     */
    private String formatHttpDate(long timestamp) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        return java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .format(instant.atZone(java.time.ZoneOffset.UTC));
    }
    
    /**
     * 格式化HTTP日期 - LocalDateTime版本
     */
    private String formatHttpDate(LocalDateTime dateTime) {
        return java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .format(dateTime.atZone(java.time.ZoneOffset.UTC));
    }
    
    /**
     * 获取文档权限信息
     * 根据文档的公开状态和用户权限确定权限和模式头部
     */
    private DocPermissionInfo getDocPermissionInfo(String workspaceId, String docId, String userId) {
        try {
            // 检查文档是否存在
            Optional<WorkspaceDoc> docOpt = docService.findById(docId);
            
            if (docOpt.isEmpty()) {
                // 文档不存在，返回默认权限
                return DocPermissionInfo.builder()
                        .publishMode("page")
                        .permissionMode("private")
                        .isPublic(false)
                        .build();
            }
            
            WorkspaceDoc doc = docOpt.get();
            
            // 检查文档是否公开
            boolean isPublic = doc.getIsPublic() != null && doc.getIsPublic();
            
            String publishMode = "page"; // 默认为页面模式
            String permissionMode = "private"; // 默认为私有
            
            if (isPublic) {
                // 公开文档的权限设置
                if (doc.getPublicMode() != null) {
                    switch (doc.getPublicMode()) {
                        case "edgeless":
                            publishMode = "edgeless";
                            break;
                        case "page":
                        default:
                            publishMode = "page";
                            break;
                    }
                }
                
                // 公开文档的权限模式
                if (doc.getPublicPermission() != null) {
                    switch (doc.getPublicPermission()) {
                        case "append-only":
                            permissionMode = "append-only";
                            break;
                        case "read-only":
                        default:
                            permissionMode = "read-only";
                            break;
                    }
                } else {
                    permissionMode = "read-only"; // 公开文档默认只读
                }
            } else {
                // 私有文档
                permissionMode = "private";
                
                // TODO: 可以根据用户在工作空间中的角色来确定具体权限
                // 这里可以检查用户是否是文档的所有者或有写权限
                // 暂时简化为私有
            }
            
            return DocPermissionInfo.builder()
                    .publishMode(publishMode)
                    .permissionMode(permissionMode)
                    .isPublic(isPublic)
                    .build();
                    
        } catch (Exception e) {
            log.warn("获取文档权限信息失败: workspaceId={}, docId={}, userId={}", 
                    workspaceId, docId, userId, e);
            
            // 发生错误时返回默认权限
            return DocPermissionInfo.builder()
                    .publishMode("page")
                    .permissionMode("private")
                    .isPublic(false)
                    .build();
        }
    }
    
    /**
     * 处理文档YJS更新数据 - AFFiNE兼容版本，这是唯一的文档创建和更新方式
     * 对应AFFiNE的pushDocUpdates方法
     */
    @PostMapping("/api/workspaces/{workspaceId}/docs/{docId}/updates")
    public ResponseEntity<Map<String, Object>> applyDocUpdate(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody DocUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        log.info("🎯📝 [AFFINE-COMPAT] AFFiNE兼容的文档更新请求: workspaceId={}, docId={}, timestamp={}, updateSize={}", 
                workspaceId, docId, request.timestamp(), request.update() != null ? request.update().length() : 0);
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            log.warn("🎯📝 [AFFINE-COMPAT] 未授权访问: workspaceId={}, docId={}", workspaceId, docId);
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        log.info("🎯📝 [AFFINE-COMPAT] 用户信息: userId={}, workspaceId={}, docId={}", 
                userId, workspaceId, docId);
        
        // 🔒 权限检查：检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            log.warn("🚫 [DOC-UPDATE] 用户无权访问文档: docId={}, userId={}", docId, userId);
            return ResponseEntity.status(403).body(Map.of(
                "success", false, 
                "error", "Access denied"
            ));
        }
        
        // 🔒 权限检查：如果是公开文档的 read-only 模式，拒绝更新
        Optional<WorkspaceDoc> docOpt = docService.findById(docId);
        if (docOpt.isPresent()) {
            WorkspaceDoc doc = docOpt.get();
            if (Boolean.TRUE.equals(doc.getPublic()) && 
                "read-only".equalsIgnoreCase(doc.getPublicPermission())) {
                log.warn("🚫 [DOC-UPDATE] 文档为只读模式，拒绝更新: docId={}", docId);
                return ResponseEntity.status(403).body(Map.of(
                    "success", false, 
                    "error", "Document is read-only and cannot be modified"
                ));
            }
        }
        
        try {
            // 🎯 解码Base64 YJS数据
            byte[] updateData = java.util.Base64.getDecoder().decode(request.update());
            
            log.info("🎯📝 [AFFINE-COMPAT] 解码YJS数据: docId={}, 原始Base64长度={}, 解码后字节长度={}", 
                    docId, request.update().length(), updateData.length);
            
            // 🎯 关键：使用AFFiNE架构的pushDocUpdates - 这是唯一的文档创建和更新方式
            List<byte[]> updates = List.of(updateData);
            String sessionIdentifier = sanitizeIdentifier(request.sessionId());
            String clientIdentifier = sanitizeIdentifier(request.clientId());
            String editorIdentifier = firstNonBlank(sessionIdentifier, clientIdentifier, userId);

            long timestamp = pgWorkspaceDocStorageAdapter.pushDocUpdates(workspaceId, docId, updates, editorIdentifier);

            log.info("🎯📝 [AFFINE-COMPAT] AFFiNE架构文档更新成功: docId={}, editorIdentifier={}, timestamp={}", 
                    docId, editorIdentifier, timestamp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", timestamp);
            response.put("docId", docId);
            response.put("accepted", true); // 对应AFFiNE的返回格式
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("🎯📝 [AFFINE-COMPAT] Base64解码失败: docId={}, error={}", docId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid Base64 data"));
        } catch (Exception e) {
            log.error("🎯📝 [AFFINE-COMPAT] AFFiNE文档更新失败: docId={}", docId, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 获取文档时间戳
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}/timestamp")
    public ResponseEntity<Map<String, Object>> getDocTimestamp(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            Authentication authentication) {
        
        log.debug("【文档时间戳API】获取时间戳: workspaceId={}, docId={}", workspaceId, docId);
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", "Access denied"));
        }
        
        try {
            long timestamp = docService.getDocTimestamp(workspaceId, docId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", timestamp);
            response.put("docId", docId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("【文档时间戳API】获取失败: docId={}", docId, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to get timestamp"));
        }
    }

    /**
     * 更新文档
     */
    @PutMapping("/api/workspaces/{workspaceId}/docs/{docId}")
    public ResponseEntity<Map<String, Object>> updateDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody UpdateDocRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档编辑权限
        if (!docService.hasDocEditPermission(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        WorkspaceDoc existingDoc = docService.findById(docId).orElse(null);
        if (existingDoc == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        if (!workspaceId.equals(existingDoc.getWorkspaceId())) {
            log.warn("Workspace mismatch when updating doc: requestedWorkspaceId={}, actualWorkspaceId={}, docId={}",
                    workspaceId, existingDoc.getWorkspaceId(), docId);
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        
        try {
            WorkspaceDoc doc = new WorkspaceDoc();
            doc.setId(docId);
            doc.setTitle(request.title());
            doc.setPublic(request.isPublic());
            
            WorkspaceDoc updatedDoc = docService.updateDoc(doc);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("doc", updatedDoc);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update document", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/api/workspaces/{workspaceId}/docs/{docId}")
    public ResponseEntity<Map<String, Object>> deleteDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档编辑权限
        if (!docService.hasDocEditPermission(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        WorkspaceDoc existingDoc = docService.findById(docId).orElse(null);
        if (existingDoc == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        if (!workspaceId.equals(existingDoc.getWorkspaceId())) {
            log.warn("Workspace mismatch when deleting doc: requestedWorkspaceId={}, actualWorkspaceId={}, docId={}",
                    workspaceId, existingDoc.getWorkspaceId(), docId);
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        
        try {
            docService.deleteDoc(docId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete document", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 搜索文档
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/search")
    public ResponseEntity<Map<String, Object>> searchDocs(
            @PathVariable String workspaceId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String sanitizedKeyword = keyword == null ? "" : keyword.trim();
        if (sanitizedKeyword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "keyword_required"));
        }

        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        // 检查工作空间访问权限
        if (!permissionService.hasWorkspaceAccess(userId, workspaceId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        int normalizedLimit = clamp(limit, 1, 100);
        List<WorkspaceDoc> docs = docService.searchDocs(workspaceId, sanitizedKeyword);
        List<WorkspaceDoc> limitedDocs = docs.stream()
                .limit(normalizedLimit)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("docs", limitedDocs);
        response.put("count", limitedDocs.size());
        response.put("keyword", sanitizedKeyword);
        response.put("limit", normalizedLimit);

        return ResponseEntity.ok()
                .header("Cache-Control", "private, max-age=30")
                .header("Vary", "Authorization, Accept")
                .header("X-Doc-Workspace", workspaceId)
                .header("X-Doc-Result-Total", String.valueOf(docs.size()))
                .header("X-Doc-Cache", "MISS")
                .body(response);
    }

    /**
     * 获取最近访问的文档
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/recent")
    public ResponseEntity<Map<String, Object>> getRecentDocs(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        if (!permissionService.hasWorkspaceAccess(userId, workspaceId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        int normalizedLimit = clamp(limit, 1, 50);
        List<WorkspaceDoc> docs = docService.getRecentDocs(userId, normalizedLimit);

        // 过滤出属于当前工作空间的文档
        List<WorkspaceDoc> workspaceDocs = docs.stream()
                .filter(doc -> workspaceId.equals(doc.getWorkspaceId()))
                .limit(normalizedLimit)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("docs", workspaceDocs);
        response.put("count", workspaceDocs.size());
        response.put("limit", normalizedLimit);

        return ResponseEntity.ok()
                .header("Cache-Control", "private, max-age=10")
                .header("Vary", "Authorization")
                .header("X-Doc-Workspace", workspaceId)
                .header("X-Doc-User", userId)
                .header("X-Doc-Result-Total", String.valueOf(workspaceDocs.size()))
                .header("X-Doc-Cache", "MISS")
                .body(response);
    }

    /**
     * 获取文档协作者
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}/collaborators")
    public ResponseEntity<Map<String, Object>> getDocCollaborators(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        Optional<WorkspaceDoc> docOpt = docService.findById(docId);
        if (docOpt.isEmpty() || !workspaceId.equals(docOpt.get().getWorkspaceId())) {
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }

        // 检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        List<String> collaborators = docService.getDocCollaborators(docId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("collaborators", collaborators);
        response.put("count", collaborators.size());
        
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Vary", "Authorization")
                .header("X-Doc-Workspace", workspaceId)
                .header("X-Doc-Id", docId)
                .header("X-Doc-Result-Total", String.valueOf(collaborators.size()))
                .header("X-Doc-Cache", "MISS")
                .body(response);
    }

    /**
     * 设置文档标题
     */
    @PutMapping("/api/workspaces/{workspaceId}/docs/{docId}/title")
    public ResponseEntity<Map<String, Object>> setDocTitle(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody SetTitleRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档编辑权限
        if (!docService.hasDocEditPermission(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        WorkspaceDoc existingDoc = docService.findById(docId).orElse(null);
        if (existingDoc == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        if (!workspaceId.equals(existingDoc.getWorkspaceId())) {
            log.warn("Workspace mismatch when setting doc title: requestedWorkspaceId={}, actualWorkspaceId={}, docId={}",
                    workspaceId, existingDoc.getWorkspaceId(), docId);
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        
        try {
            docService.setDocTitle(docId, request.title());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document title updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to set document title", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 设置文档公开状态
     */
    @PutMapping("/api/workspaces/{workspaceId}/docs/{docId}/public")
    public ResponseEntity<Map<String, Object>> setDocPublic(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody SetPublicRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 检查文档编辑权限
        if (!docService.hasDocEditPermission(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        WorkspaceDoc existingDoc = docService.findById(docId).orElse(null);
        if (existingDoc == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        if (!workspaceId.equals(existingDoc.getWorkspaceId())) {
            log.warn("Workspace mismatch when setting doc public status: requestedWorkspaceId={}, actualWorkspaceId={}, docId={}",
                    workspaceId, existingDoc.getWorkspaceId(), docId);
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }
        
        try {
            // 支持设置 publicPermission 和 publicMode
            docService.setDocPublic(
                docId, 
                request.isPublic(),
                request.publicPermission(),
                request.publicMode()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document public status updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to set document public status", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取文档统计信息
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}/stats")
    public ResponseEntity<Map<String, Object>> getDocStats(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        Optional<WorkspaceDoc> docOpt = docService.findById(docId);
        if (docOpt.isEmpty() || !workspaceId.equals(docOpt.get().getWorkspaceId())) {
            return ResponseEntity.status(404).body(Map.of("error", "Document not found"));
        }

        // 检查文档访问权限
        if (!docService.hasDocAccess(docId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        WorkspaceDocService.DocStats stats = docService.getDocStats(docId);

        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);

        String statsVersion = stats.lastModified() != null
                ? String.valueOf(stats.lastModified().toEpochMilli())
                : "0";

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Vary", "Authorization")
                .header("X-Doc-Workspace", workspaceId)
                .header("X-Doc-Id", docId)
                .header("X-Doc-Stats-Version", statsVersion)
                .header("X-Doc-Cache", "MISS")
                .body(response);
    }

    /**
     * 获取文档元数据
     */
    @GetMapping("/api/workspaces/{workspaceId}/docs/{docId}/meta")
    public ResponseEntity<Map<String, Object>> getDocMeta(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "X-User-ID", required = false) String userIdFromHeader,
            @RequestParam(value = "userId", required = false) String userIdFromParam,
            @RequestParam(value = "fields", required = false) String fieldsParam,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("获取文档元数据请求: workspaceId={}, docId={}, Accept={}, X-User-ID={}, userId={}, fields={}",
                workspaceId, docId, acceptHeader, userIdFromHeader, userIdFromParam, fieldsParam);

        Optional<WorkspaceDoc> docOpt = docService.findById(docId);
        if (docOpt.isEmpty()) {
            log.warn("文档不存在: {}", docId);
            return ResponseEntity.notFound().build();
        }

        WorkspaceDoc doc = docOpt.get();
        if (!workspaceId.equals(doc.getWorkspaceId())) {
            log.warn("文档不属于工作空间: docId={}, workspaceId={}", docId, workspaceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Document not found"));
        }

        if (!acceptsJson(acceptHeader)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(Map.of("error", "Unsupported media type", "supportedTypes", "application/json"));
        }

        boolean isPublicDoc = Boolean.TRUE.equals(doc.getPublic());
        String resolvedUserId = resolveUserId(authentication, userIdFromHeader, userIdFromParam, request);

        if (!isPublicDoc) {
            if (resolvedUserId == null) {
                log.warn("未授权访问文档元数据: docId={}", docId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }
            if (!docService.hasDocAccess(docId, resolvedUserId)) {
                log.warn("无权访问文档元数据: docId={}, userId={}", docId, resolvedUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }
        }

        Instant createdAtInstant = toInstant(doc.getCreatedAt());
        Instant updatedAtInstant = toInstant(doc.getUpdatedAt());

        Map<String, Object> meta = new HashMap<>();
        meta.put("id", doc.getId());
        meta.put("docId", doc.getDocId());
        meta.put("workspaceId", doc.getWorkspaceId());
        meta.put("title", doc.getTitle());
        meta.put("summary", doc.getSummary());
        meta.put("mode", doc.getMode());
        meta.put("defaultRole", doc.getDefaultRole());
        meta.put("isPublic", doc.getPublic());
        meta.put("publicMode", doc.getPublicMode());
        meta.put("publicPermission", doc.getPublicPermission());
        meta.put("blocked", doc.getBlocked());
        meta.put("favorite", false);
        meta.put("tags", new ArrayList<>());
        meta.put("trash", false);
        meta.put("createDate", createdAtInstant != null ? createdAtInstant.toEpochMilli() : null);
        meta.put("updatedDate", updatedAtInstant != null ? updatedAtInstant.toEpochMilli() : null);

        if (fieldsParam != null && !fieldsParam.isBlank()) {
            Set<String> requestedFields = new LinkedHashSet<>();
            for (String field : fieldsParam.split(",")) {
                String trimmed = field.trim();
                if (!trimmed.isEmpty()) {
                    requestedFields.add(trimmed);
                }
            }
            meta = filterMetaFields(meta, requestedFields);
        }

        Map<String, Object> response = Map.of("meta", meta);
        String version = updatedAtInstant != null ? String.valueOf(updatedAtInstant.toEpochMilli()) : String.valueOf(System.currentTimeMillis());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cache-Control", isPublicDoc ? "public, max-age=60" : "private, max-age=60")
                .header("Vary", "Authorization, Accept")
                .header("X-Doc-Workspace", workspaceId)
                .header("X-Doc-Id", docId)
                .header("X-Doc-Version", version)
                .header("X-Doc-Cache", "MISS")
                .header("ETag", "W/\"" + version + "\"")
                .body(response);
    }

    private boolean shouldReturnJson(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isEmpty()) {
            return false;
        }
        String normalized = acceptHeader.toLowerCase(Locale.ROOT);
        return normalized.contains(MediaType.APPLICATION_JSON_VALUE) || normalized.contains("+json");
    }

    /**
     * 健康检查端点 - 用于调试连接问题
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("【健康检查】收到健康检查请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "AFFiNE Backend is running");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试端点 - 用于调试CORS和网络问题
     */
    @GetMapping("/api/test")
    public ResponseEntity<Map<String, Object>> test() {
        log.info("【测试端点】收到测试请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test endpoint working");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 调试端点 - 手动触发根文档创建测试
     */
    @PostMapping("/api/debug/create-root-doc/{workspaceId}")
    public ResponseEntity<Map<String, Object>> debugCreateRootDoc(
            @PathVariable String workspaceId) {
        
        log.info("🧪 [DEBUG] 手动触发根文档创建测试: workspaceId={}", workspaceId);
        
        try {
            // 使用RootDocumentService通过ApplicationContext获取
            RootDocumentService rootDocService =
                applicationContext.getBean(RootDocumentService.class);
            
            // 调用根文档创建
            Boolean result = rootDocService.createRootDocument(workspaceId, "debug-user")
                    .block(); // 同步等待结果
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("workspaceId", workspaceId);
            response.put("rootDocCreated", result);
            response.put("message", "Root document creation test completed");
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("🧪 [DEBUG] 根文档创建测试结果: workspaceId={}, created={}", workspaceId, result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("🧪 [DEBUG] 根文档创建测试失败: workspaceId={}", workspaceId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("workspaceId", workspaceId);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    // 辅助方法
    
    private byte[] resolveSnapshotBytes(Snapshot snapshot) {
        if (snapshot == null || snapshot.getBlob() == null) {
            return new byte[0];
        }
        return binaryStorageService.resolvePointer(snapshot.getBlob(), snapshot.getWorkspaceId(), snapshot.getId());
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private boolean acceptsJson(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return true;
        }
        String normalized = acceptHeader.toLowerCase(Locale.ROOT);
        return normalized.contains("application/json") || normalized.contains("*/*");
    }

    private String resolveUserId(Authentication authentication, String headerUserId, String paramUserId, HttpServletRequest request) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AffineUserDetails affineUserDetails) {
                return affineUserDetails.getUserId();
            }
            if (principal instanceof String principalName && !principalName.isBlank()) {
                return principalName;
            }
        }

        if (headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId;
        }

        if (paramUserId != null && !paramUserId.isBlank()) {
            return paramUserId;
        }

        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank() && !authHeader.startsWith("Bearer ")) {
                return authHeader.trim();
            }
        }

        return null;
    }

    private Map<String, Object> filterMetaFields(Map<String, Object> meta, Set<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return meta;
        }

        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String field : fields) {
            if (meta.containsKey(field)) {
                filtered.put(field, meta.get(field));
            }
        }
        return filtered.isEmpty() ? meta : filtered;
    }

    private Instant toInstant(LocalDateTime source) {
        if (source == null) {
            return null;
        }
        return source.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * 清理标识符，移除空值和无效值
     */
    private String sanitizeIdentifier(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("null".equalsIgnoreCase(value) || "undefined".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }
    
    /**
     * 返回第一个非空白值
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
    
    // 请求数据类
    public record CreateDocRequest(String title, String docId) {}
    public record UpdateDocRequest(String title, boolean isPublic) {}
    public record SetTitleRequest(String title) {}
    public record SetPublicRequest(
        boolean isPublic,
        String publicPermission, // read-only/append-only
        String publicMode         // page/edgeless
    ) {}
    public record DocUpdateRequest(String update, Long timestamp, String sessionId, String clientId) {}
}
