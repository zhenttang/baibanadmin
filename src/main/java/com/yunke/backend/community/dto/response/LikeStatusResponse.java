package com.yunke.backend.community.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞状态响应
 */
@Data
public class LikeStatusResponse {
    
    /**
     * 是否已点赞
     */
    private Boolean isLiked;
    
    /**
     * 点赞总数
     */
    private Integer likeCount;
    
    /**
     * 点赞时间（如果已点赞）
     */
    private LocalDateTime likedAt;
}