package com.yunke.backend.modules.document.api;

import com.yunke.backend.document.domain.entity.DocumentReport;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.document.service.DocumentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/community/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文档举报", description = "文档举报相关API")
public class DocumentReportController {

    private final DocumentReportService reportService;

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return null;
        }
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }

    private String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return "用户";
        }
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @Operation(summary = "举报文档", description = "用户举报社区文档")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReport(@RequestBody ReportRequest request) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            if (reportService.hasUserReported(request.getDocumentId(), userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "您已经举报过该文档"));
            }

            DocumentReport report = reportService.createReport(
                request.getDocumentId(),
                userId,
                getCurrentUserName(),
                request.getReason(),
                request.getDescription()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "举报提交成功，我们会尽快处理",
                "report", report
            ));
        } catch (Exception e) {
            log.error("Error creating report", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "获取我的举报记录", description = "获取当前用户的举报记录")
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentReport> reports = reportService.getReportsByReporter(userId, pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("reports", reports.getContent());
            result.put("totalElements", reports.getTotalElements());
            result.put("totalPages", reports.getTotalPages());
            result.put("currentPage", reports.getNumber());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting my reports", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取举报记录失败"));
        }
    }

    @Operation(summary = "获取待审核举报列表", description = "管理员获取待审核的举报列表")
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentReport> reports = reportService.getPendingReports(pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("reports", reports.getContent());
            result.put("totalElements", reports.getTotalElements());
            result.put("totalPages", reports.getTotalPages());
            result.put("currentPage", reports.getNumber());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting pending reports", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取举报列表失败"));
        }
    }

    @Operation(summary = "审核举报", description = "管理员审核举报")
    @PostMapping("/{id}/review")
    public ResponseEntity<Map<String, Object>> reviewReport(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {

        String reviewerId = getCurrentUserId();
        if (reviewerId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            DocumentReport report = reportService.reviewReport(
                id,
                reviewerId,
                getCurrentUserName(),
                request.getResult(),
                request.getNote()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审核完成",
                "report", report
            ));
        } catch (Exception e) {
            log.error("Error reviewing report", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "获取待处理举报数量", description = "获取待处理的举报数量")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getPendingReportCount() {
        try {
            long count = reportService.getPendingReportCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting pending report count", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取举报数量失败"));
        }
    }

    @Data
    public static class ReportRequest {
        private String documentId;
        private String reason;
        private String description;
    }

    @Data
    public static class ReviewRequest {
        private String result;
        private String note;
    }
}
