package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.DocRecord;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档写入器接口
 * 支持多种存储后端的文档写入
 */
public interface DocWriter {
    
    /**
     * 创建或更新文档快照
     * 
     * @param record 文档记录
     * @return 是否成功
     */
    Mono<Boolean> upsertDoc(DocRecord record);
    
    /**
     * 推送文档更新
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param updates 更新数据列表
     * @param editorId 编辑者ID
     * @return 更新数量
     */
    Mono<Integer> pushDocUpdates(String workspaceId, String docId, List<byte[]> updates, String editorId);
    
    /**
     * 删除文档
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @return 是否成功
     */
    Mono<Boolean> deleteDoc(String workspaceId, String docId);
    
    /**
     * 创建初始文档内容
     * 
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param editorId 创建者ID
     * @return 文档记录
     */
    Mono<DocRecord> createInitialDoc(String workspaceId, String docId, String editorId);
} 