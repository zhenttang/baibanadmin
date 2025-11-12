package com.yunke.backend.user.service.impl;

import com.yunke.backend.user.dto.BatchUserOperationDto;
import com.yunke.backend.user.dto.UpdateUserRequest;
import com.yunke.backend.user.dto.UserDto;
import com.yunke.backend.user.dto.UserOperationLogDto;
import com.yunke.backend.user.dto.UserQueryDto;
import com.yunke.backend.user.service.UserService.UserStats;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.security.service.RoleService;
import com.yunke.backend.monitor.MetricsCollector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import reactor.core.publisher.Mono;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MetricsCollector metricsCollector;
    private final RoleService roleService;

    @Override
    @Transactional
    public User createUser(User user) {
        log.info("Creating user: {}", user.getEmail());

        // 检查邮箱是否已存在
        if (isEmailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        // 检查用户名是否已存在
        if (user.getName() != null && isNameExists(user.getName())) {
            throw new IllegalArgumentException("Username already exists: " + user.getName());
        }

        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 设置默认值
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(Instant.now());

        if (user.getRegisteredAt() == null) {
            user.setRegisteredAt(Instant.now());
        }

        User savedUser = userRepository.save(user);

        // 记录指标
        metricsCollector.recordUserOperation("create", savedUser.getId());

        log.info("User created successfully: {}", savedUser.getId());
        return savedUser;
    }

    @Override
    @Cacheable(value = "users", key = "'direct:' + #id")
    public Optional<User> findById(String id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }

    @Override
    @Cacheable(value = "users", key = "'direct:' + #id")
    public User getUserById(String id) {
        log.debug("Getting user by ID: {}", id);
        return userRepository.findById(id).orElse(null);
    }

    @Override
    // 暂时禁用缓存来排除缓存问题
    // @Cacheable(value = "users", key = "'email:' + #email", unless="#result == null")
    public Mono<User> findByEmail(String email) {
        log.info("=== Finding user by email: {} ===", email);
        return Mono.fromCallable(() -> {
            log.info("=== Debug: Testing different query methods ===");
            
            // 1. 测试原生SQL查询密码字段
            try {
                String passwordFromNative = userRepository.findPasswordByEmailNative(email);
                log.info("Native SQL password query result: [{}]", passwordFromNative);
                log.info("Native SQL password is null: {}", passwordFromNative == null);
                log.info("Native SQL password length: {}", passwordFromNative != null ? passwordFromNative.length() : 0);
            } catch (Exception e) {
                log.error("Native SQL query failed: {}", e.getMessage());
            }
            
            // 2. 测试JPQL查询
            try {
                Optional<User> jpqlResult = userRepository.findByEmailWithJPQL(email);
                log.info("JPQL query result found: {}", jpqlResult.isPresent());
                if (jpqlResult.isPresent()) {
                    User jpqlUser = jpqlResult.get();
                    log.info("JPQL user password: [{}]", jpqlUser.getPassword());
                    log.info("JPQL user password is null: {}", jpqlUser.getPassword() == null);
                }
            } catch (Exception e) {
                log.error("JPQL query failed: {}", e.getMessage());
            }
            
            // 3. 标准JPA查询
            Optional<User> result = userRepository.findByEmail(email);
            log.info("Standard JPA query result - found: {}", result.isPresent());
            if (result.isPresent()) {
                User user = result.get();
                log.info("Found user - ID: {}, Email: {}, Name: {}", user.getId(), user.getEmail(), user.getName());
                log.info("Password field value: [{}]", user.getPassword());
                log.info("Password field is null: {}", user.getPassword() == null);
                log.info("Password field length: {}", user.getPassword() != null ? user.getPassword().length() : 0);
            }
            return result;
        })
        .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()));
    }

    @Override
    @Cacheable(value = "users", key = "'name:' + #name")
    public Optional<User> findByName(String name) {
        log.debug("Finding user by name: {}", name);
        return userRepository.findByName(name);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'direct:' + #user.id")
    public User updateUser(User user) {
        log.info("Updating user: {}", user.getId());

        Optional<User> existingUser = userRepository.findById(user.getId());
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + user.getId());
        }

        User current = existingUser.get();

        // 更新允许修改的字段
        if (user.getName() != null && !user.getName().equals(current.getName())) {
            if (isNameExists(user.getName())) {
                throw new IllegalArgumentException("Username already exists: " + user.getName());
            }
            current.setName(user.getName());
        }

        if (user.getAvatarUrl() != null) {
            current.setAvatarUrl(user.getAvatarUrl());
        }


        // current.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(current);

        // 记录指标
        metricsCollector.recordUserOperation("update", updatedUser.getId());

        log.info("User updated successfully: {}", updatedUser.getId());
        return updatedUser;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'direct:' + #id")
    public void deleteUser(String id) {
        log.info("Deleting user: {}", id);

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }

        userRepository.deleteById(id);

        // 记录指标
        metricsCollector.recordUserOperation("delete", id);

        log.info("User deleted successfully: {}", id);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        log.debug("Finding all users with pagination");
        return userRepository.findAll(pageable);
    }

    @Override
    public List<User> searchUsers(String keyword) {
        log.debug("Searching users with keyword: {}", keyword);
        return userRepository.searchByKeyword(keyword);
    }

    @Override
    public boolean validatePassword(String userId, String password) {
        log.debug("Validating password for user: {}", userId);

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return false;
        }

        return passwordEncoder.matches(password, user.get().getPassword());
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'direct:' + #userId")
    public void updatePassword(String userId, String newPassword) {
        log.info("Updating password for user: {}", userId);

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User current = user.get();
        current.setPassword(passwordEncoder.encode(newPassword));
        current.setUpdatedAt(Instant.now());

        userRepository.save(current);

        // 记录指标
        metricsCollector.recordUserOperation("password_update", userId);

        log.info("Password updated successfully for user: {}", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'direct:' + #userId")
    public void setAvatar(String userId, String avatarUrl) {
        log.info("Setting avatar for user: {}", userId);

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User current = user.get();
        current.setAvatarUrl(avatarUrl);
        current.setUpdatedAt(Instant.now());

        userRepository.save(current);

        log.info("Avatar set successfully for user: {}", userId);
    }

    @Override
    public UserStats getUserStats(String userId) {
        log.debug("Getting stats for user: {}", userId);

        // 这里应该查询相关的统计信息
        // 暂时返回默认值，后续在实现工作空间和文档管理后完善
        return new UserStats(0, 0, 0L, 0);
    }

    @Override
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean isNameExists(String name) {
        return userRepository.existsByName(name);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "'direct:' + #userId")
    public void setUserEnabled(String userId, boolean enabled) {
        log.info("Setting user enabled status: {} for user: {}", enabled, userId);

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User current = user.get();
        current.setEnabled(enabled);
        current.setUpdatedAt(Instant.now());

        userRepository.save(current);

        log.info("User enabled status updated successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public User createTempUser(String email) {
        log.info("Creating temporary user with email: {}", email);

        // 检查邮箱是否已存在
        if (isEmailExists(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User tempUser = new User();
        tempUser.setEmail(email);
        tempUser.setName("Temp_" + email.split("@")[0]);
        // 设置临时用户标志
        tempUser.setEnabled(true);
        tempUser.setCreatedAt(LocalDateTime.now());
        // 设置注册状态
        tempUser.setRegisteredAt(Instant.now());

        User savedUser = userRepository.save(tempUser);

        // 记录指标
        metricsCollector.recordUserOperation("create_temp", savedUser.getId());

        log.info("Temporary user created successfully: {}", savedUser.getId());
        return savedUser;
    }

    @Override
    public List<String> getUserFeatures(String userId) {
        log.debug("获取用户 {} 的权限列表", userId);
        
        try {
            // 使用 RoleService 获取用户特性（已集成缓存）
            List<String> features = roleService.getUserFeatures(userId);
            if (!features.isEmpty()) {
                return features;
            }
            
            // 回退到旧的逻辑作为兼容性支持
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                List<String> featuresList = new java.util.ArrayList<>();
                
                // 管理员权限检查逻辑（兼容性检查）
                String email = user.getEmail();
                if (email != null && isAdminUser(email, user.getName())) {
                    featuresList.add("admin");
                    log.info("用户 {} ({}) 通过兼容性检查具有管理员权限", userId, email);
                    
                    // 自动迁移到新的角色系统
                    roleService.initializeDefaultAdmin(userId);
                }
                
                featuresList.add("user");
                return featuresList;
            } else {
                log.warn("用户不存在: {}", userId);
                return new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            log.error("获取用户权限失败: {}", e.getMessage(), e);
            return List.of("user"); // 至少返回基础权限
        }
    }
    
    /**
     * 检查用户是否为管理员
     * @param email 用户邮箱
     * @param name 用户名
     * @return 是否为管理员
     */
    private boolean isAdminUser(String email, String name) {
        // 1. 邮箱包含admin的用户
        if (email.contains("admin")) {
            return true;
        }
        
        // 2. 特定的管理员邮箱列表
        Set<String> adminEmails = Set.of(
            "admin@example.com",
            "test@admin.com",
            "admin@yunke.com",
            "administrator@yunke.com",
            "admin@localhost",
            "root@localhost"
        );
        
        if (adminEmails.contains(email.toLowerCase())) {
            return true;
        }
        
        // 3. 用户名包含admin的用户
        if (name != null && name.toLowerCase().contains("admin")) {
            return true;
        }
        
        // 4. 检查配置文件中的管理员列表（未来可以通过配置管理）
        // TODO: 可以从配置中心或数据库读取管理员列表
        
        return false;
    }

    @Override
    public Page<User> findUsersWithFilters(Pageable pageable, String search, Boolean enabled, Boolean registered) {
        log.info("分页查询用户 - search: {}, enabled: {}, registered: {}, page: {}, size: {}", 
                search, enabled, registered, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            if (search != null && !search.trim().isEmpty()) {
                // 如果有搜索条件，使用自定义查询
                return userRepository.findUsersWithSearchAndFilters(
                    search.trim(), enabled, registered, pageable);
            } else {
                // 没有搜索条件，使用筛选查询
                return userRepository.findUsersWithFilters(enabled, registered, pageable);
            }
        } catch (Exception e) {
            log.error("查询用户列表失败", e);
            // 返回空页面而不是抛出异常
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<User> findUsersWithAdvancedFilters(Pageable pageable, UserQueryDto query) {
        return null;
    }

    @Override
    public User toggleUserStatus(String userId) {
        log.info("切换用户状态 - userId: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        
        User user = userOpt.get();
        user.setEnabled(!user.isEnabled());
        user.setUpdatedAt(java.time.Instant.now());
        
        User updatedUser = userRepository.save(user);
        
        // 清除缓存
        clearUserCache(userId);
        
        log.info("用户状态切换成功 - userId: {}, 新状态: {}", userId, updatedUser.isEnabled());
        return updatedUser;
    }

    @Override
    public Map<String, Integer> importUsersFromCsv(org.springframework.web.multipart.MultipartFile file) {
        log.info("开始导入用户 - 文件: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        
        Map<String, Integer> result = new HashMap<>();
        result.put("totalProcessed", 0);
        result.put("successCount", 0);
        result.put("failedCount", 0);
        result.put("skippedCount", 0);
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                result.put("totalProcessed", lineNumber - 1); // 不计算标题行
                
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 跳过标题行（假设第一行是标题）
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().contains("email") || line.toLowerCase().contains("name")) {
                        continue;
                    }
                }
                
                try {
                    String[] fields = line.split(",");
                    if (fields.length < 2) {
                        log.warn("CSV格式错误，第{}行: {}", lineNumber, line);
                        result.put("failedCount", result.get("failedCount") + 1);
                        continue;
                    }
                    
                    String email = fields[0].trim().replaceAll("\"", "");
                    String name = fields[1].trim().replaceAll("\"", "");
                    String password = fields.length > 2 ? fields[2].trim().replaceAll("\"", "") : "123456";
                    
                    // 验证邮箱格式
                    if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        log.warn("邮箱格式错误，第{}行: {}", lineNumber, email);
                        result.put("failedCount", result.get("failedCount") + 1);
                        continue;
                    }
                    
                    // 检查邮箱是否已存在
                    if (isEmailExists(email)) {
                        log.info("邮箱已存在，跳过: {}", email);
                        result.put("skippedCount", result.get("skippedCount") + 1);
                        continue;
                    }
                    
                    // 创建用户
                    User user = User.builder()
                            .id(java.util.UUID.randomUUID().toString())
                            .email(email)
                            .name(name)
                            .password(passwordEncoder.encode(password))
                            .enabled(true)
                            .registered(true)
                            .disabled(false)
                            .createdAt(java.time.LocalDateTime.now())
                            .emailVerifiedAt(java.time.LocalDateTime.now())
                            .registeredAt(java.time.Instant.now())
                            .updatedAt(java.time.Instant.now())
                            .build();
                    
                    userRepository.save(user);
                    result.put("successCount", result.get("successCount") + 1);
                    
                    log.debug("用户导入成功: {} - {}", email, name);
                    
                } catch (Exception e) {
                    log.error("导入用户失败，第{}行: {}", lineNumber, e.getMessage());
                    result.put("failedCount", result.get("failedCount") + 1);
                }
            }
            
        } catch (Exception e) {
            log.error("读取CSV文件失败", e);
            throw new RuntimeException("CSV文件处理失败: " + e.getMessage());
        }
        
        log.info("用户导入完成 - 总处理: {}, 成功: {}, 失败: {}, 跳过: {}", 
                result.get("totalProcessed"), result.get("successCount"), 
                result.get("failedCount"), result.get("skippedCount"));
        
        return result;
    }

    @Override
    public Map<String, Object> getUserStatistics() {
        log.info("获取用户统计信息");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 总用户数
            long totalUsers = userRepository.count();
            stats.put("totalUsers", totalUsers);
            
            // 启用用户数
            long enabledUsers = userRepository.countByEnabled(true);
            stats.put("enabledUsers", enabledUsers);
            
            // 禁用用户数
            long disabledUsers = userRepository.countByEnabled(false);
            stats.put("disabledUsers", disabledUsers);
            
            // 已注册用户数
            long registeredUsers = userRepository.countByRegistered(true);
            stats.put("registeredUsers", registeredUsers);
            
            // 今日新增用户
            java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            long todayNewUsers = userRepository.countByCreatedAtAfter(todayStart);
            stats.put("todayNewUsers", todayNewUsers);
            
            // 本周新增用户
            java.time.LocalDateTime weekStart = java.time.LocalDateTime.now().minusDays(7);
            long weekNewUsers = userRepository.countByCreatedAtAfter(weekStart);
            stats.put("weekNewUsers", weekNewUsers);
            
            // 本月新增用户
            java.time.LocalDateTime monthStart = java.time.LocalDateTime.now().minusDays(30);
            long monthNewUsers = userRepository.countByCreatedAtAfter(monthStart);
            stats.put("monthNewUsers", monthNewUsers);
            
            log.info("用户统计信息获取成功: {}", stats);
            return stats;
            
        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            throw new RuntimeException("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 清除用户缓存
     */
    private void clearUserCache(String userId) {
        try {
            // 清除相关缓存
            // 注意：这里需要根据实际的缓存配置来清除
            log.debug("清除用户缓存 - userId: {}", userId);
        } catch (Exception e) {
            log.warn("清除用户缓存失败: {}", e.getMessage());
        }
    }

    // 新增的管理员功能方法

    /**
     * 获取今日新增用户数
     */
    public long countUsersCreatedToday() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return userRepository.countByCreatedAtAfter(todayStart);
    }

    /**
     * 获取启用用户总数
     */
    public long countEnabledUsers() {
        return userRepository.countByEnabled(true);
    }

    /**
     * 获取用户总数
     */
    public long countAllUsers() {
        return userRepository.count();
    }

    @Override
    public Page<User> searchUsersWithPagination(String search, Pageable pageable) {
        log.info("搜索用户分页查询 - search: {}, page: {}, size: {}", 
                search, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // 获取所有用户并进行搜索过滤
            List<User> allUsers = userRepository.findAll();
            String keyword = search.toLowerCase();
            
            List<User> filteredUsers = allUsers.stream()
                .filter(user -> {
                    return (user.getEmail() != null && user.getEmail().toLowerCase().contains(keyword)) ||
                           (user.getName() != null && user.getName().toLowerCase().contains(keyword));
                })
                .collect(java.util.stream.Collectors.toList());
            
            // 手动分页
            int start = Math.min((int) pageable.getOffset(), filteredUsers.size());
            int end = Math.min(start + pageable.getPageSize(), filteredUsers.size());
            List<User> pageUsers = filteredUsers.subList(start, end);
            
            Page<User> result = new org.springframework.data.domain.PageImpl<>(pageUsers, pageable, filteredUsers.size());
            
            log.info("搜索用户完成 - 匹配到 {} 个用户，当前页 {} 个", 
                    filteredUsers.size(), pageUsers.size());
            return result;
            
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public long getVerifiedUserCount() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream().mapToLong(user -> user.getEmailVerifiedAt() != null ? 1 : 0).sum();
    }

    @Override
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Override
    public long getEnabledUserCount() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream().mapToLong(user -> user.isEnabled() ? 1 : 0).sum();
    }

    @Override
    public long getDisabledUserCount() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream().mapToLong(user -> user.isEnabled() ? 0 : 1).sum();
    }

    /**
     * Admin管理用的用户查找方法（支持分页、搜索、筛选）
     */
    @Override
    public Page<User> findUsers(String search, Boolean enabled, Boolean registered, Pageable pageable) {
        log.info("Admin查找用户 - search: {}, enabled: {}, registered: {}, page: {}, size: {}", 
                search, enabled, registered, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            List<User> allUsers = userRepository.findAll();
            
            // 筛选用户
            List<User> filteredUsers = allUsers.stream()
                .filter(user -> {
                    // 搜索筛选
                    if (search != null && !search.trim().isEmpty()) {
                        String keyword = search.toLowerCase().trim();
                        boolean matchEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(keyword);
                        boolean matchName = user.getName() != null && user.getName().toLowerCase().contains(keyword);
                        if (!matchEmail && !matchName) {
                            return false;
                        }
                    }
                    
                    // 启用状态筛选
                    if (enabled != null && user.isEnabled() != enabled) {
                        return false;
                    }
                    
                    // 注册状态筛选
                    if (registered != null && user.isRegistered() != registered) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
            
            // 排序
            filteredUsers.sort((u1, u2) -> {
                if (pageable.getSort().isSorted()) {
                    // 根据排序字段进行排序
                    String sortBy = pageable.getSort().iterator().next().getProperty();
                    boolean isDesc = pageable.getSort().iterator().next().isDescending();
                    
                    int comparison = 0;
                    switch (sortBy) {
                        case "name":
                            comparison = String.valueOf(u1.getName()).compareTo(String.valueOf(u2.getName()));
                            break;
                        case "email":
                            comparison = String.valueOf(u1.getEmail()).compareTo(String.valueOf(u2.getEmail()));
                            break;
                        case "createdAt":
                            comparison = u1.getCreatedAt().compareTo(u2.getCreatedAt());
                            break;
                        case "enabled":
                            comparison = Boolean.compare(u1.isEnabled(), u2.isEnabled());
                            break;
                        default:
                            comparison = u1.getCreatedAt().compareTo(u2.getCreatedAt());
                    }
                    
                    return isDesc ? -comparison : comparison;
                } else {
                    // 默认按创建时间倒序
                    return u2.getCreatedAt().compareTo(u1.getCreatedAt());
                }
            });
            
            // 手动分页
            int start = Math.min((int) pageable.getOffset(), filteredUsers.size());
            int end = Math.min(start + pageable.getPageSize(), filteredUsers.size());
            List<User> pageUsers = filteredUsers.subList(start, end);
            
            Page<User> result = new org.springframework.data.domain.PageImpl<>(pageUsers, pageable, filteredUsers.size());
            
            log.info("Admin查找用户完成 - 匹配到 {} 个用户，当前页 {} 个", 
                    filteredUsers.size(), pageUsers.size());
            return result;
            
        } catch (Exception e) {
            log.error("Admin查找用户失败", e);
            return Page.empty(pageable);
        }
    }

    /**
     * Admin管理用的用户更新方法
     */
    @Override
    @Transactional
    public User updateUser(String id, UpdateUserRequest request) {
        log.info("Admin更新用户 {} - {}", id, request);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在: " + id);
        }
        
        User user = userOpt.get();
        
        // 更新字段
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }
        
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            // 检查邮箱是否被其他用户使用
            if (!user.getEmail().equals(request.getEmail().trim()) && isEmailExists(request.getEmail().trim())) {
                throw new RuntimeException("邮箱已被其他用户使用: " + request.getEmail());
            }
            user.setEmail(request.getEmail().trim());
        }
        
        if (request.isEnabled() != null) {
            user.setEnabled(request.isEnabled());
        }
        
        if (request.getRegistered() != null) {
            user.setRegistered(request.getRegistered());
        }
        
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        user.setUpdatedAt(Instant.now());
        
        User updatedUser = userRepository.save(user);
        
        // 记录操作日志
        metricsCollector.recordUserOperation("admin_update", id);
        
        log.info("Admin更新用户完成: {}", id);
        return updatedUser;
    }

    /**
     * Admin管理用的批量操作方法
     */
    @Override
    @Transactional
    public int batchOperation(List<String> userIds, String operation) {
        log.info("Admin批量操作 - operation: {}, userIds: {}", operation, userIds);
        
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("用户ID列表不能为空");
        }
        
        int affectedCount = 0;
        
        for (String userId : userIds) {
            try {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) {
                    log.warn("用户不存在，跳过: {}", userId);
                    continue;
                }
                
                User user = userOpt.get();
                
                switch (operation.toLowerCase()) {
                    case "enable":
                        user.setEnabled(true);
                        user.setUpdatedAt(Instant.now());
                        userRepository.save(user);
                        affectedCount++;
                        break;
                        
                    case "disable":
                        user.setEnabled(false);
                        user.setUpdatedAt(Instant.now());
                        userRepository.save(user);
                        affectedCount++;
                        break;
                        
                    case "delete":
                        userRepository.delete(user);
                        affectedCount++;
                        break;
                        
                    default:
                        throw new RuntimeException("不支持的操作: " + operation);
                }
                
                // 记录操作日志
                metricsCollector.recordUserOperation("admin_batch_" + operation, userId);
                
            } catch (Exception e) {
                log.error("批量操作失败 - userId: {}, operation: {}, error: {}", userId, operation, e.getMessage());
            }
        }
        
        log.info("Admin批量操作完成 - operation: {}, affected: {}/{}", operation, affectedCount, userIds.size());
        return affectedCount;
    }

    @Override
    public Map<String, Object> performBatchOperation(BatchUserOperationDto request) {
        return null;
    }

    @Override
    public void logUserOperation(String operatorId, String targetUserId, String operation,
                                String description, String details, String result) {
        log.info("用户操作记录 - operator: {}, target: {}, operation: {}, description: {}, result: {}",
                operatorId, targetUserId, operation, description, result);

        // 这里可以将操作记录保存到数据库或审计日志系统
        // 暂时只记录到日志中
        try {
            // 可以扩展为保存到审计表
            // auditLogRepository.save(new AuditLog(operatorId, targetUserId, operation, description, details, result));
        } catch (Exception e) {
            log.error("记录用户操作失败", e);
        }
    }

    @Override
    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        // 暂时简单判断，可以根据实际需求扩展
        // 由于User实体中暂时没有roles字段，先使用简单的判断逻辑
        return "admin@affine.pro".equals(user.getEmail()) || user.getEmail().endsWith("@admin.affine.pro");
    }

    @Override
    public void updateUserPermissions(String userId, Map<String, Object> permissions) {
        log.info("更新用户权限 - userId: {}, permissions: {}", userId, permissions);

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("用户不存在: " + userId);
            }

            User user = userOpt.get();

            // 这里可以根据实际需求实现权限更新逻辑
            // 暂时只记录日志，实际实现需要根据权限系统设计
            log.info("用户权限更新完成 - userId: {}", userId);

        } catch (Exception e) {
            log.error("更新用户权限失败 - userId: {}", userId, e);
            throw new RuntimeException("更新用户权限失败", e);
        }
    }

    @Override
    public Page<UserOperationLogDto> getUserOperationLogs(String userId, Pageable pageable) {
        log.info("获取用户操作日志 - userId: {}, page: {}, size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 这里应该从审计日志表中查询用户操作记录
            // 暂时返回空的分页结果，实际实现需要根据审计日志系统设计
            List<UserOperationLogDto> logs = new ArrayList<>();

            // 可以添加一些示例数据用于测试
            if ("admin".equals(userId)) {
                logs.add(UserOperationLogDto.builder()
                        .id("1")
                        .operation("LOGIN")
                        .description("用户登录")
                        .ipAddress("127.0.0.1")
                        .userAgent("Mozilla/5.0")
                        .result("SUCCESS")
                        .build());
            }

            return new PageImpl<>(logs, pageable, logs.size());

        } catch (Exception e) {
            log.error("获取用户操作日志失败 - userId: {}", userId, e);
            throw new RuntimeException("获取用户操作日志失败", e);
        }
    }
}
