package com.yunke.backend.document.service;

import reactor.core.publisher.Mono;

/**
 * 根文档服务接口
 * 负责工作空间根文档的创建和管理
 */
public interface RootDocumentService {
    
    /**
     * 为工作空间创建根文档
     * 根文档是工作空间的核心文档，其 docId 等于 workspaceId
     * 
     * @param workspaceId 工作空间ID
     * @param creatorUserId 创建者用户ID
     * @return 创建成功返回 true
     */
    Mono<Boolean> createRootDocument(String workspaceId, String creatorUserId);
    
    /**
     * 检查工作空间是否存在根文档
     * 
     * @param workspaceId 工作空间ID
     * @return 存在返回 true
     */
    Mono<Boolean> hasRootDocument(String workspaceId);
    
    /**
     * 获取默认的根文档内容（空白Y.js文档）
     * 
     * @return Y.js空白文档的二进制数据
     */
    byte[] getDefaultRootDocumentContent();
}