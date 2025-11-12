package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档更新Repository - 完全参考AFFiNE的文档更新存储实现
 */
@Repository
public interface DocUpdateRepository extends JpaRepository<DocUpdate, String> {
    
    /**
     * 查找指定文档的所有未合并更新
     * 对应AFFiNE的getDocUpdates方法
     */
    @Query("SELECT u FROM DocUpdate u WHERE u.spaceId = :spaceId AND u.docId = :docId " +
           "AND u.merged = false ORDER BY u.timestamp ASC")
    List<DocUpdate> findUnmergedUpdates(@Param("spaceId") String spaceId, 
                                       @Param("docId") String docId);
    
    /**
     * 标记更新为已合并
     * 对应AFFiNE的markUpdatesMerged方法
     */
    @Modifying
    @Query("UPDATE DocUpdate u SET u.merged = true WHERE u.spaceId = :spaceId " +
           "AND u.docId = :docId AND u.timestamp IN :timestamps")
    int markUpdatesMerged(@Param("spaceId") String spaceId, 
                         @Param("docId") String docId, 
                         @Param("timestamps") List<Long> timestamps);
    
    /**
     * 删除已合并的更新
     * 用于清理已处理的更新记录
     */
    @Modifying
    @Query("DELETE FROM DocUpdate u WHERE u.spaceId = :spaceId AND u.docId = :docId " +
           "AND u.merged = true AND u.timestamp IN :timestamps")
    int deleteByTimestamps(@Param("spaceId") String spaceId, 
                          @Param("docId") String docId, 
                          @Param("timestamps") List<Long> timestamps);
    
    /**
     * 删除空间内所有更新
     */
    @Modifying
    @Query("DELETE FROM DocUpdate u WHERE u.spaceId = :spaceId")
    void deleteAllBySpaceId(@Param("spaceId") String spaceId);
    
    /**
     * 查询指定文档的所有更新（包括已合并）
     */
    List<DocUpdate> findBySpaceIdAndDocId(String spaceId, String docId);

    /**
     * 根据空间ID获取所有更新
     */
    List<DocUpdate> findAllBySpaceId(String spaceId);

    /**
     * 删除指定文档的所有更新
     */
    @Modifying
    @Query("DELETE FROM DocUpdate u WHERE u.spaceId = :spaceId AND u.docId = :docId")
    void deleteBySpaceIdAndDocId(@Param("spaceId") String spaceId, @Param("docId") String docId);
}