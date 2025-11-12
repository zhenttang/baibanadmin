package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档历史Repository - 完全参考AFFiNE的文档历史存储实现
 */
@Repository
public interface DocHistoryRepository extends JpaRepository<DocHistory, String> {
    
    /**
     * 获取指定文档的历史记录列表
     * 对应AFFiNE的listDocHistories方法
     */
    @Query("SELECT h FROM DocHistory h WHERE h.spaceId = :spaceId AND h.docId = :docId " +
           "AND (:before IS NULL OR h.timestamp < :before) " +
           "ORDER BY h.timestamp DESC")
    List<DocHistory> findHistories(@Param("spaceId") String spaceId,
                                  @Param("docId") String docId,
                                  @Param("before") Long before,
                                  Pageable pageable);
    
    /**
     * 获取指定时间戳的历史记录
     * 对应AFFiNE的getDocHistory方法
     */
    Optional<DocHistory> findBySpaceIdAndDocIdAndTimestamp(String spaceId, String docId, Long timestamp);
    
    /**
     * 获取指定文档的所有历史记录
     */
    List<DocHistory> findBySpaceIdAndDocId(String spaceId, String docId);
    
    /**
     * 获取文档的最新历史记录
     * 对应AFFiNE的lastDocHistory方法
     */
    @Query("SELECT h FROM DocHistory h WHERE h.spaceId = :spaceId AND h.docId = :docId " +
           "ORDER BY h.timestamp DESC LIMIT 1")
    Optional<DocHistory> findLatestHistory(@Param("spaceId") String spaceId, 
                                         @Param("docId") String docId);
    
    /**
     * 删除过期的历史记录
     * 用于定期清理过期数据
     */
    @Modifying
    @Query("DELETE FROM DocHistory h WHERE h.expiresAt < :now")
    int deleteExpiredHistories(@Param("now") LocalDateTime now);
    
    @Query("SELECT h FROM DocHistory h WHERE h.expiresAt < :now")
    List<DocHistory> findExpiredHistories(@Param("now") LocalDateTime now);
    
    /**
     * 删除空间内所有历史记录
     */
    @Modifying
    @Query("DELETE FROM DocHistory h WHERE h.spaceId = :spaceId")
    void deleteAllBySpaceId(@Param("spaceId") String spaceId);
    
    /**
     * 根据空间ID获取所有历史记录
     */
    List<DocHistory> findAllBySpaceId(String spaceId);
    
    /**
     * 删除指定文档的所有历史记录
     */
    @Modifying
    @Query("DELETE FROM DocHistory h WHERE h.spaceId = :spaceId AND h.docId = :docId")
    void deleteBySpaceIdAndDocId(@Param("spaceId") String spaceId, @Param("docId") String docId);
}