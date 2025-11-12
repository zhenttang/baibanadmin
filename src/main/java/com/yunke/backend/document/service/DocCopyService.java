package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.CopyDocInput;
import com.yunke.backend.document.dto.DocDto;
import com.yunke.backend.document.domain.entity.DocExportFormat;
import com.yunke.backend.document.dto.DuplicateDocInput;
import reactor.core.publisher.Mono;

/**
 * 文档复制服务接口
 */
public interface DocCopyService {

    /**
     * 复制文档
     * @param sourceWorkspaceId 源工作空间ID
     * @param sourceDocId 源文档ID
     * @param targetWorkspaceId 目标工作空间ID
     * @param userId 用户ID
     * @return 复制后的文档
     */
    Mono<DocDto> copyDoc(String sourceWorkspaceId, String sourceDocId, String targetWorkspaceId, String userId);

    /**
     * 复制文档到工作空间（带选项）
     * @param sourceWorkspaceId 源工作空间ID
     * @param sourceDocId 源文档ID
     * @param targetWorkspaceId 目标工作空间ID
     * @param input 复制选项
     * @param userId 用户ID
     * @return 复制后的文档
     */
    Mono<DocDto> copyDocToWorkspace(String sourceWorkspaceId, String sourceDocId, String targetWorkspaceId,
                                    CopyDocInput input, String userId);

    /**
     * 复制文档到同一工作空间
     * @param workspaceId 工作空间ID
     * @param sourceDocId 源文档ID
     * @param userId 用户ID
     * @return 复制后的文档
     */
    Mono<DocDto> duplicateDoc(String workspaceId, String sourceDocId, String userId);
    
    /**
     * 复制文档到同一工作空间（带选项）
     * @param workspaceId 工作空间ID
     * @param sourceDocId 源文档ID
     * @param input 复制选项
     * @param userId 用户ID
     * @return 复制后的文档
     */
    Mono<DocDto> duplicateDoc(String workspaceId, String sourceDocId,
                              DuplicateDocInput input, String userId);
}