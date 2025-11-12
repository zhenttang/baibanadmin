package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.SnapshotHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SnapshotHistoryRepository extends JpaRepository<SnapshotHistory, SnapshotHistory.SnapshotHistoryId> {

    /**
     * 根据工作空间ID和文档ID查询历史记录，按时间倒序
     */
    @Query("SELECT sh FROM SnapshotHistory sh WHERE sh.workspaceId = :workspaceId AND sh.id = :pageDocId ORDER BY sh.timestamp DESC")
    Page<SnapshotHistory> findByWorkspaceIdAndIdOrderByTimestampDesc(
            @Param("workspaceId") String workspaceId,
            @Param("pageDocId") String pageDocId,
            Pageable pageable
    );

    /**
     * 根据工作空间ID和文档ID查询历史记录，支持时间范围过滤
     */
    @Query("SELECT sh FROM SnapshotHistory sh WHERE sh.workspaceId = :workspaceId AND sh.id = :pageDocId AND sh.timestamp < :before ORDER BY sh.timestamp DESC")
    Page<SnapshotHistory> findByWorkspaceIdAndIdAndTimestampBeforeOrderByTimestampDesc(
            @Param("workspaceId") String workspaceId,
            @Param("pageDocId") String pageDocId,
            @Param("before") Long before,
            Pageable pageable
    );

    /**
     * 根据工作空间ID、文档ID和时间戳查询特定历史记录
     */
    @Query("SELECT sh FROM SnapshotHistory sh WHERE sh.workspaceId = :workspaceId AND sh.id = :pageDocId AND sh.timestamp = :timestamp")
    Optional<SnapshotHistory> findByWorkspaceIdAndIdAndTimestamp(
            @Param("workspaceId") String workspaceId,
            @Param("pageDocId") String pageDocId,
            @Param("timestamp") Long timestamp
    );

    /**
     * 统计某个文档的历史记录总数
     */
    @Query("SELECT COUNT(sh) FROM SnapshotHistory sh WHERE sh.workspaceId = :workspaceId AND sh.id = :pageDocId")
    long countByWorkspaceIdAndId(
            @Param("workspaceId") String workspaceId,
            @Param("pageDocId") String pageDocId
    );

    /**
     * 删除过期的历史记录
     */
    @Query("DELETE FROM SnapshotHistory sh WHERE sh.expiredAt < :now")
    void deleteExpiredHistories(@Param("now") LocalDateTime now);

    @Query("SELECT sh FROM SnapshotHistory sh WHERE sh.expiredAt < :now")
    java.util.List<SnapshotHistory> findExpiredHistories(@Param("now") LocalDateTime now);

    /**
     * 查询最新的历史记录
     */
    @Query("SELECT sh FROM SnapshotHistory sh WHERE sh.workspaceId = :workspaceId AND sh.id = :pageDocId ORDER BY sh.timestamp DESC LIMIT 1")
    Optional<SnapshotHistory> findLatestByWorkspaceIdAndId(
            @Param("workspaceId") String workspaceId,
            @Param("pageDocId") String pageDocId
    );

    /**
     * 查询某个时间点之前的最新历史记录
     */
    @Query("SELECT sh FROM SnapshotHistory sh WHERE sh.workspaceId = :workspaceId AND sh.id = :pageDocId AND sh.timestamp <= :timestamp ORDER BY sh.timestamp DESC LIMIT 1")
    Optional<SnapshotHistory> findLatestByWorkspaceIdAndIdBeforeTimestamp(
            @Param("workspaceId") String workspaceId,
            @Param("pageDocId") String pageDocId,
            @Param("timestamp") Long timestamp
    );
}
