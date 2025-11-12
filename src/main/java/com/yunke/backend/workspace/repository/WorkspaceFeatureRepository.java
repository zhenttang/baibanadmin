package com.yunke.backend.workspace.repository;

import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工作区特性Repository
 * 对应Node.js版本的WorkspaceFeature数据访问层
 */
@Repository
public interface WorkspaceFeatureRepository extends JpaRepository<WorkspaceFeature, Integer> {

    // ==================== 基础查询 ====================

    /**
     * 查找工作区的所有特性
     */
    List<WorkspaceFeature> findByWorkspaceId(String workspaceId);

    /**
     * 查找工作区的所有激活特性
     */
    List<WorkspaceFeature> findByWorkspaceIdAndActivatedTrue(String workspaceId);

    /**
     * 查找工作区的特定特性
     */
    Optional<WorkspaceFeature> findByWorkspaceIdAndName(String workspaceId, String name);

    /**
     * 查找工作区的特定类型特性
     */
    List<WorkspaceFeature> findByWorkspaceIdAndType(String workspaceId, Integer type);

    /**
     * 查找工作区的激活特性 (按类型)
     */
    List<WorkspaceFeature> findByWorkspaceIdAndTypeAndActivatedTrue(String workspaceId, Integer type);

    // ==================== 特性存在性检查 ====================

    /**
     * 检查工作区是否有特定特性
     */
    boolean existsByWorkspaceIdAndName(String workspaceId, String name);

    /**
     * 检查工作区是否有激活的特定特性
     */
    boolean existsByWorkspaceIdAndNameAndActivatedTrue(String workspaceId, String name);

    /**
     * 检查工作区是否有未过期的特定特性
     */
    @Query("SELECT COUNT(wf) > 0 FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name = :name AND wf.activated = true AND (wf.expiredAt IS NULL OR wf.expiredAt > :now)")
    boolean existsActiveAndNotExpired(@Param("workspaceId") String workspaceId, @Param("name") String name, @Param("now") LocalDateTime now);

    // ==================== 过期处理 ====================

    /**
     * 查找已过期的工作区特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.expiredAt IS NOT NULL AND wf.expiredAt <= :now")
    List<WorkspaceFeature> findExpiredFeatures(@Param("now") LocalDateTime now);

    /**
     * 查找工作区的已过期特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.expiredAt IS NOT NULL AND wf.expiredAt <= :now")
    List<WorkspaceFeature> findExpiredFeaturesByWorkspace(@Param("workspaceId") String workspaceId, @Param("now") LocalDateTime now);

    /**
     * 查找即将过期的特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.expiredAt IS NOT NULL AND wf.expiredAt BETWEEN :now AND :threshold")
    List<WorkspaceFeature> findExpiringFeatures(@Param("workspaceId") String workspaceId, @Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    // ==================== 特性管理 ====================

    /**
     * 激活工作区特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.activated = true WHERE wf.workspaceId = :workspaceId AND wf.name = :name")
    int activateFeature(@Param("workspaceId") String workspaceId, @Param("name") String name);

    /**
     * 停用工作区特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.activated = false WHERE wf.workspaceId = :workspaceId AND wf.name = :name")
    int deactivateFeature(@Param("workspaceId") String workspaceId, @Param("name") String name);

    /**
     * 设置特性过期时间
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.expiredAt = :expiredAt WHERE wf.workspaceId = :workspaceId AND wf.name = :name")
    int setFeatureExpiration(@Param("workspaceId") String workspaceId, @Param("name") String name, @Param("expiredAt") LocalDateTime expiredAt);

    /**
     * 更新特性配置
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.configs = :configs WHERE wf.workspaceId = :workspaceId AND wf.name = :name")
    int updateFeatureConfig(@Param("workspaceId") String workspaceId, @Param("name") String name, @Param("configs") Map<String, Object> configs);

    /**
     * 移除工作区的特定特性
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceIdAndName(String workspaceId, String name);

    /**
     * 移除工作区的所有特性
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceId(String workspaceId);

    // ==================== 团队计划管理 ====================

    /**
     * 查找团队计划工作区
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.name = 'team_plan_v1' AND wf.activated = true")
    List<WorkspaceFeature> findTeamPlanWorkspaces();

    /**
     * 检查工作区是否有团队计划
     */
    @Query("SELECT COUNT(wf) > 0 FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name = 'team_plan_v1' AND wf.activated = true")
    boolean hasTeamPlan(@Param("workspaceId") String workspaceId);

    /**
     * 添加团队计划
     */
    default WorkspaceFeature addTeamPlan(String workspaceId, String reason, Map<String, Object> configs) {
        WorkspaceFeature teamPlan = WorkspaceFeature.builder()
                .workspaceId(workspaceId)
                .name("team_plan_v1")
                .type(1) // QUOTA type
                .reason(reason)
                .configs(configs)
                .activated(true)
                .build();
        return save(teamPlan);
    }

    // ==================== 配额相关查询 ====================

    /**
     * 查找工作区的配额特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.type = 1 AND wf.activated = true")
    List<WorkspaceFeature> findWorkspaceQuotaFeatures(@Param("workspaceId") String workspaceId);

    /**
     * 查找工作区当前的计划特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name LIKE '%_plan_%' AND wf.activated = true")
    List<WorkspaceFeature> findWorkspacePlanFeatures(@Param("workspaceId") String workspaceId);

    /**
     * 获取工作区的配额配置
     */
    @Query("SELECT wf.configs FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name = :planName AND wf.activated = true")
    Optional<Map<String, Object>> getWorkspaceQuotaConfig(@Param("workspaceId") String workspaceId, @Param("planName") String planName);

    // ==================== 企业特性管理 ====================

    /**
     * 查找企业级工作区
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.name IN ('sso_integration', 'audit_logs', 'enterprise_security') AND wf.activated = true")
    List<WorkspaceFeature> findEnterpriseWorkspaces();

    /**
     * 检查工作区是否有企业特性
     */
    @Query("SELECT COUNT(wf) > 0 FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name IN ('sso_integration', 'audit_logs', 'enterprise_security') AND wf.activated = true")
    boolean hasEnterpriseFeatures(@Param("workspaceId") String workspaceId);

    /**
     * 查找有SSO集成的工作区
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.name = 'sso_integration' AND wf.activated = true")
    List<WorkspaceFeature> findSsoEnabledWorkspaces();

    // ==================== 协作特性管理 ====================

    /**
     * 查找有高级协作功能的工作区
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.name IN ('real_time_collaboration', 'advanced_permissions', 'team_management') AND wf.activated = true")
    List<WorkspaceFeature> findAdvancedCollaborationWorkspaces();

    /**
     * 检查工作区是否有实时协作
     */
    @Query("SELECT COUNT(wf) > 0 FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name = 'real_time_collaboration' AND wf.activated = true")
    boolean hasRealTimeCollaboration(@Param("workspaceId") String workspaceId);

    // ==================== 统计查询 ====================

    /**
     * 统计工作区的激活特性数量
     */
    @Query("SELECT COUNT(wf) FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.activated = true")
    Long countActiveFeaturesByWorkspace(@Param("workspaceId") String workspaceId);

    /**
     * 统计特定特性的工作区数量
     */
    @Query("SELECT COUNT(wf) FROM WorkspaceFeature wf WHERE wf.name = :featureName AND wf.activated = true")
    Long countWorkspacesWithFeature(@Param("featureName") String featureName);

    /**
     * 统计各类型特性的工作区数量
     */
    @Query("SELECT wf.type, COUNT(DISTINCT wf.workspaceId) FROM WorkspaceFeature wf WHERE wf.activated = true GROUP BY wf.type")
    List<Object[]> countWorkspacesByFeatureType();

    /**
     * 统计团队计划工作区数量
     */
    @Query("SELECT COUNT(DISTINCT wf.workspaceId) FROM WorkspaceFeature wf WHERE wf.name = 'team_plan_v1' AND wf.activated = true")
    Long countTeamPlanWorkspaces();

    /**
     * 统计企业级工作区数量
     */
    @Query("SELECT COUNT(DISTINCT wf.workspaceId) FROM WorkspaceFeature wf WHERE wf.name IN ('sso_integration', 'audit_logs', 'enterprise_security') AND wf.activated = true")
    Long countEnterpriseWorkspaces();

    // ==================== 批量操作 ====================

    /**
     * 批量查询有配额特性的工作空间
     * 这是性能关键方法，用于QuotaService中的存储计算优化
     */
    @Query("SELECT DISTINCT wf.workspaceId FROM WorkspaceFeature wf WHERE wf.workspaceId IN :workspaceIds AND wf.type = 1 AND wf.activated = true")
    List<String> findWorkspaceIdsWithQuotaFeatures(@Param("workspaceIds") List<String> workspaceIds);

    /**
     * 批量激活工作区特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.activated = true WHERE wf.workspaceId IN :workspaceIds AND wf.name = :featureName")
    int batchActivateFeature(@Param("workspaceIds") List<String> workspaceIds, @Param("featureName") String featureName);

    /**
     * 批量停用工作区特性
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.activated = false WHERE wf.workspaceId IN :workspaceIds AND wf.name = :featureName")
    int batchDeactivateFeature(@Param("workspaceIds") List<String> workspaceIds, @Param("featureName") String featureName);

    /**
     * 批量删除过期特性
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WorkspaceFeature wf WHERE wf.expiredAt IS NOT NULL AND wf.expiredAt <= :now")
    int deleteExpiredFeatures(@Param("now") LocalDateTime now);

    /**
     * 批量更新特性配置
     */
    @Modifying
    @Transactional
    @Query("UPDATE WorkspaceFeature wf SET wf.configs = :configs WHERE wf.workspaceId IN :workspaceIds AND wf.name = :featureName")
    int batchUpdateFeatureConfig(@Param("workspaceIds") List<String> workspaceIds, @Param("featureName") String featureName, @Param("configs") Map<String, Object> configs);

    // ==================== 复杂查询 ====================

    /**
     * 查找工作区可配置的特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.name IN :configurableFeatures")
    List<WorkspaceFeature> findConfigurableFeatures(@Param("workspaceId") String workspaceId, @Param("configurableFeatures") List<String> configurableFeatures);

    /**
     * 查找工作区缺少的必需特性
     */
    @Query("SELECT f.name FROM Feature f WHERE f.name IN :requiredFeatures AND f.name NOT IN (SELECT wf.name FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND wf.activated = true)")
    List<String> findMissingRequiredFeatures(@Param("workspaceId") String workspaceId, @Param("requiredFeatures") List<String> requiredFeatures);

    /**
     * 查找工作区的实验性特性
     */
    @Query("SELECT wf FROM WorkspaceFeature wf WHERE wf.workspaceId = :workspaceId AND (wf.name LIKE '%beta%' OR wf.name LIKE '%experimental%') AND wf.activated = true")
    List<WorkspaceFeature> findExperimentalFeatures(@Param("workspaceId") String workspaceId);

    /**
     * 查找包含特定配置的工作区特性
     */
    @Query(value = "SELECT * FROM workspace_features WHERE workspace_id = :workspaceId AND JSON_EXTRACT(configs, CONCAT('$.', :configKey)) = :configValue", nativeQuery = true)
    List<WorkspaceFeature> findByConfigValue(@Param("workspaceId") String workspaceId, @Param("configKey") String configKey, @Param("configValue") String configValue);

    /**
     * 查找超过席位限制的工作区
     */
    @Query(value = "SELECT wf.* FROM workspace_features wf WHERE wf.name = 'team_plan_v1' AND wf.activated = true AND JSON_EXTRACT(wf.configs, '$.memberLimit') < (SELECT COUNT(*) FROM workspace_user_roles wur WHERE wur.workspace_id = wf.workspace_id)", nativeQuery = true)
    List<WorkspaceFeature> findWorkspacesExceedingSeatLimit();
}