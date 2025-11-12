package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档记录Repository - 完全参考AFFiNE的文档存储实现
 */
@Repository
public interface DocRecordRepository extends JpaRepository<DocRecord, String> {
    
    /**
     * 根据空间ID和文档ID查找文档
     * 对应AFFiNE的getDocSnapshot方法
     */
    Optional<DocRecord> findBySpaceIdAndDocId(String spaceId, String docId);
    
    /**
     * 检查文档是否存在
     * 对应AFFiNE中的文档存在性检查
     */
    boolean existsBySpaceIdAndDocId(String spaceId, String docId);
    
    /**
     * 获取空间内所有文档的时间戳
     * 对应AFFiNE的getSpaceDocTimestamps方法
     */
    @Query("SELECT d.docId, d.timestamp FROM DocRecord d WHERE d.spaceId = :spaceId " +
           "AND (:after IS NULL OR d.timestamp > :after)")
    Map<String, Long> findTimestampsBySpaceId(@Param("spaceId") String spaceId, 
                                              @Param("after") Long after);
    
    /**
     * 删除空间内所有文档
     * 对应AFFiNE的deleteSpace方法
     */
    @Modifying
    @Query("DELETE FROM DocRecord d WHERE d.spaceId = :spaceId")
    void deleteAllBySpaceId(@Param("spaceId") String spaceId);
    
    /**
     * 删除指定文档
     * 对应AFFiNE的deleteDoc方法
     */
    List<DocRecord> findAllBySpaceId(String spaceId);

    @Modifying
    @Query("DELETE FROM DocRecord d WHERE d.spaceId = :spaceId AND d.docId = :docId")
    void deleteBySpaceIdAndDocId(@Param("spaceId") String spaceId, @Param("docId") String docId);
}