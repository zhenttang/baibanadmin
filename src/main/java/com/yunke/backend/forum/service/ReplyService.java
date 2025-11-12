package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.CreateReplyRequest;
import com.yunke.backend.forum.dto.ReplyDTO;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.forum.domain.entity.ForumReply;
import com.yunke.backend.forum.repository.ForumPostRepository;

import com.yunke.backend.forum.repository.ForumReplyRepository;
import lombok.RequiredArgsConstructor;
import com.yunke.backend.forum.dto.MentionDTO;
import com.yunke.backend.infrastructure.util.MentionParser;
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

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.security.AffineUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ForumReplyRepository forumReplyRepository;
    private final ForumPostRepository forumPostRepository;
    private final MentionParser mentionParser;
    private final NotificationService notificationService;
    
    // Controller-friendly API: create using current authenticated user
    @Transactional(rollbackFor = Exception.class)
    public ReplyDTO createReply(CreateReplyRequest request) {
        User current = getCurrentUser();
        if (current == null || current.getId() == null) {
            throw new IllegalStateException("未登录或无法获取用户信息");
        }
        // 使用包含用户名的方法，便于生成@通知内容
        return createReply(request, current.getId(), current.getName());
    }

    // Spec 1: createReply(CreateReplyRequest request, Long currentUserId)
    @Transactional(rollbackFor = Exception.class)
    public ReplyDTO createReply(CreateReplyRequest request, Long currentUserId) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getPostId() == null || request.getPostId().isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Integer maxFloor = forumReplyRepository.findMaxFloorByPostId(request.getPostId());
        int floor = (maxFloor == null) ? 1 : maxFloor + 1;

        ForumReply reply = new ForumReply();
        reply.setPostId(request.getPostId());
        reply.setUserId(String.valueOf(currentUserId));
        reply.setFloor(floor);
        reply.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        reply.setContent(request.getContent());

        ForumReply saved = forumReplyRepository.save(reply);

        ForumPost post = forumPostRepository.findById(request.getPostId()).orElseThrow();
        Integer rc = post.getReplyCount() == null ? 0 : post.getReplyCount();
        post.setReplyCount(rc + 1);
        post.setLastReplyAt(LocalDateTime.now());
        forumPostRepository.save(post);

        return toDTO(saved);
    }

    // 创建回复（从简单请求 + 调用方提供用户信息）
    @Transactional(rollbackFor = Exception.class)
    public ReplyDTO createReply(CreateReplyRequest request, String userId, String userName) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getPostId() == null || request.getPostId().isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }

        ForumPost post = forumPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        if (Boolean.TRUE.equals(post.getIsLocked())) {
            throw new IllegalStateException("帖子已锁定，不能回复");
        }

        // 自动分配楼层号
        Integer maxFloor = forumReplyRepository.findMaxFloorByPostId(request.getPostId());
        int nextFloor = (maxFloor == null ? 1 : maxFloor + 1);

        // 可选的父回复校验（若传入，需同一个post）
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        if (parentId != null && parentId > 0) {
            ForumReply parent = forumReplyRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父回复不存在"));
            if (!Objects.equals(parent.getPostId(), request.getPostId())) {
                throw new IllegalArgumentException("父回复不属于该帖子");
            }
        }

        ForumReply reply = new ForumReply();
        reply.setPostId(request.getPostId());
        reply.setUserId(userId);
        reply.setUserName(userName);
        reply.setFloor(nextFloor);
        reply.setParentId(parentId == null ? 0L : parentId);
        reply.setContent(request.getContent());
        reply.setContentType("markdown");
        reply.setLikeCount(0);
        reply.setIsAuthor(Objects.equals(post.getUserId(), userId));
        reply.setIsBestAnswer(false);
        reply.setStatus("normal");

        // 解析@提及，设置首个被@用户为 replyTo
        List<MentionDTO> mentions = mentionParser.parseMentions(request.getContent(), userId);
        if (mentions != null && !mentions.isEmpty()) {
            MentionDTO first = mentions.get(0);
            reply.setReplyToUserId(first.getUserId());
            reply.setReplyToUserName(first.getUsername());
        }

        ForumReply saved = forumReplyRepository.save(reply);

        // 更新帖子统计：replyCount + 1, lastReplyAt, lastReplyUser
        Integer count = post.getReplyCount() == null ? 0 : post.getReplyCount();
        post.setReplyCount(count + 1);
        post.setLastReplyAt(LocalDateTime.now());
        post.setLastReplyUserId(userId);
        post.setLastReplyUserName(userName);
        forumPostRepository.save(post);

        // 帖子作者被回复通知（排除自己回复自己的帖子）
        try {
            if (post.getUserId() != null && !post.getUserId().equals(userId)) {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("postId", request.getPostId());
                data.put("replyId", saved.getId());
                data.put("replyFloor", saved.getFloor());
                data.put("title", "帖子被回复");
                String msg = userName + " 回复了你的帖子「" + post.getTitle() + "」";
                data.put("content", msg);
                notificationService.createNotification(
                        post.getUserId(),
                        NotificationType.FORUM_POST_REPLIED,
                        NotificationLevel.DEFAULT,
                        data
                );
            }
        } catch (Exception ignored) {
            // 通知失败不影响主流程
        }

        // 为每个被@用户创建通知（不能@自己，MentionParser已过滤）
        if (mentions != null && !mentions.isEmpty()) {
            for (MentionDTO m : mentions) {
                if (m.getUserId() == null) continue;
                try {
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("postId", request.getPostId());
                    data.put("replyId", saved.getId());
                    data.put("replyFloor", saved.getFloor());
                    data.put("title", "论坛@提及");
                    String contentMsg = userName + " 在帖子「" + post.getTitle() + "」中@了你";
                    data.put("content", contentMsg);
                    notificationService.createNotification(
                            m.getUserId(),
                            NotificationType.FORUM_MENTION,
                            NotificationLevel.DEFAULT,
                            data
                    );
                } catch (Exception ignored) {
                    // 若单个通知创建失败，不影响回复创建流程
                }
            }
        }

        return toDTO(saved);
    }

    // 分页获取帖子回复（现有版本，返回Page）
    @Transactional(readOnly = true)
    public Page<ReplyDTO> getPostRepliesPage(String postId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return getPostRepliesPage(postId, pageable);
    }

    // 分页获取帖子回复（Pageable版本）
    @Transactional(readOnly = true)
    public Page<ReplyDTO> getPostRepliesPage(String postId, Pageable pageable) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        if (pageable == null) {
            pageable = PageRequest.of(0, 20);
        }

        Page<ForumReply> page = forumReplyRepository
                .findByPostIdAndStatusOrderByFloorAsc(postId, "normal", pageable);

        List<ReplyDTO> list = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    // Spec 2: getPostReplies(String postId, int page, int size) returning List<ReplyDTO>
    @Transactional(readOnly = true)
    public List<ReplyDTO> getPostReplies(String postId, int page, int size) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        Pageable pageable = PageRequest.of(page, size);
        Page<ForumReply> p = forumReplyRepository.findByPostIdOrderByFloorAsc(postId, pageable);
        return p.getContent().stream().map(this::toDTO).collect(Collectors.toList());
    }

    // 真删除
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReply(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("回复ID不能为空");
        }
        ForumReply reply = forumReplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("回复不存在"));

        // 先拿到postId，执行删除
        String postId = reply.getPostId();
        forumReplyRepository.delete(reply);

        // 维护帖子回复数（简单减量，最小0）
        if (postId != null) {
            forumPostRepository.findById(postId).ifPresent(post -> {
                Integer cnt = post.getReplyCount() == null ? 0 : post.getReplyCount();
                post.setReplyCount(Math.max(0, cnt - 1));
                forumPostRepository.save(post);
            });
        }
        return true;
    }

    // 标记最佳答案，保证同一帖子只有一个最佳（现有版本，接收postId与replyId）
    @Transactional(rollbackFor = Exception.class)
    public boolean markBestAnswer(String postId, Long replyId) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        if (replyId == null) {
            throw new IllegalArgumentException("replyId不能为空");
        }

        ForumReply target = forumReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("回复不存在"));
        if (!postId.equals(target.getPostId())) {
            throw new IllegalArgumentException("回复不属于该帖子");
        }

        // 取消该帖其他最佳答案
        Page<ForumReply> all = forumReplyRepository
                .findByPostIdAndStatusOrderByFloorAsc(postId, "normal", Pageable.unpaged());
        boolean needSave = false;
        for (ForumReply r : all.getContent()) {
            if (Boolean.TRUE.equals(r.getIsBestAnswer()) && !Objects.equals(r.getId(), replyId)) {
                r.setIsBestAnswer(false);
                needSave = true;
            }
        }
        if (needSave) {
            forumReplyRepository.saveAll(all.getContent());
        }

        // 标记目标为最佳
        if (!Boolean.TRUE.equals(target.getIsBestAnswer())) {
            target.setIsBestAnswer(true);
            forumReplyRepository.save(target);
        }
        return true;
    }

    // Spec 4: 标记最佳答案（根据replyId）并返回ReplyDTO
    @Transactional(rollbackFor = Exception.class)
    public ReplyDTO markBestAnswer(Long replyId) {
        if (replyId == null) {
            throw new IllegalArgumentException("replyId不能为空");
        }
        ForumReply reply = forumReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("回复不存在"));

        forumReplyRepository.findByPostIdAndIsBestAnswerTrue(reply.getPostId())
                .ifPresent(old -> {
                    old.setIsBestAnswer(false);
                    forumReplyRepository.save(old);
                });

        reply.setIsBestAnswer(true);
        ForumReply saved = forumReplyRepository.save(reply);
        return toDTO(saved);
    }

    // Helper for controller compatibility: boolean variant
    @Transactional(rollbackFor = Exception.class)
    public boolean markBestAnswerBool(Long replyId) {
        markBestAnswer(replyId);
        return true;
    }

    // 实体转DTO
    private ReplyDTO toDTO(ForumReply entity) {
        if (entity == null) {
            return null;
        }
        ReplyDTO dto = new ReplyDTO();
        dto.setId(entity.getId());
        dto.setPostId(entity.getPostId());
        // ForumReply.userId 是字符串，这里尝试转换成Long，失败则置空
        try {
            if (entity.getUserId() != null) {
                dto.setUserId(Long.parseLong(entity.getUserId()));
            }
        } catch (NumberFormatException ignored) {
            dto.setUserId(null);
        }
        dto.setUsername(entity.getUserName());
        dto.setFloor(entity.getFloor());
        dto.setParentId(entity.getParentId());
        dto.setContent(entity.getContent());
        dto.setLikeCount(entity.getLikeCount());
        dto.setIsBestAnswer(entity.getIsBestAnswer());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
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

    private Long nullSafeParseLong(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
