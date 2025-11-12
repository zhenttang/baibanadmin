package com.yunke.backend.user.service;

import com.yunke.backend.user.dto.response.UserInfo;
import com.yunke.backend.user.domain.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 用户关注服务接口
 */
public interface UserFollowService {
    
    /**
     * 关注用户
     *
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @throws RuntimeException 如果已经关注或关注自己
     */
    void followUser(String followerId, String followingId);
    
    /**
     * 取消关注用户
     *
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @throws RuntimeException 如果没有关注关系
     */
    void unfollowUser(String followerId, String followingId);
    
    /**
     * 检查用户A是否关注了用户B
     *
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否关注
     */
    boolean isFollowing(String followerId, String followingId);
    
    /**
     * 获取用户的关注列表
     * 
     * @param userId 用户ID
     * @param page 分页参数
     * @return 关注的用户列表
     */
    Page<UserInfo> getFollowingList(String userId, Pageable pageable);
    
    /**
     * 获取用户的粉丝列表
     * 
     * @param userId 用户ID
     * @param page 分页参数
     * @return 粉丝用户列表
     */
    Page<UserInfo> getFollowersList(String userId, Pageable pageable);
    
    /**
     * 获取用户的关注统计
     * 
     * @param userId 用户ID
     * @return 关注数和粉丝数
     */
    FollowStats getFollowStats(String userId);
    
    /**
     * 获取互相关注的用户列表
     * 
     * @param userId 用户ID
     * @return 互相关注的用户ID列表
     */
    List<String> getMutualFollows(String userId);
    
    /**
     * 批量检查关注状态
     *
     * @param followerId 关注者ID
     * @param followingIds 被关注者ID列表
     * @return 关注状态映射
     */
    java.util.Map<String, Boolean> batchCheckFollowStatus(String followerId, List<String> followingIds);
    
    /**
     * 关注统计信息
     */
    class FollowStats {
        private int followingCount;
        private int followersCount;
        
        public FollowStats(int followingCount, int followersCount) {
            this.followingCount = followingCount;
            this.followersCount = followersCount;
        }
        
        // getters and setters
        public int getFollowingCount() { return followingCount; }
        public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }
        public int getFollowersCount() { return followersCount; }
        public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }
    }
}