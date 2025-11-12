package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.CreateReportRequest;
import com.yunke.backend.forum.dto.HandleReportRequest;
import com.yunke.backend.forum.dto.ReportDTO;
import com.yunke.backend.forum.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/reports")
@RequiredArgsConstructor
@Tag(name = "Forum Report")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "创建举报")
    @PostMapping
    public ApiResponse<ReportDTO> createReport(@Valid @RequestBody CreateReportRequest request) {
        // TODO: 从认证上下文获取真实userId
        Long currentUserId = 1L;
        return ApiResponse.success(reportService.createReport(request, currentUserId));
    }

    @Operation(summary = "获取待处理举报列表")
    @GetMapping("/pending")
    public ApiResponse<List<ReportDTO>> getPendingReports() {
        return ApiResponse.success(reportService.getPendingReports());
    }

    @Operation(summary = "获取我的举报历史")
    @GetMapping("/my-reports/{userId}")
    public ApiResponse<List<ReportDTO>> getMyReports(@PathVariable Long userId) {
        return ApiResponse.success(reportService.getMyReports(userId));
    }

    @Operation(summary = "处理举报")
    @PutMapping("/{id}/handle")
    public ApiResponse<ReportDTO> handleReport(
            @PathVariable Long id,
            @Valid @RequestBody HandleReportRequest request) {
        // TODO: 从认证上下文获取真实handlerId
        Long handlerId = 1L;
        return ApiResponse.success(reportService.handleReport(id, request, handlerId));
    }
}

