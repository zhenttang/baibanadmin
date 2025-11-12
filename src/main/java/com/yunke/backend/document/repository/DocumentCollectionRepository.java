package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档收藏Repository接口
 */
@Repository
public interface DocumentCollectionRepository extends JpaRepository<DocumentCollection, Long> {

    /**
     * 根据文档ID和用户ID查找收藏记录
     */
    DocumentCollection findByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 检查用户是否已收藏文档
     */
    boolean existsByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 根据用户ID查找所有收藏记录
     */
    List<DocumentCollection> findByUserId(String userId);

    /**
     * 根据文档ID查找所有收藏记录
     */
    List<DocumentCollection> findByDocumentId(String documentId);

    /**
     * 根据用户ID和收藏夹ID查找收藏记录
     */
    List<DocumentCollection> findByUserIdAndFolderId(String userId, Integer folderId);

    /**
     * 根据用户ID查找所有收藏夹ID
     */
    @Query("SELECT DISTINCT dc.folderId FROM DocumentCollection dc WHERE dc.userId = :userId")
    List<Integer> findFolderIdsByUserId(@Param("userId") String userId);

    /**
     * 删除指定文档和用户的收藏记录
     */
    void deleteByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 删除文档的所有收藏记录
     */
    void deleteByDocumentId(String documentId);

    /**
     * 删除用户指定收藏夹的所有记录
     */
    void deleteByUserIdAndFolderId(String userId, Integer folderId);

    /**
     * 统计文档的收藏数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentCollection dc WHERE dc.documentId = :documentId")
    Long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计用户的收藏数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentCollection dc WHERE dc.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 统计用户指定收藏夹的文档数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentCollection dc WHERE dc.userId = :userId AND dc.folderId = :folderId")
    Long countByUserIdAndFolderId(@Param("userId") String userId, @Param("folderId") Integer folderId);

    /**
     * 查找用户收藏的所有文档ID
     */
    @Query("SELECT dc.documentId FROM DocumentCollection dc WHERE dc.userId = :userId")
    List<String> findDocumentIdsByUserId(@Param("userId") String userId);

    /**
     * 查找收藏指定文档的所有用户ID
     */
    @Query("SELECT dc.userId FROM DocumentCollection dc WHERE dc.documentId = :documentId")
    List<String> findUserIdsByDocumentId(@Param("documentId") String documentId);
}