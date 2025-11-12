package com.yunke.backend.modules.document.api;

import com.yunke.backend.document.collaboration.SpaceSyncGateway;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.storage.impl.WorkspaceDocStorageAdapter;
import com.yunke.backend.document.util.YjsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档协作HTTP API控制器
 * 提供文档同步的HTTP接口，作为WebSocket的补充
 * 
 * 对应AFFiNE的文档同步HTTP API
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/docs/{docId}/sync")
@RequiredArgsConstructor
@Slf4j
public class DocSyncController {
    
    private final SpaceSyncGateway syncGateway;
    private final WorkspaceDocService docService;
    private final WorkspaceDocStorageAdapter storageAdapter;
    
    /**
     * 获取文档当前状态
     * GET /api/workspaces/{workspaceId}/docs/{docId}/sync
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoc(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.info("📄 [DocSyncController] 获取文档: workspaceId={}, docId={}", workspaceId, docId);
        
        try {
            // 从存储适配器获取文档
            var docRecord = storageAdapter.getDoc(workspaceId, docId);
            
            if (docRecord == null || docRecord.getBlob() == null) {
                log.warn("⚠️ [DocSyncController] 文档不存在: docKey={}:{}", workspaceId, docId);
                return ResponseEntity.notFound().build();
            }
            
            byte[] docData = docRecord.getBlob();
            long timestamp = docRecord.getTimestamp();
            
            Map<String, Object> response = new HashMap<>();
            response.put("doc", Base64.getEncoder().encodeToString(docData));
            response.put("timestamp", timestamp);
            response.put("workspaceId", workspaceId);
            response.put("docId", docId);
            
            // 尝试提取明文内容用于搜索和预览
            try {
                String plainText = YjsUtils.extractPlainText(docData);
                if (plainText != null && !plainText.trim().isEmpty()) {
                    response.put("plainText", plainText.substring(0, Math.min(500, plainText.length())));
                }
            } catch (Exception e) {
                log.debug("📝 [DocSyncController] 提取明文失败: docKey={}:{}", workspaceId, docId, e);
            }
            
            log.info("✅ [DocSyncController] 文档获取成功: docKey={}:{}, size={}B", 
                    workspaceId, docId, docData.length);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [DocSyncController] 获取文档失败: docKey={}:{}", workspaceId, docId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取文档失败: " + e.getMessage()));
        }
    }
    
    /**
     * 应用文档更新
     * POST /api/workspaces/{workspaceId}/docs/{docId}/sync/updates
     */
    @PostMapping("/updates")
    public ResponseEntity<Map<String, Object>> applyUpdate(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody Map<String, Object> request) {
        
        log.info("🔄 [DocSyncController] 应用文档更新: workspaceId={}, docId={}", workspaceId, docId);
        
        try {
            String updateBase64 = (String) request.get("update");
            
            if (updateBase64 == null || updateBase64.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "缺少更新数据"));
            }
            
            // 应用更新
            long timestamp = docService.applyYjsUpdate(workspaceId, docId, updateBase64);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", timestamp);
            response.put("workspaceId", workspaceId);
            response.put("docId", docId);
            
            log.info("✅ [DocSyncController] 文档更新成功: docKey={}:{}, timestamp={}", 
                    workspaceId, docId, timestamp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [DocSyncController] 应用文档更新失败: docKey={}:{}", workspaceId, docId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "应用更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取文档统计信息
     * GET /api/workspaces/{workspaceId}/docs/{docId}/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDocStats(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.info("📊 [DocSyncController] 获取文档统计: workspaceId={}, docId={}", workspaceId, docId);
        
        try {
            var docRecord = storageAdapter.getDoc(workspaceId, docId);
            if (docRecord == null) {
                return ResponseEntity.notFound().build();
            }
            
            var roomStatus = syncGateway.getDocRoomStatus(workspaceId, docId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("workspaceId", workspaceId);
            response.put("docId", docId);
            response.put("size", docRecord.getBlob() != null ? docRecord.getBlob().length : 0);
            response.put("lastModified", docRecord.getTimestamp());
            response.put("activeClients", roomStatus.get("clientCount"));
            response.put("editor", docRecord.getEditor());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [DocSyncController] 获取文档统计失败: docKey={}:{}", workspaceId, docId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取统计失败: " + e.getMessage()));
        }
    }
}