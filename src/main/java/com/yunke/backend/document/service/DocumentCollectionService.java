package com.yunke.backend.document.service;


import com.yunke.backend.community.dto.request.CollectRequest;
import com.yunke.backend.community.dto.response.CollectedDocumentInfo;
import org.springframework.data.domain.Page;

/**
 * 文档收藏服务接口
 */
public interface DocumentCollectionService {
    
    /**
     * 收藏文档
     * @param documentId 文档ID
     * @param userId 用户ID
     * @param request 收藏请求
     */
    void collectDocument(String documentId, String userId, CollectRequest request);
    
    /**
     * 取消收藏文档
     * @param documentId 文档ID
     * @param userId 用户ID
     */
    void uncollectDocument(String documentId, String userId);
    
    /**
     * 获取用户收藏列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 收藏文档列表
     */
    Page<CollectedDocumentInfo> getUserCollections(String userId, int page, int size);
    
    /**
     * 检查用户是否已收藏文档
     * @param documentId 文档ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    boolean isCollected(String documentId, String userId);
    
    /**
     * 获取文档收藏数量
     * @param documentId 文档ID
     * @return 收藏数量
     */
    Long getDocumentCollectionCount(String documentId);
    
    /**
     * 获取用户收藏数量
     * @param userId 用户ID
     * @return 收藏数量
     */
    Long getUserCollectionCount(String userId);
}