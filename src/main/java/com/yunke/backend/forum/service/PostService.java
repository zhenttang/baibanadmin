package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.CreatePostRequest;
import com.yunke.backend.forum.dto.PostDTO;
import com.yunke.backend.forum.dto.UpdatePostRequest;
import com.yunke.backend.forum.domain.entity.Forum;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.forum.repository.ForumPostRepository;
import com.yunke.backend.forum.repository.ForumRepository;
import com.yunke.backend.security.AffineUserDetails;
import lombok.RequiredArgsConstructor;
import com.yunke.backend.notification.enums.NotificationLevel;
import com.yunke.backend.notification.enums.NotificationType;
import com.yunke.backend.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final ForumPostRepository forumPostRepository;
    private final ForumRepository forumRepository;
    private final PostTagService postTagService;
    private final EditHistoryService editHistoryService;
    private final NotificationService notificationService;

    // 1. 创建帖子，生成UUID作为id
    @Transactional(rollbackFor = Exception.class)
    public PostDTO createPost(CreatePostRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getForumId() == null) {
            throw new IllegalArgumentException("forumId不能为空");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("内容不能为空");
        }

        Forum forum = forumRepository.findById(request.getForumId())
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));

        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException("未登录或无法获取用户信息");
        }

        ForumPost post = new ForumPost();
        post.setId(UUID.randomUUID().toString()); // UUID作为ID
        post.setForumId(forum.getId());
        post.setForumName(forum.getName());
        post.setUserId(currentUser.getId());
        post.setUserName(currentUser.getName());
        post.setUserAvatar(currentUser.getAvatarUrl());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setContentType(request.getContentType() == null ? "markdown" : request.getContentType());
        post.setStatus(ForumPost.PostStatus.NORMAL);
        post.setIsSticky(Boolean.FALSE);
        post.setIsEssence(Boolean.FALSE);
        post.setIsLocked(Boolean.FALSE);
        post.setIsHot(Boolean.FALSE);
        post.setViewCount(0);
        post.setReplyCount(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setLastReplyAt(null);

        // 初始化热度分数
        post.setHotScore(calculateHotScore(post));

        ForumPost saved = forumPostRepository.save(post);

        // 更新板块帖子数（简单增量）
        Integer current = forum.getPostCount() == null ? 0 : forum.getPostCount();
        forum.setPostCount(current + 1);
        forumRepository.save(forum);

        // 解析并保存标签（可选）
        try {
            postTagService.addTagsToPost(saved.getId(), request.getTags());
        } catch (Exception e) {
            // 标签失败不影响发帖主流程
        }

        return toDTO(saved);
    }

    // 2. 获取帖子详情，增加浏览次数
    @Transactional(rollbackFor = Exception.class)
    public PostDTO getPost(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        if (post.getStatus() == ForumPost.PostStatus.DELETED || post.getStatus() == ForumPost.PostStatus.HIDDEN) {
            throw new IllegalArgumentException("帖子不存在或已删除");
        }

        int views = post.getViewCount() == null ? 0 : post.getViewCount();
        post.setViewCount(views + 1);
        // 浏览数变化后，重算热度
        post.setHotScore(calculateHotScore(post));
        ForumPost updated = forumPostRepository.save(post);
        return toDTO(updated);
    }

    // 3. 更新帖子
    @Transactional(rollbackFor = Exception.class)
    public PostDTO updatePost(String id, UpdatePostRequest request) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        if (post.getStatus() == ForumPost.PostStatus.DELETED) {
            throw new IllegalStateException("已删除的帖子不可编辑");
        }
        if (Boolean.TRUE.equals(post.getIsLocked())) {
            throw new IllegalStateException("帖子已锁定，不可编辑");
        }

        String originalTitle = post.getTitle();
        String originalContent = post.getContent();

        String newTitle = originalTitle;
        String newContent = originalContent;

        if (request != null) {
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                newTitle = request.getTitle();
            }
            if (request.getContent() != null && !request.getContent().isBlank()) {
                newContent = request.getContent();
            }
        }

        boolean changed = !(java.util.Objects.equals(originalTitle, newTitle)
                && java.util.Objects.equals(originalContent, newContent));

        if (changed) {
            User currentUser = getCurrentUser();
            String editorId = currentUser == null ? null : currentUser.getId();
            String editorName = currentUser == null ? null : currentUser.getName();
            try {
                editHistoryService.recordEdit(post.getId(), originalTitle, originalContent, editorId, editorName);
            } catch (Exception ignored) {
                // 历史记录失败不影响编辑主流程
            }
        }

        post.setTitle(newTitle);
        post.setContent(newContent);

        // 内容变化不影响热度公式中的变量（非必需重算），这里保持不变
        ForumPost saved = forumPostRepository.save(post);
        return toDTO(saved);
    }

    // 4. 软删除（设置status为DELETED）
    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePost(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        if (post.getStatus() == ForumPost.PostStatus.DELETED) {
            return true;
        }

        post.setStatus(ForumPost.PostStatus.DELETED);
        post.setDeletedAt(LocalDateTime.now());
        forumPostRepository.save(post);

        // 维护板块帖子数量（简单减量）
        if (post.getForumId() != null) {
            forumRepository.findById(post.getForumId()).ifPresent(forum -> {
                Integer pc = forum.getPostCount() == null ? 0 : forum.getPostCount();
                forum.setPostCount(Math.max(0, pc - 1));
                forumRepository.save(forum);
            });
        }

        return true;
    }

    // 5. 置顶操作（设置/切换isSticky）
    @Transactional(rollbackFor = Exception.class)
    public Boolean setSticky(String id, boolean sticky) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        post.setIsSticky(sticky);
        forumPostRepository.save(post);

        // 版主操作通知（置顶）
        try {
            User actor = getCurrentUser();
            if (post.getUserId() != null && actor != null && post.getUserId().equals(actor.getId())) {
                // 不通知自己
            } else {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("postId", post.getId());
                data.put("action", "sticky");
                data.put("title", "帖子版主操作");
                data.put("content", "版主置顶了你的帖子「" + post.getTitle() + "」");
                notificationService.createNotification(
                        post.getUserId(),
                        NotificationType.FORUM_POST_MODERATED,
                        NotificationLevel.DEFAULT,
                        data
                );
            }
        } catch (Exception ignored) {
            // 通知失败不影响主流程
        }
        return true;
    }

    // 6. 加精操作（设置/切换isEssence）
    @Transactional(rollbackFor = Exception.class)
    public Boolean setEssence(String id, boolean essence) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        post.setIsEssence(essence);
        forumPostRepository.save(post);

        // 版主操作通知（加精）
        try {
            User actor = getCurrentUser();
            if (post.getUserId() != null && actor != null && post.getUserId().equals(actor.getId())) {
                // 不通知自己
            } else {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("postId", post.getId());
                data.put("action", "essence");
                data.put("title", "帖子版主操作");
                data.put("content", "版主加精了你的帖子「" + post.getTitle() + "」");
                notificationService.createNotification(
                        post.getUserId(),
                        NotificationType.FORUM_POST_MODERATED,
                        NotificationLevel.DEFAULT,
                        data
                );
            }
        } catch (Exception ignored) {
            // 通知失败不影响主流程
        }
        return true;
    }

    // 7. 锁定操作（设置/切换isLocked）
    @Transactional(rollbackFor = Exception.class)
    public Boolean lockPost(String id, boolean locked) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        post.setIsLocked(locked);
        forumPostRepository.save(post);

        // 版主操作通知（锁定）
        try {
            User actor = getCurrentUser();
            if (post.getUserId() != null && actor != null && post.getUserId().equals(actor.getId())) {
                // 不通知自己
            } else {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("postId", post.getId());
                data.put("action", "lock");
                data.put("title", "帖子版主操作");
                data.put("content", "版主锁定了你的帖子「" + post.getTitle() + "」");
                notificationService.createNotification(
                        post.getUserId(),
                        NotificationType.FORUM_POST_MODERATED,
                        NotificationLevel.DEFAULT,
                        data
                );
            }
        } catch (Exception ignored) {
            // 通知失败不影响主流程
        }
        return true;
    }

    // 8. 分页获取板块帖子列表（含简单筛选）
    @Transactional(readOnly = true)
    public Page<PostDTO> listPosts(int page, int size, Long forumId, String userId,
                                   String keyword, Boolean essence, Boolean sticky, String sort) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<ForumPost> p;
        if (keyword != null && !keyword.isBlank()) {
            p = forumPostRepository.searchPosts(keyword.trim(), ForumPost.PostStatus.NORMAL, pageable);
        } else if (forumId != null) {
            p = forumPostRepository.findByForumIdAndStatusOrderByIsStickyDescLastReplyAtDesc(
                    forumId, ForumPost.PostStatus.NORMAL, pageable);
        } else if (userId != null && !userId.isBlank()) {
            p = forumPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (Boolean.TRUE.equals(essence)) {
            p = forumPostRepository.findByIsEssenceTrueAndStatusOrderByCreatedAtDesc(
                    ForumPost.PostStatus.NORMAL, pageable);
        } else {
            // 默认按热度排序
            p = forumPostRepository.findHotPosts(ForumPost.PostStatus.NORMAL, pageable);
        }

        List<ForumPost> list = p.getContent();
        if (Boolean.TRUE.equals(sticky)) {
            list = list.stream().filter(po -> Boolean.TRUE.equals(po.getIsSticky())).collect(Collectors.toList());
        }

        List<PostDTO> dtos = list.stream().map(this::toDTO).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, p.getTotalElements());
    }

    // 别名：仅按forumId分页
    @Transactional(readOnly = true)
    public Page<PostDTO> getForumPosts(Long forumId, int page, int size) {
        return listPosts(page, size, forumId, null, null, null, null, null);
    }

    // 9. 计算热度分数
    public BigDecimal calculateHotScore(ForumPost post) {
        if (post == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int replyCount = post.getReplyCount() == null ? 0 : post.getReplyCount();
        int likeCount = post.getLikeCount() == null ? 0 : post.getLikeCount();
        int viewCount = post.getViewCount() == null ? 0 : post.getViewCount();

        // 时间衰减因子: 1 / (1 + daysSinceCreated)
        LocalDateTime createdAt = post.getCreatedAt();
        long days = 0;
        if (createdAt != null) {
            days = Math.max(0, ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        }
        double timeDecay = 1.0d / (1.0d + (double) days);

        double base = replyCount * 5.0d + likeCount * 3.0d + viewCount * 0.1d;
        double score = base * timeDecay;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    // 10. 实体转DTO
    public PostDTO toDTO(ForumPost post) {
        if (post == null) {
            return null;
        }
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setForumId(post.getForumId());
        dto.setForumName(post.getForumName());
        dto.setUserId(post.getUserId());
        dto.setUserName(post.getUserName());
        dto.setUserAvatar(post.getUserAvatar());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setContentType(post.getContentType());
        dto.setStatus(post.getStatus() == null ? null : post.getStatus().name());
        dto.setIsSticky(post.getIsSticky());
        dto.setIsEssence(post.getIsEssence());
        dto.setIsLocked(post.getIsLocked());
        dto.setIsHot(post.getIsHot());
        dto.setViewCount(post.getViewCount());
        dto.setReplyCount(post.getReplyCount());
        dto.setLikeCount(post.getLikeCount());
        dto.setCollectCount(post.getCollectCount());
        dto.setHotScore(post.getHotScore());
        dto.setQualityScore(post.getQualityScore());
        dto.setLastReplyAt(post.getLastReplyAt());
        dto.setLastReplyUserId(post.getLastReplyUserId());
        dto.setLastReplyUserName(post.getLastReplyUserName());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setIsLiked(post.getIsLiked());
        dto.setIsCollected(post.getIsCollected());
        return dto;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof AffineUserDetails aud) {
            return aud.getUser();
        }
        return null;
    }
}
