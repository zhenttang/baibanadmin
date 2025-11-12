package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.CreateReplyRequest;
import com.yunke.backend.forum.dto.ReplyDTO;
import com.yunke.backend.forum.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/replies")
@RequiredArgsConstructor
@Tag(name = "Forum Reply")
public class ReplyController {

    private final ReplyService replyService;

    @Operation(summary = "创建回复")
    @PostMapping
    public ApiResponse<ReplyDTO> createReply(@Valid @RequestBody CreateReplyRequest request) {
        // TODO 集成认证后，从SecurityContext获取真实用户
        ReplyDTO dto = replyService.createReply(request, "1", "User1");
        return ApiResponse.success(dto);
    }

    @Operation(summary = "获取帖子回复列表")
    @GetMapping("/post/{postId}")
    public ApiResponse<Page<ReplyDTO>> getRepliesByPostId(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.success(replyService.getPostRepliesPage(postId, page, size));
    }

    @Operation(summary = "删除回复")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteReply(@PathVariable Long id) {
        return ApiResponse.success(replyService.deleteReply(id));
    }

    @Operation(summary = "标记为最佳答案")
    @PostMapping("/{id}/best-answer")
    public ApiResponse<Boolean> markBestAnswer(@PathVariable("id") Long replyId,
                                               @RequestParam("postId") String postId) {
        return ApiResponse.success(replyService.markBestAnswer(postId, replyId));
    }
}
