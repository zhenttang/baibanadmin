package com.yunke.backend.community.repository;

import com.yunke.backend.community.domain.entity.CommunityDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 社区文档Repository接口
 */
@Repository
public interface CommunityDocumentRepository extends JpaRepository<CommunityDocument, String>, JpaSpecificationExecutor<CommunityDocument> {

    /**
     * 根据作者ID查找文档
     */
    List<CommunityDocument> findByAuthorId(String authorId);
    
    /**
     * 根据作者ID查找公开文档（分页）
     */
    Page<CommunityDocument> findByAuthorIdAndIsPublicTrue(String authorId, Pageable pageable);
    
    /**
     * 使用过滤条件查找文档
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE " +
           "(:authorId IS NULL OR cd.authorId = :authorId) AND " +
           "(:categoryId IS NULL OR cd.categoryId = :categoryId) AND " +
           "(:isPaid IS NULL OR cd.isPaid = :isPaid) AND " +
           "(:status IS NULL OR cd.status = :status) AND " +
           "cd.isPublic = :isPublic " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'published_at' THEN cd.publishedAt END DESC, " +
           "CASE WHEN :sortBy = 'view_count' THEN cd.viewCount END DESC, " +
           "CASE WHEN :sortBy = 'like_count' THEN cd.likeCount END DESC")
    Page<CommunityDocument> findDocumentsWithFilters(
        @Param("authorId") String authorId,
        @Param("categoryId") Integer categoryId,
        @Param("isPaid") Boolean isPaid,
        @Param("status") String status,
        @Param("isPublic") Boolean isPublic,
        @Param("sortBy") String sortBy,
        Pageable pageable
    );

    /**
     * 根据分类ID查找文档
     */
    Page<CommunityDocument> findByCategoryId(Integer categoryId, Pageable pageable);

    /**
     * 查找公开的文档
     */
    Page<CommunityDocument> findByIsPublicTrue(Pageable pageable);

    /**
     * 查找付费文档
     */
    Page<CommunityDocument> findByIsPaidTrue(Pageable pageable);

    /**
     * 查找免费文档
     */
    Page<CommunityDocument> findByIsPaidFalse(Pageable pageable);

    /**
     * 根据价格范围查找文档
     */
    Page<CommunityDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 根据标题模糊搜索
     */
    Page<CommunityDocument> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 根据描述模糊搜索
     */
    Page<CommunityDocument> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

    /**
     * 综合搜索：标题或描述包含关键词
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE " +
           "(LOWER(cd.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(cd.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "cd.isPublic = true")
    Page<CommunityDocument> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找热门文档（按浏览量排序）
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.isPublic = true ORDER BY cd.viewCount DESC")
    Page<CommunityDocument> findPopularDocuments(Pageable pageable);

    /**
     * 查找最受喜欢的文档（按点赞数排序）
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.isPublic = true ORDER BY cd.likeCount DESC")
    Page<CommunityDocument> findMostLikedDocuments(Pageable pageable);

    /**
     * 查找最新发布的文档
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.status = 'published' AND cd.isPublic = true ORDER BY cd.publishedAt DESC")
    Page<CommunityDocument> findLatestDocuments(Pageable pageable);

    /**
     * 查找精选文档
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.status = 'published' AND cd.isFeatured = true ORDER BY cd.publishedAt DESC")
    Page<CommunityDocument> findFeaturedDocuments(Pageable pageable);

    /**
     * 查找置顶文档
     */
    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.status = 'published' AND cd.isSticky = true ORDER BY cd.publishedAt DESC")
    Page<CommunityDocument> findStickyDocuments(Pageable pageable);

    /**
     * 根据workspaceId和sourceDocId查找文档
     */
    Optional<CommunityDocument> findByWorkspaceIdAndSourceDocId(String workspaceId, String sourceDocId);

    /**
     * 统计作者的文档数量
     */
    @Query("SELECT COUNT(cd) FROM CommunityDocument cd WHERE cd.authorId = :authorId AND cd.isPublic = true")
    Long countByAuthorId(@Param("authorId") String authorId);

    /**
     * 统计分类下的文档数量
     */
    @Query("SELECT COUNT(cd) FROM CommunityDocument cd WHERE cd.categoryId = :categoryId AND cd.isPublic = true")
    Long countByCategoryId(@Param("categoryId") Integer categoryId);

    /**
     * 增加浏览量
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommunityDocument cd SET cd.viewCount = cd.viewCount + 1 WHERE cd.id = :id")
    void incrementViewCount(@Param("id") String id);

    /**
     * 增加点赞量
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommunityDocument cd SET cd.likeCount = cd.likeCount + 1 WHERE cd.id = :id")
    void incrementLikeCount(@Param("id") String id);

    /**
     * 减少点赞量
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommunityDocument cd SET cd.likeCount = cd.likeCount - 1 WHERE cd.id = :id AND cd.likeCount > 0")
    void decrementLikeCount(@Param("id") String id);

    /**
     * 增加收藏量
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommunityDocument cd SET cd.collectCount = cd.collectCount + 1 WHERE cd.id = :id")
    void incrementCollectCount(@Param("id") String id);

    /**
     * 减少收藏量
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommunityDocument cd SET cd.collectCount = cd.collectCount - 1 WHERE cd.id = :id AND cd.collectCount > 0")
    void decrementCollectCount(@Param("id") String id);

    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.id IN :documentIds ORDER BY cd.publishedAt DESC")
    Page<CommunityDocument> findByIdIn(@Param("documentIds") List<String> documentIds, Pageable pageable);

    @Query("SELECT cd FROM CommunityDocument cd WHERE cd.authorId IN :authorIds AND cd.isPublic = true AND cd.status = 'published' ORDER BY cd.publishedAt DESC")
    Page<CommunityDocument> findByAuthorIdIn(@Param("authorIds") List<String> authorIds, Pageable pageable);
}