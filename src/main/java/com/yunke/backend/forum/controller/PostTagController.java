package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.community.dto.community.TagInfo;
import com.yunke.backend.forum.dto.PostDTO;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.forum.service.PostService;
import com.yunke.backend.forum.service.PostTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "ForumPostTags")
public class PostTagController {

    private final PostTagService postTagService;
    private final PostService postService;

    @Operation(summary = "获取帖子标签")
    @GetMapping("/posts/{id}/tags")
    public ApiResponse<List<TagInfo>> getPostTags(@PathVariable("id") String postId) {
        return ApiResponse.success(postTagService.getPostTags(postId));
    }

    @Operation(summary = "按标签获取帖子（分页）")
    @GetMapping("/tags/{tagId}/posts")
    public ApiResponse<Page<PostDTO>> getPostsByTag(
            @PathVariable("tagId") Integer tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<ForumPost> data = postTagService.findPostsByTag(tagId, pageable.getPageNumber(), pageable.getPageSize());
        List<PostDTO> dtos = data.getContent().stream().map(postService::toDTO).collect(Collectors.toList());
        Page<PostDTO> dtoPage = new PageImpl<>(dtos, pageable, data.getTotalElements());
        return ApiResponse.success(dtoPage);
    }

    @Operation(summary = "热门标签TOP20")
    @GetMapping("/tags/popular")
    public ApiResponse<List<TagInfo>> getPopularTags() {
        return ApiResponse.success(postTagService.getPopularTagsTopN(20));
    }
}

