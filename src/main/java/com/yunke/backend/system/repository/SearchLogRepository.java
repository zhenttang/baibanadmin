package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.SearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索日志Repository接口
 */
@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 根据用户ID查找搜索记录
     */
    List<SearchLog> findByUserId(String userId);

    /**
     * 根据用户ID分页查找搜索记录
     */
    Page<SearchLog> findByUserId(String userId, Pageable pageable);

    /**
     * 根据搜索类型查找记录
     */
    List<SearchLog> findBySearchType(SearchLog.SearchType searchType);

    /**
     * 根据时间范围查找记录
     */
    List<SearchLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID和时间范围查找记录
     */
    List<SearchLog> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找热门搜索关键词
     */
    @Query("SELECT sl.searchKeyword, COUNT(sl) as searchCount FROM SearchLog sl " +
           "WHERE sl.createdAt >= :startTime " +
           "GROUP BY sl.searchKeyword " +
           "ORDER BY searchCount DESC")
    List<Object[]> findPopularKeywords(@Param("startTime") LocalDateTime startTime, Pageable pageable);

    /**
     * 查找用户的搜索历史关键词
     */
    @Query("SELECT DISTINCT sl.searchKeyword FROM SearchLog sl " +
           "WHERE sl.userId = :userId " +
           "ORDER BY sl.createdAt DESC")
    List<String> findUserSearchHistory(@Param("userId") String userId, Pageable pageable);

    /**
     * 统计指定关键词的搜索次数
     */
    @Query("SELECT COUNT(sl) FROM SearchLog sl WHERE sl.searchKeyword = :keyword")
    Long countBySearchKeyword(@Param("keyword") String keyword);

    /**
     * 统计指定时间范围内的搜索次数
     */
    @Query("SELECT COUNT(sl) FROM SearchLog sl WHERE sl.createdAt BETWEEN :startTime AND :endTime")
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户的搜索次数
     */
    @Query("SELECT COUNT(sl) FROM SearchLog sl WHERE sl.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 统计各搜索类型的使用次数
     */
    @Query("SELECT sl.searchType, COUNT(sl) FROM SearchLog sl " +
           "WHERE sl.createdAt >= :startTime " +
           "GROUP BY sl.searchType")
    List<Object[]> countBySearchType(@Param("startTime") LocalDateTime startTime);

    /**
     * 查找无结果的搜索记录
     */
    List<SearchLog> findByResultCountEquals(Integer resultCount);

    /**
     * 查找搜索结果较少的记录
     */
    List<SearchLog> findByResultCountLessThan(Integer maxResultCount);

    /**
     * 删除指定时间之前的搜索记录
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);

    /**
     * 查找相似的搜索建议
     */
    @Query("SELECT DISTINCT sl.searchKeyword FROM SearchLog sl " +
           "WHERE LOWER(sl.searchKeyword) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND sl.resultCount > 0 " +
           "ORDER BY sl.createdAt DESC")
    List<String> findSimilarKeywords(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计平均搜索结果数量
     */
    @Query("SELECT AVG(sl.resultCount) FROM SearchLog sl WHERE sl.createdAt >= :startTime")
    Double calculateAverageResultCount(@Param("startTime") LocalDateTime startTime);
}