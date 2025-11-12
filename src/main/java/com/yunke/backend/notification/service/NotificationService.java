package com.yunke.backend.notification.service;

import com.yunke.backend.notification.domain.entity.Notification;
import com.yunke.backend.notification.enums.NotificationLevel;
import com.yunke.backend.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     * @param userId 用户ID
     * @param type 通知类型
     * @param level 通知级别
     * @param body 通知内容
     * @return 通知对象
     */
    Notification createNotification(String userId, NotificationType type, NotificationLevel level, Map<String, Object> body);

    /**
     * 根据用户ID获取通知列表（分页）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 通知列表
     */
    Page<Notification> getNotifications(String userId, Pageable pageable);

    /**
     * 根据用户ID获取未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long getUnreadCount(String userId);

    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAsRead(String notificationId, String userId);

    /**
     * 标记用户所有通知为已读
     * @param userId 用户ID
     * @return 标记的通知数量
     */
    int markAllAsRead(String userId);

    /**
     * 删除通知
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteNotification(String notificationId, String userId);

    /**
     * 获取通知详情
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 通知对象
     */
    Notification getNotificationById(String notificationId, String userId);

    /**
     * 创建提及通知
     * @param mentionedUserId 被提及用户ID
     * @param workspaceId 工作空间ID
     * @param docId 文档ID
     * @param docTitle 文档标题
     * @param mentionByUserId 提及者用户ID
     * @return 通知对象
     */
    Notification createMentionNotification(String mentionedUserId, String workspaceId, String docId, String docTitle, String mentionByUserId);

    /**
     * 创建邀请通知
     * @param invitedUserId 被邀请用户ID
     * @param workspaceId 工作空间ID
     * @param workspaceName 工作空间名称
     * @param inviterUserId 邀请者用户ID
     * @return 通知对象
     */
    Notification createInvitationNotification(String invitedUserId, String workspaceId, String workspaceName, String inviterUserId);

    // ==================== 社区通知方法 ====================

    /**
     * 创建点赞通知
     * @param documentId 文档ID
     * @param documentTitle 文档标题
     * @param authorId 作者ID
     * @param likerUserId 点赞用户ID
     * @param likerUserName 点赞用户名
     * @return 通知对象
     */
    Notification createLikeNotification(String documentId, String documentTitle, String authorId,
                                       String likerUserId, String likerUserName);

    /**
     * 创建评论通知
     * @param documentId 文档ID
     * @param documentTitle 文档标题
     * @param authorId 作者ID
     * @param commentId 评论ID
     * @param commentContent 评论内容
     * @param commenterUserId 评论用户ID
     * @param commenterUserName 评论用户名
     * @return 通知对象
     */
    Notification createCommentNotification(String documentId, String documentTitle, String authorId,
                                          Long commentId, String commentContent,
                                          String commenterUserId, String commenterUserName);

    /**
     * 创建收藏通知
     * @param documentId 文档ID
     * @param documentTitle 文档标题
     * @param authorId 作者ID
     * @param collectorUserId 收藏用户ID
     * @param collectorUserName 收藏用户名
     * @return 通知对象
     */
    Notification createCollectNotification(String documentId, String documentTitle, String authorId,
                                          String collectorUserId, String collectorUserName);

    /**
     * 创建关注通知
     * @param followingUserId 被关注用户ID
     * @param followerUserId 关注用户ID
     * @param followerUserName 关注用户名
     * @return 通知对象
     */
    Notification createFollowNotification(String followingUserId, String followerUserId, String followerUserName);
}