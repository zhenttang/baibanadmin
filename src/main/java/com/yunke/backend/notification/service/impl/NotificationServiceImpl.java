package com.yunke.backend.notification.service.impl;

import com.yunke.backend.notification.domain.entity.Notification;
import com.yunke.backend.notification.enums.NotificationLevel;
import com.yunke.backend.notification.enums.NotificationType;
import com.yunke.backend.notification.repository.NotificationRepository;
import com.yunke.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification createNotification(String userId, NotificationType type, NotificationLevel level, Map<String, Object> data) {
        try {
            // 从data中提取title和content，如果没有则使用默认值
            String title = (String) data.getOrDefault("title", getDefaultTitle(type));
            String content = (String) data.getOrDefault("content", getDefaultContent(type));
            
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .level(level)
                    .title(title)
                    .content(content)
                    .data(data)
                    .read(false)
                    .build();
            
            Notification savedNotification = notificationRepository.save(notification);
            log.info("Created notification {} for user {}", savedNotification.getId(), userId);
            
            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating notification for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    @Override
    public Page<Notification> getNotifications(String userId, Pageable pageable) {
        try {
            return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } catch (Exception e) {
            log.error("Error getting notifications for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get notifications", e);
        }
    }

    @Override
    public long getUnreadCount(String userId) {
        try {
            return notificationRepository.countUnreadByUserId(userId);
        } catch (Exception e) {
            log.error("Error getting unread count for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public boolean markAsRead(String notificationId, String userId) {
        try {
            int updatedRows = notificationRepository.markAsRead(notificationId, userId);
            if (updatedRows > 0) {
                log.info("Marked notification {} as read for user {}", notificationId, userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error marking notification {} as read for user {}: {}", notificationId, userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(String userId) {
        try {
            int updatedRows = notificationRepository.markAllAsRead(userId);
            log.info("Marked {} notifications as read for user {}", updatedRows, userId);
            return updatedRows;
        } catch (Exception e) {
            log.error("Error marking all notifications as read for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public boolean deleteNotification(String notificationId, String userId) {
        try {
            int deletedRows = notificationRepository.deleteByIdAndUserId(notificationId, userId);
            if (deletedRows > 0) {
                log.info("Deleted notification {} for user {}", notificationId, userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting notification {} for user {}: {}", notificationId, userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Notification getNotificationById(String notificationId, String userId) {
        try {
            return notificationRepository.findByIdAndUserId(notificationId, userId);
        } catch (Exception e) {
            log.error("Error getting notification {} for user {}: {}", notificationId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get notification", e);
        }
    }

    @Override
    @Transactional
    public Notification createMentionNotification(String mentionedUserId, String workspaceId, String docId, String docTitle, String mentionByUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("workspaceId", workspaceId);
        data.put("docId", docId);
        data.put("docTitle", docTitle);
        data.put("mentionByUserId", mentionByUserId);
        data.put("title", "有人在文档中提及了您");
        data.put("content", "在文档 '" + docTitle + "' 中被提及");
        
        return createNotification(mentionedUserId, NotificationType.MENTION, NotificationLevel.DEFAULT, data);
    }

    @Override
    @Transactional
    public Notification createInvitationNotification(String invitedUserId, String workspaceId, String workspaceName, String inviterUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("workspaceId", workspaceId);
        data.put("workspaceName", workspaceName);
        data.put("inviterUserId", inviterUserId);
        data.put("title", "工作空间邀请");
        data.put("content", "您被邀请加入工作空间 '" + workspaceName + "'");
        
        return createNotification(invitedUserId, NotificationType.INVITATION, NotificationLevel.DEFAULT, data);
    }

    /**
     * 根据通知类型获取默认标题
     */
    private String getDefaultTitle(NotificationType type) {
        return switch (type) {
            case MENTION -> "提及通知";
            case INVITATION -> "邀请通知";
            case INVITATION_ACCEPTED -> "邀请已接受";
            case INVITATION_BLOCKED -> "邀请被阻止";
            case INVITATION_REJECTED -> "邀请被拒绝";
            case INVITATION_REVIEW_REQUEST -> "邀请审核请求";
            case INVITATION_REVIEW_APPROVED -> "邀请审核通过";
            case INVITATION_REVIEW_DECLINED -> "邀请审核拒绝";
            case FORUM_MENTION -> "论坛@提及";
            case FORUM_POST_REPLIED -> "帖子被回复";
            case FORUM_REPLY_LIKED -> "回复点赞";
            case FORUM_POST_LIKED -> "帖子点赞";
            case FORUM_POST_MODERATED -> "帖子版主操作";
        };
    }

    /**
     * 根据通知类型获取默认内容
     */
    private String getDefaultContent(NotificationType type) {
        return switch (type) {
            case MENTION -> "您在文档中被提及";
            case INVITATION -> "您收到新的邀请";
            case INVITATION_ACCEPTED -> "您的邀请已被接受";
            case INVITATION_BLOCKED -> "您的邀请被阻止";
            case INVITATION_REJECTED -> "您的邀请被拒绝";
            case INVITATION_REVIEW_REQUEST -> "有新的邀请需要审核";
            case INVITATION_REVIEW_APPROVED -> "邀请审核已通过";
            case INVITATION_REVIEW_DECLINED -> "邀请审核已拒绝";
            case FORUM_MENTION -> "您在帖子中被@提及";
            case FORUM_POST_REPLIED -> "您的帖子有了新回复";
            case FORUM_REPLY_LIKED -> "您的回复获得了新的点赞";
            case FORUM_POST_LIKED -> "您的帖子获得了新的点赞";
            case FORUM_POST_MODERATED -> "版主对您的帖子进行了操作";
        };
    }

    // ==================== 社区通知实现 ====================

    @Override
    @Transactional
    public Notification createLikeNotification(String documentId, String documentTitle, String authorId,
                                              String likerUserId, String likerUserName) {
        // 不给自己发通知
        if (authorId.equals(likerUserId)) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("documentId", documentId);
        data.put("documentTitle", documentTitle);
        data.put("likerUserId", likerUserId);
        data.put("likerUserName", likerUserName);
        data.put("title", "您的文档获得了新的点赞");
        data.put("content", likerUserName + " 点赞了您的文档《" + documentTitle + "》");

        try {
            return createNotification(authorId, NotificationType.MENTION, NotificationLevel.DEFAULT, data);
        } catch (Exception e) {
            log.error("Failed to create like notification", e);
            return null;
        }
    }

    @Override
    @Transactional
    public Notification createCommentNotification(String documentId, String documentTitle, String authorId,
                                                 Long commentId, String commentContent,
                                                 String commenterUserId, String commenterUserName) {
        if (authorId.equals(commenterUserId)) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("documentId", documentId);
        data.put("documentTitle", documentTitle);
        data.put("commentId", commentId);
        data.put("commenterUserId", commenterUserId);
        data.put("commenterUserName", commenterUserName);
        data.put("title", "您的文档有新评论");
        String preview = commentContent.length() > 50
            ? commentContent.substring(0, 50) + "..."
            : commentContent;
        data.put("content", commenterUserName + " 评论了您的文档《" + documentTitle + "》: " + preview);

        try {
            return createNotification(authorId, NotificationType.MENTION, NotificationLevel.DEFAULT, data);
        } catch (Exception e) {
            log.error("Failed to create comment notification", e);
            return null;
        }
    }

    @Override
    @Transactional
    public Notification createCollectNotification(String documentId, String documentTitle, String authorId,
                                                 String collectorUserId, String collectorUserName) {
        if (authorId.equals(collectorUserId)) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("documentId", documentId);
        data.put("documentTitle", documentTitle);
        data.put("collectorUserId", collectorUserId);
        data.put("collectorUserName", collectorUserName);
        data.put("title", "您的文档被收藏了");
        data.put("content", collectorUserName + " 收藏了您的文档《" + documentTitle + "》");

        try {
            return createNotification(authorId, NotificationType.MENTION, NotificationLevel.DEFAULT, data);
        } catch (Exception e) {
            log.error("Failed to create collect notification", e);
            return null;
        }
    }

    @Override
    @Transactional
    public Notification createFollowNotification(String followingUserId, String followerUserId, String followerUserName) {
        Map<String, Object> data = new HashMap<>();
        data.put("followerUserId", followerUserId);
        data.put("followerUserName", followerUserName);
        data.put("title", "您有新的关注者");
        data.put("content", followerUserName + " 关注了您");

        try {
            return createNotification(followingUserId, NotificationType.MENTION, NotificationLevel.DEFAULT, data);
        } catch (Exception e) {
            log.error("Failed to create follow notification", e);
            return null;
        }
    }
}
