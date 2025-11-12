package com.yunke.backend.user.dto.response;

import lombok.Data;

/**
 * 用户信息响应
 */
@Data
public class UserInfo {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户邮箱
     */
    private String email;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 关注数
     */
    private Integer followingCount;
    
    /**
     * 粉丝数
     */
    private Integer followersCount;
    
    /**
     * 当前用户是否关注了此用户
     */
    private Boolean isFollowing;
    
    /**
     * 是否互相关注
     */
    private Boolean isMutualFollow;
    
    /**
     * 注册时间
     */
    private String createdAt;
}