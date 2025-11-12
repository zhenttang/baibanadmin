package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.CreateForumRequest;
import com.yunke.backend.forum.dto.ForumDTO;
import com.yunke.backend.forum.dto.ForumStatsDTO;
import com.yunke.backend.forum.dto.UpdateForumRequest;
import com.yunke.backend.forum.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/forums")
@RequiredArgsConstructor
@Tag(name = "Forum")
public class ForumController {

    private final ForumService forumService;

    @Operation(summary = "创建板块")
    @PostMapping
    public ApiResponse<ForumDTO> createForum(@Valid @RequestBody CreateForumRequest request) {
        ForumDTO dto = forumService.createForum(request);
        return ApiResponse.success(dto);
    }

    @Operation(summary = "获取板块列表")
    @GetMapping
    public ApiResponse<List<ForumDTO>> listForums() {
        return ApiResponse.success(forumService.listForums());
    }

    @Operation(summary = "获取板块详情")
    @GetMapping("/{id}")
    public ApiResponse<ForumDTO> getForum(@PathVariable Long id) {
        return ApiResponse.success(forumService.getForum(id));
    }

    @Operation(summary = "更新板块")
    @PutMapping("/{id}")
    public ApiResponse<ForumDTO> updateForum(@PathVariable Long id, @Valid @RequestBody UpdateForumRequest request) {
        return ApiResponse.success(forumService.updateForum(id, request));
    }

    @Operation(summary = "删除板块")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteForum(@PathVariable Long id) {
        return ApiResponse.success(forumService.deleteForum(id));
    }

    @Operation(summary = "获取板块统计")
    @GetMapping("/{id}/stats")
    public ApiResponse<ForumStatsDTO> getForumStats(@PathVariable Long id) {
        return ApiResponse.success(forumService.getForumStats(id));
    }
}
