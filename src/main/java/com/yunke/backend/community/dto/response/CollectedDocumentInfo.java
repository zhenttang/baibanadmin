package com.yunke.backend.community.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏文档信息
 */
@Data
public class CollectedDocumentInfo {
    
    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档描述
     */
    private String description;
    
    /**
     * 作者ID
     */
    private String authorId;
    
    /**
     * 作者名称
     */
    private String authorName;
    
    /**
     * 收藏夹名称
     */
    private String collectionName;
    
    /**
     * 收藏时间
     */
    private LocalDateTime collectedAt;
    
    /**
     * 文档浏览量
     */
    private Integer viewCount;
    
    /**
     * 文档点赞数
     */
    private Integer likeCount;
}