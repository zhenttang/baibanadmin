package com.yunke.backend.document.service;


import com.yunke.backend.community.dto.response.LikeStatusResponse;
import com.yunke.backend.community.dto.response.UserLikeInfo;
import org.springframework.data.domain.Page;

/**
 * 文档点赞服务接口
 */
public interface DocumentLikeService {
    
    /**
     * 点赞文档
     * @param documentId 文档ID
     * @param userId 用户ID
     */
    void likeDocument(String documentId, String userId);
    
    /**
     * 取消点赞文档
     * @param documentId 文档ID
     * @param userId 用户ID
     */
    void unlikeDocument(String documentId, String userId);
    
    /**
     * 获取文档点赞状态
     * @param documentId 文档ID
     * @param userId 用户ID
     * @return 点赞状态信息
     */
    LikeStatusResponse getLikeStatus(String documentId, String userId);
    
    /**
     * 获取文档点赞用户列表
     * @param documentId 文档ID
     * @param page 页码
     * @param size 每页大小
     * @return 点赞用户列表
     */
    Page<UserLikeInfo> getDocumentLikes(String documentId, int page, int size);
}