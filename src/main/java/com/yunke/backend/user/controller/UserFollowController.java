package com.yunke.backend.user.controller;

import com.yunke.backend.user.dto.response.UserInfo;
import com.yunke.backend.user.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户关注控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/community/follows")
@RequiredArgsConstructor
public class UserFollowController {
    
    private final UserFollowService userFollowService;
    
    /**
     * 关注用户
     */
    @PostMapping("/{targetUserId}")
    public ResponseEntity<Map<String, Object>> followUser(
            @PathVariable String targetUserId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("关注用户请求: {} 关注 {}", userId, targetUserId);
        
        try {
            userFollowService.followUser(userId, targetUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "关注成功");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("关注用户失败: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 取消关注用户
     */
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Map<String, Object>> unfollowUser(
            @PathVariable String targetUserId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("取消关注用户请求: {} 取消关注 {}", userId, targetUserId);
        
        try {
            userFollowService.unfollowUser(userId, targetUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "取消关注成功");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("取消关注用户失败: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查关注状态
     */
    @GetMapping("/{targetUserId}/status")
    public ResponseEntity<Map<String, Object>> checkFollowStatus(
            @PathVariable String targetUserId,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean isFollowing = userFollowService.isFollowing(userId, targetUserId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isFollowing", isFollowing);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    public ResponseEntity<Map<String, Object>> getFollowingList(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("获取关注列表: userId={}, page={}, size={}", userId, page, size);
        
        Page<UserInfo> followingPage = userFollowService.getFollowingList(userId, PageRequest.of(page, size));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "items", followingPage.getContent(),
            "page", followingPage.getNumber(),
            "size", followingPage.getSize(),
            "total", followingPage.getTotalElements(),
            "totalPages", followingPage.getTotalPages()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取粉丝列表
     */
    @GetMapping("/followers")
    public ResponseEntity<Map<String, Object>> getFollowersList(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("获取粉丝列表: userId={}, page={}, size={}", userId, page, size);
        
        Page<UserInfo> followersPage = userFollowService.getFollowersList(userId, PageRequest.of(page, size));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "items", followersPage.getContent(),
            "page", followersPage.getNumber(),
            "size", followersPage.getSize(),
            "total", followersPage.getTotalElements(),
            "totalPages", followersPage.getTotalPages()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取关注统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFollowStats(
            @RequestHeader("X-User-Id") String userId) {
        
        UserFollowService.FollowStats stats = userFollowService.getFollowStats(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "followingCount", stats.getFollowingCount(),
            "followersCount", stats.getFollowersCount()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取互相关注的用户
     */
    @GetMapping("/mutual")
    public ResponseEntity<Map<String, Object>> getMutualFollows(
            @RequestHeader("X-User-Id") String userId) {
        
        List<String> mutualFollows = userFollowService.getMutualFollows(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mutualFollows);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 批量检查关注状态
     */
    @PostMapping("/batch-check")
    public ResponseEntity<Map<String, Object>> batchCheckFollowStatus(
            @RequestBody List<String> userIds,
            @RequestHeader("X-User-Id") String followerId) {
        
        Map<String, Boolean> followStatus = userFollowService.batchCheckFollowStatus(followerId, userIds);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", followStatus);
        
        return ResponseEntity.ok(response);
    }
}