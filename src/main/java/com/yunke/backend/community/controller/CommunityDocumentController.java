package com.yunke.backend.community.controller;

import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.document.domain.entity.DocumentCategory;
import com.yunke.backend.document.domain.entity.DocumentComment;
import com.yunke.backend.document.domain.entity.DocumentPurchase;
import com.yunke.backend.document.domain.entity.DocumentTag;
import com.yunke.backend.document.dto.DocumentStatistics;

import com.yunke.backend.common.PageResponse;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.community.service.CommunityDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/documents")
@RequiredArgsConstructor
@Slf4j
public class CommunityDocumentController {

    private final CommunityDocumentService communityDocumentService;

    private Mono<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.empty();
        }
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        return Mono.just(userDetails.getUserId());
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setItems(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotal(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    /**
     * 发布文档到社区
     * POST /api/community/documents
     */
    @PostMapping
    public Mono<ResponseEntity<CommunityDocument>> publishDocument(@RequestBody PublishDocumentRequest request) {
        log.info("发布文档到社区: workspaceId={}, sourceDocId={}", request.workspaceId(), request.sourceDocId());

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.publishDocument(
                request.workspaceId(),
                request.sourceDocId(),
                request.title(),
                request.description(),
                request.contentSnapshot(),
                request.coverImage(),
                userId,
                request.categoryId(),
                request.tagIds(),
                request.isPaid(),
                request.price()
            ))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 更新社区文档
     * PUT /api/community/documents/{documentId}
     */
    @PutMapping("/{documentId}")
    public Mono<ResponseEntity<CommunityDocument>> updateDocument(
            @PathVariable String documentId,
            @RequestBody UpdateDocumentRequest request) {
        log.info("更新社区文档: documentId={}", documentId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.updateDocument(
                documentId,
                userId,
                request.title(),
                request.description(),
                request.coverImage(),
                request.categoryId(),
                request.tagIds(),
                request.isPaid(),
                request.price()
            ))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 删除社区文档
     * DELETE /api/community/documents/{documentId}
     */
    @DeleteMapping("/{documentId}")
    public Mono<ResponseEntity<Void>> deleteDocument(@PathVariable String documentId) {
        log.info("删除社区文档: documentId={}", documentId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.deleteDocument(documentId, userId))
            .map(success -> success ?
                ResponseEntity.noContent().<Void>build() :
                ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build())
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 获取文档详情
     * GET /api/community/documents/{documentId}
     */
    @GetMapping("/{documentId}")
    public Mono<ResponseEntity<CommunityDocument>> getDocument(@PathVariable String documentId) {
        log.info("获取文档详情: documentId={}", documentId);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.getDocumentById(documentId, userId.isEmpty() ? null : userId))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * 获取公开文档列表
     * GET /api/community/documents?page=0&size=20&categoryId=1&isPaid=false&sort=latest
     */
    @GetMapping
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getPublicDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(defaultValue = "latest") String sort) {
        log.info("获取公开文档列表: page={}, size={}, categoryId={}, isPaid={}, sort={}",
            page, size, categoryId, isPaid, sort);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.getPublicDocuments(
                categoryId, isPaid, sort, pageable, userId.isEmpty() ? null : userId))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 搜索文档
     * GET /api/community/documents/search?keyword=xxx&page=0&size=20
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> searchDocuments(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("搜索文档: keyword={}, categoryId={}, isPaid={}, page={}, size={}",
            keyword, categoryId, isPaid, page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.searchDocuments(
                keyword, categoryId, isPaid, pageable, userId.isEmpty() ? null : userId))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 获取热门文档
     * GET /api/community/documents/popular
     */
    @GetMapping("/popular")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getPopularDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取热门文档: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.getPopularDocuments(pageable, userId.isEmpty() ? null : userId))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 获取精选文档
     * GET /api/community/documents/featured
     */
    @GetMapping("/featured")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getFeaturedDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取精选文档: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.getFeaturedDocuments(pageable, userId.isEmpty() ? null : userId))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 获取最新文档
     * GET /api/community/documents/latest
     */
    @GetMapping("/latest")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getLatestDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取最新文档: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.getLatestDocuments(pageable, userId.isEmpty() ? null : userId))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 获取作者的文档列表
     * GET /api/community/documents/author/{authorId}
     */
    @GetMapping("/author/{authorId}")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getDocumentsByAuthor(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取作者文档列表: authorId={}, page={}, size={}", authorId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .defaultIfEmpty("")
            .flatMap(userId -> communityDocumentService.getDocumentsByAuthor(authorId, pageable, userId.isEmpty() ? null : userId))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 点赞文档
     * POST /api/community/documents/{documentId}/like
     */
    @PostMapping("/{documentId}/like")
    public Mono<ResponseEntity<Map<String, Boolean>>> likeDocument(@PathVariable String documentId) {
        log.info("点赞文档: documentId={}", documentId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.likeDocument(documentId, userId))
            .map(success -> ResponseEntity.ok(Map.of("success", success)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 取消点赞
     * DELETE /api/community/documents/{documentId}/like
     */
    @DeleteMapping("/{documentId}/like")
    public Mono<ResponseEntity<Map<String, Boolean>>> unlikeDocument(@PathVariable String documentId) {
        log.info("取消点赞: documentId={}", documentId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.unlikeDocument(documentId, userId))
            .map(success -> ResponseEntity.ok(Map.of("success", success)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 收藏文档
     * POST /api/community/documents/{documentId}/collect
     */
    @PostMapping("/{documentId}/collect")
    public Mono<ResponseEntity<Map<String, Boolean>>> collectDocument(
            @PathVariable String documentId,
            @RequestBody(required = false) CollectDocumentRequest request) {
        log.info("收藏文档: documentId={}", documentId);

        Integer folderId = request != null ? request.folderId() : null;
        String notes = request != null ? request.notes() : null;

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.collectDocument(documentId, userId, folderId, notes))
            .map(success -> ResponseEntity.ok(Map.of("success", success)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 取消收藏
     * DELETE /api/community/documents/{documentId}/collect
     */
    @DeleteMapping("/{documentId}/collect")
    public Mono<ResponseEntity<Map<String, Boolean>>> uncollectDocument(@PathVariable String documentId) {
        log.info("取消收藏: documentId={}", documentId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.uncollectDocument(documentId, userId))
            .map(success -> ResponseEntity.ok(Map.of("success", success)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 获取用户收藏的文档
     * GET /api/community/documents/collected?folderId=1&page=0&size=20
     */
    @GetMapping("/collected")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getCollectedDocuments(
            @RequestParam(required = false) Integer folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取用户收藏文档: folderId={}, page={}, size={}", folderId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.getCollectedDocuments(userId, folderId, pageable))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 关注作者
     * POST /api/community/authors/{authorId}/follow
     */
    @PostMapping("/authors/{authorId}/follow")
    public Mono<ResponseEntity<Map<String, Boolean>>> followAuthor(@PathVariable String authorId) {
        log.info("关注作者: authorId={}", authorId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.followAuthor(userId, authorId))
            .map(success -> ResponseEntity.ok(Map.of("success", success)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 取消关注
     * DELETE /api/community/authors/{authorId}/follow
     */
    @DeleteMapping("/authors/{authorId}/follow")
    public Mono<ResponseEntity<Map<String, Boolean>>> unfollowAuthor(@PathVariable String authorId) {
        log.info("取消关注: authorId={}", authorId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.unfollowAuthor(userId, authorId))
            .map(success -> ResponseEntity.ok(Map.of("success", success)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 获取关注作者的文档
     * GET /api/community/documents/following
     */
    @GetMapping("/following")
    public Mono<ResponseEntity<PageResponse<CommunityDocument>>> getFollowingDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取关注作者文档: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.getFollowingDocuments(userId, pageable))
            .map(this::toPageResponse)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 添加评论
     * POST /api/community/documents/{documentId}/comments
     */
    @PostMapping("/{documentId}/comments")
    public Mono<ResponseEntity<DocumentComment>> addComment(
            @PathVariable String documentId,
            @RequestBody AddCommentRequest request) {
        log.info("添加评论: documentId={}, parentId={}", documentId, request.parentId());

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.addComment(
                documentId, userId, request.content(), request.parentId()))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 删除评论
     * DELETE /api/community/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public Mono<ResponseEntity<Void>> deleteComment(@PathVariable Long commentId) {
        log.info("删除评论: commentId={}", commentId);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.deleteComment(commentId, userId))
            .map(success -> success ?
                ResponseEntity.noContent().<Void>build() :
                ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build())
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 获取文档评论
     * GET /api/community/documents/{documentId}/comments?parentId=0&page=0&size=20
     */
    @GetMapping("/{documentId}/comments")
    public Mono<ResponseEntity<PageResponse<DocumentComment>>> getDocumentComments(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "0") Long parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取文档评论: documentId={}, parentId={}, page={}, size={}",
            documentId, parentId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        return communityDocumentService.getDocumentComments(documentId, parentId, pageable)
            .map(this::toPageResponse)
            .map(ResponseEntity::ok);
    }

    /**
     * 购买付费文档
     * POST /api/community/documents/{documentId}/purchase
     */
    @PostMapping("/{documentId}/purchase")
    public Mono<ResponseEntity<DocumentPurchase>> purchaseDocument(
            @PathVariable String documentId,
            @RequestBody PurchaseDocumentRequest request) {
        log.info("购买付费文档: documentId={}, paymentMethod={}", documentId, request.paymentMethod());

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.purchaseDocument(
                documentId, userId, request.paymentMethod()))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * 记录文档浏览
     * POST /api/community/documents/{documentId}/view
     */
    @PostMapping("/{documentId}/view")
    public Mono<ResponseEntity<Map<String, Object>>> recordView(
            @PathVariable String documentId,
            @RequestBody(required = false) RecordViewRequest request) {
        log.info("记录文档浏览: documentId={}", documentId);

        String ipAddress = request != null ? request.ipAddress() : null;
        String userAgent = request != null ? request.userAgent() : null;
        Integer viewDuration = (request != null && request.viewDuration() != null) ? request.viewDuration() : 0;

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);

        return getCurrentUserId()
            .flatMap(userId -> communityDocumentService.recordView(
                documentId, userId, ipAddress, userAgent, viewDuration))
            .then(communityDocumentService.incrementViewCount(documentId))
            .then(Mono.just(ResponseEntity.ok(successResponse)))
            .defaultIfEmpty(ResponseEntity.ok(successResponse));
    }

    /**
     * 获取文档统计信息
     * GET /api/community/documents/{documentId}/statistics
     */
    @GetMapping("/{documentId}/statistics")
    public Mono<ResponseEntity<DocumentStatistics>> getDocumentStatistics(@PathVariable String documentId) {
        log.info("获取文档统计: documentId={}", documentId);

        return communityDocumentService.getDocumentStatistics(documentId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有分类
     * GET /api/community/categories
     */
    @GetMapping("/categories")
    public Flux<DocumentCategory> getAllCategories() {
        log.info("获取所有分类");
        return communityDocumentService.getAllCategories();
    }

    /**
     * 创建分类
     * POST /api/community/categories
     */
    @PostMapping("/categories")
    public Mono<ResponseEntity<DocumentCategory>> createCategory(@RequestBody CreateCategoryRequest request) {
        log.info("创建分类: name={}", request.name());

        return communityDocumentService.createCategory(
            request.name(), request.slug(), request.parentId(),
            request.description(), request.icon())
            .map(ResponseEntity::ok);
    }

    /**
     * 获取所有标签
     * GET /api/community/tags
     */
    @GetMapping("/tags")
    public Flux<DocumentTag> getAllTags() {
        log.info("获取所有标签");
        return communityDocumentService.getAllTags();
    }

    /**
     * 创建标签
     * POST /api/community/tags
     */
    @PostMapping("/tags")
    public Mono<ResponseEntity<DocumentTag>> createTag(@RequestBody CreateTagRequest request) {
        log.info("创建标签: name={}", request.name());

        return communityDocumentService.createTag(request.name(), request.slug(), request.color())
            .map(ResponseEntity::ok);
    }

    /**
     * 获取文档的标签
     * GET /api/community/documents/{documentId}/tags
     */
    @GetMapping("/{documentId}/tags")
    public Flux<DocumentTag> getDocumentTags(@PathVariable String documentId) {
        log.info("获取文档标签: documentId={}", documentId);
        return communityDocumentService.getDocumentTags(documentId);
    }

    // ==================== Request DTOs ====================

    public record PublishDocumentRequest(
        String workspaceId,
        String sourceDocId,
        String title,
        String description,
        String contentSnapshot,
        String coverImage,
        Integer categoryId,
        List<Integer> tagIds,
        Boolean isPaid,
        BigDecimal price
    ) {}

    public record UpdateDocumentRequest(
        String title,
        String description,
        String coverImage,
        Integer categoryId,
        List<Integer> tagIds,
        Boolean isPaid,
        BigDecimal price
    ) {}

    public record CollectDocumentRequest(
        Integer folderId,
        String notes
    ) {}

    public record AddCommentRequest(
        String content,
        Long parentId
    ) {}

    public record PurchaseDocumentRequest(
        String paymentMethod
    ) {}

    public record RecordViewRequest(
        String ipAddress,
        String userAgent,
        Integer viewDuration
    ) {}

    public record CreateCategoryRequest(
        String name,
        String slug,
        Integer parentId,
        String description,
        String icon
    ) {}

    public record CreateTagRequest(
        String name,
        String slug,
        String color
    ) {}
}
