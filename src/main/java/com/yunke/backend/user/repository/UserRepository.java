package com.yunke.backend.user.repository;

import com.yunke.backend.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户存储库
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 使用原生SQL查询用户密码字段 - 调试用
     */
    @Query(value = "SELECT password FROM users WHERE email = :email", nativeQuery = true)
    String findPasswordByEmailNative(@Param("email") String email);

    /**
     * 使用JPQL查询用户所有字段 - 调试用
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithJPQL(@Param("email") String email);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByName(String name);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据关键词搜索用户（邮箱或用户名）
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 查找已注册用户
     */
    @Query("SELECT u FROM User u WHERE u.registered IS NOT NULL")
    Page<User> findRegisteredUsers(Pageable pageable);

    /**
     * 查找未注册用户
     */
    @Query("SELECT u FROM User u WHERE u.registered IS NULL")
    Page<User> findUnregisteredUsers(Pageable pageable);

    /**
     * 根据创建时间范围查找用户
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") java.time.Instant startDate, 
                                     @Param("endDate") java.time.Instant endDate);

    /**
     * 带筛选条件的分页查询（无搜索）
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:enabled IS NULL OR u.enabled = :enabled) AND " +
           "(:registered IS NULL OR u.registered = :registered) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findUsersWithFilters(@Param("enabled") Boolean enabled,
                                    @Param("registered") Boolean registered,
                                    Pageable pageable);

    /**
     * 带搜索和筛选条件的分页查询
     */
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled) AND " +
           "(:registered IS NULL OR u.registered = :registered) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findUsersWithSearchAndFilters(@Param("search") String search,
                                             @Param("enabled") Boolean enabled,
                                             @Param("registered") Boolean registered,
                                             Pageable pageable);

    /**
     * 统计启用/禁用用户数量
     */
    long countByEnabled(boolean enabled);

    /**
     * 统计注册用户数量
     */
    long countByRegistered(boolean registered);

    /**
     * 统计指定时间后创建的用户数量
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);
}