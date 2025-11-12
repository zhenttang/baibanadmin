package com.yunke.backend.user.service;

import com.yunke.backend.controller.AdminController;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.dto.UserQueryDto;
import com.yunke.backend.user.dto.UpdateUserRequest;
import com.yunke.backend.user.dto.BatchUserOperationDto;
import com.yunke.backend.user.dto.UserOperationLogDto;
import com.yunke.backend.system.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 创建用户
     */
    User createUser(User user);

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(String id);

    /**
     * 根据ID获取用户（直接返回User，如果不存在则返回null）
     */
    User getUserById(String id);

    /**
     * 根据邮箱查找用户
     */
    Mono<User> findByEmail(String email);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByName(String name);

    /**
     * 更新用户信息
     */
    User updateUser(User user);

    /**
     * 删除用户
     */
    void deleteUser(String id);

    /**
     * 分页查询用户
     */
    Page<User> findAll(Pageable pageable);

    /**
     * 搜索用户
     */
    List<User> searchUsers(String keyword);

    /**
     * 验证用户密码
     */
    boolean validatePassword(String userId, String password);

    /**
     * 更新用户密码
     */
    void updatePassword(String userId, String newPassword);

    /**
     * 设置用户头像
     */
    void setAvatar(String userId, String avatarUrl);

    /**
     * 获取用户统计信息
     */
    UserStats getUserStats(String userId);

    /**
     * 检查邮箱是否已存在
     */
    boolean isEmailExists(String email);

    /**
     * 检查用户名是否已存在
     */
    boolean isNameExists(String name);

    /**
     * 启用/禁用用户
     */
    void setUserEnabled(String userId, boolean enabled);

    /**
     * 创建临时用户
     * @param email 用户邮箱
     * @return 创建的临时用户
     */
    User createTempUser(String email);

    /**
     * 获取用户权限列表
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> getUserFeatures(String userId);

    /**
     * 带筛选条件的分页查询用户
     * @param pageable 分页参数
     * @param search 搜索关键词（邮箱或姓名）
     * @param enabled 启用状态筛选
     * @param registered 注册状态筛选
     * @return 用户分页结果
     */
    Page<User> findUsersWithFilters(Pageable pageable, String search, Boolean enabled, Boolean registered);

    /**
     * 高级筛选查询用户（支持UserQueryDto）
     * @param pageable 分页参数
     * @param query 查询条件
     * @return 用户分页结果
     */
    Page<User> findUsersWithAdvancedFilters(Pageable pageable, UserQueryDto query);

    /**
     * 切换用户启用/禁用状态
     * @param userId 用户ID
     * @return 更新后的用户
     */
    User toggleUserStatus(String userId);

    /**
     * 从CSV文件批量导入用户
     * @param file CSV文件
     * @return 导入结果统计
     */
    Map<String, Integer> importUsersFromCsv(MultipartFile file);

    /**
     * 获取用户统计信息
     * @return 统计信息
     */
    Map<String, Object> getUserStatistics();

    /**
     * 获取总用户数
     */
    long getTotalUserCount();

    /**
     * 获取启用用户数
     */
    long getEnabledUserCount();

    /**
     * 获取禁用用户数
     */
    long getDisabledUserCount();

    /**
     * 获取已验证邮箱用户数
     */
    long getVerifiedUserCount();

    /**
     * 带搜索的分页查询
     */
    Page<User> searchUsersWithPagination(String search, Pageable pageable);

    /**
     * Admin管理用的用户查找方法（支持分页、搜索、筛选）
     */
    Page<User> findUsers(String search, Boolean enabled, Boolean registered, Pageable pageable);

    /**
     * Admin管理用的用户更新方法
     */
    User updateUser(String id, UpdateUserRequest request);

    /**
     * Admin管理用的批量操作方法
     */
    int batchOperation(List<String> userIds, String operation);

    /**
     * 执行批量用户操作（增强版）
     * @param request 批量操作请求
     * @return 操作结果
     */
    Map<String, Object> performBatchOperation(BatchUserOperationDto request);

    /**
     * 获取用户操作日志
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 操作日志分页结果
     */
    Page<UserOperationLogDto> getUserOperationLogs(String userId, Pageable pageable);

    /**
     * 更新用户权限
     * @param userId 用户ID
     * @param permissions 权限配置
     */
    void updateUserPermissions(String userId, Map<String, Object> permissions);

    /**
     * 检查用户是否为管理员
     * @param user 用户对象
     * @return 是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 记录用户操作日志
     * @param operatorId 操作者ID
     * @param targetUserId 目标用户ID
     * @param operation 操作类型
     * @param description 操作描述
     * @param details 操作详情
     * @param result 操作结果
     */
    void logUserOperation(String operatorId, String targetUserId, String operation, 
                         String description, String details, String result);

    /**
     * 用户统计信息
     */
    record UserStats(
            int workspaceCount,
            int docCount,
            long storageUsed,
            int collaborationCount
    ) {}
}