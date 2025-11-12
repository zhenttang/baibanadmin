package com.yunke.backend.user.controller;

import com.yunke.backend.system.dto.FeatureDto;
import com.yunke.backend.system.service.QuotaService;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.system.service.FeatureService;

import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    
    @Autowired
    private QuotaService quotaService;
    
    @Autowired
    private FeatureService featureService;

    /**
     * 获取当前认证用户ID
     */
    private Mono<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.empty();
        }

        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return Mono.just(userDetails.getUserId());
    }
    
    /**
     * 获取当前用户的配额信息
     * GET /api/users/me/quota
     */
    @GetMapping("/me/quota")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentUserQuota() {
        log.info("GET /api/users/me/quota - 获取当前用户配额信息");
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("获取用户配额: userId={}", userId);
                    return quotaService.getUserQuotaWithUsage(userId)
                            .map(quotaData -> {
                                log.info("成功获取用户配额: userId={}, data={}", userId, quotaData);
                                return ResponseEntity.ok(quotaData);
                            })
                            .onErrorResume(e -> {
                                log.error("获取用户配额失败: userId={}", userId, e);
                                return Mono.just(ResponseEntity.status(500).body(Map.of(
                                        "error", "获取用户配额失败",
                                        "message", e.getMessage()
                                )));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("未授权访问 /api/users/me/quota");
                    return Mono.just(ResponseEntity.status(401).body(Map.of("error", "未授权")));
                }));
    }
    
    /**
     * 获取当前用户的特性信息
     * GET /api/users/me/features
     */
    @GetMapping("/me/features")
    public Mono<ResponseEntity<FeatureDto.UserFeatureListDto>> getCurrentUserFeatures() {
        log.info("GET /api/users/me/features - 获取当前用户特性信息");
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("获取用户特性: userId={}", userId);
                    
                    // 使用与 FeatureController.getUserFeatures 相同的逻辑
                    return Mono.zip(
                            featureService.getUserActiveFeatures(userId),
                            featureService.getUserFeatures(userId),
                            featureService.getUserQuota(userId),
                            featureService.isAdmin(userId)
                    ).flatMap(tuple -> {
                        List<String> activeFeatures = tuple.getT1();
                        List<String> earlyAccessTypes = tuple.getT2().stream()
                                .filter(uf -> uf.getName().contains("early_access"))
                                .map(uf -> uf.getName().replace("_early_access", "").replace("early_access", "app"))
                                .toList();

                        FeatureDto.UserFeatureListDto result = FeatureDto.UserFeatureListDto.builder()
                                .userId(userId)
                                .activeFeatures(activeFeatures)
                                .allFeatures(tuple.getT2().stream()
                                        .map(this::convertToUserFeatureInfo)
                                        .toList())
                                .quotaInfo(tuple.getT3().orElse(Map.of()))
                                .isAdmin(tuple.getT4())
                                .earlyAccessTypes(earlyAccessTypes)
                                .build();

                        log.info("成功获取用户特性: userId={}, activeFeatures={}, isAdmin={}", 
                                userId, activeFeatures.size(), tuple.getT4());
                        return Mono.just(ResponseEntity.ok(result));
                    }).onErrorResume(e -> {
                        log.error("获取用户特性失败: userId={}", userId, e);
                        return Mono.just(ResponseEntity.status(500).body(
                                FeatureDto.UserFeatureListDto.builder()
                                        .userId(userId)
                                        .activeFeatures(List.of())
                                        .allFeatures(List.of())
                                        .quotaInfo(Map.of())
                                        .isAdmin(false)
                                        .earlyAccessTypes(List.of())
                                        .build()
                        ));
                    });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("未授权访问 /api/users/me/features");
                    return Mono.just(ResponseEntity.status(401).body(
                            FeatureDto.UserFeatureListDto.builder()
                                    .userId("")
                                    .activeFeatures(List.of())
                                    .allFeatures(List.of())
                                    .quotaInfo(Map.of())
                                    .isAdmin(false)
                                    .earlyAccessTypes(List.of())
                                    .build()
                    ));
                }));
    }
    
    /**
     * 转换 UserFeature 实体到 DTO
     */
    private FeatureDto.UserFeatureInfo convertToUserFeatureInfo(UserFeature userFeature) {
        return FeatureDto.UserFeatureInfo.builder()
                .id(userFeature.getId())
                .userId(userFeature.getUserId())
                .featureName(userFeature.getName())
                .displayName(userFeature.getName()) // 简化版，直接使用 name
                .type(userFeature.getType())
                .reason(userFeature.getReason())
                .activated(userFeature.isActivated())
                .createdAt(userFeature.getCreatedAt())
                .expiredAt(userFeature.getExpiredAt())
                .build();
    }

    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = userService.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.getContent());
        response.put("totalElements", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        response.put("currentPage", users.getNumber());
        response.put("size", users.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String id) {
        Optional<User> user = userService.findById(id);
        
        if (user.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("user", user.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String keyword) {
        
        List<User> users = userService.searchUsers(keyword);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("count", users.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String id) {
        UserService.UserStats stats = userService.getUserStats(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean exists = userService.isEmailExists(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("available", !exists);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String name) {
        boolean exists = userService.isNameExists(name);
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("available", !exists);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新用户信息（管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @RequestBody UpdateUserRequest request) {
        
        User user = new User();
        user.setId(id);
        user.setName(request.name());
        user.setAvatarUrl(request.avatarUrl());
        
        try {
            User updatedUser = userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", updatedUser);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 启用/禁用用户（管理员）
     */
    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setUserEnabled(
            @PathVariable String id,
            @RequestBody SetEnabledRequest request) {
        
        try {
            userService.setUserEnabled(id, request.enabled());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User status updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 上传用户头像（文件上传）
     */
    @PutMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) {
        
        log.info("收到头像上传请求，文件大小: {}, 类型: {}", file.getSize(), file.getContentType());
        
        // 检查认证
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        // 验证文件
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "只支持图片文件"));
        }
        
        // 检查文件大小 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件大小不能超过5MB"));
        }
        
        try {
            // 创建上传目录
            String uploadDir = "uploads/avatars";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问URL
            String avatarUrl = "/uploads/avatars/" + filename;
            
            // 更新用户头像
            userService.setAvatar(userId, avatarUrl);
            
            log.info("头像上传成功，用户: {}, 文件: {}", userId, filename);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "头像上传成功");
            response.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "文件上传失败: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("更新头像失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 删除用户头像
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, Object>> removeAvatar(Authentication authentication) {
        
        // 检查认证
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        try {
            // 获取当前头像URL以便删除文件
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                String currentAvatarUrl = userOpt.get().getAvatarUrl();
                
                // 删除头像文件（如果是本地文件）
                if (currentAvatarUrl != null && currentAvatarUrl.startsWith("/uploads/")) {
                    try {
                        Path filePath = Paths.get("." + currentAvatarUrl);
                        Files.deleteIfExists(filePath);
                        log.info("删除头像文件: {}", filePath);
                    } catch (IOException e) {
                        log.warn("删除头像文件失败: {}", e.getMessage());
                    }
                }
            }
            
            // 清除用户头像
            userService.setAvatar(userId, null);
            
            log.info("头像删除成功，用户: {}", userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "头像删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("删除头像失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前用户设置
     * GET /api/users/me/settings
     */
    @GetMapping("/me/settings")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentUserSettings() {
        log.info("GET /api/users/me/settings - 获取当前用户设置");
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("获取用户设置: userId={}", userId);
                    
                    // 返回默认设置或从数据库读取
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("theme", "light");
                    settings.put("language", "zh-CN");
                    settings.put("notifications", true);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("settings", settings);
                    
                    return Mono.just(ResponseEntity.ok(response));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("未授权访问 /api/users/me/settings");
                    return Mono.just(ResponseEntity.status(401).body(Map.of("error", "未授权")));
                }));
    }
    
    /**
     * 更新当前用户设置
     * PUT /api/users/me/settings
     */
    @PutMapping("/me/settings")
    public Mono<ResponseEntity<Map<String, Object>>> updateCurrentUserSettings(
            @RequestBody Map<String, Object> settings) {
        log.info("PUT /api/users/me/settings - 更新当前用户设置");
        
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("更新用户设置: userId={}, settings={}", userId, settings);
                    
                    // 这里应该保存到数据库，现在简单返回成功
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("settings", settings);
                    
                    return Mono.just(ResponseEntity.ok(response));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("未授权访问 PUT /api/users/me/settings");
                    return Mono.just(ResponseEntity.status(401).body(Map.of("error", "未授权")));
                }));
    }

    /**
     * 获取用户公开信息
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<Map<String, Object>> getPublicUserInfo(@PathVariable String id) {
        log.info("获取用户公开信息, userId: {}", id);
        
        Optional<User> userOpt = userService.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // 只返回公开信息，不包含敏感信息
            Map<String, Object> publicUserInfo = new HashMap<>();
            publicUserInfo.put("id", user.getId());
            publicUserInfo.put("name", user.getName() != null ? user.getName() : "Unknown User");
            publicUserInfo.put("email", user.getEmail() != null ? user.getEmail() : "unknown@example.com");
            publicUserInfo.put("avatarUrl", user.getAvatarUrl());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", publicUserInfo);
            
            return ResponseEntity.ok(response);
        } else {
            log.warn("用户不存在: {}", id);
            
            // 返回默认用户信息而不是404，避免前端错误
            Map<String, Object> defaultUserInfo = new HashMap<>();
            defaultUserInfo.put("id", id);
            defaultUserInfo.put("name", "Unknown User");
            defaultUserInfo.put("email", "unknown@example.com");
            defaultUserInfo.put("avatarUrl", null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", defaultUserInfo);
            
            return ResponseEntity.ok(response);
        }
    }

    // 请求数据类
    public record UpdateUserRequest(String name, String avatarUrl) {}
    public record SetEnabledRequest(boolean enabled) {}
    public record SetAvatarRequest(String avatarUrl) {}
}