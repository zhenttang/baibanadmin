package com.yunke.backend.community.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户点赞信息
 */
@Data
public class UserLikeInfo {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 点赞时间
     */
    private LocalDateTime likedAt;
}