package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.Update;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UpdateRepository extends JpaRepository<Update, Update.UpdateId> {

    /**
     * 根据工作空间ID和文档ID查找更新
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId ORDER BY u.createdAt DESC")
    List<Update> findByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);

    /**
     * 根据工作空间ID和文档ID分页查找更新
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId ORDER BY u.createdAt DESC")
    Page<Update> findByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("docId") String docId, Pageable pageable);

    /**
     * 根据工作空间ID查找所有更新
     */
    List<Update> findByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间ID和时间范围查找更新
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.createdAt BETWEEN :startTime AND :endTime ORDER BY u.createdAt DESC")
    List<Update> findByWorkspaceIdAndCreatedAtBetween(@Param("workspaceId") String workspaceId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 根据工作空间ID、文档ID和时间范围查找更新
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId AND u.createdAt >= :sinceTime ORDER BY u.createdAt ASC")
    List<Update> findByWorkspaceIdAndIdAndCreatedAtAfter(@Param("workspaceId") String workspaceId,
                                                         @Param("docId") String docId,
                                                         @Param("sinceTime") LocalDateTime sinceTime);

    /**
     * 根据创建者查找更新
     */
    Page<Update> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * 根据工作空间ID删除所有更新
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间ID和文档ID删除更新
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId")
    void deleteByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);

    /**
     * 统计工作空间更新数量
     */
    long countByWorkspaceId(String workspaceId);

    /**
     * 统计文档更新数量
     */
    @Query("SELECT COUNT(u) FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId")
    long countByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);

    /**
     * 查找过期更新
     */
    @Query("SELECT u FROM Update u WHERE u.createdAt < :expireTime")
    List<Update> findExpiredUpdates(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 批量删除过期更新
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Update u WHERE u.createdAt < :expireTime")
    void deleteExpiredUpdates(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 查找最近的更新
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId ORDER BY u.createdAt DESC")
    List<Update> findRecentUpdates(@Param("workspaceId") String workspaceId, @Param("docId") String docId, Pageable pageable);

    /**
     * 根据工作空间ID、文档ID和时间戳查找之后的更新 - DatabaseDocReader需要
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId AND u.createdAt > :timestamp ORDER BY u.createdAt ASC")
    List<Update> findByWorkspaceIdAndDocIdAfterTimestamp(@Param("workspaceId") String workspaceId, 
                                                        @Param("docId") String docId, 
                                                        @Param("timestamp") LocalDateTime timestamp);

    /**
     * 查找工作空间和文档的最新更新 - DatabaseDocReader需要
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId ORDER BY u.createdAt DESC")
    List<Update> findTopByWorkspaceIdAndDocId(@Param("workspaceId") String workspaceId, @Param("docId") String docId, Pageable pageable);
    
    /**
     * 查找工作空间和文档的最新更新的包装方法
     */
    default Optional<Update> findLatestByWorkspaceIdAndDocId(String workspaceId, String docId) {
        List<Update> updates = findTopByWorkspaceIdAndDocId(workspaceId, docId, 
                org.springframework.data.domain.PageRequest.of(0, 1));
        return updates.isEmpty() ? Optional.empty() : Optional.of(updates.get(0));
    }
    
    /**
     * 根据工作空间ID和文档ID查找最新更新 - WorkspaceDocServiceImpl需要
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId ORDER BY u.createdAt DESC")
    List<Update> findTopByWorkspaceIdAndIdOrderByCreatedAtDesc(@Param("workspaceId") String workspaceId, @Param("docId") String docId);
    
    /**
     * 根据工作空间ID和文档ID查找最新序号 - WorkspaceDocServiceImpl需要
     */
    @Query("SELECT u FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId ORDER BY u.seq DESC")
    List<Update> findTopByWorkspaceIdAndIdOrderBySeqDesc(@Param("workspaceId") String workspaceId, @Param("docId") String docId);

    @Query("SELECT COALESCE(MAX(u.seq), 0) FROM Update u WHERE u.workspaceId = :workspaceId AND u.id = :docId")
    int findMaxSeqByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);
    
    /**
     * 根据blob字段查找更新（用于获取workspaceId）
     * 用于删除时更新QuotaUsage
     */
    @Query("SELECT u FROM Update u WHERE u.blob = :blob")
    Optional<Update> findByBlob(@Param("blob") byte[] blob);
}
