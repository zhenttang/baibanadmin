package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.*;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.workspace.dto.WorkspaceExportDto;
import com.yunke.backend.workspace.dto.WorkspaceExportInput;
import com.yunke.backend.workspace.dto.WorkspaceImportResultDto;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 文档导入导出服务接口
 */
public interface DocImportExportService {

    /**
     * 智能导入文档
     * @param workspaceId 工作空间ID
     * @param input 导入参数
     * @param userId 用户ID
     * @return 导入的文档
     */
    Mono<DocDto> smartImportDoc(String workspaceId, ImportDocInput input, String userId);

    /**
     * 批量导入文档
     * @param workspaceId 工作空间ID
     * @param inputs 导入参数列表
     * @param userId 用户ID
     * @return 导入结果列表
     */
    Mono<List<DocImportResultDto>> batchImportDocs(String workspaceId, List<ImportDocInput> inputs, String userId);

    /**
     * 高级导出文档
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param input 导出参数
     * @param userId 用户ID
     * @return 导出结果
     */
    Mono<DocExportDto> advancedExportDoc(String workspaceId, String docId, AdvancedExportInput input, String userId);

    /**
     * 批量导出文档
     * @param workspaceId 工作空间ID
     * @param docIds 文档ID列表
     * @param format 导出格式
     * @param userId 用户ID
     * @return 导出结果列表
     */
    Mono<List<DocExportDto>> batchExportDocs(String workspaceId, List<String> docIds, DocExportFormat format, String userId);

    /**
     * 导出工作空间
     * @param workspaceId 工作空间ID
     * @param input 导出参数
     * @param userId 用户ID
     * @return 导出结果
     */
    Mono<WorkspaceExportDto> exportWorkspace(String workspaceId, WorkspaceExportInput input, String userId);

    /**
     * 从ZIP导入工作空间
     * @param workspaceId 工作空间ID
     * @param zipContent ZIP内容
     * @param userId 用户ID
     * @return 导入结果
     */
    Mono<WorkspaceImportResultDto> importWorkspaceFromZip(String workspaceId, byte[] zipContent, String userId);

    /**
     * 转换文档格式
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param sourceFormat 源格式
     * @param targetFormat 目标格式
     * @param userId 用户ID
     * @return 转换结果
     */
    Mono<DocExportDto> convertDocFormat(String workspaceId, String docId, DocExportFormat sourceFormat, DocExportFormat targetFormat, String userId);

    /**
     * 预览导入
     * @param input 导入参数
     * @return 预览结果
     */
    Mono<ImportPreviewDto> previewImport(ImportDocInput input);
}