package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 特性开关Repository
 * 对应Node.js版本的特性数据访问层
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Integer> {

    // ==================== 基础查询 ====================

    /**
     * 通过特性名称查找特性
     */
    Optional<Feature> findByName(String name);

    /**
     * 通过特性名称和版本查找特性
     */
    Optional<Feature> findByNameAndVersion(String name, Integer version);

    /**
     * 查找所有启用的特性
     */
    List<Feature> findByEnabledTrue();

    /**
     * 查找所有未废弃的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.deprecatedVersion = 0")
    List<Feature> findAllActive();

    /**
     * 通过特性类型查找特性
     */
    List<Feature> findByType(Integer type);

    /**
     * 查找所有启用且未废弃的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.enabled = true AND f.deprecatedVersion = 0")
    List<Feature> findAllEnabledAndActive();

    // ==================== 特性分类查询 ====================

    /**
     * 查找用户特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name IN :userFeatureNames AND f.enabled = true")
    List<Feature> findUserFeatures(@Param("userFeatureNames") List<String> userFeatureNames);

    /**
     * 查找工作区特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name IN :workspaceFeatureNames AND f.enabled = true")
    List<Feature> findWorkspaceFeatures(@Param("workspaceFeatureNames") List<String> workspaceFeatureNames);

    /**
     * 查找配额相关特性
     */
    @Query("SELECT f FROM Feature f WHERE f.type = 1 OR f.name LIKE '%_plan_%' OR f.name LIKE '%storage%'")
    List<Feature> findQuotaFeatures();

    /**
     * 查找实验性特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name LIKE '%beta%' OR f.name LIKE '%experimental%'")
    List<Feature> findExperimentalFeatures();

    // ==================== 管理员特性查询 ====================

    /**
     * 查找管理员特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name IN ('administrator', 'audit_logs', 'enterprise_security')")
    List<Feature> findAdminFeatures();

    /**
     * 查找早期访问特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name LIKE '%early_access%'")
    List<Feature> findEarlyAccessFeatures();

    // ==================== 版本和兼容性查询 ====================

    /**
     * 查找指定版本范围内的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.version BETWEEN :minVersion AND :maxVersion")
    List<Feature> findByVersionRange(@Param("minVersion") Integer minVersion, @Param("maxVersion") Integer maxVersion);

    /**
     * 查找废弃的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.deprecatedVersion > 0")
    List<Feature> findDeprecatedFeatures();

    /**
     * 查找指定版本后废弃的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.deprecatedVersion > :version")
    List<Feature> findDeprecatedAfterVersion(@Param("version") Integer version);

    // ==================== 配置查询 ====================

    /**
     * 查找包含特定配置的特性
     */
    @Query(value = "SELECT * FROM features WHERE JSON_EXTRACT(configs, CONCAT('$.', :configKey)) = :configValue", nativeQuery = true)
    List<Feature> findByConfigValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

    /**
     * 查找包含配置的特性
     */
    @Query(value = "SELECT * FROM features WHERE JSON_LENGTH(configs) > 0", nativeQuery = true)
    List<Feature> findFeaturesWithConfigs();

    // ==================== 名称模式查询 ====================

    /**
     * 通过名称模式查找特性
     */
    List<Feature> findByNameContaining(String namePattern);

    /**
     * 通过名称模式查找启用的特性
     */
    List<Feature> findByNameContainingAndEnabledTrue(String namePattern);

    /**
     * 查找以指定前缀开头的特性
     */
    List<Feature> findByNameStartingWith(String prefix);

    /**
     * 查找以指定后缀结尾的特性
     */
    List<Feature> findByNameEndingWith(String suffix);

    // ==================== 统计查询 ====================

    /**
     * 统计启用的特性数量
     */
    @Query("SELECT COUNT(f) FROM Feature f WHERE f.enabled = true")
    Long countEnabledFeatures();

    /**
     * 统计废弃的特性数量
     */
    @Query("SELECT COUNT(f) FROM Feature f WHERE f.deprecatedVersion > 0")
    Long countDeprecatedFeatures();

    /**
     * 统计各类型特性数量
     */
    @Query("SELECT f.type, COUNT(f) FROM Feature f GROUP BY f.type")
    List<Object[]> countFeaturesByType();

    /**
     * 统计用户特性数量
     */
    @Query("SELECT COUNT(f) FROM Feature f WHERE f.name IN :userFeatureNames")
    Long countUserFeatures(@Param("userFeatureNames") List<String> userFeatureNames);

    /**
     * 统计工作区特性数量
     */
    @Query("SELECT COUNT(f) FROM Feature f WHERE f.name IN :workspaceFeatureNames")
    Long countWorkspaceFeatures(@Param("workspaceFeatureNames") List<String> workspaceFeatureNames);

    // ==================== 批量操作 ====================

    /**
     * 批量启用特性
     */
    @Query("UPDATE Feature f SET f.enabled = true WHERE f.name IN :featureNames")
    int enableFeatures(@Param("featureNames") List<String> featureNames);

    /**
     * 批量禁用特性
     */
    @Query("UPDATE Feature f SET f.enabled = false WHERE f.name IN :featureNames")
    int disableFeatures(@Param("featureNames") List<String> featureNames);

    /**
     * 批量废弃特性
     */
    @Query("UPDATE Feature f SET f.deprecatedVersion = :version WHERE f.name IN :featureNames")
    int deprecateFeatures(@Param("featureNames") List<String> featureNames, @Param("version") Integer version);

    // ==================== 自定义查询方法 ====================

    /**
     * 检查特性是否存在且启用
     */
    default boolean isFeatureEnabledByName(String name) {
        return findByName(name)
                .map(Feature::getEnabled)
                .orElse(false);
    }

    /**
     * 获取特性的配置
     */
    default Optional<Object> getFeatureConfig(String name, String configKey) {
        return findByName(name)
                .map(Feature::getConfigs)
                .map(configs -> configs.get(configKey));
    }

    /**
     * 检查特性是否已废弃
     */
    default boolean isFeatureDeprecated(String name) {
        return findByName(name)
                .map(Feature::isDeprecated)
                .orElse(false);
    }

    // ==================== 注意：JPA Repository 不支持 Reactive 类型 ====================
    // 如需 Reactive 支持，请使用 R2DBC 或在 Service 层进行转换

    // ==================== 复杂查询 ====================

    /**
     * 查找可配置的用户特性
     * 排除系统级和自动分配的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name IN :configurableFeatures AND f.enabled = true AND f.deprecatedVersion = 0")
    List<Feature> findConfigurableUserFeatures(@Param("configurableFeatures") List<String> configurableFeatures);

    /**
     * 查找可在工作区配置的特性
     */
    @Query("SELECT f FROM Feature f WHERE f.name IN :configurableFeatures AND f.enabled = true AND f.deprecatedVersion = 0")
    List<Feature> findConfigurableWorkspaceFeatures(@Param("configurableFeatures") List<String> configurableFeatures);

    /**
     * 查找特定环境的特性 (自托管 vs 云端)
     */
    @Query(value = "SELECT * FROM features WHERE JSON_EXTRACT(configs, '$.environment') = :environment OR JSON_EXTRACT(configs, '$.environment') IS NULL", nativeQuery = true)
    List<Feature> findByEnvironment(@Param("environment") String environment);
}