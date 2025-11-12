package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.forum.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "Forum Like")
public class LikeController {

    private final LikeService likeService;

    // 帖子点赞
    @Operation(summary = "点赞帖子")
    @PostMapping("/posts/{id}/like")
    public ApiResponse<Void> likePost(@PathVariable("id") String postId) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            likeService.likePost(postId, userId);
            return ApiResponse.success("点赞成功");
        } catch (Exception e) {
            return ApiResponse.error("点赞失败：" + e.getMessage());
        }
    }

    // 取消点赞帖子
    @Operation(summary = "取消点赞帖子")
    @DeleteMapping("/posts/{id}/like")
    public ApiResponse<Void> unlikePost(@PathVariable("id") String postId) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            likeService.unlikePost(postId, userId);
            return ApiResponse.success("取消点赞成功");
        } catch (Exception e) {
            return ApiResponse.error("取消点赞失败：" + e.getMessage());
        }
    }

    // 回复点赞
    @Operation(summary = "点赞回复")
    @PostMapping("/replies/{id}/like")
    public ApiResponse<Void> likeReply(@PathVariable("id") Long replyId) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            likeService.likeReply(replyId, userId);
            return ApiResponse.success("点赞成功");
        } catch (Exception e) {
            return ApiResponse.error("点赞失败：" + e.getMessage());
        }
    }

    // 取消点赞回复
    @Operation(summary = "取消点赞回复")
    @DeleteMapping("/replies/{id}/like")
    public ApiResponse<Void> unlikeReply(@PathVariable("id") Long replyId) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            likeService.unlikeReply(replyId, userId);
            return ApiResponse.success("取消点赞成功");
        } catch (Exception e) {
            return ApiResponse.error("取消点赞失败：" + e.getMessage());
        }
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return null;
        }
        return ((AffineUserDetails) authentication.getPrincipal()).getUserId();
    }
}

