package com.yunke.backend.document.controller;

import com.yunke.backend.workspace.service.WorkspaceRepairService;
import com.yunke.backend.workspace.service.WorkspaceRepairService.RootDocumentCheckResult;
import com.yunke.backend.workspace.service.WorkspaceRepairService.RootDocumentRepairResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 根文档管理控制器
 * 用于管理和修复工作空间根文档问题
 * 
 * 这是一个管理员工具，用于修复现有工作空间的根文档缺失问题
 */
@RestController
@RequestMapping("/api/admin/root-docs")
@RequiredArgsConstructor
@Slf4j
public class RootDocumentController {

    private final WorkspaceRepairService workspaceRepairService;

    /**
     * 检查所有工作空间的根文档状态
     * 
     * GET /api/admin/root-docs/status
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> checkRootDocumentStatus() {
        log.info("🔍 [ROOT-DOC-API] 接收到根文档状态检查请求");

        return workspaceRepairService.checkAllWorkspacesRootDocuments()
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("totalWorkspaces", result.totalWorkspaces());
                    response.put("workspacesWithRootDoc", result.workspacesWithRootDoc());
                    response.put("workspacesWithoutRootDoc", result.workspacesWithoutRootDoc());
                    response.put("success", true);
                    response.put("message", String.format(
                            "检查完成：总共 %d 个工作空间，%d 个有根文档，%d 个缺少根文档",
                            result.totalWorkspaces(),
                            result.workspacesWithRootDoc(),
                            result.workspacesWithoutRootDoc()
                    ));
                    
                    log.info("📊 [ROOT-DOC-API] 返回检查结果: {}", response);
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> {
                    log.error("❌ [ROOT-DOC-API] 检查根文档状态失败: {}", error.getMessage(), error);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "检查根文档状态时发生错误"
                )));
    }

    /**
     * 为所有缺少根文档的工作空间创建根文档
     * 
     * POST /api/admin/root-docs/create-missing
     */
    @PostMapping("/create-missing")
    public Mono<ResponseEntity<Map<String, Object>>> createMissingRootDocuments() {
        log.info("🛠️ [ROOT-DOC-API] 接收到批量创建缺失根文档的请求");

        return workspaceRepairService.repairAllMissingRootDocuments()
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("totalProcessed", result.totalProcessed());
                    response.put("successCount", result.successCount());
                    response.put("skippedCount", result.skippedCount());
                    response.put("errorCount", result.errorCount());
                    response.put("success", result.errorCount() == 0);
                    response.put("message", String.format(
                            "批量修复完成：处理 %d 个工作空间，成功 %d 个，跳过 %d 个，失败 %d 个",
                            result.totalProcessed(),
                            result.successCount(),
                            result.skippedCount(),
                            result.errorCount()
                    ));
                    
                    log.info("🏁 [ROOT-DOC-API] 返回批量修复结果: {}", response);
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> {
                    log.error("❌ [ROOT-DOC-API] 批量创建根文档失败: {}", error.getMessage(), error);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "批量创建根文档时发生错误"
                )));
    }

    /**
     * 为特定工作空间创建根文档
     * 
     * POST /api/admin/root-docs/create/{workspaceId}
     */
    @PostMapping("/create/{workspaceId}")
    public Mono<ResponseEntity<Map<String, Object>>> createRootDocumentForWorkspace(
            @PathVariable String workspaceId) {
        
        log.info("🎯 [ROOT-DOC-API] 接收到为特定工作空间创建根文档的请求: workspaceId='{}'", workspaceId);
        
        return workspaceRepairService.repairWorkspaceRootDocument(workspaceId)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("workspaceId", workspaceId);
                    response.put("success", success);
                    
                    if (success) {
                        response.put("message", "根文档创建成功");
                        log.info("✅ [ROOT-DOC-API] 根文档创建成功: workspaceId='{}'", workspaceId);
                    } else {
                        response.put("message", "根文档创建失败或已存在");
                        log.warn("⚠️ [ROOT-DOC-API] 根文档创建失败或已存在: workspaceId='{}'", workspaceId);
                    }
                    
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> {
                    log.error("❌ [ROOT-DOC-API] 创建根文档时发生错误: workspaceId='{}', error={}", 
                            workspaceId, error.getMessage(), error);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of(
                        "workspaceId", workspaceId,
                        "success", false,
                        "message", "创建根文档时发生错误"
                )));
    }
}