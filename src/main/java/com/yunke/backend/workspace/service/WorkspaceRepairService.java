package com.yunke.backend.workspace.service;

import reactor.core.publisher.Mono;

/**
 * 工作空间修复服务接口
 * 用于修复现有工作空间的问题，如缺失的根文档
 */
public interface WorkspaceRepairService {
    
    /**
     * 检查所有工作空间的根文档状态
     * 
     * @return 检查结果统计
     */
    Mono<RootDocumentCheckResult> checkAllWorkspacesRootDocuments();
    
    /**
     * 为所有缺少根文档的工作空间创建根文档
     * 
     * @return 修复结果统计
     */
    Mono<RootDocumentRepairResult> repairAllMissingRootDocuments();
    
    /**
     * 为特定工作空间创建根文档
     * 
     * @param workspaceId 工作空间ID
     * @return 是否创建成功
     */
    Mono<Boolean> repairWorkspaceRootDocument(String workspaceId);
    
    /**
     * 根文档检查结果
     */
    record RootDocumentCheckResult(
        int totalWorkspaces,
        int workspacesWithRootDoc,
        int workspacesWithoutRootDoc
    ) {}
    
    /**
     * 根文档修复结果
     */
    record RootDocumentRepairResult(
        int totalProcessed,
        int successCount,
        int skippedCount,
        int errorCount
    ) {}
}