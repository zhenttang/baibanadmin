package com.yunke.backend.notification.controller;

import com.yunke.backend.notification.domain.entity.Notification;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "通知管理", description = "用户通知相关API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前认证用户ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return null;
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }

    @Operation(summary = "获取通知列表", description = "分页获取用户的通知列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取通知列表"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @Parameter(description = "页码（从0开始）", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") int size) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notifications = notificationService.getNotifications(userId, pageable);
            
            Map<String, Object> result = new HashMap<>();
            result.put("notifications", notifications.getContent());
            result.put("totalElements", notifications.getTotalElements());
            result.put("totalPages", notifications.getTotalPages());
            result.put("currentPage", notifications.getNumber());
            result.put("pageSize", notifications.getSize());
            result.put("hasNext", notifications.hasNext());
            result.put("hasPrevious", notifications.hasPrevious());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting notifications for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "获取通知列表失败"));
        }
    }

    @Operation(summary = "获取未读通知数量", description = "获取用户未读通知的数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取未读通知数量"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting unread count for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "获取未读通知数量失败"));
        }
    }

    @Operation(summary = "标记通知为已读", description = "标记指定通知为已读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功标记为已读"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "通知不存在")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @Parameter(description = "通知ID") @PathVariable String notificationId) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            boolean success = notificationService.markAsRead(notificationId, userId);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "通知已标记为已读"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "通知不存在"));
            }
        } catch (Exception e) {
            log.error("Error marking notification {} as read for user {}: {}", notificationId, userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "标记通知为已读失败"));
        }
    }

    @Operation(summary = "标记所有通知为已读", description = "标记用户的所有通知为已读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功标记所有通知为已读"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            int count = notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "所有通知已标记为已读", "count", count));
        } catch (Exception e) {
            log.error("Error marking all notifications as read for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "标记所有通知为已读失败"));
        }
    }

    @Operation(summary = "删除通知", description = "删除指定的通知")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功删除通知"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "通知不存在")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @Parameter(description = "通知ID") @PathVariable String notificationId) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            boolean success = notificationService.deleteNotification(notificationId, userId);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "通知已删除"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "通知不存在"));
            }
        } catch (Exception e) {
            log.error("Error deleting notification {} for user {}: {}", notificationId, userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "删除通知失败"));
        }
    }

    @Operation(summary = "获取通知详情", description = "获取指定通知的详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取通知详情"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "通知不存在")
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> getNotificationById(
            @Parameter(description = "通知ID") @PathVariable String notificationId) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            Notification notification = notificationService.getNotificationById(notificationId, userId);
            if (notification != null) {
                return ResponseEntity.ok(Map.of("notification", notification));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "通知不存在"));
            }
        } catch (Exception e) {
            log.error("Error getting notification {} for user {}: {}", notificationId, userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "获取通知详情失败"));
        }
    }

    @Operation(summary = "创建提及通知", description = "创建用户提及通知")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功创建提及通知"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping("/mention")
    public ResponseEntity<Map<String, Object>> createMentionNotification(
            @RequestBody Map<String, Object> requestBody) {
        
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        try {
            String userId = (String) requestBody.get("userId");
            String workspaceId = (String) requestBody.get("workspaceId");
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = (Map<String, Object>) requestBody.get("doc");
            
            String docId = (String) doc.get("id");
            String docTitle = (String) doc.get("title");
            
            Notification notification = notificationService.createMentionNotification(
                userId, workspaceId, docId, docTitle, currentUserId);
            
            return ResponseEntity.ok(Map.of("success", true, "notification", notification));
        } catch (Exception e) {
            log.error("Error creating mention notification: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "创建提及通知失败"));
        }
    }
}