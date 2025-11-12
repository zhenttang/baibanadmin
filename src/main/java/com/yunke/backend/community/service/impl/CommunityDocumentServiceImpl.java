package com.yunke.backend.community.service.impl;

import com.yunke.backend.document.dto.DocumentStatistics;
import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.document.domain.entity.DocumentCategory;
import com.yunke.backend.document.domain.entity.DocumentTag;
import com.yunke.backend.document.domain.entity.DocumentComment;
import com.yunke.backend.document.domain.entity.DocumentPurchase;
import com.yunke.backend.document.domain.entity.DocumentTagRelation;
import com.yunke.backend.document.domain.entity.DocumentLike;
import com.yunke.backend.document.domain.entity.DocumentCollection;
import com.yunke.backend.document.domain.entity.DocumentView;
import com.yunke.backend.user.domain.entity.UserFollow;
import com.yunke.backend.community.repository.CommunityDocumentRepository;
import com.yunke.backend.document.repository.DocumentCategoryRepository;
import com.yunke.backend.document.repository.DocumentTagRepository;
import com.yunke.backend.document.repository.DocumentTagRelationRepository;
import com.yunke.backend.document.repository.DocumentLikeRepository;
import com.yunke.backend.document.repository.DocumentCollectionRepository;
import com.yunke.backend.user.repository.UserFollowRepository;
import com.yunke.backend.document.repository.DocumentCommentRepository;
import com.yunke.backend.document.repository.DocumentPurchaseRepository;
import com.yunke.backend.document.repository.DocumentViewRepository;
import com.yunke.backend.community.service.CommunityDocumentService;
import com.yunke.backend.notification.service.NotificationService;
import com.yunke.backend.payment.service.PaymentService;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityDocumentServiceImpl implements CommunityDocumentService {

    private final CommunityDocumentRepository documentRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final DocumentTagRepository tagRepository;
    private final DocumentTagRelationRepository tagRelationRepository;
    private final DocumentLikeRepository likeRepository;
    private final DocumentCollectionRepository collectionRepository;
    private final UserFollowRepository followRepository;
    private final DocumentCommentRepository commentRepository;
    private final DocumentPurchaseRepository purchaseRepository;
    private final DocumentViewRepository viewRepository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final WorkspaceDocRepository workspaceDocRepository;
    private final UserRepository userRepository;
    private final SnapshotRepository snapshotRepository;

    @Override
    public Mono<CommunityDocument> publishDocument(
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
    ) {
        return Mono.fromCallable(() -> {
            // 生成文档ID
            CommunityDocument document = new CommunityDocument();
            document.setId(UUID.randomUUID().toString());
            document.setWorkspaceId(workspaceId);
            document.setSourceDocId(sourceDocId);
            document.setTitle(title);
            document.setDescription(description);
            document.setContentSnapshot(null);
            document.setCoverImage(coverImage);
            document.setAuthorId(authorId);

            // 从User表获取作者信息
            User author = userRepository.findById(authorId).orElse(null);
            if (author != null) {
                document.setAuthorName(author.getName());
                document.setAuthorAvatar(author.getAvatarUrl());
            } else {
                document.setAuthorName("未知用户");
                document.setAuthorAvatar("/avatars/default.png");
            }

            // 从WorkspaceDoc获取文档元信息，并将源文档设为公开
            try {
                WorkspaceDoc sourceDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(workspaceId, sourceDocId).orElse(null);
                if (sourceDoc != null) {
                    // 如果前端没传title，使用原文档title
                    if (title == null || title.isEmpty()) {
                        document.setTitle(sourceDoc.getTitle() != null ? sourceDoc.getTitle() : "未命名文档");
                    }
                    // 如果前端没传description，使用原文档summary
                    if (description == null || description.isEmpty()) {
                        document.setDescription(sourceDoc.getSummary());
                    }

                    // 将源文档设为公开，这样其他用户可以访问
                    if (!sourceDoc.getPublic()) {
                        sourceDoc.setPublic(true);
                        workspaceDocRepository.save(sourceDoc);
                        log.info("源文档已设为公开: workspaceId={}, docId={}", workspaceId, sourceDocId);
                    }
                }
            } catch (Exception e) {
                log.warn("获取源文档信息失败: workspaceId={}, sourceDocId={}, error={}",
                    workspaceId, sourceDocId, e.getMessage());
            }

            document.setCategoryId(categoryId);
            document.setIsPaid(isPaid != null ? isPaid : false);
            document.setPrice(price != null ? price : BigDecimal.ZERO);
            document.setIsPublic(true);
            document.setStatus("published");
            document.setPublishedAt(LocalDateTime.now());

            CommunityDocument saved = documentRepository.save(document);

            // 添加标签关联
            if (tagIds != null && !tagIds.isEmpty()) {
                for (Integer tagId : tagIds) {
                    DocumentTagRelation relation = new DocumentTagRelation();
                    relation.setDocumentId(saved.getId());
                    relation.setTagId(tagId);
                    tagRelationRepository.save(relation);
                }
            }

            log.info("文档发布成功: id={}, title={}, author={}", saved.getId(), saved.getTitle(), saved.getAuthorName());
            return saved;
        });
    }

    @Override
    public Mono<CommunityDocument> updateDocument(
            String documentId,
            String userId,
            String title,
            String description,
            String coverImage,
            Integer categoryId,
            List<Integer> tagIds,
            Boolean isPaid,
            BigDecimal price
    ) {
        return Mono.fromCallable(() -> {
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            if (!document.getAuthorId().equals(userId)) {
                throw new RuntimeException("无权限修改此文档");
            }

            if (title != null) document.setTitle(title);
            if (description != null) document.setDescription(description);
            if (coverImage != null) document.setCoverImage(coverImage);
            if (categoryId != null) document.setCategoryId(categoryId);
            if (isPaid != null) document.setIsPaid(isPaid);
            if (price != null) document.setPrice(price);

            CommunityDocument saved = documentRepository.save(document);

            // 更新标签
            if (tagIds != null) {
                // 删除旧标签
                tagRelationRepository.deleteByDocumentId(documentId);

                // 添加新标签
                for (Integer tagId : tagIds) {
                    DocumentTagRelation relation = new DocumentTagRelation();
                    relation.setDocumentId(documentId);
                    relation.setTagId(tagId);
                    tagRelationRepository.save(relation);
                }
            }

            return saved;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteDocument(String documentId, String userId) {
        return Mono.fromCallable(() -> {
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            if (!document.getAuthorId().equals(userId)) {
                throw new RuntimeException("无权限删除此文档");
            }

            document.setStatus("deleted");
            document.setDeletedAt(LocalDateTime.now());
            documentRepository.save(document);

            return true;
        });
    }

    @Override
    public Mono<CommunityDocument> getDocumentById(String documentId, String currentUserId) {
        return Mono.fromCallable(() -> {
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            // 增加浏览量（异步，不影响返回）
            documentRepository.incrementViewCount(documentId);

            // 从WorkspaceDoc获取实时的title和summary
            try {
                WorkspaceDoc sourceDoc = workspaceDocRepository.findByWorkspaceIdAndDocId(
                    document.getWorkspaceId(), document.getSourceDocId()
                ).orElse(null);

                if (sourceDoc != null) {
                    // 使用源文档的实时title（如果社区文档没有自定义标题）
                    if (document.getTitle() == null || document.getTitle().isEmpty()) {
                        document.setTitle(sourceDoc.getTitle() != null ? sourceDoc.getTitle() : "未命名文档");
                    }
                    // 使用源文档的summary
                    if (document.getDescription() == null || document.getDescription().isEmpty()) {
                        document.setDescription(sourceDoc.getSummary());
                    }
                }
            } catch (Exception e) {
                log.warn("获取源文档信息失败: documentId={}, error={}", documentId, e.getMessage());
            }

            // 不从Snapshot获取contentSnapshot
            // 社区文档只存储引用，实际内容通过跳转到源文档查看
            document.setContentSnapshot(null);

            // 填充用户相关状态
            if (currentUserId != null) {
                document.setIsLiked(likeRepository.existsByDocumentIdAndUserId(documentId, currentUserId));
                document.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(documentId, currentUserId));
                document.setIsFollowing(followRepository.existsByFollowerIdAndFollowingId(currentUserId, document.getAuthorId()));
            }

            return document;
        });
    }

    @Override
    public Mono<Boolean> hasAccessPermission(String documentId, String userId) {
        return Mono.fromCallable(() -> {
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            // 公开文档所有人可访问
            if (document.getIsPublic()) {
                return true;
            }

            // 作者可访问
            if (userId != null && document.getAuthorId().equals(userId)) {
                return true;
            }

            // 需要关注才能访问
            if (document.getRequireFollow() && userId != null) {
                return followRepository.existsByFollowerIdAndFollowingId(userId, document.getAuthorId());
            }

            // 需要购买才能访问
            if (document.getRequirePurchase() && userId != null) {
                return purchaseRepository.existsByDocumentIdAndUserIdAndStatus(documentId, userId, "completed");
            }

            return false;
        });
    }

    @Override
    public Mono<Page<CommunityDocument>> getPublicDocuments(
            Integer categoryId,
            Boolean isPaid,
            String sortBy,
            Pageable pageable,
            String currentUserId
    ) {
        return Mono.fromCallable(() -> {
            Page<CommunityDocument> page = documentRepository.findDocumentsWithFilters(
                    null,
                    categoryId,
                    isPaid,
                    "published",
                    true,
                    sortBy != null ? sortBy : "published_at",
                    pageable
            );

            // 填充用户状态
            if (currentUserId != null) {
                page.getContent().forEach(doc -> {
                    doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                    doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                });
            }

            return page;
        });
    }

    @Override
    public Mono<Page<CommunityDocument>> searchDocuments(
            String keyword,
            Integer categoryId,
            Boolean isPaid,
            Pageable pageable,
            String currentUserId
    ) {
        return Mono.fromCallable(() -> {
            Page<CommunityDocument> page = documentRepository.searchByKeyword(keyword, pageable);

            // 填充用户状态
            if (currentUserId != null) {
                page.getContent().forEach(doc -> {
                    doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                    doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                });
            }

            return page;
        });
    }

    @Override
    public Mono<Page<CommunityDocument>> getPopularDocuments(Pageable pageable, String currentUserId) {
        return Mono.fromCallable(() -> {
            Page<CommunityDocument> page = documentRepository.findPopularDocuments(pageable);

            if (currentUserId != null) {
                page.getContent().forEach(doc -> {
                    doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                    doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                });
            }

            return page;
        });
    }

    @Override
    public Mono<Page<CommunityDocument>> getFeaturedDocuments(Pageable pageable, String currentUserId) {
        return Mono.fromCallable(() -> {
            Page<CommunityDocument> page = documentRepository.findFeaturedDocuments(pageable);

            if (currentUserId != null) {
                page.getContent().forEach(doc -> {
                    doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                    doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                });
            }

            return page;
        });
    }

    @Override
    public Mono<Page<CommunityDocument>> getLatestDocuments(Pageable pageable, String currentUserId) {
        return Mono.fromCallable(() -> {
            Page<CommunityDocument> page = documentRepository.findLatestDocuments(pageable);

            if (currentUserId != null) {
                page.getContent().forEach(doc -> {
                    doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                    doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                });
            }

            return page;
        });
    }

    @Override
    public Mono<Page<CommunityDocument>> getDocumentsByAuthor(
            String authorId,
            Pageable pageable,
            String currentUserId
    ) {
        return Mono.fromCallable(() -> {
            Page<CommunityDocument> page = documentRepository.findByAuthorIdAndIsPublicTrue(authorId, pageable);

            if (currentUserId != null) {
                page.getContent().forEach(doc -> {
                    doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                    doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), currentUserId));
                });
            }

            return page;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> likeDocument(String documentId, String userId) {
        return Mono.fromCallable(() -> {
            if (likeRepository.existsByDocumentIdAndUserId(documentId, userId)) {
                return false;
            }

            DocumentLike like = new DocumentLike();
            like.setDocumentId(documentId);
            like.setUserId(userId);
            likeRepository.save(like);

            CommunityDocument document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                User liker = userService.getUserById(userId);
                String likerName = liker != null ? liker.getName() : "用户";

                notificationService.createLikeNotification(
                    documentId,
                    document.getTitle(),
                    document.getAuthorId(),
                    userId,
                    likerName
                );
            }

            return true;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> unlikeDocument(String documentId, String userId) {
        return Mono.fromCallable(() -> {
            likeRepository.deleteByDocumentIdAndUserId(documentId, userId);
            return true;
        });
    }

    @Override
    public Mono<Boolean> isDocumentLiked(String documentId, String userId) {
        return Mono.fromCallable(() ->
                likeRepository.existsByDocumentIdAndUserId(documentId, userId)
        );
    }

    @Override
    @Transactional
    public Mono<Boolean> collectDocument(String documentId, String userId, Integer folderId, String notes) {
        return Mono.fromCallable(() -> {
            if (collectionRepository.existsByDocumentIdAndUserId(documentId, userId)) {
                return false;
            }

            DocumentCollection collection = new DocumentCollection();
            collection.setDocumentId(documentId);
            collection.setUserId(userId);
            collection.setFolderId(folderId);
            collection.setNotes(notes);
            collectionRepository.save(collection);

            CommunityDocument document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                User collector = userService.getUserById(userId);
                String collectorName = collector != null ? collector.getName() : "用户";

                notificationService.createCollectNotification(
                    documentId,
                    document.getTitle(),
                    document.getAuthorId(),
                    userId,
                    collectorName
                );
            }

            return true;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> uncollectDocument(String documentId, String userId) {
        return Mono.fromCallable(() -> {
            collectionRepository.deleteByDocumentIdAndUserId(documentId, userId);
            return true;
        });
    }

    @Override
    public Mono<Boolean> isDocumentCollected(String documentId, String userId) {
        return Mono.fromCallable(() ->
                collectionRepository.existsByDocumentIdAndUserId(documentId, userId)
        );
    }

    @Override
    public Mono<Page<CommunityDocument>> getCollectedDocuments(
            String userId,
            Integer folderId,
            Pageable pageable
    ) {
        return Mono.fromCallable(() -> {
            List<String> documentIds;
            if (folderId != null) {
                documentIds = collectionRepository.findByUserIdAndFolderId(userId, folderId)
                        .stream()
                        .map(DocumentCollection::getDocumentId)
                        .toList();
            } else {
                documentIds = collectionRepository.findDocumentIdsByUserId(userId);
            }

            if (documentIds.isEmpty()) {
                return Page.empty(pageable);
            }

            Page<CommunityDocument> page = documentRepository.findByIdIn(documentIds, pageable);

            page.getContent().forEach(doc -> {
                doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), userId));
                doc.setIsCollected(true);
                doc.setIsFollowing(followRepository.existsByFollowerIdAndFollowingId(userId, doc.getAuthorId()));
            });

            return page;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> followAuthor(String followerId, String followingId) {
        return Mono.fromCallable(() -> {
            if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
                return false;
            }

            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            followRepository.save(follow);

            return true;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> unfollowAuthor(String followerId, String followingId) {
        return Mono.fromCallable(() -> {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
            return true;
        });
    }

    @Override
    public Mono<Boolean> isFollowing(String followerId, String followingId) {
        return Mono.fromCallable(() ->
                followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        );
    }

    @Override
    public Mono<Page<CommunityDocument>> getFollowingDocuments(String userId, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<String> followingIds = followRepository.findFollowingIdsByFollowerId(userId);

            if (followingIds.isEmpty()) {
                return Page.empty(pageable);
            }

            Page<CommunityDocument> page = documentRepository.findByAuthorIdIn(followingIds, pageable);

            page.getContent().forEach(doc -> {
                doc.setIsLiked(likeRepository.existsByDocumentIdAndUserId(doc.getId(), userId));
                doc.setIsCollected(collectionRepository.existsByDocumentIdAndUserId(doc.getId(), userId));
                doc.setIsFollowing(true);
            });

            return page;
        });
    }

    @Override
    @Transactional
    public Mono<DocumentComment> addComment(String documentId, String userId, String content, Long parentId) {
        return Mono.fromCallable(() -> {
            // 获取评论者信息
            User commenter = userRepository.findById(userId).orElse(null);
            String userName = commenter != null ? commenter.getName() : "用户";
            String userAvatar = commenter != null ? commenter.getAvatarUrl() : "/avatars/default.png";

            // 创建评论
            DocumentComment comment = new DocumentComment();
            comment.setDocumentId(documentId);
            comment.setUserId(userId);
            comment.setUserName(userName);
            comment.setUserAvatar(userAvatar);
            comment.setContent(content);
            comment.setParentId(parentId != null ? parentId : 0L);
            comment.setStatus("normal");

            DocumentComment savedComment = commentRepository.save(comment);

            // 创建评论通知
            CommunityDocument document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                notificationService.createCommentNotification(
                    documentId,
                    document.getTitle(),
                    document.getAuthorId(),
                    savedComment.getId(),
                    content,
                    userId,
                    userName
                );
            }

            return savedComment;
        });
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteComment(Long commentId, String userId) {
        return Mono.fromCallable(() -> {
            DocumentComment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("评论不存在"));

            if (!comment.getUserId().equals(userId)) {
                throw new RuntimeException("无权限删除此评论");
            }

            commentRepository.softDelete(commentId);
            return true;
        });
    }

    @Override
    public Mono<Page<DocumentComment>> getDocumentComments(
            String documentId,
            Long parentId,
            Pageable pageable
    ) {
        return Mono.fromCallable(() ->
                commentRepository.findByDocumentIdAndParentIdAndStatus(
                        documentId,
                        parentId != null ? parentId : 0L,
                        "normal",
                        pageable
                )
        );
    }

    @Override
    @Transactional
    public Mono<DocumentPurchase> purchaseDocument(
            String documentId,
            String userId,
            String paymentMethod
    ) {
        return Mono.fromCallable(() -> {
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            if (!document.getIsPaid()) {
                throw new RuntimeException("该文档不需要购买");
            }

            if (purchaseRepository.existsByDocumentIdAndUserIdAndStatus(documentId, userId, "completed")) {
                throw new RuntimeException("您已经购买过该文档");
            }

            DocumentPurchase purchase = new DocumentPurchase();
            purchase.setDocumentId(documentId);
            purchase.setUserId(userId);
            purchase.setPrice(document.getDiscountPrice() != null ? document.getDiscountPrice() : document.getPrice());
            purchase.setPaymentMethod(paymentMethod);
            purchase.setStatus("pending");

            DocumentPurchase saved = purchaseRepository.save(purchase);
            log.info("Created purchase record {} for document {} by user {}", saved.getId(), documentId, userId);

            return saved;
        });
    }

    @Override
    public Mono<Boolean> hasUserPurchased(String documentId, String userId) {
        return Mono.fromCallable(() ->
                purchaseRepository.existsByDocumentIdAndUserIdAndStatus(documentId, userId, "completed")
        );
    }

    @Override
    @Transactional
    public Mono<Void> recordView(
            String documentId,
            String userId,
            String ipAddress,
            String userAgent,
            Integer viewDuration
    ) {
        return Mono.fromRunnable(() -> {
            DocumentView view = new DocumentView();
            view.setDocumentId(documentId);
            view.setUserId(userId);
            view.setIpAddress(ipAddress);
            view.setUserAgent(userAgent);
            view.setViewDuration(viewDuration != null ? viewDuration : 0);
            viewRepository.save(view);
        });
    }

    @Override
    @Transactional
    public Mono<Void> incrementViewCount(String documentId) {
        return Mono.fromRunnable(() ->
                documentRepository.incrementViewCount(documentId)
        );
    }

    @Override
    public Flux<DocumentCategory> getAllCategories() {
        return Flux.fromIterable(categoryRepository.findAll());
    }

    @Override
    @Transactional
    public Mono<DocumentCategory> createCategory(
            String name,
            String slug,
            Integer parentId,
            String description,
            String icon
    ) {
        return Mono.fromCallable(() -> {
            DocumentCategory category = new DocumentCategory();
            category.setName(name);
            category.setSlug(slug);
            category.setParentId(parentId != null ? parentId : 0);
            category.setDescription(description);
            category.setIcon(icon);
            category.setIsActive(true);

            return categoryRepository.save(category);
        });
    }

    @Override
    public Flux<DocumentTag> getAllTags() {
        return Flux.fromIterable(tagRepository.findAll());
    }

    @Override
    @Transactional
    public Mono<DocumentTag> createTag(String name, String slug, String color) {
        return Mono.fromCallable(() -> {
            DocumentTag tag = new DocumentTag();
            tag.setName(name);
            tag.setSlug(slug);
            tag.setColor(color != null ? color : "#999999");

            return tagRepository.save(tag);
        });
    }

    @Override
    public Flux<DocumentTag> getDocumentTags(String documentId) {
        return Flux.fromIterable(
                tagRelationRepository.findByDocumentId(documentId)
                        .stream()
                        .map(relation -> tagRepository.findById(relation.getTagId()).orElse(null))
                        .filter(tag -> tag != null)
                        .toList()
        );
    }

    @Override
    @Transactional
    public Mono<Void> addTagToDocument(String documentId, Integer tagId) {
        return Mono.fromRunnable(() -> {
            DocumentTagRelation relation = new DocumentTagRelation();
            relation.setDocumentId(documentId);
            relation.setTagId(tagId);
            tagRelationRepository.save(relation);
        });
    }

    @Override
    @Transactional
    public Mono<Void> removeTagFromDocument(String documentId, Integer tagId) {
        return Mono.fromRunnable(() ->
                tagRelationRepository.deleteByDocumentIdAndTagId(documentId, tagId)
        );
    }

    @Override
    public Mono<DocumentStatistics> getDocumentStatistics(String documentId) {
        return Mono.fromCallable(() -> {
            CommunityDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));

            Long uniqueViewers = viewRepository.countDistinctUsersByDocumentId(documentId);
            Double avgViewDuration = viewRepository.averageViewDurationByDocumentId(documentId);

            return DocumentStatistics.builder()
                    .viewCount(document.getViewCount())
                    .likeCount(document.getLikeCount())
                    .collectCount(document.getCollectCount())
                    .commentCount(document.getCommentCount())
                    .shareCount(document.getShareCount())
                    .purchaseCount(document.getPurchaseCount())
                    .qualityScore(document.getQualityScore())
                    .avgRating(document.getAvgRating())
                    .ratingCount(document.getRatingCount())
                    .uniqueViewers(uniqueViewers)
                    .avgViewDuration(avgViewDuration != null ? avgViewDuration : 0.0)
                    .build();
        });
    }
}
