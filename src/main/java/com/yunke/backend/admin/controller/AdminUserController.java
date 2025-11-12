package com.yunke.backend.admin.controller;

import com.yunke.backend.admin.dto.AdminUserDto;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.user.dto.BatchOperationRequest;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.dto.BatchUserOperationDto;
import com.yunke.backend.user.dto.CreateUserRequest;
import com.yunke.backend.user.dto.UserOperationLogDto;
import com.yunke.backend.user.dto.UpdateUserRequest;
import com.yunke.backend.user.dto.UserQueryDto;
import com.yunke.backend.workspace.enums.WorkspaceMemberStatus;
import com.yunke.backend.workspace.repository.WorkspaceMemberRepository;
import com.yunke.backend.system.repository.QuotaUsageRepository;
import com.yunke.backend.community.repository.CommunityDocumentRepository;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.system.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 管理员用户管理控制器 - 增强版
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final QuotaUsageRepository quotaUsageRepository;
    private final CommunityDocumentRepository communityDocumentRepository;

    /**
     * 获取用户列表（增强版：支持复杂查询和新DTO）
     */
    @PostMapping("/query")
    public ResponseEntity<PaginatedResponse<AdminUserDto>> getUsersWithQuery(@Valid @RequestBody UserQueryDto query) {
        log.info("查询用户列表 - 查询条件: {}", query);
        
        try {
            // 构建排序
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            if (query.getSortBy() != null) {
                Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDirection()) ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, query.getSortBy());
            }
            
            // 构建分页
            int page = query.getPage() != null ? query.getPage() : 0;
            int size = query.getSize() != null ? query.getSize() : 20;
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // 执行查询（这里需要在UserService中实现）
            Page<User> users = userService.findUsersWithAdvancedFilters(pageable, query);
            
            // 转换为AdminUserDto
            List<AdminUserDto> userDtos = users.getContent().stream()
                .map(this::convertToAdminUserDto)
                .toList();
            
            PaginatedResponse<AdminUserDto> response = PaginatedResponse.<AdminUserDto>builder()
                .content(userDtos)
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .currentPage(users.getNumber())
                .size(users.getSize())
                .hasNext(users.hasNext())
                .hasPrevious(users.hasPrevious())
                .build();
            
            log.info("用户查询完成 - 总数: {}, 当前页: {}", users.getTotalElements(), page);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询用户列表时发生错误", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取用户列表（兼容旧版本）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean registered) {
        
        log.info("获取用户列表 - page: {}, size: {}, search: {}, enabled: {}, registered: {}", 
                page, size, search, enabled, registered);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = userService.findUsersWithFilters(pageable, search, enabled, registered);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.getContent().stream().map(this::convertToAdminUserDto).toList());
        response.put("totalElements", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        response.put("currentPage", users.getNumber());
        response.put("size", users.getSize());
        response.put("hasNext", users.hasNext());
        response.put("hasPrevious", users.hasPrevious());
        
        log.info("返回用户列表 - 总数: {}, 当前页: {}", users.getTotalElements(), page);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取用户详情（增强版）
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable String id) {
        log.info("获取用户详情 - userId: {}", id);
        
        Optional<User> userOpt = userService.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            AdminUserDto userDto = convertToAdminUserDto(user);
            
            log.info("找到用户 - email: {}, name: {}", user.getEmail(), user.getName());
            return ResponseEntity.ok(userDto);
        } else {
            log.warn("用户不存在 - userId: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量操作用户（增强版）
     */
    @PostMapping("/batch-operation")
    public ResponseEntity<Map<String, Object>> batchOperation(@Valid @RequestBody BatchUserOperationDto request) {
        log.info("批量操作用户 - 操作: {}, 用户数量: {}, 原因: {}", 
                request.getOperation(), request.getUserIds().size(), request.getReason());
        
        try {
            Map<String, Object> result = userService.performBatchOperation(request);
            
            log.info("批量操作完成 - 成功: {}, 失败: {}", 
                    result.get("successCount"), result.get("failedCount"));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量操作时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "批量操作失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取用户操作日志
     */
    @GetMapping("/{id}/operation-logs")
    public ResponseEntity<PaginatedResponse<UserOperationLogDto>> getUserOperationLogs(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("获取用户操作日志 - userId: {}, page: {}, size: {}", id, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operatedAt"));
            // 这里需要在UserService中实现
            Page<UserOperationLogDto> logs = userService.getUserOperationLogs(id, pageable);
            
            PaginatedResponse<UserOperationLogDto> response = PaginatedResponse.<UserOperationLogDto>builder()
                .content(logs.getContent())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .currentPage(logs.getNumber())
                .size(logs.getSize())
                .hasNext(logs.hasNext())
                .hasPrevious(logs.hasPrevious())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户操作日志时发生错误", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 更新用户权限
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<Map<String, Object>> updateUserPermissions(
            @PathVariable String id,
            @RequestBody Map<String, Object> permissions) {
        
        log.info("更新用户权限 - userId: {}, permissions: {}", id, permissions);
        
        try {
            userService.updateUserPermissions(id, permissions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户权限更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新用户权限时发生错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "更新权限失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 保留原有的方法...
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("创建新用户 - email: {}, name: {}", request.getEmail(), request.getName());
        
        try {
            User user = new User();
            user.setEmail(request.getEmail());
            user.setName(request.getName());
            user.setPassword(request.getPassword());
            user.setEnabled(request.isEnabled() != null ? request.isEnabled() : true);
            user.setRegistered(true);
            
            User createdUser = userService.createUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户创建成功");
            response.put("user", convertToAdminUserDto(createdUser));
            
            log.info("用户创建成功 - userId: {}, email: {}", createdUser.getId(), createdUser.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("创建用户失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("创建用户时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "创建用户失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        log.info("更新用户信息 - userId: {}, email: {}", id, request.getEmail());
        
        try {
            User user = new User();
            user.setId(id);
            user.setEmail(request.getEmail());
            user.setName(request.getName());
            user.setAvatarUrl(request.getAvatarUrl());
            user.setEnabled(request.isEnabled());
            
            User updatedUser = userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户信息更新成功");
            response.put("user", convertToAdminUserDto(updatedUser));
            
            log.info("用户信息更新成功 - userId: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("更新用户失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("更新用户时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "更新用户失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        log.info("删除用户 - userId: {}", id);
        
        try {
            userService.deleteUser(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户删除成功");
            
            log.info("用户删除成功 - userId: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("删除用户失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("删除用户时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "删除用户失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable String id,
            @RequestBody(required = false) ResetPasswordRequest request) {
        
        log.info("重置用户密码 - userId: {}", id);
        
        try {
            String newPassword = request != null && request.getPassword() != null ? 
                request.getPassword() : generateRandomPassword();
            
            userService.updatePassword(id, newPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "密码重置成功");
            response.put("newPassword", newPassword);
            
            log.info("用户密码重置成功 - userId: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("重置密码失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("重置密码时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "重置密码失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable String id) {
        log.info("切换用户状态 - userId: {}", id);
        
        try {
            User user = userService.toggleUserStatus(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", user.isEnabled() ? "用户已启用" : "用户已禁用");
            response.put("user", convertToAdminUserDto(user));
            
            log.info("用户状态切换成功 - userId: {}, 新状态: {}", id, user.isEnabled());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("切换用户状态失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("切换用户状态时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "操作失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchOperation(@Valid @RequestBody BatchOperationRequest request) {
        log.info("批量操作用户 - 操作: {}, 用户数量: {}", request.getOperation(), request.getUserIds().size());
        
        try {
            int successCount = 0;
            int failedCount = 0;
            List<String> failedUserIds = new ArrayList<>();
            
            for (String userId : request.getUserIds()) {
                try {
                    switch (request.getOperation().toLowerCase()) {
                        case "enable":
                            userService.setUserEnabled(userId, true);
                            successCount++;
                            break;
                        case "disable":
                            userService.setUserEnabled(userId, false);
                            successCount++;
                            break;
                        case "delete":
                            userService.deleteUser(userId);
                            successCount++;
                            break;
                        default:
                            failedCount++;
                            failedUserIds.add(userId);
                            log.warn("不支持的批量操作: {}", request.getOperation());
                    }
                } catch (Exception e) {
                    failedCount++;
                    failedUserIds.add(userId);
                    log.error("批量操作失败 - userId: {}, error: {}", userId, e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("批量操作完成 - 成功: %d, 失败: %d", successCount, failedCount));
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("failedUserIds", failedUserIds);
            
            log.info("批量操作完成 - 成功: {}, 失败: {}", successCount, failedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量操作时发生未知错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "批量操作失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/batch-import")
    public ResponseEntity<Map<String, Object>> batchImport(@RequestParam("file") MultipartFile file) {
        log.info("批量导入用户 - 文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        try {
            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "文件不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (!"text/csv".equals(file.getContentType()) && 
                !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "文件格式必须是CSV");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Integer> importResult = userService.importUsersFromCsv(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户导入完成");
            response.put("totalProcessed", importResult.get("totalProcessed"));
            response.put("successCount", importResult.get("successCount"));
            response.put("failedCount", importResult.get("failedCount"));
            response.put("skippedCount", importResult.get("skippedCount"));
            
            log.info("用户导入完成 - 总数: {}, 成功: {}, 失败: {}, 跳过: {}", 
                    importResult.get("totalProcessed"), importResult.get("successCount"), 
                    importResult.get("failedCount"), importResult.get("skippedCount"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量导入用户时发生错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "导入失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        log.info("获取用户统计信息");
        
        try {
            Map<String, Object> stats = userService.getUserStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户统计信息时发生错误", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "获取统计信息失败");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 转换User实体为AdminUserDto
     */
    private AdminUserDto convertToAdminUserDto(User user) {
        int workspaceCount = workspaceMemberRepository.findByUserIdAndStatus(
            user.getId(), WorkspaceMemberStatus.ACCEPTED).size();
        Long documentCount = communityDocumentRepository.countByAuthorId(user.getId());
        Long storageUsed = quotaUsageRepository.getUserStorageUsage(user.getId());

        return AdminUserDto.builder()
            .id(user.getId())
            .name(user.getName())
            .username(user.getEmail())
            .email(user.getEmail())
            .avatarUrl(user.getAvatarUrl())
            .locale("zh-CN")
            .enabled(user.isEnabled())
            .registered(user.isRegistered())
            .emailVerified(user.getEmailVerifiedAt() != null)
            .isAdmin(userService.isAdmin(user))
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt() != null ? LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneId.systemDefault()) : null)
            .registeredAt(user.getRegisteredAt() != null ? LocalDateTime.ofInstant(user.getRegisteredAt(), ZoneId.systemDefault()) : null)
            .emailVerifiedAt(user.getEmailVerifiedAt())
            .stripeCustomerId(user.getStripeCustomerId())
            .status(user.isEnabled() ? "active" : "disabled")
            .workspaceCount(workspaceCount)
            .documentCount(documentCount != null ? documentCount.intValue() : 0)
            .storageUsed(storageUsed != null ? storageUsed : 0L)
            .features(new ArrayList<>())
            .build();
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    public static class ResetPasswordRequest {
        private String password;
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
