package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentViewRepository extends JpaRepository<DocumentView, Long> {

    /**
     * 根据文档ID查找浏览记录
     */
    Page<DocumentView> findByDocumentId(String documentId, Pageable pageable);

    /**
     * 根据用户ID查找浏览记录
     */
    Page<DocumentView> findByUserId(String userId, Pageable pageable);

    /**
     * 根据文档ID和用户ID查找浏览记录
     */
    List<DocumentView> findByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 根据IP地址查找浏览记录
     */
    List<DocumentView> findByIpAddress(String ipAddress);

    /**
     * 查找指定时间范围内的浏览记录
     */
    @Query("SELECT dv FROM DocumentView dv WHERE dv.viewedAt BETWEEN :startDate AND :endDate")
    List<DocumentView> findByViewedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计文档的浏览次数
     */
    @Query("SELECT COUNT(dv) FROM DocumentView dv WHERE dv.documentId = :documentId")
    Long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计用户的浏览次数
     */
    @Query("SELECT COUNT(dv) FROM DocumentView dv WHERE dv.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 统计文档在指定时间范围内的浏览次数
     */
    @Query("SELECT COUNT(dv) FROM DocumentView dv WHERE dv.documentId = :documentId AND dv.viewedAt BETWEEN :startDate AND :endDate")
    Long countByDocumentIdAndViewedAtBetween(
        @Param("documentId") String documentId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计文档的独立访客数（按用户ID去重）
     */
    @Query("SELECT COUNT(DISTINCT dv.userId) FROM DocumentView dv WHERE dv.documentId = :documentId AND dv.userId IS NOT NULL")
    Long countDistinctUsersByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计文档的独立访客数（按IP地址去重，包含匿名用户）
     */
    @Query("SELECT COUNT(DISTINCT dv.ipAddress) FROM DocumentView dv WHERE dv.documentId = :documentId")
    Long countDistinctIpsByDocumentId(@Param("documentId") String documentId);

    /**
     * 查询文档的平均浏览时长
     */
    @Query("SELECT AVG(dv.viewDuration) FROM DocumentView dv WHERE dv.documentId = :documentId AND dv.viewDuration > 0")
    Double averageViewDurationByDocumentId(@Param("documentId") String documentId);

    /**
     * 查询热门文档（按浏览次数排序）
     */
    @Query("SELECT dv.documentId, COUNT(dv) as viewCount FROM DocumentView dv " +
           "WHERE dv.viewedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY dv.documentId ORDER BY viewCount DESC")
    List<Object[]> findMostViewedDocuments(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * 删除指定时间之前的浏览记录（数据归档）
     */
    void deleteByViewedAtBefore(LocalDateTime date);

    /**
     * 删除文档的所有浏览记录
     */
    void deleteByDocumentId(String documentId);
}
