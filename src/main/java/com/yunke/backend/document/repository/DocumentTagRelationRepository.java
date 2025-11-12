package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentTagRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档标签关联Repository接口
 */
@Repository
public interface DocumentTagRelationRepository extends JpaRepository<DocumentTagRelation, Long> {

    /**
     * 根据文档ID查找所有标签关联
     */
    List<DocumentTagRelation> findByDocumentId(String documentId);

    /**
     * 根据文档ID和实体类型查找所有标签关联
     */
    List<DocumentTagRelation> findByDocumentIdAndEntityType(String documentId, String entityType);

    /**
     * 根据标签ID查找所有文档关联
     */
    List<DocumentTagRelation> findByTagId(Integer tagId);

    /**
     * 根据标签ID与实体类型查找关联
     */
    List<DocumentTagRelation> findByTagIdAndEntityType(Integer tagId, String entityType);

    /**
     * 根据文档ID和标签ID查找关联
     */
    DocumentTagRelation findByDocumentIdAndTagId(String documentId, Integer tagId);

    /**
     * 根据文档ID、标签ID与实体类型查找关联
     */
    DocumentTagRelation findByDocumentIdAndTagIdAndEntityType(String documentId, Integer tagId, String entityType);

    /**
     * 检查文档和标签是否已关联
     */
    boolean existsByDocumentIdAndTagId(String documentId, Integer tagId);

    /**
     * 检查文档和标签是否已关联（按实体类型）
     */
    boolean existsByDocumentIdAndTagIdAndEntityType(String documentId, Integer tagId, String entityType);

    /**
     * 删除文档的所有标签关联
     */
    void deleteByDocumentId(String documentId);

    /**
     * 删除指定实体类型的文档的所有标签关联
     */
    void deleteByDocumentIdAndEntityType(String documentId, String entityType);

    /**
     * 删除标签的所有文档关联
     */
    void deleteByTagId(Integer tagId);

    /**
     * 删除指定文档和标签的关联
     */
    void deleteByDocumentIdAndTagId(String documentId, Integer tagId);

    /**
     * 删除指定文档、标签、实体类型的关联
     */
    void deleteByDocumentIdAndTagIdAndEntityType(String documentId, Integer tagId, String entityType);

    /**
     * 统计文档的标签数量
     */
    @Query("SELECT COUNT(dtr) FROM DocumentTagRelation dtr WHERE dtr.documentId = :documentId")
    Long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计指定实体类型下文档的标签数量
     */
    @Query("SELECT COUNT(dtr) FROM DocumentTagRelation dtr WHERE dtr.documentId = :documentId AND dtr.entityType = :entityType")
    Long countByDocumentIdAndEntityType(@Param("documentId") String documentId, @Param("entityType") String entityType);

    /**
     * 统计标签关联的文档数量
     */
    @Query("SELECT COUNT(dtr) FROM DocumentTagRelation dtr WHERE dtr.tagId = :tagId")
    Long countByTagId(@Param("tagId") Integer tagId);

    /**
     * 统计指定实体类型下标签关联的文档数量
     */
    @Query("SELECT COUNT(dtr) FROM DocumentTagRelation dtr WHERE dtr.tagId = :tagId AND dtr.entityType = :entityType")
    Long countByTagIdAndEntityType(@Param("tagId") Integer tagId, @Param("entityType") String entityType);

    /**
     * 查找包含指定标签的所有文档ID
     */
    @Query("SELECT dtr.documentId FROM DocumentTagRelation dtr WHERE dtr.tagId = :tagId")
    List<String> findDocumentIdsByTagId(@Param("tagId") Integer tagId);

    /**
     * 查找包含指定标签的所有文档ID（按实体类型）
     */
    @Query("SELECT dtr.documentId FROM DocumentTagRelation dtr WHERE dtr.tagId = :tagId AND dtr.entityType = :entityType")
    List<String> findDocumentIdsByTagIdAndEntityType(@Param("tagId") Integer tagId, @Param("entityType") String entityType);

    /**
     * 分页查找包含指定标签的文档ID（按实体类型）
     */
    @Query("SELECT dtr.documentId FROM DocumentTagRelation dtr WHERE dtr.tagId = :tagId AND dtr.entityType = :entityType")
    org.springframework.data.domain.Page<String> findDocumentIdsByTagIdAndEntityType(@Param("tagId") Integer tagId, @Param("entityType") String entityType, org.springframework.data.domain.Pageable pageable);

    /**
     * 查找包含任意指定标签的文档ID
     */
    @Query("SELECT DISTINCT dtr.documentId FROM DocumentTagRelation dtr WHERE dtr.tagId IN :tagIds")
    List<String> findDocumentIdsByTagIds(@Param("tagIds") List<Integer> tagIds);

    /**
     * 查找同时包含所有指定标签的文档ID
     */
    @Query("SELECT dtr.documentId FROM DocumentTagRelation dtr WHERE dtr.tagId IN :tagIds " +
           "GROUP BY dtr.documentId HAVING COUNT(DISTINCT dtr.tagId) = :tagCount")
    List<String> findDocumentIdsContainingAllTags(@Param("tagIds") List<Integer> tagIds, @Param("tagCount") long tagCount);
}
