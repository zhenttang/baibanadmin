package com.yunke.backend.workspace.repository;

import com.yunke.backend.workspace.domain.entity.WorkspaceIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 工作空间ID映射数据访问层
 */
@Repository
public interface WorkspaceIdMappingRepository extends JpaRepository<WorkspaceIdMapping, Long> {
    
    /**
     * 根据短格式ID查找映射
     */
    @Query("SELECT w FROM WorkspaceIdMapping w WHERE w.shortId = :shortId AND w.enabled = true")
    Optional<WorkspaceIdMapping> findByShortIdAndEnabledTrue(@Param("shortId") String shortId);
    
    /**
     * 根据UUID格式ID查找映射
     */
    @Query("SELECT w FROM WorkspaceIdMapping w WHERE w.uuidId = :uuidId AND w.enabled = true")
    Optional<WorkspaceIdMapping> findByUuidIdAndEnabledTrue(@Param("uuidId") String uuidId);
    
    /**
     * 检查短格式ID是否存在
     */
    boolean existsByShortIdAndEnabledTrue(String shortId);
    
    /**
     * 检查UUID格式ID是否存在
     */
    boolean existsByUuidIdAndEnabledTrue(String uuidId);
    
    /**
     * 根据短格式ID获取UUID
     */
    @Query("SELECT w.uuidId FROM WorkspaceIdMapping w WHERE w.shortId = :shortId AND w.enabled = true")
    Optional<String> findUuidByShortId(@Param("shortId") String shortId);
    
    /**
     * 根据UUID获取短格式ID
     */
    @Query("SELECT w.shortId FROM WorkspaceIdMapping w WHERE w.uuidId = :uuidId AND w.enabled = true")
    Optional<String> findShortIdByUuid(@Param("uuidId") String uuidId);
}