package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.EditHistoryDTO;
import com.yunke.backend.forum.service.EditHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
@Tag(name = "Post Edit History")
public class EditHistoryController {

    private final EditHistoryService editHistoryService;

    @Operation(summary = "获取帖子编辑历史（分页）")
    @GetMapping("/{id}/history")
    public ApiResponse<Page<EditHistoryDTO>> getPostHistory(@PathVariable("id") String postId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(editHistoryService.getPostHistory(postId, page, size));
    }

    @Operation(summary = "查看历史版本详情")
    @GetMapping("/history/{historyId}")
    public ApiResponse<EditHistoryDTO> getHistoryDetail(@PathVariable Long historyId) {
        return ApiResponse.success(editHistoryService.getHistoryDetail(historyId));
    }
}

