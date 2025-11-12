package com.yunke.backend.forum.service;

import com.yunke.backend.forum.repository.ForumReplyRepository;
import com.yunke.backend.system.domain.entity.EntityLike;
import com.yunke.backend.system.domain.entity.EntityLike.EntityType;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.forum.domain.entity.ForumReply;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.notification.enums.NotificationLevel;
import com.yunke.backend.notification.enums.NotificationType;
import com.yunke.backend.system.repository.EntityLikeRepository;
import com.yunke.backend.forum.repository.ForumPostRepository;

import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final EntityLikeRepository entityLikeRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 通用：点赞
    @Transactional(rollbackFor = Exception.class)
    public boolean like(EntityType type, String entityId, String userId) {
        if (type == null) throw new IllegalArgumentException("entityType不能为空");
        if (entityId == null || entityId.isBlank()) throw new IllegalArgumentException("entityId不能为空");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId不能为空");

        boolean exists = entityLikeRepository.existsByUserIdAndEntityTypeAndEntityId(userId, type, entityId);
        if (exists) {
            return true; // 幂等
        }

        // 校验实体存在并更新计数
        switch (type) {
            case POST -> incrementPostLike(entityId);
            case REPLY -> incrementReplyLike(entityId);
            case DOCUMENT -> { /* 文档计数在社区模块中维护，这里仅记录关系 */ }
            default -> throw new IllegalArgumentException("不支持的实体类型");
        }

        EntityLike like = new EntityLike();
        like.setEntityType(type);
        like.setEntityId(entityId);
        like.setUserId(userId);
        entityLikeRepository.save(like);

        // 异步式（非阻塞主流程）通知：try-catch 包裹
        try {
            // 获取点赞用户名称
            String actorName = userRepository.findById(userId).map(User::getName).orElse("用户");

            if (type == EntityType.POST) {
                forumPostRepository.findById(entityId).ifPresent(post -> {
                    // 不能通知自己
                    if (post.getUserId() != null && post.getUserId().equals(userId)) return;

                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("postId", post.getId());
                    data.put("title", "帖子点赞");
                    data.put("content", actorName + " 赞了你的帖子");
                    notificationService.createNotification(
                            post.getUserId(),
                            NotificationType.FORUM_POST_LIKED,
                            NotificationLevel.DEFAULT,
                            data
                    );
                });
            } else if (type == EntityType.REPLY) {
                Long replyId = parseLongOrThrow(entityId);
                forumReplyRepository.findById(replyId).ifPresent(reply -> {
                    // 不能通知自己
                    if (reply.getUserId() != null && reply.getUserId().equals(userId)) return;

                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("postId", reply.getPostId());
                    data.put("replyId", reply.getId());
                    data.put("title", "回复点赞");
                    data.put("content", actorName + " 赞了你的回复");
                    notificationService.createNotification(
                            reply.getUserId(),
                            NotificationType.FORUM_REPLY_LIKED,
                            NotificationLevel.DEFAULT,
                            data
                    );
                });
            }
        } catch (Exception ignored) {
            // 通知失败不影响主流程
        }

        return true;
    }

    // 通用：取消点赞
    @Transactional(rollbackFor = Exception.class)
    public boolean unlike(EntityType type, String entityId, String userId) {
        if (type == null) throw new IllegalArgumentException("entityType不能为空");
        if (entityId == null || entityId.isBlank()) throw new IllegalArgumentException("entityId不能为空");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId不能为空");

        boolean exists = entityLikeRepository.existsByUserIdAndEntityTypeAndEntityId(userId, type, entityId);
        if (!exists) {
            return true; // 幂等
        }

        // 先删除关系，再更新计数
        entityLikeRepository.deleteByUserIdAndEntityTypeAndEntityId(userId, type, entityId);

        switch (type) {
            case POST -> decrementPostLike(entityId);
            case REPLY -> decrementReplyLike(entityId);
            case DOCUMENT -> { /* 文档计数在社区模块中维护，这里仅记录关系 */ }
            default -> throw new IllegalArgumentException("不支持的实体类型");
        }
        return true;
    }

    // 通用：是否已点赞
    @Transactional(readOnly = true)
    public boolean isLiked(EntityType type, String entityId, String userId) {
        if (type == null) return false;
        if (entityId == null || userId == null) return false;
        return entityLikeRepository.existsByUserIdAndEntityTypeAndEntityId(userId, type, entityId);
    }

    // 便捷方法：帖子点赞
    @Transactional(rollbackFor = Exception.class)
    public boolean likePost(String postId, String userId) {
        return like(EntityType.POST, postId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unlikePost(String postId, String userId) {
        return unlike(EntityType.POST, postId, userId);
    }

    // 便捷方法：回复点赞
    @Transactional(rollbackFor = Exception.class)
    public boolean likeReply(Long replyId, String userId) {
        if (replyId == null) throw new IllegalArgumentException("replyId不能为空");
        return like(EntityType.REPLY, String.valueOf(replyId), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unlikeReply(Long replyId, String userId) {
        if (replyId == null) throw new IllegalArgumentException("replyId不能为空");
        return unlike(EntityType.REPLY, String.valueOf(replyId), userId);
    }

    // 内部：计数维护
    private void incrementPostLike(String postId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        int current = post.getLikeCount() == null ? 0 : post.getLikeCount();
        post.setLikeCount(current + 1);
        forumPostRepository.save(post);
    }

    private void decrementPostLike(String postId) {
        forumPostRepository.findById(postId).ifPresent(post -> {
            int current = post.getLikeCount() == null ? 0 : post.getLikeCount();
            post.setLikeCount(Math.max(0, current - 1));
            forumPostRepository.save(post);
        });
    }

    private void incrementReplyLike(String replyIdStr) {
        Long replyId = parseLongOrThrow(replyIdStr);
        ForumReply reply = forumReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("回复不存在"));
        int current = reply.getLikeCount() == null ? 0 : reply.getLikeCount();
        reply.setLikeCount(current + 1);
        forumReplyRepository.save(reply);
    }

    private void decrementReplyLike(String replyIdStr) {
        Long replyId = parseLongOrThrow(replyIdStr);
        forumReplyRepository.findById(replyId).ifPresent(reply -> {
            int current = reply.getLikeCount() == null ? 0 : reply.getLikeCount();
            reply.setLikeCount(Math.max(0, current - 1));
            forumReplyRepository.save(reply);
        });
    }

    private Long parseLongOrThrow(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的ID: " + value);
        }
    }
}
