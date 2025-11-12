package com.yunke.backend.user.service.impl;

import com.yunke.backend.user.dto.response.UserInfo;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.domain.entity.UserFollow;
import com.yunke.backend.user.repository.UserFollowRepository;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.notification.service.NotificationService;
import com.yunke.backend.user.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户关注服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }

        // 检查是否已经关注
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("已经关注了该用户");
        }

        // 创建关注记录
        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(followerId);
        userFollow.setFollowingId(followingId);
        userFollow.setCreatedAt(LocalDateTime.now());

        userFollowRepository.save(userFollow);
        log.info("用户 {} 关注了用户 {}", followerId, followingId);

        User follower = userRepository.findById(followerId).orElse(null);
        String followerName = follower != null ? follower.getName() : "用户";

        notificationService.createFollowNotification(
            followingId,
            followerId,
            followerName
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(String followerId, String followingId) {
        // 检查关注关系是否存在
        if (!userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("没有关注该用户");
        }

        // 删除关注记录
        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("用户 {} 取消关注了用户 {}", followerId, followingId);
    }
    
    @Override
    public boolean isFollowing(String followerId, String followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }
    
    @Override
    public Page<UserInfo> getFollowingList(String userId, Pageable pageable) {
        try {
            List<String> followingIds = userFollowRepository.findFollowingIdsByFollowerId(userId);

            List<UserInfo> userInfos = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), followingIds.size());

            if (start < followingIds.size()) {
                List<String> pageUserIds = followingIds.subList(start, end);
                for (String followingId : pageUserIds) {
                    User user = userRepository.findById(followingId).orElse(null);
                    if (user != null) {
                        UserInfo userInfo = new UserInfo();
                        userInfo.setId(user.getId());
                        userInfo.setNickname(user.getName());
                        userInfo.setEmail(user.getEmail());
                        userInfo.setAvatar(user.getAvatarUrl() != null ? user.getAvatarUrl() : "/avatars/default.png");
                        userInfos.add(userInfo);
                    }
                }
            }

            return new PageImpl<>(userInfos, pageable, followingIds.size());
        } catch (Exception e) {
            log.error("获取关注列表失败", e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }
    
    @Override
    public Page<UserInfo> getFollowersList(String userId, Pageable pageable) {
        try {
            List<String> followerIds = userFollowRepository.findFollowerIdsByFollowingId(userId);

            List<UserInfo> userInfos = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), followerIds.size());

            if (start < followerIds.size()) {
                List<String> pageUserIds = followerIds.subList(start, end);
                for (String followerId : pageUserIds) {
                    User user = userRepository.findById(followerId).orElse(null);
                    if (user != null) {
                        UserInfo userInfo = new UserInfo();
                        userInfo.setId(user.getId());
                        userInfo.setNickname(user.getName());
                        userInfo.setEmail(user.getEmail());
                        userInfo.setAvatar(user.getAvatarUrl() != null ? user.getAvatarUrl() : "/avatars/default.png");
                        userInfos.add(userInfo);
                    }
                }
            }

            return new PageImpl<>(userInfos, pageable, followerIds.size());
        } catch (Exception e) {
            log.error("获取粉丝列表失败", e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }
    
    @Override
    public FollowStats getFollowStats(String userId) {
        Long followingCount = userFollowRepository.countByFollowerId(userId);
        Long followersCount = userFollowRepository.countByFollowingId(userId);

        return new FollowStats(
            followingCount != null ? followingCount.intValue() : 0,
            followersCount != null ? followersCount.intValue() : 0
        );
    }
    
    @Override
    public List<String> getMutualFollows(String userId) {
        return userFollowRepository.findMutualFollows(userId);
    }
    
    @Override
    public Map<String, Boolean> batchCheckFollowStatus(String followerId, List<String> followingIds) {
        Map<String, Boolean> result = new HashMap<>();

        for (String followingId : followingIds) {
            boolean isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
            result.put(followingId, isFollowing);
        }

        return result;
    }
}