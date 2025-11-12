package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.forum.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "Forum Collection")
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "收藏帖子")
    @PostMapping("/posts/{id}/collect")
    public ApiResponse<Boolean> collectPost(@PathVariable("id") String postId) {
        return ApiResponse.success(collectionService.collectPost(postId));
    }

    @Operation(summary = "取消收藏帖子")
    @DeleteMapping("/posts/{id}/collect")
    public ApiResponse<Boolean> uncollectPost(@PathVariable("id") String postId) {
        return ApiResponse.success(collectionService.uncollectPost(postId));
    }

    @Operation(summary = "我的收藏帖子列表（分页）")
    @GetMapping("/collections/my")
    public ApiResponse<Page<ForumPost>> myCollections(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(collectionService.listMyCollectedPosts(page, size));
    }
}

