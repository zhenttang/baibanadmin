package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.DocDto;
import com.yunke.backend.document.dto.CreateDocInput;
import com.yunke.backend.document.dto.UpdateDocInput;
import com.yunke.backend.document.dto.DocPermissionsDto;
import com.yunke.backend.document.dto.PublicDocMode;
import com.yunke.backend.document.dto.DocUserRoleDto;
import com.yunke.backend.document.dto.DocHistoryDto;
import com.yunke.backend.document.dto.DocDiffDto;
import com.yunke.backend.document.dto.ApplyUpdateInput;
import com.yunke.backend.document.dto.DocMetaDto;
import com.yunke.backend.document.dto.UpdateDocMetaInput;
import com.yunke.backend.document.dto.DocStatsDto;
import com.yunke.backend.document.dto.DocActivityDto;
import com.yunke.backend.document.dto.ImportDocInput;
import com.yunke.backend.document.dto.DocImportResultDto;
import com.yunke.backend.document.dto.DocExportDto;
import com.yunke.backend.document.dto.AdvancedExportInput;
import com.yunke.backend.workspace.dto.WorkspaceExportInput;
import com.yunke.backend.workspace.dto.WorkspaceExportDto;
import com.yunke.backend.workspace.dto.WorkspaceImportResultDto;
import com.yunke.backend.document.dto.ImportPreviewDto;
import com.yunke.backend.system.dto.SnapshotDto;
import com.yunke.backend.system.dto.SaveSnapshotInput;
import com.yunke.backend.system.dto.UpdateDto;
import com.yunke.backend.common.dto.PaginationInput;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.document.domain.entity.DocRole;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档管理服务接口
 * 提供文档的增删改查、版本管理、权限控制等功能
 */
public interface DocService {

    // ==================== 文档 CRUD 操作 ====================
    
    /**
     * 创建新文档
     */
    Mono<DocDto> createDoc(String workspaceId, CreateDocInput input, String userId);
    
    /**
     * 获取文档详情
     */
    Mono<DocDto> getDoc(String workspaceId, String docId, String userId);
    
    /**
     * 更新文档内容
     */
    Mono<DocDto> updateDoc(String workspaceId, String docId, UpdateDocInput input, String userId);
    
    /**
     * 删除文档
     */
    Mono<Boolean> deleteDoc(String workspaceId, String docId, String userId);
    
    /**
     * 恢复已删除文档
     */
    Mono<DocDto> restoreDoc(String workspaceId, String docId, String userId);
    
    // ==================== 文档权限管理 ====================
    
    /**
     * 获取文档权限
     */
    Mono<DocPermissionsDto> getDocPermissions(String workspaceId, String docId, String userId);
    
    /**
     * 更新文档默认角色
     */
    Mono<Boolean> updateDocDefaultRole(String workspaceId, String docId, DocRole role, String userId);
    
    /**
     * 设置文档公开状态
     */
    Mono<DocDto> publishDoc(String workspaceId, String docId, PublicDocMode mode, String userId);
    
    /**
     * 取消文档公开
     */
    Mono<DocDto> revokePublicDoc(String workspaceId, String docId, String userId);
    
    // ==================== 文档用户权限 ====================
    
    /**
     * 授予用户文档权限
     */
    Mono<Boolean> grantDocUserRoles(String workspaceId, String docId, List<String> userIds, DocRole role, String operatorId);
    
    /**
     * 撤销用户文档权限
     */
    Mono<Boolean> revokeDocUserRole(String workspaceId, String docId, String userId, String operatorId);
    
    /**
     * 更新用户文档权限
     */
    Mono<Boolean> updateDocUserRole(String workspaceId, String docId, String userId, DocRole role, String operatorId);
    
    /**
     * 获取文档用户权限列表
     */
    Mono<PaginatedResponse<DocUserRoleDto>> getDocUserRoles(String workspaceId, String docId, PaginationInput pagination, String userId);
    
    // ==================== 文档搜索和列表 ====================
    
    /**
     * 获取工作空间文档列表
     */
    Mono<PaginatedResponse<DocDto>> getWorkspaceDocs(String workspaceId, PaginationInput pagination, String userId);
    
    /**
     * 搜索文档
     */
    Mono<PaginatedResponse<DocDto>> searchDocs(String workspaceId, String query, PaginationInput pagination, String userId);
    
    /**
     * 获取公开文档列表
     */
    Mono<PaginatedResponse<DocDto>> getPublicDocs(String workspaceId, PaginationInput pagination);
    
    // ==================== 文档版本管理 ====================
    
    /**
     * 获取文档历史版本
     */
    Mono<PaginatedResponse<DocHistoryDto>> getDocHistories(String workspaceId, String docId, PaginationInput pagination, String userId);
    
    /**
     * 恢复文档到指定版本
     */
    Mono<LocalDateTime> recoverDocToVersion(String workspaceId, String docId, LocalDateTime timestamp, String userId);
    
    /**
     * 获取文档差异
     */
    Mono<DocDiffDto> getDocDiff(String workspaceId, String docId, byte[] stateVector, String userId);
    
    // ==================== 文档同步相关 ====================
    
    /**
     * 获取文档快照
     */
    Mono<SnapshotDto> getDocSnapshot(String workspaceId, String docId, String userId);
    
    /**
     * 保存文档快照
     */
    Mono<Boolean> saveDocSnapshot(String workspaceId, String docId, SaveSnapshotInput input, String userId);
    
    /**
     * 应用文档更新
     */
    Mono<Boolean> applyDocUpdate(String workspaceId, String docId, ApplyUpdateInput input, String userId);
    
    /**
     * 获取文档更新流
     */
    Flux<UpdateDto> getDocUpdates(String workspaceId, String docId, LocalDateTime since, String userId);
    
    // ==================== 文档元数据 ====================
    
    /**
     * 获取文档元数据
     */
    Mono<DocMetaDto> getDocMeta(String workspaceId, String docId, String userId);
    
    /**
     * 更新文档元数据
     */
    Mono<DocMetaDto> updateDocMeta(String workspaceId, String docId, UpdateDocMetaInput input, String userId);
    
    /**
     * 复制文档
     */
    Mono<DocDto> copyDoc(String sourceWorkspaceId, String sourceDocId, String targetWorkspaceId, String userId);
    
    /**
     * 复制文档
     */
    Mono<DocDto> duplicateDoc(String workspaceId, String docId, String userId);
    
    // ==================== 统计和分析 ====================
    
    /**
     * 获取工作空间文档统计
     */
    Mono<DocStatsDto> getWorkspaceDocStats(String workspaceId, String userId);
    
    /**
     * 获取用户文档活动
     */
    Mono<PaginatedResponse<DocActivityDto>> getUserDocActivity(String userId, PaginationInput pagination);
    
    // ==================== 高级导入导出功能 ====================
    
    /**
     * 智能导入文档 - 自动检测格式并转换
     */
    Mono<DocDto> smartImportDoc(String workspaceId, ImportDocInput input, String userId);
    
    /**
     * 批量导入文档
     */
    Mono<List<DocImportResultDto>> batchImportDocs(String workspaceId, List<ImportDocInput> inputs, String userId);
    
    /**
     * 高级导出文档 - 支持自定义格式选项
     */
    Mono<DocExportDto> advancedExportDoc(String workspaceId, String docId, AdvancedExportInput input, String userId);
    
    /**
     * 批量导出文档
     */
    Mono<List<DocExportDto>> batchExportDocs(String workspaceId, List<String> docIds, DocExportFormat format, String userId);
    
    /**
     * 导出整个工作空间
     */
    Mono<WorkspaceExportDto> exportWorkspace(String workspaceId, WorkspaceExportInput input, String userId);
    
    /**
     * 从ZIP文件导入工作空间
     */
    Mono<WorkspaceImportResultDto> importWorkspaceFromZip(String workspaceId, byte[] zipContent, String userId);
    
    /**
     * 转换文档格式
     */
    Mono<DocExportDto> convertDocFormat(String workspaceId, String docId, DocExportFormat sourceFormat, DocExportFormat targetFormat, String userId);
    
    /**
     * 预览导入结果
     */
    Mono<ImportPreviewDto> previewImport(ImportDocInput input);
}

