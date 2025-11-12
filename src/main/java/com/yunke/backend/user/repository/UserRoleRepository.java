package com.yunke.backend.user.repository;

import com.yunke.backend.user.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户角色仓库
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {
    
    /**
     * 根据用户ID查找有效角色
     * 注意：使用原生SQL避免JPA的collation问题
     */
    @Query(value = "SELECT * FROM user_roles WHERE user_id COLLATE utf8mb4_unicode_ci = :userId COLLATE utf8mb4_unicode_ci " +
           "AND enabled = true " +
           "AND (expires_at IS NULL OR expires_at > :now)", nativeQuery = true)
    List<UserRole> findActiveRolesByUserId(@Param("userId") String userId, 
                                          @Param("now") LocalDateTime now);
    
    /**
     * 根据用户ID和角色查找
     */
    Optional<UserRole> findByUserIdAndRole(String userId, UserRole.Role role);
    
    /**
     * 检查用户是否具有指定角色
     * 返回匹配的记录数，需要调用方检查是否 > 0
     */
    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE user_id COLLATE utf8mb4_unicode_ci = :userId COLLATE utf8mb4_unicode_ci " +
           "AND role = :role " +
           "AND enabled = true " +
           "AND (expires_at IS NULL OR expires_at > :now)", nativeQuery = true)
    Long countRole(@Param("userId") String userId, 
                   @Param("role") String role, 
                   @Param("now") LocalDateTime now);
    
    /**
     * 检查用户是否具有管理员权限
     * 返回匹配的记录数，需要调用方检查是否 > 0
     */
    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE user_id COLLATE utf8mb4_unicode_ci = :userId COLLATE utf8mb4_unicode_ci " +
           "AND role IN ('ADMIN', 'SUPER_ADMIN') " +
           "AND enabled = true " +
           "AND (expires_at IS NULL OR expires_at > :now)", nativeQuery = true)
    Long countAdminRole(@Param("userId") String userId, 
                        @Param("now") LocalDateTime now);
    
    /**
     * 检查用户是否具有指定角色
     * @deprecated 使用 countRole 代替，避免 MySQL 布尔值转换问题
     */
    @Deprecated
    default boolean hasRole(String userId, String role, LocalDateTime now) {
        Long count = countRole(userId, role, now);
        return count != null && count > 0;
    }
    
    /**
     * 检查用户是否具有管理员权限
     * @deprecated 使用 countAdminRole 代替，避免 MySQL 布尔值转换问题
     */
    @Deprecated
    default boolean hasAdminRole(String userId, LocalDateTime now) {
        Long count = countAdminRole(userId, now);
        return count != null && count > 0;
    }
    
    /**
     * 获取具有指定角色的所有用户
     */
    @Query(value = "SELECT * FROM user_roles WHERE role = :role " +
           "AND enabled = true " +
           "AND (expires_at IS NULL OR expires_at > NOW())", nativeQuery = true)
    List<UserRole> findUsersByRole(@Param("role") String role);
    
    /**
     * 清理过期角色
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE user_roles SET enabled = false WHERE expires_at <= :now", nativeQuery = true)
    int disableExpiredRoles(@Param("now") LocalDateTime now);
}