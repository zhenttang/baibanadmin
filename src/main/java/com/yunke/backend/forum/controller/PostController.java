package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.*;
import com.yunke.backend.forum.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
@Tag(name = "ForumPost")
public class PostController {

    private final PostService postService;

    @Operation(summary = "发帖")
    @PostMapping
    public ApiResponse<PostDTO> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(request));
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id}")
    public ApiResponse<PostDTO> getPost(@PathVariable String id) {
        return ApiResponse.success(postService.getPost(id));
    }

    @Operation(summary = "编辑帖子")
    @PutMapping("/{id}")
    public ApiResponse<PostDTO> updatePost(@PathVariable String id, @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(postService.updatePost(id, request));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deletePost(@PathVariable String id) {
        return ApiResponse.success(postService.deletePost(id));
    }

    @Operation(summary = "置顶")
    @PutMapping("/{id}/sticky")
    public ApiResponse<Boolean> setSticky(@PathVariable String id, @RequestParam("value") boolean sticky) {
        return ApiResponse.success(postService.setSticky(id, sticky));
    }

    @Operation(summary = "加精")
    @PutMapping("/{id}/essence")
    public ApiResponse<Boolean> setEssence(@PathVariable String id, @RequestParam("value") boolean essence) {
        return ApiResponse.success(postService.setEssence(id, essence));
    }

    @Operation(summary = "锁定")
    @PutMapping("/{id}/lock")
    public ApiResponse<Boolean> lockPost(@PathVariable String id, @RequestParam("value") boolean locked) {
        return ApiResponse.success(postService.lockPost(id, locked));
    }

    @Operation(summary = "板块帖子列表")
    @GetMapping("/forum/{forumId}")
    public ApiResponse<Page<PostDTO>> getForumPosts(
            @PathVariable Long forumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                postService.getForumPosts(forumId, page, size)
        );
    }
}
