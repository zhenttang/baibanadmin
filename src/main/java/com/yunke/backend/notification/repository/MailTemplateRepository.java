package com.yunke.backend.notification.repository;

import com.yunke.backend.notification.domain.entity.MailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 邮件模板Repository
 * 对应Node.js版本的邮件模板管理
 * 参考: /packages/backend/server/src/mails/
 */
@Repository
public interface MailTemplateRepository extends JpaRepository<MailTemplate, Integer> {

    // ==================== 基础查询 ====================

    /**
     * 根据模板名称查询模板
     */
    Optional<MailTemplate> findByName(String name);

    /**
     * 根据模板名称和版本查询模板
     */
    Optional<MailTemplate> findByNameAndVersion(String name, Integer version);

    /**
     * 查询启用的模板
     */
    List<MailTemplate> findByEnabledTrue();

    /**
     * 根据分类查询模板
     */
    List<MailTemplate> findByCategory(MailTemplate.TemplateCategory category);

    /**
     * 根据分类查询启用的模板
     */
    List<MailTemplate> findByCategoryAndEnabledTrue(MailTemplate.TemplateCategory category);

    /**
     * 根据语言查询模板
     */
    List<MailTemplate> findByLanguage(String language);

    /**
     * 根据语言查询启用的模板
     */
    List<MailTemplate> findByLanguageAndEnabledTrue(String language);

    // ==================== 模糊查询 ====================

    /**
     * 根据显示名称模糊查询
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.displayName LIKE %:displayName%")
    List<MailTemplate> findByDisplayNameContaining(@Param("displayName") String displayName);

    /**
     * 根据描述模糊查询
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.description LIKE %:keyword%")
    List<MailTemplate> findByDescriptionContaining(@Param("keyword") String keyword);

    /**
     * 综合搜索模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE " +
           "t.name LIKE %:keyword% OR " +
           "t.displayName LIKE %:keyword% OR " +
           "t.description LIKE %:keyword%")
    Page<MailTemplate> searchTemplates(@Param("keyword") String keyword, Pageable pageable);

    // ==================== 版本管理 ====================

    /**
     * 查询指定模板的最新版本
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = :name " +
           "ORDER BY t.version DESC")
    Optional<MailTemplate> findLatestVersionByName(@Param("name") String name);

    /**
     * 查询指定模板的所有版本
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = :name " +
           "ORDER BY t.version DESC")
    List<MailTemplate> findAllVersionsByName(@Param("name") String name);

    /**
     * 查询指定模板的最高版本号
     */
    @Query("SELECT MAX(t.version) FROM MailTemplate t WHERE t.name = :name")
    Optional<Integer> findMaxVersionByName(@Param("name") String name);

    /**
     * 检查模板名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查指定版本的模板是否存在
     */
    boolean existsByNameAndVersion(String name, Integer version);

    // ==================== 分类统计 ====================

    /**
     * 统计各分类的模板数量
     */
    @Query("SELECT t.category, COUNT(t) FROM MailTemplate t GROUP BY t.category")
    List<Object[]> countByCategory();

    /**
     * 统计启用的模板数量
     */
    @Query("SELECT COUNT(t) FROM MailTemplate t WHERE t.enabled = true")
    Long countEnabledTemplates();

    /**
     * 统计各语言的模板数量
     */
    @Query("SELECT t.language, COUNT(t) FROM MailTemplate t GROUP BY t.language")
    List<Object[]> countByLanguage();

    /**
     * 统计各分类的启用模板数量
     */
    @Query("SELECT t.category, COUNT(t) FROM MailTemplate t WHERE t.enabled = true " +
           "GROUP BY t.category")
    List<Object[]> countEnabledByCategory();

    // ==================== 复杂查询 ====================

    /**
     * 查询用户相关的启用模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.category = 'USER' " +
           "AND t.enabled = true ORDER BY t.displayName")
    List<MailTemplate> findEnabledUserTemplates();

    /**
     * 查询工作空间相关的启用模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.category = 'WORKSPACE' " +
           "AND t.enabled = true ORDER BY t.displayName")
    List<MailTemplate> findEnabledWorkspaceTemplates();

    /**
     * 查询团队相关的启用模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.category = 'TEAM' " +
           "AND t.enabled = true ORDER BY t.displayName")
    List<MailTemplate> findEnabledTeamTemplates();

    /**
     * 查询文档相关的启用模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.category = 'DOCUMENT' " +
           "AND t.enabled = true ORDER BY t.displayName")
    List<MailTemplate> findEnabledDocumentTemplates();

    /**
     * 查询系统相关的启用模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.category = 'SYSTEM' " +
           "AND t.enabled = true ORDER BY t.displayName")
    List<MailTemplate> findEnabledSystemTemplates();

    /**
     * 查询通知相关的启用模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.category = 'NOTIFICATION' " +
           "AND t.enabled = true ORDER BY t.displayName")
    List<MailTemplate> findEnabledNotificationTemplates();

    /**
     * 根据分类、语言和启用状态查询模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE " +
           "(:category IS NULL OR t.category = :category) AND " +
           "(:language IS NULL OR t.language = :language) AND " +
           "(:enabled IS NULL OR t.enabled = :enabled) " +
           "ORDER BY t.category, t.displayName")
    Page<MailTemplate> findTemplatesByCriteria(@Param("category") MailTemplate.TemplateCategory category,
                                              @Param("language") String language,
                                              @Param("enabled") Boolean enabled,
                                              Pageable pageable);

    /**
     * 查询最近创建的模板
     */
    @Query("SELECT t FROM MailTemplate t ORDER BY t.createdAt DESC")
    Page<MailTemplate> findRecentTemplates(Pageable pageable);

    /**
     * 查询最近更新的模板
     */
    @Query("SELECT t FROM MailTemplate t ORDER BY t.updatedAt DESC")
    Page<MailTemplate> findRecentlyUpdatedTemplates(Pageable pageable);

    /**
     * 查询指定创建者的模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.createdBy = :createdBy " +
           "ORDER BY t.createdAt DESC")
    List<MailTemplate> findByCreatedBy(@Param("createdBy") String createdBy);

    // ==================== 预定义模板查询 ====================

    /**
     * 查询用户注册模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = 'user-sign-up' " +
           "AND t.enabled = true ORDER BY t.version DESC")
    Optional<MailTemplate> findUserSignUpTemplate();

    /**
     * 查询密码重置模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = 'user-password-change' " +
           "AND t.enabled = true ORDER BY t.version DESC")
    Optional<MailTemplate> findPasswordResetTemplate();

    /**
     * 查询邮箱验证模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = 'user-email-verify' " +
           "AND t.enabled = true ORDER BY t.version DESC")
    Optional<MailTemplate> findEmailVerifyTemplate();

    /**
     * 查询工作空间邀请模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = 'workspace-invitation' " +
           "AND t.enabled = true ORDER BY t.version DESC")
    Optional<MailTemplate> findWorkspaceInvitationTemplate();

    /**
     * 查询文档提及模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = 'document-mention' " +
           "AND t.enabled = true ORDER BY t.version DESC")
    Optional<MailTemplate> findDocumentMentionTemplate();

    // ==================== 高级功能 ====================

    /**
     * 根据模板名称获取最新启用版本
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name = :name " +
           "AND t.enabled = true ORDER BY t.version DESC")
    Optional<MailTemplate> findLatestEnabledVersionByName(@Param("name") String name);

    /**
     * 查询需要更新的旧版本模板
     */
    @Query("SELECT t FROM MailTemplate t WHERE t.name IN " +
           "(SELECT DISTINCT t2.name FROM MailTemplate t2 WHERE t2.version > t.version) " +
           "ORDER BY t.name, t.version")
    List<MailTemplate> findOutdatedTemplates();

    /**
     * 查询重复的模板名称
     */
    @Query("SELECT t.name FROM MailTemplate t GROUP BY t.name HAVING COUNT(t) > 1")
    List<String> findDuplicateTemplateNames();
}