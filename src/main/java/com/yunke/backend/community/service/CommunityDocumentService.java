package com.yunke.backend.community.service;

import com.yunke.backend.document.dto.DocumentStatistics;
import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.document.domain.entity.DocumentComment;
import com.yunke.backend.document.domain.entity.DocumentPurchase;
import com.yunke.backend.document.domain.entity.DocumentCategory;
import com.yunke.backend.document.domain.entity.DocumentTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * 社区文档服务接口 - 完整实现
 */
public interface CommunityDocumentService {

    /**
     * 发布文档到社区
     */
    Mono<CommunityDocument> publishDocument(
        String workspaceId,
        String sourceDocId,
        String title,
        String description,
        String contentSnapshot,
        String coverImage,
        String authorId,
        Integer categoryId,
        List<Integer> tagIds,
        Boolean isPaid,
        BigDecimal price
    );

    /**
     * 更新社区文档
     */
    Mono<CommunityDocument> updateDocument(
        String documentId,
        String userId,
        String title,
        String description,
        String coverImage,
        Integer categoryId,
        List<Integer> tagIds,
        Boolean isPaid,
        BigDecimal price
    );

    /**
     * 删除社区文档（软删除）
     */
    Mono<Boolean> deleteDocument(String documentId, String userId);

    /**
     * 根据ID获取文档详情
     */
    Mono<CommunityDocument> getDocumentById(String documentId, String currentUserId);

    /**
     * 检查用户是否有权限访问文档
     */
    Mono<Boolean> hasAccessPermission(String documentId, String userId);

    /**
     * 获取公开文档列表（分页）
     */
    Mono<Page<CommunityDocument>> getPublicDocuments(
        Integer categoryId,
        Boolean isPaid,
        String sortBy,
        Pageable pageable,
        String currentUserId
    );

    /**
     * 搜索文档
     */
    Mono<Page<CommunityDocument>> searchDocuments(
        String keyword,
        Integer categoryId,
        Boolean isPaid,
        Pageable pageable,
        String currentUserId
    );

    /**
     * 获取热门文档
     */
    Mono<Page<CommunityDocument>> getPopularDocuments(Pageable pageable, String currentUserId);

    /**
     * 获取精选文档
     */
    Mono<Page<CommunityDocument>> getFeaturedDocuments(Pageable pageable, String currentUserId);

    /**
     * 获取最新文档
     */
    Mono<Page<CommunityDocument>> getLatestDocuments(Pageable pageable, String currentUserId);

    /**
     * 获取作者的文档列表
     */
    Mono<Page<CommunityDocument>> getDocumentsByAuthor(
        String authorId,
        Pageable pageable,
        String currentUserId
    );

    /**
     * 点赞文档
     */
    Mono<Boolean> likeDocument(String documentId, String userId);

    /**
     * 取消点赞
     */
    Mono<Boolean> unlikeDocument(String documentId, String userId);

    /**
     * 检查用户是否已点赞
     */
    Mono<Boolean> isDocumentLiked(String documentId, String userId);

    /**
     * 收藏文档
     */
    Mono<Boolean> collectDocument(String documentId, String userId, Integer folderId, String notes);

    /**
     * 取消收藏
     */
    Mono<Boolean> uncollectDocument(String documentId, String userId);

    /**
     * 检查用户是否已收藏
     */
    Mono<Boolean> isDocumentCollected(String documentId, String userId);

    /**
     * 获取用户收藏的文档
     */
    Mono<Page<CommunityDocument>> getCollectedDocuments(
        String userId,
        Integer folderId,
        Pageable pageable
    );

    /**
     * 关注作者
     */
    Mono<Boolean> followAuthor(String followerId, String followingId);

    /**
     * 取消关注
     */
    Mono<Boolean> unfollowAuthor(String followerId, String followingId);

    /**
     * 检查是否已关注
     */
    Mono<Boolean> isFollowing(String followerId, String followingId);

    /**
     * 获取关注的作者发布的文档
     */
    Mono<Page<CommunityDocument>> getFollowingDocuments(
        String userId,
        Pageable pageable
    );

    /**
     * 添加评论
     */
    Mono<DocumentComment> addComment(
        String documentId,
        String userId,
        String content,
        Long parentId
    );

    /**
     * 删除评论
     */
    Mono<Boolean> deleteComment(Long commentId, String userId);

    /**
     * 获取文档评论
     */
    Mono<Page<DocumentComment>> getDocumentComments(
        String documentId,
        Long parentId,
        Pageable pageable
    );

    /**
     * 购买付费文档
     */
    Mono<DocumentPurchase> purchaseDocument(
        String documentId,
        String userId,
        String paymentMethod
    );

    /**
     * 检查用户是否已购买
     */
    Mono<Boolean> hasUserPurchased(String documentId, String userId);

    /**
     * 记录文档浏览
     */
    Mono<Void> recordView(
        String documentId,
        String userId,
        String ipAddress,
        String userAgent,
        Integer viewDuration
    );

    /**
     * 增加浏览量
     */
    Mono<Void> incrementViewCount(String documentId);

    /**
     * 获取所有分类
     */
    Flux<DocumentCategory> getAllCategories();

    /**
     * 创建分类
     */
    Mono<DocumentCategory> createCategory(
        String name,
        String slug,
        Integer parentId,
        String description,
        String icon
    );

    /**
     * 获取所有标签
     */
    Flux<DocumentTag> getAllTags();

    /**
     * 创建标签
     */
    Mono<DocumentTag> createTag(String name, String slug, String color);

    /**
     * 获取文档的标签
     */
    Flux<DocumentTag> getDocumentTags(String documentId);

    /**
     * 为文档添加标签
     */
    Mono<Void> addTagToDocument(String documentId, Integer tagId);

    /**
     * 移除文档标签
     */
    Mono<Void> removeTagFromDocument(String documentId, Integer tagId);

    /**
     * 获取文档统计信息
     */
    Mono<DocumentStatistics> getDocumentStatistics(String documentId);
}
