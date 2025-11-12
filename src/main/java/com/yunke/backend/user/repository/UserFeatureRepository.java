package com.yunke.backend.user.repository;

import com.yunke.backend.user.domain.entity.UserFeature;
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
 * 用户特性Repository
 * 对应Node.js版本的UserFeature数据访问层
 */
@Repository
public interface UserFeatureRepository extends JpaRepository<UserFeature, Integer> {

    // ==================== 基础查询 ====================

    /**
     * 查找用户的所有特性
     */
    List<UserFeature> findByUserId(String userId);

    /**
     * 查找用户的所有激活特性
     */
    List<UserFeature> findByUserIdAndActivatedTrue(String userId);

    /**
     * 查找用户的特定特性
     */
    Optional<UserFeature> findByUserIdAndName(String userId, String name);

    /**
     * 查找用户的特定类型特性
     */
    List<UserFeature> findByUserIdAndType(String userId, Integer type);

    /**
     * 查找用户的激活特性 (按类型)
     */
    List<UserFeature> findByUserIdAndTypeAndActivatedTrue(String userId, Integer type);

    // ==================== 特性存在性检查 ====================

    /**
     * 检查用户是否有特定特性
     */
    boolean existsByUserIdAndName(String userId, String name);

    /**
     * 检查用户是否有激活的特定特性
     */
    boolean existsByUserIdAndNameAndActivatedTrue(String userId, String name);

    /**
     * 检查用户是否有未过期的特定特性
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFeature uf WHERE uf.userId = :userId AND uf.name = :name AND uf.activated = true AND (uf.expiredAt IS NULL OR uf.expiredAt > :now)")
    boolean existsActiveAndNotExpired(@Param("userId") String userId, @Param("name") String name, @Param("now") LocalDateTime now);

    // ==================== 过期处理 ====================

    /**
     * 查找已过期的用户特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.expiredAt IS NOT NULL AND uf.expiredAt <= :now")
    List<UserFeature> findExpiredFeatures(@Param("now") LocalDateTime now);

    /**
     * 查找用户的已过期特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND uf.expiredAt IS NOT NULL AND uf.expiredAt <= :now")
    List<UserFeature> findExpiredFeaturesByUser(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * 查找即将过期的特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND uf.expiredAt IS NOT NULL AND uf.expiredAt BETWEEN :now AND :threshold")
    List<UserFeature> findExpiringFeatures(@Param("userId") String userId, @Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    // ==================== 特性管理 ====================

    /**
     * 激活用户特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFeature uf SET uf.activated = true WHERE uf.userId = :userId AND uf.name = :name")
    int activateFeature(@Param("userId") String userId, @Param("name") String name);

    /**
     * 停用用户特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFeature uf SET uf.activated = false WHERE uf.userId = :userId AND uf.name = :name")
    int deactivateFeature(@Param("userId") String userId, @Param("name") String name);

    /**
     * 设置特性过期时间
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFeature uf SET uf.expiredAt = :expiredAt WHERE uf.userId = :userId AND uf.name = :name")
    int setFeatureExpiration(@Param("userId") String userId, @Param("name") String name, @Param("expiredAt") LocalDateTime expiredAt);

    /**
     * 移除用户的特定特性
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndName(String userId, String name);

    /**
     * 移除用户的所有特性
     */
    @Modifying
    @Transactional
    void deleteByUserId(String userId);

    // ==================== 早期访问管理 ====================

    /**
     * 查找早期访问用户
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.name = :earlyAccessType AND uf.activated = true")
    List<UserFeature> findEarlyAccessUsers(@Param("earlyAccessType") String earlyAccessType);

    /**
     * 检查用户是否有早期访问权限
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFeature uf WHERE uf.userId = :userId AND uf.name = :earlyAccessType AND uf.activated = true")
    boolean hasEarlyAccess(@Param("userId") String userId, @Param("earlyAccessType") String earlyAccessType);

    /**
     * 添加早期访问权限
     */
    default UserFeature addEarlyAccess(String userId, String earlyAccessType, String reason) {
        UserFeature userFeature = UserFeature.builder()
                .userId(userId)
                .name(earlyAccessType)
                .type(0) // FEATURE type
                .reason(reason)
                .activated(true)
                .build();
        return save(userFeature);
    }

    // ==================== 管理员权限管理 ====================

    /**
     * 查找所有管理员
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.name = 'administrator' AND uf.activated = true")
    List<UserFeature> findAdministrators();

    /**
     * 检查用户是否为管理员
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFeature uf WHERE uf.userId = :userId AND uf.name = 'administrator' AND uf.activated = true")
    boolean isAdmin(@Param("userId") String userId);

    /**
     * 添加管理员权限
     */
    default UserFeature addAdmin(String userId, String reason) {
        UserFeature adminFeature = UserFeature.builder()
                .userId(userId)
                .name("administrator")
                .type(0) // FEATURE type
                .reason(reason)
                .activated(true)
                .build();
        return save(adminFeature);
    }

    // ==================== 配额相关查询 ====================

    /**
     * 查找用户的配额特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND uf.type = 1 AND uf.activated = true")
    List<UserFeature> findUserQuotaFeatures(@Param("userId") String userId);

    /**
     * 查找用户当前的计划特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND uf.name LIKE '%_plan_%' AND uf.activated = true")
    List<UserFeature> findUserPlanFeatures(@Param("userId") String userId);

    /**
     * 切换用户计划
     */
    @Modifying
    @Transactional
    default void switchUserPlan(String userId, String fromPlan, String toPlan, String reason) {
        // 停用旧计划
        deactivateFeature(userId, fromPlan);
        
        // 激活新计划
        UserFeature newPlan = UserFeature.builder()
                .userId(userId)
                .name(toPlan)
                .type(1) // QUOTA type
                .reason(reason)
                .activated(true)
                .build();
        save(newPlan);
    }

    // ==================== 统计查询 ====================

    /**
     * 统计用户的激活特性数量
     */
    @Query("SELECT COUNT(uf) FROM UserFeature uf WHERE uf.userId = :userId AND uf.activated = true")
    Long countActiveFeaturesByUser(@Param("userId") String userId);

    /**
     * 统计特定特性的用户数量
     */
    @Query("SELECT COUNT(uf) FROM UserFeature uf WHERE uf.name = :featureName AND uf.activated = true")
    Long countUsersWithFeature(@Param("featureName") String featureName);

    /**
     * 统计各类型特性的用户数量
     */
    @Query("SELECT uf.type, COUNT(DISTINCT uf.userId) FROM UserFeature uf WHERE uf.activated = true GROUP BY uf.type")
    List<Object[]> countUsersByFeatureType();

    /**
     * 统计管理员用户数量
     */
    @Query("SELECT COUNT(DISTINCT uf.userId) FROM UserFeature uf WHERE uf.name = 'administrator' AND uf.activated = true")
    Long countAdministrators();

    /**
     * 统计早期访问用户数量
     */
    @Query("SELECT COUNT(DISTINCT uf.userId) FROM UserFeature uf WHERE uf.name LIKE '%early_access%' AND uf.activated = true")
    Long countEarlyAccessUsers();

    // ==================== 批量操作 ====================

    /**
     * 批量激活用户特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFeature uf SET uf.activated = true WHERE uf.userId IN :userIds AND uf.name = :featureName")
    int batchActivateFeature(@Param("userIds") List<String> userIds, @Param("featureName") String featureName);

    /**
     * 批量停用用户特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFeature uf SET uf.activated = false WHERE uf.userId IN :userIds AND uf.name = :featureName")
    int batchDeactivateFeature(@Param("userIds") List<String> userIds, @Param("featureName") String featureName);

    /**
     * 批量删除过期特性
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserFeature uf WHERE uf.expiredAt IS NOT NULL AND uf.expiredAt <= :now")
    int deleteExpiredFeatures(@Param("now") LocalDateTime now);

    // ==================== 复杂查询 ====================

    /**
     * 查找用户可配置的特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND uf.name IN :configurableFeatures")
    List<UserFeature> findConfigurableFeatures(@Param("userId") String userId, @Param("configurableFeatures") List<String> configurableFeatures);

    /**
     * 查找用户缺少的必需特性
     */
    @Query("SELECT f.name FROM Feature f WHERE f.name IN :requiredFeatures AND f.name NOT IN (SELECT uf.name FROM UserFeature uf WHERE uf.userId = :userId AND uf.activated = true)")
    List<String> findMissingRequiredFeatures(@Param("userId") String userId, @Param("requiredFeatures") List<String> requiredFeatures);

    /**
     * 查找用户的实验性特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND (uf.name LIKE '%beta%' OR uf.name LIKE '%experimental%') AND uf.activated = true")
    List<UserFeature> findExperimentalFeatures(@Param("userId") String userId);

    // ==================== UserFeatureServiceImpl需要的方法 ====================

    /**
     * 检查用户是否有指定特性
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFeature uf WHERE uf.userId = :userId AND uf.name = :featureName AND uf.activated = true")
    boolean hasFeature(@Param("userId") String userId, @Param("featureName") String featureName);

    /**
     * 查找用户的所有活跃特性名称
     */
    @Query("SELECT uf.name FROM UserFeature uf WHERE uf.userId = :userId AND uf.activated = true")
    List<String> findActiveFeatureNames(@Param("userId") String userId);

    /**
     * 查找用户的指定特性类型的所有活跃特性
     */
    @Query("SELECT uf FROM UserFeature uf WHERE uf.userId = :userId AND uf.type = :type AND uf.activated = true")
    List<UserFeature> findActiveFeaturesByType(@Param("userId") String userId, @Param("type") Integer type);
}