package com.yunke.backend.forum.controller;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.*;
import com.yunke.backend.forum.service.ModeratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/moderators")
@RequiredArgsConstructor
@Tag(name = "Forum Moderator")
public class ModeratorController {
    
    private final ModeratorService moderatorService;
    
    @Operation(summary = "任命版主")
    @PostMapping
    public ApiResponse<ModeratorDTO> appointModerator(@Valid @RequestBody CreateModeratorRequest request) {
        ModeratorDTO dto = moderatorService.addModerator(request);
        return ApiResponse.success(dto);
    }
    
    @Operation(summary = "获取板块版主列表")
    @GetMapping("/forum/{forumId}")
    public ApiResponse<List<ModeratorDTO>> getForumModerators(@PathVariable Long forumId) {
        return ApiResponse.success(moderatorService.listModerators(forumId));
    }
    
    @Operation(summary = "更新版主权限")
    @PutMapping("/{id}/permissions")
    public ApiResponse<ModeratorDTO> updatePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionsRequest request) {
        return ApiResponse.success(moderatorService.updatePermissions(id, request));
    }
    
    @Operation(summary = "撤销版主")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> removeModerator(@PathVariable Long id) {
        return ApiResponse.success(moderatorService.removeModerator(id));
    }
    
    @Operation(summary = "检查权限")
    @GetMapping("/check-permission")
    public ApiResponse<Boolean> checkPermission(
            @RequestParam Long forumId,
            @RequestParam String userId,
            @RequestParam String permission) {
        return ApiResponse.success(moderatorService.hasPermission(forumId, userId, permission));
    }
}
