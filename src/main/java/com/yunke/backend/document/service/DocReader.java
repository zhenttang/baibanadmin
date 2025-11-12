package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.DocRecord;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 文档读取器接口
 * 支持多种存储后端的文档读取
 */
public interface DocReader {
    
    /**
     * 获取文档记录
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @return 文档记录
     */
    Mono<Optional<DocRecord>> getDoc(String workspaceId, String docId);
    
    /**
     * 获取文档快照
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @return 文档快照数据
     */
    Mono<Optional<byte[]>> getDocSnapshot(String workspaceId, String docId);
    
    /**
     * 获取文档更新列表
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param since 从指定时间开始
     * @return 更新列表
     */
    Mono<byte[]> getDocUpdates(String workspaceId, String docId, LocalDateTime since);
    
    /**
     * 获取文档的状态向量差异
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param stateVector 客户端状态向量
     * @return 差异更新数据
     */
    Mono<byte[]> getDocDiff(String workspaceId, String docId, byte[] stateVector);
    
    /**
     * 检查文档是否存在
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @return 是否存在
     */
    Mono<Boolean> docExists(String workspaceId, String docId);
    
    /**
     * 获取文档最后修改时间
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @return 最后修改时间
     */
    Mono<Optional<LocalDateTime>> getDocLastModified(String workspaceId, String docId);
}