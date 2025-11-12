package com.yunke.backend.document.controller;

import com.yunke.backend.document.dto.DocHistoryDto;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.document.service.DocumentHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文档历史记录控制器
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/docs")
@RequiredArgsConstructor
@Slf4j
public class DocumentHistoryController {

    private final DocumentHistoryService documentHistoryService;

    /**
     * 获取文档历史记录列表
     * GET /api/workspaces/{workspaceId}/docs/{pageDocId}/histories
     */
    @GetMapping("/{pageDocId}/histories")
    public ResponseEntity<PaginatedResponse<DocHistoryDto>> getDocumentHistories(
            @PathVariable String workspaceId,
            @PathVariable String pageDocId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "10") int take
    ) {
        log.info("获取文档历史记录列表: workspaceId={}, pageDocId={}, before={}, take={}",
                workspaceId, pageDocId, before, take);

        try {
            PaginatedResponse<DocHistoryDto> response = documentHistoryService
                    .getDocumentHistories(workspaceId, pageDocId, before, take);
            
            log.info("成功获取文档历史记录: workspaceId={}, pageDocId={}, count={}",
                    workspaceId, pageDocId, response.getData().size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("获取文档历史记录参数错误", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("获取文档历史记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取特定时间戳的文档快照
     * GET /api/workspaces/{workspaceId}/docs/{pageDocId}/histories/{timestamp}
     */
    @GetMapping("/{pageDocId}/histories/{timestamp}")
    public ResponseEntity<byte[]> getDocumentSnapshot(
            @PathVariable String workspaceId,
            @PathVariable String pageDocId,
            @PathVariable String timestamp
    ) {
        log.info("获取文档快照: workspaceId={}, pageDocId={}, timestamp={}",
                workspaceId, pageDocId, timestamp);

        try {
            byte[] snapshot = documentHistoryService.getDocumentSnapshot(workspaceId, pageDocId, timestamp);
            
            if (snapshot == null || snapshot.length == 0) {
                log.warn("快照数据为空: workspaceId={}, pageDocId={}, timestamp={}",
                        workspaceId, pageDocId, timestamp);
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(snapshot.length);
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=3600"); // 缓存1小时
            
            log.info("成功获取文档快照: workspaceId={}, pageDocId={}, timestamp={}, size={}",
                    workspaceId, pageDocId, timestamp, snapshot.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(snapshot);
        } catch (IllegalArgumentException e) {
            log.error("获取文档快照参数错误", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("获取文档快照失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 恢复文档到指定版本
     * POST /api/workspaces/{workspaceId}/docs/{docId}/recover
     */
    @PostMapping("/{docId}/recover")
    public ResponseEntity<Void> recoverDocumentVersion(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody Map<String, String> request
    ) {
        String timestamp = request.get("timestamp");
        
        log.info("恢复文档版本: workspaceId={}, docId={}, timestamp={}",
                workspaceId, docId, timestamp);

        try {
            if (timestamp == null || timestamp.isEmpty()) {
                log.error("恢复文档版本时间戳为空");
                return ResponseEntity.badRequest().build();
            }

            documentHistoryService.recoverDocumentVersion(workspaceId, docId, timestamp);
            
            log.info("成功恢复文档版本: workspaceId={}, docId={}, timestamp={}",
                    workspaceId, docId, timestamp);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("恢复文档版本参数错误", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("恢复文档版本失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 保存文档快照（内部使用）
     * POST /api/workspaces/{workspaceId}/docs/{docId}/snapshots
     */
    @PostMapping("/{docId}/snapshots")
    public ResponseEntity<DocHistoryDto> saveDocumentSnapshot(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody byte[] blob,
            @RequestParam(required = false) String createdBy
    ) {
        log.info("保存文档快照: workspaceId={}, docId={}, createdBy={}",
                workspaceId, docId, createdBy);

        try {
            var savedSnapshot = documentHistoryService.saveDocumentSnapshot(
                    workspaceId, docId, blob, null, createdBy);
            
            DocHistoryDto dto = documentHistoryService.convertToDto(savedSnapshot);
            
            log.info("成功保存文档快照: workspaceId={}, docId={}, timestamp={}",
                    workspaceId, docId, dto.getTimestamp());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            log.error("保存文档快照失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 清理过期的历史记录（管理员使用）
     * DELETE /api/workspaces/{workspaceId}/docs/histories/cleanup
     */
    @DeleteMapping("/histories/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredHistories(
            @PathVariable String workspaceId
    ) {
        log.info("清理过期历史记录: workspaceId={}", workspaceId);

        try {
            int cleanedCount = documentHistoryService.cleanupExpiredHistories();
            
            Map<String, Object> response = Map.of(
                    "cleaned", cleanedCount,
                    "workspaceId", workspaceId
            );
            
            log.info("成功清理过期历史记录: workspaceId={}, count={}", workspaceId, cleanedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理过期历史记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 调试接口：创建测试历史记录
     * POST /api/workspaces/{workspaceId}/docs/{docId}/histories/test
     */
    @PostMapping("/{docId}/histories/test")
    public ResponseEntity<Map<String, Object>> createTestHistory(
            @PathVariable String workspaceId,
            @PathVariable String docId
    ) {
        log.info("🧪 创建测试历史记录: workspaceId={}, docId={}", workspaceId, docId);

        try {
            // 创建测试数据 (简单的 Y.js 空文档格式)
            byte[] testBlob = new byte[] { 0x00, 0x00 }; // Y.js 空更新
            
            var savedSnapshot = documentHistoryService.saveDocumentSnapshot(
                    workspaceId, 
                    docId, 
                    testBlob, 
                    null, 
                    "test-user"
            );
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "workspaceId", workspaceId,
                    "docId", docId,
                    "timestamp", String.valueOf(savedSnapshot.getTimestamp()),
                    "message", "测试历史记录已创建"
            );
            
            log.info("✅ 测试历史记录创建成功: {}", response);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 创建测试历史记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}