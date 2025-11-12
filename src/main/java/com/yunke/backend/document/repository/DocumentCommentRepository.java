package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {

    /**
     * 根据文档ID查找所有评论（分页）
     */
    Page<DocumentComment> findByDocumentIdAndStatus(String documentId, String status, Pageable pageable);

    /**
     * 根据文档ID查找顶级评论
     */
    Page<DocumentComment> findByDocumentIdAndParentIdAndStatus(String documentId, Long parentId, String status, Pageable pageable);

    /**
     * 根据父评论ID查找回复
     */
    List<DocumentComment> findByParentIdAndStatus(Long parentId, String status);

    /**
     * 根据用户ID查找评论
     */
    Page<DocumentComment> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    /**
     * 根据文档ID和用户ID查找评论
     */
    List<DocumentComment> findByDocumentIdAndUserIdAndStatus(String documentId, String userId, String status);

    /**
     * 统计文档的评论数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentComment dc WHERE dc.documentId = :documentId AND dc.status = 'normal'")
    Long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计用户的评论数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentComment dc WHERE dc.userId = :userId AND dc.status = 'normal'")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 统计父评论的回复数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentComment dc WHERE dc.parentId = :parentId AND dc.status = 'normal'")
    Long countByParentId(@Param("parentId") Long parentId);

    /**
     * 增加评论点赞数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DocumentComment dc SET dc.likeCount = dc.likeCount + 1 WHERE dc.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    /**
     * 减少评论点赞数
     */
    @Modifying
    @Transactional
    @Query("UPDATE DocumentComment dc SET dc.likeCount = dc.likeCount - 1 WHERE dc.id = :id AND dc.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    /**
     * 删除文档的所有评论
     */
    void deleteByDocumentId(String documentId);

    /**
     * 删除用户的所有评论
     */
    void deleteByUserId(String userId);

    /**
     * 软删除评论（更新状态为deleted）
     */
    @Modifying
    @Transactional
    @Query("UPDATE DocumentComment dc SET dc.status = 'deleted' WHERE dc.id = :id")
    void softDelete(@Param("id") Long id);
}
