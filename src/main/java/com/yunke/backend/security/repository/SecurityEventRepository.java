package com.yunke.backend.security.repository;

import com.yunke.backend.system.domain.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全事件Repository
 */
@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    
    /**
     * 根据事件类型查询
     */
    Page<SecurityEvent> findByEventTypeOrderByEventTimeDesc(String eventType, Pageable pageable);
    
    /**
     * 根据严重程度查询
     */
    Page<SecurityEvent> findBySeverityOrderByEventTimeDesc(String severity, Pageable pageable);
    
    /**
     * 根据用户查询
     */
    Page<SecurityEvent> findByUsernameOrderByEventTimeDesc(String username, Pageable pageable);
    
    /**
     * 根据IP地址查询
     */
    Page<SecurityEvent> findBySourceIpOrderByEventTimeDesc(String sourceIp, Pageable pageable);
    
    /**
     * 根据时间范围查询
     */
    @Query("SELECT s FROM SecurityEvent s WHERE s.eventTime BETWEEN :startTime AND :endTime ORDER BY s.eventTime DESC")
    Page<SecurityEvent> findByEventTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
    
    /**
     * 查询未处理的事件
     */
    Page<SecurityEvent> findByHandledFalseOrderByEventTimeDesc(Pageable pageable);
    
    /**
     * 根据多条件查询
     */
    @Query("SELECT s FROM SecurityEvent s WHERE " +
           "(:eventType IS NULL OR s.eventType = :eventType) AND " +
           "(:severity IS NULL OR s.severity = :severity) AND " +
           "(:handled IS NULL OR s.handled = :handled) AND " +
           "(:sourceIp IS NULL OR s.sourceIp = :sourceIp) AND " +
           "s.eventTime BETWEEN :startTime AND :endTime " +
           "ORDER BY s.eventTime DESC")
    Page<SecurityEvent> findByFilters(
            @Param("eventType") String eventType,
            @Param("severity") String severity,
            @Param("handled") Boolean handled,
            @Param("sourceIp") String sourceIp,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
    
    /**
     * 统计指定时间范围内的事件数量
     */
    @Query("SELECT COUNT(s) FROM SecurityEvent s WHERE s.eventTime BETWEEN :startTime AND :endTime")
    long countByEventTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据严重程度统计
     */
    @Query("SELECT s.severity, COUNT(s) FROM SecurityEvent s WHERE s.eventTime BETWEEN :startTime AND :endTime GROUP BY s.severity")
    List<Object[]> countBySeverityAndEventTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据事件类型统计
     */
    @Query("SELECT s.eventType, COUNT(s) FROM SecurityEvent s WHERE s.eventTime BETWEEN :startTime AND :endTime GROUP BY s.eventType")
    List<Object[]> countByEventTypeAndEventTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计IP访问频次
     */
    @Query("SELECT s.sourceIp, COUNT(s) FROM SecurityEvent s WHERE s.eventTime BETWEEN :startTime AND :endTime GROUP BY s.sourceIp ORDER BY COUNT(s) DESC")
    List<Object[]> getTopSourceIps(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);
    
    /**
     * 统计国家分布
     */
    @Query("SELECT s.country, COUNT(s) FROM SecurityEvent s WHERE s.country IS NOT NULL AND s.eventTime BETWEEN :startTime AND :endTime GROUP BY s.country ORDER BY COUNT(s) DESC")
    List<Object[]> getCountryDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定时间之前的事件
     */
    void deleteByEventTimeBefore(LocalDateTime cutoffTime);
}