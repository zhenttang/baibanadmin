package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档点赞Repository接口
 */
@Repository
public interface DocumentLikeRepository extends JpaRepository<DocumentLike, Long> {

    /**
     * 根据文档ID和用户ID查找点赞记录
     */
    DocumentLike findByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 检查用户是否已点赞文档
     */
    boolean existsByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 根据用户ID查找所有点赞记录
     */
    List<DocumentLike> findByUserId(String userId);

    /**
     * 根据文档ID查找所有点赞记录
     */
    List<DocumentLike> findByDocumentId(String documentId);

    /**
     * 删除指定文档和用户的点赞记录
     */
    void deleteByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 删除文档的所有点赞记录
     */
    void deleteByDocumentId(String documentId);

    /**
     * 统计文档的点赞数量
     */
    @Query("SELECT COUNT(dl) FROM DocumentLike dl WHERE dl.documentId = :documentId")
    Long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计用户的点赞数量
     */
    @Query("SELECT COUNT(dl) FROM DocumentLike dl WHERE dl.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 查找用户点赞的所有文档ID
     */
    @Query("SELECT dl.documentId FROM DocumentLike dl WHERE dl.userId = :userId")
    List<String> findDocumentIdsByUserId(@Param("userId") String userId);

    /**
     * 查找点赞指定文档的所有用户ID
     */
    @Query("SELECT dl.userId FROM DocumentLike dl WHERE dl.documentId = :documentId")
    List<String> findUserIdsByDocumentId(@Param("documentId") String documentId);
}