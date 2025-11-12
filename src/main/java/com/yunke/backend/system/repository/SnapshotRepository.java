package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.Snapshot;
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
public interface SnapshotRepository extends JpaRepository<Snapshot, Snapshot.SnapshotId> {

    /**
     * 根据工作空间ID和文档ID查找快照
     */
    Optional<Snapshot> findByWorkspaceIdAndId(String workspaceId, String id);

    /**
     * 检查工作空间ID和文档ID的快照是否存在
     */
    boolean existsByWorkspaceIdAndId(String workspaceId, String id);

    /**
     * 根据工作空间ID和文档ID分页查找快照
     */
    Page<Snapshot> findByWorkspaceIdAndId(String workspaceId, String id, Pageable pageable);

    /**
     * 根据工作空间ID查找所有快照
     */
    List<Snapshot> findByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间ID和文档ID查找最新快照
     */
    @Query("SELECT s FROM Snapshot s WHERE s.workspaceId = :workspaceId AND s.id = :docId ORDER BY s.updatedAt DESC")
    Optional<Snapshot> findLatestByWorkspaceIdAndId(@Param("workspaceId") String workspaceId, @Param("docId") String docId);

    /**
     * 根据工作空间ID和时间范围查找快照
     */
    @Query("SELECT s FROM Snapshot s WHERE s.workspaceId = :workspaceId AND s.updatedAt BETWEEN :startTime AND :endTime ORDER BY s.updatedAt DESC")
    Page<Snapshot> findByWorkspaceIdAndUpdatedAtBetween(@Param("workspaceId") String workspaceId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime,
                                                        Pageable pageable);

    /**
     * 根据工作空间ID和时间戳查找快照（大于指定时间戳）
     */
    @Query("SELECT s FROM Snapshot s WHERE s.workspaceId = :workspaceId AND s.updatedAt > :timestamp ORDER BY s.updatedAt ASC")
    List<Snapshot> findByWorkspaceIdAndTimestampGreaterThan(@Param("workspaceId") String workspaceId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * 根据创建者查找快照
     */
    Page<Snapshot> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * 根据工作空间ID删除所有快照
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceId(String workspaceId);

    /**
     * 根据工作空间ID和文档ID删除快照
     */
    @Modifying
    @Transactional
    void deleteByWorkspaceIdAndId(String workspaceId, String id);

    /**
     * 统计工作空间快照数量
     */
    long countByWorkspaceId(String workspaceId);

    /**
     * 查找过期快照
     */
    @Query("SELECT s FROM Snapshot s WHERE s.updatedAt < :expireTime")
    List<Snapshot> findExpiredSnapshots(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 批量删除过期快照
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Snapshot s WHERE s.updatedAt < :expireTime")
    void deleteExpiredSnapshots(@Param("expireTime") LocalDateTime expireTime);
    
    /**
     * 根据blob字段查找快照（用于获取workspaceId）
     * 用于删除时更新QuotaUsage
     */
    @Query("SELECT s FROM Snapshot s WHERE s.blob = :blob")
    Optional<Snapshot> findByBlob(@Param("blob") byte[] blob);
}