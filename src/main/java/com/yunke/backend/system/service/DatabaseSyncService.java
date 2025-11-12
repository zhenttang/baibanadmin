package com.yunke.backend.system.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * 数据库同步服务接口
 * 处理AFFiNE前端的数据库集合同步请求
 */
public interface DatabaseSyncService {
    
    /**
     * 处理数据库同步请求
     * 
     * @param workspaceId 工作空间ID
     * @param collectionName 集合名称 (如: docProperties, docCustomPropertyInfo, folders)
     * @param userId 用户ID
     * @return Y.js格式的文档数据
     */
    Mono<ResponseEntity<byte[]>> handleDatabaseSync(String workspaceId, String collectionName, String userId);
    
    /**
     * 检查是否支持指定的集合同步
     * 
     * @param collectionName 集合名称
     * @return 是否支持
     */
    boolean isCollectionSupported(String collectionName);
    
    /**
     * 获取支持的集合列表
     * 
     * @return 支持的集合名称数组
     */
    String[] getSupportedCollections();
    
    /**
     * 处理数据库同步差异请求
     * 
     * @param workspaceId 工作空间ID
     * @param collectionName 集合名称
     * @param stateVector 状态向量
     * @param userId 用户ID
     * @return 差异数据
     */
    Mono<ResponseEntity<byte[]>> handleDatabaseSyncDiff(String workspaceId, String collectionName, byte[] stateVector, String userId);
}