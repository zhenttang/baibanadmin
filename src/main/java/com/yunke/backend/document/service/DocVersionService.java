package com.yunke.backend.document.service;


import com.yunke.backend.common.dto.PaginatedResponse;

import com.yunke.backend.common.dto.PaginationInput;
import com.yunke.backend.document.dto.DocDiffDto;
import com.yunke.backend.document.dto.DocHistoryDto;
import com.yunke.backend.document.dto.DocVersionDto;
import com.yunke.backend.system.dto.SnapshotDto;
import com.yunke.backend.system.dto.UpdateDto;
import reactor.core.publisher.Mono;

/**
 * 文档版本服务接口
 */
public interface DocVersionService {

    /**
     * 创建文档版本
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param content 文档内容
     * @param userId 用户ID
     * @return 创建的版本信息
     */
    Mono<DocVersionDto> createVersion(String workspaceId, String docId, byte[] content, String userId);

    /**
     * 获取文档版本
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param versionId 版本ID
     * @param userId 用户ID
     * @return 版本信息
     */
    Mono<DocVersionDto> getVersion(String workspaceId, String docId, String versionId, String userId);

    /**
     * 获取文档版本列表
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param pagination 分页参数
     * @param userId 用户ID
     * @return 版本列表
     */
    Mono<PaginatedResponse<DocVersionDto>> getVersions(String workspaceId, String docId, PaginationInput pagination, String userId);

    /**
     * 恢复到指定版本
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param versionId 版本ID
     * @param userId 用户ID
     * @return 操作结果
     */
    Mono<Boolean> restoreVersion(String workspaceId, String docId, String versionId, String userId);
    
    /**
     * 获取文档历史记录
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param pagination 分页参数
     * @param userId 用户ID
     * @return 历史记录列表
     */
    Mono<PaginatedResponse<DocHistoryDto>> getDocHistories(String workspaceId, String docId, PaginationInput pagination, String userId);
    
    /**
     * 恢复文档到指定时间点
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param timestamp 时间戳
     * @param userId 用户ID
     * @return 恢复时间
     */
    Mono<java.time.LocalDateTime> recoverDocToVersion(String workspaceId, String docId, java.time.LocalDateTime timestamp, String userId);
    
    /**
     * 获取文档差异
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param stateVector 状态向量
     * @param userId 用户ID
     * @return 差异信息
     */
    Mono<DocDiffDto> getDocDiff(String workspaceId, String docId, byte[] stateVector, String userId);
    
    /**
     * 获取最新快照
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 快照信息
     */
    Mono<SnapshotDto> getLatestSnapshot(String workspaceId, String docId, String userId);
    
    /**
     * 创建快照
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param blob 二进制数据
     * @param state 状态数据
     * @param userId 用户ID
     * @return 快照信息
     */
    Mono<SnapshotDto> createSnapshot(String workspaceId, String docId, byte[] blob, byte[] state, String userId);
    
    /**
     * 应用文档更新
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param update 更新数据
     * @param userId 用户ID
     * @return 操作结果
     */
    Mono<Boolean> applyDocUpdate(String workspaceId, String docId, byte[] update, String userId);
    
    /**
     * 获取文档更新列表
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param since 开始时间
     * @param userId 用户ID
     * @return 更新列表
     */
    reactor.core.publisher.Flux<UpdateDto> getDocUpdates(String workspaceId, String docId, java.time.LocalDateTime since, String userId);
}