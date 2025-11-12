package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.*;
import com.yunke.backend.forum.service.UserPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/points")
@RequiredArgsConstructor
@Tag(name = "User Points")
public class UserPointController {
    
    private final UserPointService userPointService;
    
    @Operation(summary = "获取用户积分")
    @GetMapping("/user/{userId}")
    public ApiResponse<UserPointDTO> getUserPoints(@PathVariable Long userId) {
        return ApiResponse.success(userPointService.getUserPoints(userId));
    }
    
    @Operation(summary = "每日签到")
    @PostMapping("/sign-in/{userId}")
    public ApiResponse<UserPointDTO> signIn(@PathVariable Long userId) {
        return ApiResponse.success(userPointService.signIn(userId));
    }
    
    @Operation(summary = "增加积分")
    @PostMapping("/add")
    public ApiResponse<UserPointDTO> addPoints(@Valid @RequestBody PointOperationRequest request) {
        return ApiResponse.success(userPointService.addPoints(request));
    }
    
    @Operation(summary = "扣减积分")
    @PostMapping("/deduct")
    public ApiResponse<UserPointDTO> deductPoints(@Valid @RequestBody PointOperationRequest request) {
        return ApiResponse.success(userPointService.deductPoints(request));
    }
}

