package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.ConfigOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置操作日志Repository
 */
@Repository
public interface ConfigOperationLogRepository extends JpaRepository<ConfigOperationLog, Long> {
    
    /**
     * 根据模块名称查询日志
     */
    Page<ConfigOperationLog> findByModuleNameOrderByOperationTimeDesc(
            String moduleName, Pageable pageable);
    
    /**
     * 根据操作用户查询日志
     */
    Page<ConfigOperationLog> findByOperatorOrderByOperationTimeDesc(
            String operator, Pageable pageable);
    
    /**
     * 根据操作类型查询日志
     */
    Page<ConfigOperationLog> findByOperationTypeOrderByOperationTimeDesc(
            String operationType, Pageable pageable);
    
    /**
     * 根据时间范围查询日志
     */
    @Query("SELECT c FROM ConfigOperationLog c WHERE c.operationTime BETWEEN :startTime AND :endTime ORDER BY c.operationTime DESC")
    Page<ConfigOperationLog> findByOperationTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
    
    /**
     * 根据模块名称和配置键查询最近的日志
     */
    List<ConfigOperationLog> findTop10ByModuleNameAndConfigKeyOrderByOperationTimeDesc(
            String moduleName, String configKey);
    
    /**
     * 查询指定模块的最近N条日志
     */
    @Query("SELECT c FROM ConfigOperationLog c WHERE (:moduleName IS NULL OR c.moduleName = :moduleName) ORDER BY c.operationTime DESC")
    Page<ConfigOperationLog> findRecentLogs(@Param("moduleName") String moduleName, Pageable pageable);
    
    /**
     * 统计指定时间范围内的操作次数
     */
    @Query("SELECT COUNT(c) FROM ConfigOperationLog c WHERE c.operationTime BETWEEN :startTime AND :endTime")
    long countOperationsBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定时间之前的日志（用于日志清理）
     */
    void deleteByOperationTimeBefore(LocalDateTime cutoffTime);
}