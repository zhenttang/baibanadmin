package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.AttachmentDTO;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.forum.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "Forum Attachment")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "上传帖子附件")
    @PostMapping("/posts/{id}/attachments")
    public ApiResponse<AttachmentDTO> uploadAttachment(
            @PathVariable("id") String postId,
            @RequestParam("file") MultipartFile file
    ) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            AttachmentDTO dto = attachmentService.uploadAttachment(postId, file, userId);
            return ApiResponse.success(dto);
        } catch (Exception e) {
            return ApiResponse.error("上传失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取帖子附件列表")
    @GetMapping("/posts/{id}/attachments")
    public ApiResponse<List<AttachmentDTO>> getAttachments(@PathVariable("id") String postId) {
        try {
            return ApiResponse.success(attachmentService.getPostAttachments(postId));
        } catch (Exception e) {
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/attachments/{id}")
    public ApiResponse<Boolean> deleteAttachment(@PathVariable("id") Long id) {
        String userId = currentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录或无法获取用户信息");
        }
        try {
            boolean ok = attachmentService.deleteAttachment(id, userId);
            return ApiResponse.success(ok);
        } catch (Exception e) {
            return ApiResponse.error("删除失败：" + e.getMessage());
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

