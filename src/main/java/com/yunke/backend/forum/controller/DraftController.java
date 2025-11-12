package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.CreateDraftRequest;
import com.yunke.backend.forum.dto.DraftDTO;
import com.yunke.backend.forum.dto.PostDTO;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.forum.service.DraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/drafts")
@RequiredArgsConstructor
@Tag(name = "Forum Draft")
public class DraftController {

    private final DraftService draftService;

    @Operation(summary = "保存草稿（新建/更新）")
    @PostMapping
    public ApiResponse<DraftDTO> saveDraft(@Valid @RequestBody CreateDraftRequest request) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            return ApiResponse.success(draftService.saveDraft(request, userId));
        } catch (Exception e) {
            return ApiResponse.error("保存草稿失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取草稿")
    @GetMapping("/{id}")
    public ApiResponse<DraftDTO> getDraft(@PathVariable Long id) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            return ApiResponse.success(draftService.getDraft(id, userId));
        } catch (Exception e) {
            return ApiResponse.error("获取草稿失败：" + e.getMessage());
        }
    }

    @Operation(summary = "我的草稿列表")
    @GetMapping("/my")
    public ApiResponse<Page<DraftDTO>> listMyDrafts(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            return ApiResponse.success(draftService.listMyDrafts(userId, page, size));
        } catch (Exception e) {
            return ApiResponse.error("获取草稿列表失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteDraft(@PathVariable Long id) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            return ApiResponse.success(draftService.deleteDraft(id, userId));
        } catch (Exception e) {
            return ApiResponse.error("删除草稿失败：" + e.getMessage());
        }
    }

    @Operation(summary = "发布草稿为帖子")
    @PostMapping("/{id}/publish")
    public ApiResponse<PostDTO> publishDraft(@PathVariable Long id) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            return ApiResponse.success(draftService.publishDraft(id, userId));
        } catch (Exception e) {
            return ApiResponse.error("发布草稿失败：" + e.getMessage());
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

